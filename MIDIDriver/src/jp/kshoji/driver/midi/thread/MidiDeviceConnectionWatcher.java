package jp.kshoji.driver.midi.thread;

import java.util.HashMap;
import java.util.HashSet;

import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbDeviceUtils;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

/**
 * Detects USB MIDI Device Connected
 * 
 * @author K.Shoji
 */
public class MidiDeviceConnectionWatcher {
	private MidiDeviceConnectionWatchThread thread;
	
	/**
	 * Constructor
	 * 
	 * @param context
	 * @param deviceAttachedListener
	 * @param deviceDetachedListener
	 */
	public MidiDeviceConnectionWatcher(Context context, OnMidiDeviceAttachedListener deviceAttachedListener, OnMidiDeviceDetachedListener deviceDetachedListener) {
		thread = new MidiDeviceConnectionWatchThread(context, deviceAttachedListener, deviceDetachedListener);
	}
	
	public final void checkConnectedDevicesImmediately() {
		thread.checkConnectedDevices();
	}

	public final void start() {
		thread.start();
	}
	
	public final void stop() {
		thread.stopFlag = true;
	}
	
	/**
	 * USB Device polling thread
	 * 
	 * @author K.Shoji
	 */
	class MidiDeviceConnectionWatchThread extends Thread {
		private UsbManager usbManager;
		private OnMidiDeviceAttachedListener deviceAttachedListener;
		private OnMidiDeviceDetachedListener deviceDetachedListener;
		private HashSet<String> deviceNameSet;
		boolean stopFlag;
		
		MidiDeviceConnectionWatchThread(Context context, OnMidiDeviceAttachedListener deviceAttachedListener, OnMidiDeviceDetachedListener deviceDetachedListener) {
			usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
			this.deviceAttachedListener = deviceAttachedListener;
			this.deviceDetachedListener = deviceDetachedListener;
			deviceNameSet = new HashSet<String>();
			stopFlag = false;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			super.run();
			
			while (stopFlag == false) {
				checkConnectedDevices();
				
				try {
					sleep(1000);
				} catch (InterruptedException e) {
					Log.d(Constants.TAG, "Thread interrupted", e);
				}
			}
		}

		/**
		 * checks Attached/Detached devices
		 */
		synchronized void checkConnectedDevices() {
			HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
			
			// check attached device
			for (String deviceName : deviceList.keySet()) {
				if (!deviceNameSet.contains(deviceName)) {
					deviceNameSet.add(deviceName);
					UsbDevice device = deviceList.get(deviceName);
					
					UsbInterface midiInterface = UsbDeviceUtils.findMidiInterface(device);
					if (midiInterface != null) {
						deviceAttachedListener.onDeviceAttached(device);
					}
				}
			}
			
			// check detached device
			for (String deviceName : deviceNameSet) {
				if (!deviceList.keySet().contains(deviceName)) {
					deviceList.keySet().add(deviceName);
					UsbDevice device = deviceList.get(deviceName);
					
					deviceDetachedListener.onDeviceDetached(device);
				}
			}
		}
	}
}
