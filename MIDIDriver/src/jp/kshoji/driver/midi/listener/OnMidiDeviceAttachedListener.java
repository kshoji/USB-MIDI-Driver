package jp.kshoji.driver.midi.listener;

import android.hardware.usb.UsbDevice;
import android.support.annotation.NonNull;

/**
 * Listener for MIDI attached events
 * 
 * @author K.Shoji
 */
public interface OnMidiDeviceAttachedListener {

	/**
	 * device has been attached
	 * 
	 * @param usbDevice the attached UsbDevice
	 */
	void onDeviceAttached(@NonNull UsbDevice usbDevice);
}
