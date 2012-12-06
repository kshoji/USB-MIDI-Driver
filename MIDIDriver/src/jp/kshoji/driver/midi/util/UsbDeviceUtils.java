package jp.kshoji.driver.midi.util;

import java.util.HashSet;
import java.util.Set;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;

/**
 * Utility for finding MIDI device
 * 
 * @author K.Shoji
 */
public final class UsbDeviceUtils {

	/**
	 * Find {@link UsbInterface} from {@link UsbDevice} with the direction
	 * 
	 * @param device
	 * @param direction {@link UsbConstants.USB_DIR_IN} or {@link UsbConstants.USB_DIR_OUT}
	 * @return {@link Set<UsbInterface>} always not null
	 */
	public static Set<UsbInterface> findMidiInterfaces(final UsbDevice device, int direction) {
		Set<UsbInterface> usbInterfaces = new HashSet<UsbInterface>();
		
		int count = device.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = device.getInterface(i);
			
			if (findMidiEndpoint(usbInterface, direction) != null) {
				usbInterfaces.add(usbInterface);
			}
		}
		return usbInterfaces;
	}
	
	/**
	 * Find {@link UsbEndpoint} from {@link findMidiEndpoint} with the direction
	 * 
	 * @param usbInterface
	 * @param direction
	 * @return {@link UsbEndpoint}, null if not found
	 */
	public static UsbEndpoint findMidiEndpoint(final UsbInterface usbInterface, int direction) {
		// standard USB MIDI interface
		if (usbInterface.getInterfaceClass() == 1 && usbInterface.getInterfaceSubclass() == 3) {
			for (int endpointIndex = 0; endpointIndex < usbInterface.getEndpointCount(); endpointIndex++) {
				UsbEndpoint endpoint = usbInterface.getEndpoint(endpointIndex);
				if (endpoint.getDirection() == direction) {
					return endpoint;
				}
			}
		} else {
			// non standard USB MIDI interface
			for (int endpointIndex = 0; endpointIndex < usbInterface.getEndpointCount(); endpointIndex++) {
				UsbEndpoint endpoint = usbInterface.getEndpoint(endpointIndex);
				if ((endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK || endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT)) {
					if (endpoint.getDirection() == direction) {
						return endpoint;
					}
				}
			}
		}
		return null;
	}
}
