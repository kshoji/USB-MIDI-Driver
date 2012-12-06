package jp.kshoji.driver.midi.activity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.thread.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbDeviceUtils;
import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;

/**
 * base Activity for using USB MIDI interface.
 * launchMode must be "singleTask" or "singleInstance".
 * 
 * @author K.Shoji
 */
public abstract class AbstractMidiActivity extends Activity implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {
	Map<UsbDevice, UsbDeviceConnection> deviceConnections = null;
	Map<UsbDevice, Set<MidiInputDevice>> midiInputDevices = null;
	Map<UsbDevice, Set<MidiOutputDevice>> midiOutputDevices = null;
	private MidiDeviceConnectionWatcher deviceConnectionWatcher = null;
	Handler deviceDetachedHandler = null;

	private OnMidiDeviceAttachedListener deviceAttachedListener = new OnMidiDeviceAttachedListener() {
		/*
		 * (non-Javadoc)
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener#onDeviceAttached(android.hardware.usb.UsbDevice, android.hardware.usb.UsbInterface)
		 */
		@Override
		public synchronized void onDeviceAttached(final UsbDevice attachedDevice) {
			UsbManager usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
			
			UsbDeviceConnection deviceConnection = usbManager.openDevice(attachedDevice);
			deviceConnections.put(attachedDevice, deviceConnection);
			
			Set<UsbInterface> inputInterfaces = UsbDeviceUtils.findMidiInterfaces(attachedDevice, UsbConstants.USB_DIR_IN);
			for (UsbInterface usbInterface : inputInterfaces) {
				try {
					MidiInputDevice midiInputDevice = new MidiInputDevice(attachedDevice, deviceConnection, usbInterface, AbstractMidiActivity.this);
					Set<MidiInputDevice> inputDevices = midiInputDevices.get(attachedDevice);
					if (inputDevices == null) {
						inputDevices = new HashSet<MidiInputDevice>();
					}
					inputDevices.add(midiInputDevice);
					midiInputDevices.put(attachedDevice, inputDevices);
				} catch (IllegalArgumentException iae) {
					Log.i(Constants.TAG, "This device didn't have any input endpoints.", iae);
				}
			}
			
			Set<UsbInterface> outputInterfaces = UsbDeviceUtils.findMidiInterfaces(attachedDevice, UsbConstants.USB_DIR_OUT);
			for (UsbInterface usbInterface : outputInterfaces) {
				try {
					MidiOutputDevice midiOutputDevice = new MidiOutputDevice(deviceConnection, usbInterface);
					Set<MidiOutputDevice> outputDevices = midiOutputDevices.get(attachedDevice);
					if (outputDevices == null) {
						outputDevices = new HashSet<MidiOutputDevice>();
					}
					outputDevices.add(midiOutputDevice);
					midiOutputDevices.put(attachedDevice, outputDevices);
				} catch (IllegalArgumentException iae) {
					Log.i(Constants.TAG, "This device didn't have any output endpoints.", iae);
				}
			}
			
			Log.d(Constants.TAG, "Device " + attachedDevice.getDeviceName() + " has been attached.");
			
			AbstractMidiActivity.this.onDeviceAttached(attachedDevice);
		}
	};

	private OnMidiDeviceDetachedListener deviceDetachedListener = new OnMidiDeviceDetachedListener() {
		/*
		 * (non-Javadoc)
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
		 */
		@Override
		public void onDeviceDetached(final UsbDevice detachedDevice) {
			UsbDeviceConnection deviceConnection = null;
			if (deviceConnections != null) {
				deviceConnection = deviceConnections.get(detachedDevice);
			}
			
			if (deviceConnection != null) {
				Set<UsbInterface> inputInterfaces = UsbDeviceUtils.findMidiInterfaces(detachedDevice, UsbConstants.USB_DIR_IN);
				Set<UsbInterface> outputInterfaces = UsbDeviceUtils.findMidiInterfaces(detachedDevice, UsbConstants.USB_DIR_OUT);
				
				Set<UsbInterface> allInterfaces = new HashSet<UsbInterface>();
				allInterfaces.addAll(inputInterfaces);
				allInterfaces.addAll(outputInterfaces);
				
				for (UsbInterface usbInterface : allInterfaces) {
					if (usbInterface != null) {
						deviceConnection.releaseInterface(usbInterface);
					}
				}
				
				deviceConnection.close();
			}
			
			// Stop input device's thread.
			Set<MidiInputDevice> inputDevices = midiInputDevices.get(detachedDevice);
			if (inputDevices != null && inputDevices.size() > 0) {
				for (MidiInputDevice inputDevice : inputDevices) {
					if (inputDevice != null) {
						inputDevice.stop();
					}
				}
				midiInputDevices.remove(detachedDevice);
			}
			
			Log.d(Constants.TAG, "Device " + detachedDevice.getDeviceName() + " has been detached.");
			
			Message message = new Message();
			message.obj = detachedDevice;
			deviceDetachedHandler.sendMessage(message);
		}
	};

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		deviceConnections = new HashMap<UsbDevice, UsbDeviceConnection>();
		midiInputDevices = new HashMap<UsbDevice, Set<MidiInputDevice>>();
		midiOutputDevices = new HashMap<UsbDevice, Set<MidiOutputDevice>>();
		deviceDetachedHandler = new Handler(new Callback() {
			/*
			 * (non-Javadoc)
			 * @see android.os.Handler.Callback#handleMessage(android.os.Message)
			 */
			@Override
			public boolean handleMessage(Message msg) {
				UsbDevice usbDevice = (UsbDevice) msg.obj;
				onDeviceDetached(usbDevice);
				return true;
			}
		});

		deviceConnectionWatcher = new MidiDeviceConnectionWatcher(getApplicationContext(), deviceAttachedListener, deviceDetachedListener);
		deviceConnectionWatcher.start();
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.stop();
		}
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
	}
	
	/**
	 * get connected USB MIDI devices.
	 * 
	 * @return connected UsbDevice set, null if not available
	 */
	public final Set<UsbDevice> getConnectedUsbDevices() {
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.checkConnectedDevicesImmediately();
		}
		if (deviceConnections != null) {
			return deviceConnections.keySet();
		}
		
		return new HashSet<UsbDevice>();
	}
	
	/**
	 * get MIDI output device, if available.
	 * 
	 * @param usbDevice
	 * @return MidiOutputDevice, null if not available
	 */
	public final Set<MidiOutputDevice> getMidiOutputDevices(final UsbDevice usbDevice) {
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.checkConnectedDevicesImmediately();
		}
		if (midiOutputDevices != null) {
			return midiOutputDevices.get(usbDevice);
		}
		
		return null;
	}
}
