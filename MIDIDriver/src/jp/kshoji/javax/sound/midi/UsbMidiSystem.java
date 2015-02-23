package jp.kshoji.javax.sound.midi;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.thread.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;
import jp.kshoji.driver.usb.util.DeviceFilter;
import jp.kshoji.javax.sound.midi.usb.UsbMidiDevice;

/**
 * {@link jp.kshoji.javax.sound.midi.MidiSystem} initializer / terminator for Android USB MIDI.
 *
 * @author K.Shoji
 */
public final class UsbMidiSystem implements OnMidiDeviceAttachedListener, OnMidiDeviceDetachedListener {
    private final Context context;
	private final List<DeviceFilter> deviceFilters;
    private final Map<UsbDevice, Set<UsbMidiDevice>> midiDevices = new HashMap<UsbDevice, Set<UsbMidiDevice>>();
    private final Map<UsbDevice, UsbDeviceConnection> deviceConnections = new HashMap<UsbDevice, UsbDeviceConnection>();

    private MidiDeviceConnectionWatcher deviceConnectionWatcher;
    private UsbManager usbManager;

    /**
     * Constructor
     *
     * @param context the context
     */
    public UsbMidiSystem(@NonNull Context context) {
        this.context = context.getApplicationContext();
        deviceFilters = DeviceFilter.getDeviceFilters(context);
    }

    /**
     * Initializes {@link jp.kshoji.javax.sound.midi.MidiSystem}
     */
    public void initialize() {
        usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            throw new NullPointerException("UsbManager is null");
        }

        deviceConnectionWatcher = new MidiDeviceConnectionWatcher(context, usbManager, this, this);
    }

    /**
     * Terminates {@link jp.kshoji.javax.sound.midi.MidiSystem}
     */
    public void terminate() {
        synchronized (midiDevices) {
            for (Map.Entry<UsbDevice, Set<UsbMidiDevice>> midiDeviceEntry  : midiDevices.entrySet()) {
                for (UsbMidiDevice midiDevice : midiDeviceEntry.getValue()) {
                    midiDevice.close();
                }
            }

            midiDevices.clear();
        }

        synchronized (deviceConnections) {
            deviceConnections.clear();
        }

        deviceConnectionWatcher.stop();
        deviceConnectionWatcher = null;

        usbManager = null;
    }

    @Override
    public void onDeviceAttached(@NonNull UsbDevice attachedDevice) {
        deviceConnectionWatcher.notifyDeviceGranted();

        UsbDeviceConnection deviceConnection = usbManager.openDevice(attachedDevice);
        if (deviceConnection == null) {
            return;
        }

        synchronized (deviceConnections) {
            deviceConnections.put(attachedDevice, deviceConnection);
        }

        synchronized (midiDevices) {
            midiDevices.put(attachedDevice, findAllUsbMidiDevices(attachedDevice, deviceConnection));
        }

        Log.d(Constants.TAG, "Device " + attachedDevice.getDeviceName() + " has been attached.");
    }

    @Override
    public void onDeviceDetached(@NonNull UsbDevice detachedDevice) {
        UsbDeviceConnection usbDeviceConnection;
        synchronized (deviceConnections) {
            usbDeviceConnection = deviceConnections.get(detachedDevice);
        }

        if (usbDeviceConnection == null) {
            return;
        }

        synchronized (midiDevices) {
            Set<UsbMidiDevice> detachedMidiDevices = midiDevices.get(detachedDevice);
            if (detachedMidiDevices != null) {
                for (UsbMidiDevice usbMidiDevice : detachedMidiDevices) {
                    usbMidiDevice.close();
                }
            }

            midiDevices.remove(detachedDevice);
        }

        Log.d(Constants.TAG, "Device " + detachedDevice.getDeviceName() + " has been detached.");
    }

    /**
     * Find {@link Set<UsbMidiDevice>} from {@link UsbDevice}<br />
     * method for jp.kshoji.javax.sound.midi package.
     *
     * @param usbDevice the device
     * @param usbDeviceConnection the device connection
     * @return {@link Set<UsbMidiDevice>}, always not null
     */
    @NonNull
    private Set<UsbMidiDevice> findAllUsbMidiDevices(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection) {
		Set<UsbMidiDevice> result = new HashSet<UsbMidiDevice>();

		Set<UsbInterface> interfaces = UsbMidiDeviceUtils.findAllMidiInterfaces(usbDevice, deviceFilters);
		for (UsbInterface usbInterface : interfaces) {
			UsbEndpoint inputEndpoint = UsbMidiDeviceUtils.findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_IN, deviceFilters);
			UsbEndpoint outputEndpoint = UsbMidiDeviceUtils.findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_OUT, deviceFilters);

            if (inputEndpoint != null || outputEndpoint != null) {
                result.add(new UsbMidiDevice(usbDevice, usbDeviceConnection, usbInterface, inputEndpoint, outputEndpoint));
            }
		}

		return Collections.unmodifiableSet(result);
	}
}
