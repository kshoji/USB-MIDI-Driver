package jp.kshoji.driver.midi.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.fragment.AbstractMidiFragment;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.thread.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;
import jp.kshoji.driver.usb.util.DeviceFilter;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;

/**
 * base Activity for using {@link AbstractMidiFragment}s.
 * In this implement, each devices will be detected on connect.
 * launchMode must be "singleTask" or "singleInstance".
 * 
 * @author K.Shoji
 */
public class MidiFragmentHostActivity extends Activity implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {
	/**
	 * Implementation for multiple device connections.
	 * 
	 * @author K.Shoji
	 */
	final class OnMidiDeviceAttachedListenerImpl implements OnMidiDeviceAttachedListener {
		private final UsbManager usbManager;

		/**
		 * constructor
		 * 
		 * @param usbManager
		 */
		public OnMidiDeviceAttachedListenerImpl(UsbManager usbManager) {
			this.usbManager = usbManager;
		}

		/*
		 * (non-Javadoc)
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener#onDeviceAttached(android.hardware.usb.UsbDevice)
		 */
		@Override
		public synchronized void onDeviceAttached(UsbDevice attachedDevice) {
			// these fields are null; when this event fired while Activity destroying.
			if (midiInputDevices == null || midiOutputDevices == null || deviceConnections == null) {
				// nothing to do
				return;
			}

			deviceConnectionWatcher.notifyDeviceGranted();

			UsbDeviceConnection deviceConnection = usbManager.openDevice(attachedDevice);
			if (deviceConnection == null) {
				return;
			}

			deviceConnections.put(attachedDevice, deviceConnection);

			List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(getApplicationContext());

			Set<MidiInputDevice> foundInputDevices = UsbMidiDeviceUtils.findMidiInputDevices(attachedDevice, deviceConnection, deviceFilters, MidiFragmentHostActivity.this);
			for (MidiInputDevice midiInputDevice : foundInputDevices) {
				try {
					Set<MidiInputDevice> inputDevices = midiInputDevices.get(attachedDevice);
					if (inputDevices == null) {
						inputDevices = new HashSet<MidiInputDevice>();
					}
					inputDevices.add(midiInputDevice);
					midiInputDevices.put(attachedDevice, inputDevices);
				} catch (IllegalArgumentException iae) {
					Log.d(Constants.TAG, "This device didn't have any input endpoints.", iae);
				}
			}

			Set<MidiOutputDevice> foundOutputDevices = UsbMidiDeviceUtils.findMidiOutputDevices(attachedDevice, deviceConnection, deviceFilters);
			for (MidiOutputDevice midiOutputDevice : foundOutputDevices) {
				try {
					Set<MidiOutputDevice> outputDevices = midiOutputDevices.get(attachedDevice);
					if (outputDevices == null) {
						outputDevices = new HashSet<MidiOutputDevice>();
					}
					outputDevices.add(midiOutputDevice);
					midiOutputDevices.put(attachedDevice, outputDevices);
				} catch (IllegalArgumentException iae) {
					Log.d(Constants.TAG, "This device didn't have any output endpoints.", iae);
				}
			}

			Log.d(Constants.TAG, "Device " + attachedDevice.getDeviceName() + " has been attached.");

			MidiFragmentHostActivity.this.onDeviceAttached(attachedDevice);
		}
	}

	/**
	 * Implementation for multiple device connections.
	 * 
	 * @author K.Shoji
	 */
	final class OnMidiDeviceDetachedListenerImpl implements OnMidiDeviceDetachedListener {
		/*
		 * (non-Javadoc)
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
		 */
		@Override
		public synchronized void onDeviceDetached(UsbDevice detachedDevice) {
			// these fields are null; when this event fired while Activity destroying.
			if (midiInputDevices == null || midiOutputDevices == null || deviceConnections == null) {
				// nothing to do
				return;
			}

			AsyncTask<UsbDevice, Void, Void> task = new AsyncTask<UsbDevice, Void, Void>() {

				@Override
				protected Void doInBackground(UsbDevice... params) {
					if (params == null || params.length < 1) {
						return null;
					}
					
					UsbDevice usbDevice = params[0];

					// Stop input device's thread.
					Set<MidiInputDevice> inputDevices = midiInputDevices.get(usbDevice);
					if (inputDevices != null && inputDevices.size() > 0) {
						for (MidiInputDevice inputDevice : inputDevices) {
							if (inputDevice != null) {
								inputDevice.stop();
							}
						}
						midiInputDevices.remove(usbDevice);
					}

					Set<MidiOutputDevice> outputDevices = midiOutputDevices.get(usbDevice);
					if (outputDevices != null) {
						for (MidiOutputDevice outputDevice : outputDevices) {
							if (outputDevice != null) {
								outputDevice.stop();
							}
						}
						midiOutputDevices.remove(usbDevice);
					}

					UsbDeviceConnection deviceConnection = deviceConnections.get(usbDevice);
					if (deviceConnection != null) {
						deviceConnection.close();

						deviceConnections.remove(usbDevice);
					}

					Log.d(Constants.TAG, "Device " + usbDevice.getDeviceName() + " has been detached.");

					Message message = Message.obtain(deviceDetachedHandler);
					message.obj = usbDevice;
					deviceDetachedHandler.sendMessage(message);

					return null;
				}

			};
			task.execute(detachedDevice);
		}
	}

	Map<UsbDevice, UsbDeviceConnection> deviceConnections = null;
	Map<UsbDevice, Set<MidiInputDevice>> midiInputDevices = null;
	Map<UsbDevice, Set<MidiOutputDevice>> midiOutputDevices = null;
	OnMidiDeviceAttachedListener deviceAttachedListener = null;
	OnMidiDeviceDetachedListener deviceDetachedListener = null;
	Handler deviceDetachedHandler = null;
	MidiDeviceConnectionWatcher deviceConnectionWatcher = null;

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		deviceConnections = new HashMap<UsbDevice, UsbDeviceConnection>();
		midiInputDevices = new HashMap<UsbDevice, Set<MidiInputDevice>>();
		midiOutputDevices = new HashMap<UsbDevice, Set<MidiOutputDevice>>();

		UsbManager usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
		deviceAttachedListener = new OnMidiDeviceAttachedListenerImpl(usbManager);
		deviceDetachedListener = new OnMidiDeviceDetachedListenerImpl();

		deviceDetachedHandler = new Handler(new Callback() {
			/*
			 * (non-Javadoc)
			 * @see android.os.Handler.Callback#handleMessage(android.os.Message)
			 */
			@Override
			public boolean handleMessage(Message msg) {
				Log.d(Constants.TAG, "(handleMessage) detached device:" + msg.obj);
				UsbDevice usbDevice = (UsbDevice) msg.obj;
				onDeviceDetached(usbDevice);
				return true;
			}
		});

		deviceConnectionWatcher = new MidiDeviceConnectionWatcher(getApplicationContext(), usbManager, deviceAttachedListener, deviceDetachedListener);
	}

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		deviceConnectionWatcher.stop();
		deviceConnectionWatcher = null;

		if (midiInputDevices != null) {
			for (Set<MidiInputDevice> inputDevices : midiInputDevices.values()) {
				if (inputDevices != null) {
					for (MidiInputDevice inputDevice : inputDevices) {
						if (inputDevice != null) {
							inputDevice.stop();
						}
					}
				}
			}

			midiInputDevices.clear();
		}
		midiInputDevices = null;

		if (midiOutputDevices != null) {
			midiOutputDevices.clear();
		}
		midiOutputDevices = null;

		deviceConnections = null;
	}

	/**
	 * Get connected USB MIDI devices.
	 * 
	 * @return connected UsbDevice set
	 */
	public final Set<UsbDevice> getConnectedUsbDevices() {
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.checkConnectedDevicesImmediately();
		}
		if (deviceConnections != null) {
			return Collections.unmodifiableSet(deviceConnections.keySet());
		}

		return Collections.unmodifiableSet(new HashSet<UsbDevice>());
	}

	/**
	 * Get MIDI output device, if available.
	 * 
	 * @param usbDevice
	 * @return {@link Set<MidiOutputDevice>}
	 */
	public final Set<MidiOutputDevice> getMidiOutputDevices(UsbDevice usbDevice) {
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.checkConnectedDevicesImmediately();
		}
		if (midiOutputDevices != null && midiOutputDevices.get(usbDevice) != null) {
			return Collections.unmodifiableSet(midiOutputDevices.get(usbDevice));
		}

		return Collections.unmodifiableSet(new HashSet<MidiOutputDevice>());
	}

	List<WeakReference<Fragment>> attachedFragments = new ArrayList<WeakReference<Fragment>>();
	
	@Override
	public void onAttachFragment(Fragment fragment) {
		super.onAttachFragment(fragment);
		attachedFragments.add(new WeakReference<Fragment>(fragment));
	}
	
	/**
	 * Get attached {@link AbstractMidiFragment}s. Invisible Fragments are also included.
	 * 
	 * @return {@link AbstractMidiFragment}s attached with this Activity
	 */
	private final List<AbstractMidiFragment> getMidiFragments() {
	    ArrayList<AbstractMidiFragment> midiFragments = new ArrayList<AbstractMidiFragment>();

	    for(WeakReference<Fragment> reference : attachedFragments) {
	        Fragment fragment = reference.get();
	        if(fragment != null && fragment instanceof AbstractMidiFragment) {
	        	midiFragments.add((AbstractMidiFragment) fragment);
	        }
	    }
	    
	    return midiFragments;
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiMiscellaneousFunctionCodes(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int)
	 */
	@Override
	public void onMidiMiscellaneousFunctionCodes(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiMiscellaneousFunctionCodes(sender, cable, byte1, byte2, byte3);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiCableEvents(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int)
	 */
	@Override
	public void onMidiCableEvents(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiCableEvents(sender, cable, byte1, byte2, byte3);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiSystemCommonMessage(jp.kshoji.driver.midi.device.MidiInputDevice, int, byte[])
	 */
	@Override
	public void onMidiSystemCommonMessage(MidiInputDevice sender, int cable, byte[] bytes) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiSystemCommonMessage(sender, cable, bytes);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiSystemExclusive(jp.kshoji.driver.midi.device.MidiInputDevice, int, byte[])
	 */
	@Override
	public void onMidiSystemExclusive(MidiInputDevice sender, int cable, byte[] systemExclusive) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiSystemExclusive(sender, cable, systemExclusive);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiNoteOff(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int)
	 */
	@Override
	public void onMidiNoteOff(MidiInputDevice sender, int cable, int channel, int note, int velocity) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiNoteOff(sender, cable, channel, note, velocity);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiNoteOn(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int)
	 */
	@Override
	public void onMidiNoteOn(MidiInputDevice sender, int cable, int channel, int note, int velocity) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiNoteOn(sender, cable, channel, note, velocity);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiPolyphonicAftertouch(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int)
	 */
	@Override
	public void onMidiPolyphonicAftertouch(MidiInputDevice sender, int cable, int channel, int note, int pressure) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiPolyphonicAftertouch(sender, cable, channel, note, pressure);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiControlChange(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int)
	 */
	@Override
	public void onMidiControlChange(MidiInputDevice sender, int cable, int channel, int function, int value) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiControlChange(sender, cable, channel, function, value);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiProgramChange(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int)
	 */
	@Override
	public void onMidiProgramChange(MidiInputDevice sender, int cable, int channel, int program) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiProgramChange(sender, cable, channel, program);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiChannelAftertouch(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int)
	 */
	@Override
	public void onMidiChannelAftertouch(MidiInputDevice sender, int cable, int channel, int pressure) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiChannelAftertouch(sender, cable, channel, pressure);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiPitchWheel(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int)
	 */
	@Override
	public void onMidiPitchWheel(MidiInputDevice sender, int cable, int channel, int amount) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiPitchWheel(sender, cable, channel, amount);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiSingleByte(jp.kshoji.driver.midi.device.MidiInputDevice, int, int)
	 */
	@Override
	public void onMidiSingleByte(MidiInputDevice sender, int cable, int byte1) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiSingleByte(sender, cable, byte1);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener#onDeviceAttached(android.hardware.usb.UsbDevice)
	 */
	@Override
	public void onDeviceAttached(UsbDevice usbDevice) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onDeviceAttached(usbDevice);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
	 */
	@Override
	public void onDeviceDetached(UsbDevice usbDevice) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onDeviceDetached(usbDevice);
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiRPNReceived(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int, int)
	 */
	@Override
	public void onMidiRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiRPNReceived(sender, cable, channel, function, valueMSB, valueLSB);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiNRPNReceived(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int, int)
	 */
	@Override
	public void onMidiNRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
		List<AbstractMidiFragment> midiFragments = getMidiFragments();
		for (AbstractMidiFragment fragment : midiFragments) {
			fragment.onMidiNRPNReceived(sender, cable, channel, function, valueMSB, valueLSB);
		}
	}
}
