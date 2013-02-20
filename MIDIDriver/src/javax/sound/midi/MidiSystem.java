package javax.sound.midi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.thread.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;
import jp.kshoji.driver.usb.util.DeviceFilter;
import jp.kshoji.javax.sound.midi.UsbMidiDevice;
import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * MidiSystem porting for Android USB MIDI.<br />
 * Implemented Receiver and Transmitter only.
 * 
 * @author K.Shoji
 */
public final class MidiSystem {
	static List<DeviceFilter> deviceFilters = null;
	static Set<UsbMidiDevice> midiDevices = null;
	static Map<UsbDevice, UsbDeviceConnection> deviceConnections;
	static OnMidiDeviceAttachedListener deviceAttachedListener = null;
	static OnMidiDeviceDetachedListener deviceDetachedListener = null;
	private static MidiDeviceConnectionWatcher deviceConnectionWatcher = null;
	
	/**
	 * Find {@link Set<UsbMidiDevice>} from {@link UsbDevice}<br />
	 * method for jp.kshoji.javax.sound.midi package.
	 * 
	 * @param usbDevice
	 * @param usbDeviceConnection 
	 * @return {@link Set<UsbMidiDevice>}, always not null
	 */
	static Set<UsbMidiDevice> findAllUsbMidiDevices(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection) {
		Set<UsbMidiDevice> result = new HashSet<UsbMidiDevice>();
		
		Set<UsbInterface> interfaces = UsbMidiDeviceUtils.findAllMidiInterfaces(usbDevice, deviceFilters);
		for (UsbInterface usbInterface : interfaces) {
			UsbEndpoint inputEndpoint = UsbMidiDeviceUtils.findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_IN, deviceFilters);
			UsbEndpoint outputEndpoint = UsbMidiDeviceUtils.findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_OUT, deviceFilters);
			
			result.add(new UsbMidiDevice(usbDevice, usbDeviceConnection, usbInterface, inputEndpoint, outputEndpoint));
		}
		
		return result;
	}

	
	/**
	 * Implementation for multiple device connections.
	 * 
	 * @author K.Shoji
	 */
	static final class OnMidiDeviceAttachedListenerImpl implements OnMidiDeviceAttachedListener {
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
			UsbDeviceConnection deviceConnection = usbManager.openDevice(attachedDevice);
			if (deviceConnection == null) {
				return;
			}

			synchronized (deviceConnection) {
				deviceConnections.put(attachedDevice, deviceConnection);
			}

			synchronized (midiDevices) {
				midiDevices.addAll(findAllUsbMidiDevices(attachedDevice, deviceConnection));
			}

			Log.d(Constants.TAG, "Device " + attachedDevice.getDeviceName() + " has been attached.");
		}
	}

	/**
	 * Implementation for multiple device connections.
	 * 
	 * @author K.Shoji
	 */
	static final class OnMidiDeviceDetachedListenerImpl implements OnMidiDeviceDetachedListener {
		/*
		 * (non-Javadoc)
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
		 */
		@Override
		public void onDeviceDetached(UsbDevice detachedDevice) {
			UsbDeviceConnection usbDeviceConnection;
			synchronized (deviceConnections) {
				usbDeviceConnection = deviceConnections.get(detachedDevice);
			}

			if (usbDeviceConnection == null) {
				return;
			}

			Set<UsbMidiDevice> detachedMidiDevices = findAllUsbMidiDevices(detachedDevice, usbDeviceConnection);
			for (UsbMidiDevice usbMidiDevice : detachedMidiDevices) {
				usbMidiDevice.close();
			}

			synchronized (midiDevices) {
				midiDevices.removeAll(detachedMidiDevices);
			}

			Log.d(Constants.TAG, "Device " + detachedDevice.getDeviceName() + " has been detached.");
		}
	}

	/**
	 * Initializes MidiSystem
	 * 
	 * @param context
	 * @throws NullPointerException
	 */
	public static void initialize(Context context) throws NullPointerException {
		if (context == null) {
			throw new NullPointerException("context is null");
		}

		UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		if (usbManager == null) {
			throw new NullPointerException("UsbManager is null");
		}

		deviceFilters = DeviceFilter.getDeviceFilters(context);
		midiDevices = new HashSet<UsbMidiDevice>();
		deviceConnections = new HashMap<UsbDevice, UsbDeviceConnection>();
		deviceAttachedListener = new OnMidiDeviceAttachedListenerImpl(usbManager);
		deviceDetachedListener = new OnMidiDeviceDetachedListenerImpl();
		deviceConnectionWatcher = new MidiDeviceConnectionWatcher(context, usbManager, deviceAttachedListener, deviceDetachedListener);
	}

	/**
	 * Terminates MidiSystem
	 */
	public static void terminate() {
		if (midiDevices != null) {
			synchronized (midiDevices) {
				for (UsbMidiDevice midiDevice : midiDevices) {
					midiDevice.close();
				}
				midiDevices.clear();
			}
		}
		midiDevices = null;

		if (deviceConnections != null) {
			synchronized (deviceConnections) {
				deviceConnections.clear();
			}
		}
		deviceConnections = null;

		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.stop();
		}
		deviceConnectionWatcher = null;
	}

	private MidiSystem() {
	}

	/**
	 * get all connected {@link MidiDevice.Info} as array
	 * 
	 * @return device informations
	 */
	public static MidiDevice.Info[] getMidiDeviceInfo() {
		List<MidiDevice.Info> result = new ArrayList<MidiDevice.Info>();
		if (midiDevices != null) {
			for (MidiDevice midiDevice : midiDevices) {
				result.add(midiDevice.getDeviceInfo());
			}
		}
		return result.toArray(new MidiDevice.Info[0]);
	}

	/**
	 * get {@link MidiDevice} by device information
	 * 
	 * @param info
	 * @return {@link MidiDevice}
	 * @throws MidiUnavailableException
	 */
	public static MidiDevice getMidiDevice(MidiDevice.Info info) throws MidiUnavailableException {
		if (midiDevices != null) {
			for (MidiDevice midiDevice : midiDevices) {
				if (info.equals(midiDevice.getDeviceInfo())) {
					return midiDevice;
				}
			}
		}

		throw new IllegalArgumentException("Requested device not installed: " + info);
	}

	/**
	 * get the first detected Receiver
	 * 
	 * @return {@link Receiver}
	 * @throws MidiUnavailableException
	 */
	public static Receiver getReceiver() throws MidiUnavailableException {
		if (midiDevices != null) {
			for (MidiDevice midiDevice : midiDevices) {
				Receiver receiver = midiDevice.getReceiver();
				if (receiver != null) {
					return receiver;
				}
			}
		}
		return null;
	}

	/**
	 * get the first detected Transmitter
	 * 
	 * @return {@link Transmitter}
	 * @throws MidiUnavailableException
	 */
	public static Transmitter getTransmitter() throws MidiUnavailableException {
		if (midiDevices != null) {
			for (MidiDevice midiDevice : midiDevices) {
				Transmitter transmitter = midiDevice.getTransmitter();
				if (transmitter != null) {
					return transmitter;
				}
			}
		}
		return null;
	}
}
