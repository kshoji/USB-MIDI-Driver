package jp.kshoji.driver.midi.util;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

/**
 * Utility for finding MIDI device
 * 
 * @author K.Shoji
 */
public class UsbDeviceUtils {
	public static UsbInterface findMidiInterface(UsbDevice device) {
		int count = device.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = device.getInterface(i);
			if (usbInterface.getInterfaceClass() == 1 && usbInterface.getInterfaceSubclass() == 3) {
				return usbInterface;
			}

			UsbEndpoint midiInputEndpoint = null;
			UsbEndpoint midiOutputEndpoint = null;
			
			if (usbInterface.getEndpointCount() >= 2) {
				// has more than 2 endpoints

				for (int endpointIndex = 0; endpointIndex < usbInterface.getEndpointCount(); endpointIndex++) {
					UsbEndpoint endpoint = usbInterface.getEndpoint(endpointIndex);
					if (endpoint.getMaxPacketSize() == 64 && //
							endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK) {
						// buffer size == 64
						// bulk transfer
						if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
							midiInputEndpoint = midiInputEndpoint == null ? endpoint : midiInputEndpoint;
						} else {
							midiOutputEndpoint = midiOutputEndpoint == null ? endpoint : midiOutputEndpoint;
						}
					}
				}

				if (midiInputEndpoint != null || midiOutputEndpoint != null) {
					return usbInterface;
				}
			}
		}
		return null;
	}
}
