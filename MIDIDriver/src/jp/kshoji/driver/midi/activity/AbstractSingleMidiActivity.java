package jp.kshoji.driver.midi.activity;

import java.util.List;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.thread.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;
import jp.kshoji.driver.usb.util.DeviceFilter;
import android.app.Activity;
import android.content.Context;
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
 * In this implement, the only one device (connected at first) will be detected.
 * launchMode must be "singleTask" or "singleInstance".
 * 
 * @author K.Shoji
 */
public abstract class AbstractSingleMidiActivity extends Activity implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {
	UsbDevice device = null;
	UsbDeviceConnection deviceConnection = null;
	MidiInputDevice midiInputDevice = null;
	MidiOutputDevice midiOutputDevice = null;
	private MidiDeviceConnectionWatcher deviceConnectionWatcher = null;
	Handler deviceDetachedHandler = null;

	private OnMidiDeviceAttachedListener deviceAttachedListener = new OnMidiDeviceAttachedListener() {
		/*
		 * (non-Javadoc)
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener#onDeviceAttached(android.hardware.usb.UsbDevice, android.hardware.usb.UsbInterface)
		 */
		@Override
		public synchronized void onDeviceAttached(final UsbDevice attachedDevice) {
			if (device != null) {
				// already one device has been connected
				return;
			}

			UsbManager usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
			
			deviceConnection = usbManager.openDevice(attachedDevice);
			if (deviceConnection == null) {
				return;
			}
			
			List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(getApplicationContext());

			Set<MidiInputDevice> foundDevices = UsbMidiDeviceUtils.findMidiInputDevices(attachedDevice, deviceConnection, deviceFilters, AbstractSingleMidiActivity.this);
			midiInputDevice = (MidiInputDevice) foundDevices.toArray()[0];
			
			Set<MidiOutputDevice> foundOutputDevices = UsbMidiDeviceUtils.findMidiOutputDevices(attachedDevice, deviceConnection, deviceFilters);
			midiOutputDevice = (MidiOutputDevice) foundOutputDevices.toArray()[0];
			
			Log.d(Constants.TAG, "Device " + attachedDevice.getDeviceName() + " has been attached.");
			
			AbstractSingleMidiActivity.this.onDeviceAttached(attachedDevice);
		}
	};

	private OnMidiDeviceDetachedListener deviceDetachedListener = new OnMidiDeviceDetachedListener() {
		/*
		 * (non-Javadoc)
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
		 */
		@Override
		public void onDeviceDetached(final UsbDevice detachedDevice) {
			// Stop input device's thread.
			if (midiInputDevice != null) {
				midiInputDevice.stop();
				midiInputDevice = null;
			}
			
			midiOutputDevice = null;

			if (deviceConnection != null) {
				List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(getApplicationContext());

				Set<UsbInterface> allInterfaces = UsbMidiDeviceUtils.findAllMidiInterfaces(detachedDevice, deviceFilters);
				
				for (UsbInterface usbInterface : allInterfaces) {
					if (usbInterface != null) {
						deviceConnection.releaseInterface(usbInterface);
					}
				}
				
				deviceConnection.close();
				deviceConnection = null;
			}
			device = null;

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
		
		if (midiInputDevice != null) {
			midiInputDevice.stop();
			midiInputDevice = null;
		}
		
		midiOutputDevice = null;
	}
	
	/**
	 * Get MIDI output device, if available.
	 * 
	 * @param usbDevice
	 * @return MidiOutputDevice, null if not available
	 */
	public final MidiOutputDevice getMidiOutputDevice() {
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.checkConnectedDevicesImmediately();
		}
		
		return midiOutputDevice;
	}
}
