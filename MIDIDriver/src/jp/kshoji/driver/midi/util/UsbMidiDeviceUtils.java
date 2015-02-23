package jp.kshoji.driver.midi.util;

import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.usb.util.DeviceFilter;

/**
 * Utility for finding MIDI device
 * 
 * @author K.Shoji
 */
public final class UsbMidiDeviceUtils {

	/**
	 * Find {@link UsbInterface} from {@link UsbDevice} with the direction
	 * 
	 * @param usbDevice the UsbDevice
	 * @param direction {@link UsbConstants#USB_DIR_IN} or {@link UsbConstants#USB_DIR_OUT}
	 * @param deviceFilters the List of {@link DeviceFilter}
	 * @return {@link Set<UsbInterface>} always not null
	 */
    @NonNull
    public static Set<UsbInterface> findMidiInterfaces(@NonNull UsbDevice usbDevice, int direction, @NonNull List<DeviceFilter> deviceFilters) {
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
     * @param usbDevice the UsbDevice
     * @param deviceFilters the List of {@link DeviceFilter}
	 * @return {@link Set<UsbInterface>} always not null
	 */
    @NonNull
    public static Set<UsbInterface> findAllMidiInterfaces(@NonNull UsbDevice usbDevice, @NonNull List<DeviceFilter> deviceFilters) {
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
	 * Find {@link Set<MidiInputDevice>} from {@link UsbDevice}
	 *
     * @param usbDevice the UsbDevice
	 * @param usbDeviceConnection the UsbDeviceConnection
     * @param deviceFilters the List of {@link DeviceFilter}
	 * @param inputEventListener the OnMidiInputEventListener
	 * @return {@link Set<MidiInputDevice>} always not null
	 */
    @NonNull
    public static Set<MidiInputDevice> findMidiInputDevices(@NonNull UsbDevice usbDevice, @NonNull UsbDeviceConnection usbDeviceConnection, @NonNull List<DeviceFilter> deviceFilters, @NonNull OnMidiInputEventListener inputEventListener) {
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
     * @param usbDevice the UsbDevice
     * @param usbDeviceConnection the UsbDeviceConnection
     * @param deviceFilters the List of {@link DeviceFilter}
	 * @return {@link Set<MidiOutputDevice>} always not null
	 */
    @NonNull
    public static Set<MidiOutputDevice> findMidiOutputDevices(@NonNull UsbDevice usbDevice, @NonNull UsbDeviceConnection usbDeviceConnection, @NonNull List<DeviceFilter> deviceFilters) {
		Set<MidiOutputDevice> devices = new HashSet<MidiOutputDevice>();
		
		int count = usbDevice.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = usbDevice.getInterface(i);

            UsbEndpoint endpoint = findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_OUT, deviceFilters);
			if (endpoint != null) {
				devices.add(new MidiOutputDevice(usbDevice, usbDeviceConnection, usbInterface, endpoint));
			}
		}
		
		return Collections.unmodifiableSet(devices);
	}

	/**
	 * Find {@link UsbEndpoint} with the direction
	 *
     * @param usbDevice the UsbDevice
	 * @param usbInterface the UsbInterface
     * @param direction {@link UsbConstants#USB_DIR_IN} or {@link UsbConstants#USB_DIR_OUT}
     * @param deviceFilters the List of {@link DeviceFilter}
	 * @return {@link UsbEndpoint}, null if not found
	 */
    @Nullable
    public static UsbEndpoint findMidiEndpoint(@NonNull UsbDevice usbDevice, @NonNull UsbInterface usbInterface, int direction, @NonNull List<DeviceFilter> deviceFilters) {
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
