package jp.kshoji.driver.midi.listener;

import android.hardware.usb.UsbDevice;

/**
 * Listener for MIDI attached events
 * 
 * @author K.Shoji
 */
public interface OnMidiDeviceAttachedListener {
	/**
	 * device has been attached
	 * 
	 * @param usbDevice
	 */
	void onDeviceAttached(final UsbDevice usbDevice);
}
