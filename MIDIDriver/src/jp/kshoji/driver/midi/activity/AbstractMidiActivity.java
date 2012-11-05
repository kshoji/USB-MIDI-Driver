package jp.kshoji.driver.midi.activity;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiEventListener;
import jp.kshoji.driver.midi.receiver.UsbMidiBroadcastReceiver;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbDeviceUtils;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;

/**
 * base Activity for using USB MIDI interface.
 * 
 * @author K.Shoji
 */
public abstract class AbstractMidiActivity extends Activity implements OnMidiDeviceDetachedListener, OnMidiEventListener {
	private UsbMidiBroadcastReceiver usbReceiver;
	private UsbDeviceConnection deviceConnection = null;
	private MidiInputDevice midiInputDevice = null;
	private MidiOutputDevice midiOutputDevice = null;

	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String action = getIntent().getAction();
		if (!UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			Log.d(Constants.TAG, "Intent action is not 'android.hardware.usb.action.USB_DEVICE_ATTACHED'. Usb device can only attached with category 'android.intent.category.LAUNCHER'.");
		}
		
		usbReceiver = new UsbMidiBroadcastReceiver(this);
		
		IntentFilter filter = new IntentFilter();
		filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		registerReceiver(usbReceiver, filter);
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		super.onResume();
		
		Intent intent = getIntent();
		String action = intent.getAction();
		
		if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			if (device != null) {
				UsbInterface usbInterface = UsbDeviceUtils.findMidiInterface(device);
				if (usbInterface != null) {
					onDeviceAttached(device, usbInterface);
				}
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (usbReceiver != null) {
			unregisterReceiver(usbReceiver);
		}
	}

	/**
	 * USB device has been attached.
	 * 
	 * @param attachedDevice
	 * @param usbInterface
	 */
	private final void onDeviceAttached(UsbDevice attachedDevice, UsbInterface usbInterface) {
		UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		
		deviceConnection = usbManager.openDevice(attachedDevice);

		try {
			midiInputDevice = new MidiInputDevice(deviceConnection, usbInterface, this);
			midiInputDevice.start();
		} catch (IllegalArgumentException iae) {
			Log.i(Constants.TAG, "This device didn't have any input endpoints.", iae);
		}
		
		try {
			midiOutputDevice = new MidiOutputDevice(deviceConnection, usbInterface);
		} catch (IllegalArgumentException iae) {
			Log.i(Constants.TAG, "This device didn't have any output endpoints.", iae);
		}
		
		onDeviceAttached();
	}
	
	/**
	 * Will be called when device has been attached.
	 */
	protected void onDeviceAttached() {
		// do nothing. must be override
	}
	
	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
	 */
	@Override
	public final void onDeviceDetached(UsbDevice detachedDevice) {
		if (midiInputDevice != null) {
			midiInputDevice.stop();
		}

		if (deviceConnection != null) {
			UsbInterface midiInterface = UsbDeviceUtils.findMidiInterface(detachedDevice);
			if (midiInterface != null) {
				deviceConnection.releaseInterface(midiInterface);
			}
			deviceConnection.close();
		}
		
		midiInputDevice = null;
		midiOutputDevice = null;
		
		Log.d(Constants.TAG, "Device has been detached. The activity must be finished.");
		onDeviceDetached();
	}
	
	/**
	 * Will be called when device has been detached.
	 * The activity must be finished in this method.
	 */
	protected void onDeviceDetached() {
		// do nothing. must be override
	}

	/**
	 * get MIDI output device, if available.
	 * 
	 * @return
	 */
	public final MidiOutputDevice getMidiOutputDevice() {
		return midiOutputDevice;
	}
}
