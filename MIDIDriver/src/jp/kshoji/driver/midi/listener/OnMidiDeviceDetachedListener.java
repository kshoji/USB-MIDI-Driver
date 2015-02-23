package jp.kshoji.driver.midi.listener;

import android.hardware.usb.UsbDevice;
import android.support.annotation.NonNull;

/**
 * Listener for MIDI detached events
 * 
 * @author K.Shoji
 */
public interface OnMidiDeviceDetachedListener {

	/**
	 * device has been detached
	 * 
	 * @param usbDevice the detached UsbDevice
	 */
	void onDeviceDetached(@NonNull UsbDevice usbDevice);
}
