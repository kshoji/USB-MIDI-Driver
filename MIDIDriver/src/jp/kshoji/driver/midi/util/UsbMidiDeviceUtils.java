package jp.kshoji.driver.midi.util;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
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
		Set<UsbInterface> usbInterfaces = new HashSet<>();
		
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
		Set<UsbInterface> usbInterfaces = new HashSet<>();
		
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
     * @param deviceFilters the List of {@link jp.kshoji.driver.usb.util.DeviceFilter}
	 * @return {@link Set<MidiInputDevice>} always not null
	 */
    @NonNull
    public static Set<MidiInputDevice> findMidiInputDevices(@NonNull UsbDevice usbDevice, @NonNull UsbDeviceConnection usbDeviceConnection, @NonNull List<DeviceFilter> deviceFilters) {
		Set<MidiInputDevice> devices = new HashSet<>();
        Set<Integer> registeredEndpointNumbers = new HashSet<>();

		int count = usbDevice.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = usbDevice.getInterface(i);

			UsbEndpoint endpoint = findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_IN, deviceFilters);
			if (endpoint != null) {
                // multiple endpoints on the same address. use the first one.
                if (!registeredEndpointNumbers.contains(endpoint.getEndpointNumber())) {
                    registeredEndpointNumbers.add(endpoint.getEndpointNumber());
                    devices.add(new MidiInputDevice(usbDevice, usbDeviceConnection, usbInterface, endpoint));
                }
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
		Set<MidiOutputDevice> devices = new HashSet<>();
        Set<Integer> registeredEndpointNumbers = new HashSet<>();

		int count = usbDevice.getInterfaceCount();
		for (int i = 0; i < count; i++) {
			UsbInterface usbInterface = usbDevice.getInterface(i);

            UsbEndpoint endpoint = findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_OUT, deviceFilters);
			if (endpoint != null) {
                // multiple endpoints on the same address. use the first one.
                if (!registeredEndpointNumbers.contains(endpoint.getEndpointNumber())) {
                    registeredEndpointNumbers.add(endpoint.getEndpointNumber());
                    devices.add(new MidiOutputDevice(usbDevice, usbDeviceConnection, usbInterface, endpoint));
                }
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

    private static final int USB_REQUEST_GET_DESCRIPTOR = 0x06;
    private static final int USB_DATA_TYPE_STRING = 0x03;

    /**
     * Get UsbDevice's product name
     *
     * @param usbDevice the UsbDevice
     * @param usbDeviceConnection the UsbDeviceConnection
     * @return the product name
     */
    @SuppressLint("NewApi")
    @Nullable
    public static String getProductName(@NonNull UsbDevice usbDevice, @NonNull UsbDeviceConnection usbDeviceConnection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return usbDevice.getProductName();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            byte[] rawDescriptors = usbDeviceConnection.getRawDescriptors();

            try {
                byte[] buffer = new byte[255];
                int indexOfProductName = rawDescriptors[15] & 0xff;

                int productNameLength = usbDeviceConnection.controlTransfer(UsbConstants.USB_DIR_IN, USB_REQUEST_GET_DESCRIPTOR, (USB_DATA_TYPE_STRING << 8) | indexOfProductName, 0, buffer, 255, 0);
                return new String(buffer, 2, productNameLength - 2, "UTF-16LE");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Get UsbDevice's manufacturer name
     *
     * @param usbDevice the UsbDevice
     * @param usbDeviceConnection the UsbDeviceConnection
     * @return the manufacturer name
     */
    @SuppressLint("NewApi")
    @Nullable
    public static String getManufacturerName(@NonNull UsbDevice usbDevice, @NonNull UsbDeviceConnection usbDeviceConnection) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return usbDevice.getManufacturerName();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            byte[] rawDescriptors = usbDeviceConnection.getRawDescriptors();

            try {
                byte[] buffer = new byte[255];
                int indexOfManufacturerName = rawDescriptors[14] & 0xff;

                int manufacturerNameLength = usbDeviceConnection.controlTransfer(UsbConstants.USB_DIR_IN, USB_REQUEST_GET_DESCRIPTOR, (USB_DATA_TYPE_STRING << 8) | indexOfManufacturerName, 0, buffer, 255, 0);
                return new String(buffer, 2, manufacturerNameLength - 2, "UTF-16LE");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
