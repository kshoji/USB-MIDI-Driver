package jp.kshoji.javax.sound.midi;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

import jp.kshoji.driver.midi.device.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.javax.sound.midi.usb.UsbMidiDevice;

/**
 * {@link jp.kshoji.javax.sound.midi.MidiSystem} initializer / terminator for Android USB MIDI.
 *
 * @author K.Shoji
 */
public final class UsbMidiSystem implements OnMidiDeviceAttachedListener, OnMidiDeviceDetachedListener {
    private final Context context;
    private final Map<String, UsbMidiDevice> midiDeviceMap = new HashMap<String, UsbMidiDevice>();

    private MidiDeviceConnectionWatcher deviceConnectionWatcher;
    private UsbManager usbManager;

    /**
     * Constructor
     *
     * @param context the context
     */
    public UsbMidiSystem(@NonNull Context context) {
        this.context = context.getApplicationContext();
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
        synchronized (midiDeviceMap) {
            for (Map.Entry<String, UsbMidiDevice> midiDeviceEntry  : midiDeviceMap.entrySet()) {
                midiDeviceEntry.getValue().close();
            }

            midiDeviceMap.clear();
        }

        deviceConnectionWatcher.stop();
        deviceConnectionWatcher = null;

        usbManager = null;
    }

    @Override
    public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
        // deprecated method.
        // do nothing
    }

    @Override
    public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {
        synchronized (midiDeviceMap) {
            UsbMidiDevice existingDevice = midiDeviceMap.get(midiInputDevice.getDeviceAddress());
            if (existingDevice != null) {
                existingDevice.addMidiInputDevice(midiInputDevice);
                MidiSystem.addMidiDevice(existingDevice);
            } else {
                UsbMidiDevice midiDevice = new UsbMidiDevice(midiInputDevice, null);
                midiDeviceMap.put(midiInputDevice.getDeviceAddress(), midiDevice);
                MidiSystem.addMidiDevice(midiDevice);
            }
        }
    }

    @Override
    public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {
        synchronized (midiDeviceMap) {
            UsbMidiDevice existingDevice = midiDeviceMap.get(midiOutputDevice.getDeviceAddress());
            if (existingDevice != null) {
                existingDevice.addMidiOutputDevice(midiOutputDevice);
                MidiSystem.addMidiDevice(existingDevice);
            } else {
                UsbMidiDevice midiDevice = new UsbMidiDevice(null, midiOutputDevice);
                midiDeviceMap.put(midiOutputDevice.getDeviceAddress(), midiDevice);
                MidiSystem.addMidiDevice(midiDevice);
            }
        }
    }

    @Override
    public void onDeviceDetached(@NonNull UsbDevice usbDevice) {
        // deprecated method.
        // do nothing
    }

    @Override
    public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {
        synchronized (midiDeviceMap) {
            UsbMidiDevice existingDevice = midiDeviceMap.get(midiInputDevice.getDeviceAddress());
            if (existingDevice != null) {
                existingDevice.removeMidiInputDevice(midiInputDevice);

                if (existingDevice.getMidiOutputDevices().isEmpty() && existingDevice.getMidiInputDevices().isEmpty()) {
                    existingDevice.close();

                    // both of devices are disconnected
                    midiDeviceMap.remove(midiInputDevice.getDeviceAddress());
                    MidiSystem.removeMidiDevice(existingDevice);
                }
            }
        }
    }

    @Override
    public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {
        synchronized (midiDeviceMap) {
            UsbMidiDevice existingDevice = midiDeviceMap.get(midiOutputDevice.getDeviceAddress());
            if (existingDevice != null) {
                existingDevice.removeMidiOutputDevice(midiOutputDevice);

                if (existingDevice.getMidiOutputDevices().isEmpty() && existingDevice.getMidiInputDevices().isEmpty()) {
                    existingDevice.close();

                    // both of devices are disconnected
                    midiDeviceMap.remove(midiOutputDevice.getDeviceAddress());
                    MidiSystem.removeMidiDevice(existingDevice);
                }
            }
        }
    }
}
