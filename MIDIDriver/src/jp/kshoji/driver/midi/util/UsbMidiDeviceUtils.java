package jp.kshoji.driver.midi.util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.usb.util.DeviceFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

/**
 * Utility for finding MIDI device
 * 
 * @author K.Shoji
 */
public final class UsbMidiDeviceUtils {
	/**
	 * Find {@link UsbInterface} from {@link UsbDevice} with the direction
	 * 
	 * @param usbDevice
	 * @param direction {@link UsbConstants.USB_DIR_IN} or {@link UsbConstants.USB_DIR_OUT}
	 * @param deviceFilters
	 * @return {@link Set<UsbInterface>} always not null
	 */
	public static Set<UsbInterface> findMidiInterfaces(UsbDevice usbDevice, int direction, List<DeviceFilter> deviceFilters) {
		Set<UsbInterface> usbInterfaces = new HashSet<UsbInterface>();
		
		int count = usbDevice.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = usbDevice.getInterface(i);
			
			if (findMidiEndpoint(usbDevice, usbInterface, direction, deviceFilters) != null) {
				usbInterfaces.add(usbInterface);
			}
		}
		return Collections.unmodifiableSet(usbInterfaces);
	}
	
	/**
	 * Find all {@link UsbInterface} from {@link UsbDevice}
	 * 
	 * @param usbDevice
	 * @param deviceFilters
	 * @return {@link Set<UsbInterface>} always not null
	 */
	public static Set<UsbInterface> findAllMidiInterfaces(UsbDevice usbDevice, List<DeviceFilter> deviceFilters) {
		Set<UsbInterface> usbInterfaces = new HashSet<UsbInterface>();
		
		int count = usbDevice.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = usbDevice.getInterface(i);
			
			if (findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_IN, deviceFilters) != null) {
				usbInterfaces.add(usbInterface);
			}
			if (findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_OUT, deviceFilters) != null) {
				usbInterfaces.add(usbInterface);
			}
		}
		return Collections.unmodifiableSet(usbInterfaces);
	}

	/**
	 * Find {@link Set<MidiIntputDevice>} from {@link UsbDevice}
	 * 
	 * @param usbDevice
	 * @param usbDeviceConnection
	 * @param deviceFilters
	 * @param inputEventListener
	 * @return {@link Set<MidiIntputDevice>} always not null
	 */
	public static Set<MidiInputDevice> findMidiInputDevices(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection, List<DeviceFilter> deviceFilters, OnMidiInputEventListener inputEventListener) {
		Set<MidiInputDevice> devices = new HashSet<MidiInputDevice>();

		int count = usbDevice.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = usbDevice.getInterface(i);

			UsbEndpoint endpoint = findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_IN, deviceFilters);
			if (endpoint != null) {
				devices.add(new MidiInputDevice(usbDevice, usbDeviceConnection, usbInterface, endpoint, inputEventListener));
			}
		}

		return Collections.unmodifiableSet(devices);
	}
	
	/**
	 * Find {@link Set<MidiOutputDevice>} from {@link UsbDevice}
	 * 
	 * @param usbDevice
	 * @param usbDeviceConnection
	 * @param deviceFilters
	 * @return {@link Set<MidiOutputDevice>} always not null
	 */
	public static Set<MidiOutputDevice> findMidiOutputDevices(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection, List<DeviceFilter> deviceFilters) {
		Set<MidiOutputDevice> devices = new HashSet<MidiOutputDevice>();
		
		int count = usbDevice.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = usbDevice.getInterface(i);
			if (usbInterface == null) {
				continue;
			}

			UsbEndpoint endpoint = findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_OUT, deviceFilters);
			if (endpoint != null) {
				devices.add(new MidiOutputDevice(usbDevice, usbDeviceConnection, usbInterface, endpoint));
			}
		}
		
		return Collections.unmodifiableSet(devices);
	}

	/**
	 * Find {@link UsbEndpoint} from {@link findMidiEndpoint} with the direction
	 * 
	 * @param usbDevice
	 * @param usbInterface
	 * @param direction
	 * @param deviceFilters
	 * @return {@link UsbEndpoint}, null if not found
	 */
	public static UsbEndpoint findMidiEndpoint(UsbDevice usbDevice, UsbInterface usbInterface, int direction, List<DeviceFilter> deviceFilters) {
		int endpointCount = usbInterface.getEndpointCount();
		
		// standard USB MIDI interface
		if (usbInterface.getInterfaceClass() == 1 && usbInterface.getInterfaceSubclass() == 3) {
			for (int endpointIndex = 0; endpointIndex < endpointCount; endpointIndex++) {
				UsbEndpoint endpoint = usbInterface.getEndpoint(endpointIndex);
				if (endpoint.getDirection() == direction) {
					return endpoint;
				}
			}
		} else {
			boolean filterMatched = false;
			for (DeviceFilter deviceFilter : deviceFilters) {
				if (deviceFilter.matches(usbDevice)) {
					filterMatched = true;
					break;
				}
			}
			
			if (filterMatched == false) {
				Log.d(Constants.TAG, "unsupported interface: " + usbInterface);
				return null;
			}

			// non standard USB MIDI interface
			for (int endpointIndex = 0; endpointIndex < endpointCount; endpointIndex++) {
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
