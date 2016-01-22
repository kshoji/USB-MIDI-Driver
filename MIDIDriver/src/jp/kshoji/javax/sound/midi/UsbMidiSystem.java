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
import jp.kshoji.javax.sound.midi.usb.UsbMidiSynthesizer;

/**
 * {@link jp.kshoji.javax.sound.midi.MidiSystem} initializer / terminator for Android USB MIDI.
 *
 * @author K.Shoji
 */
public final class UsbMidiSystem implements OnMidiDeviceAttachedListener, OnMidiDeviceDetachedListener {
    private final Context context;
    private final Map<String, UsbMidiDevice> midiDeviceMap = new HashMap<>();
    private final Map<String, UsbMidiSynthesizer> midiSynthesizerMap = new HashMap<>();

    private MidiDeviceConnectionWatcher deviceConnectionWatcher;
    private UsbManager usbManager;

    /**
     * Constructor
     *
     * @param context the context
     */
    public UsbMidiSystem(@NonNull final Context context) {
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
            for (final Map.Entry<String, UsbMidiDevice> midiDeviceEntry  : midiDeviceMap.entrySet()) {
                midiDeviceEntry.getValue().close();
            }

            midiDeviceMap.clear();
        }

        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.stop();
            deviceConnectionWatcher = null;
        }

        usbManager = null;
    }

    @Override
    public void onDeviceAttached(@NonNull final UsbDevice usbDevice) {
        // deprecated method.
        // do nothing
    }

    @Override
    public void onMidiInputDeviceAttached(@NonNull final MidiInputDevice midiInputDevice) {
        final UsbMidiDevice addedDevice;
        synchronized (midiDeviceMap) {
            final UsbMidiDevice existingDevice = midiDeviceMap.get(midiInputDevice.getDeviceAddress());
            if (existingDevice != null) {
                addedDevice = existingDevice;
                existingDevice.addMidiInputDevice(midiInputDevice);
                MidiSystem.addMidiDevice(existingDevice);
            } else {
                final UsbMidiDevice midiDevice = new UsbMidiDevice(midiInputDevice, null);
                addedDevice = midiDevice;
                midiDeviceMap.put(midiInputDevice.getDeviceAddress(), midiDevice);
                MidiSystem.addMidiDevice(midiDevice);
            }
        }

        synchronized (midiSynthesizerMap) {
            final UsbMidiSynthesizer existingSynthesizer = midiSynthesizerMap.get(midiInputDevice.getDeviceAddress());
            if (existingSynthesizer == null) {
                final UsbMidiSynthesizer synthesizer = new UsbMidiSynthesizer(addedDevice);
                MidiSystem.addSynthesizer(synthesizer);
                midiSynthesizerMap.put(midiInputDevice.getDeviceAddress(), synthesizer);
            }
        }
    }

    @Override
    public void onMidiOutputDeviceAttached(@NonNull final MidiOutputDevice midiOutputDevice) {
        final UsbMidiDevice addedDevice;
        synchronized (midiDeviceMap) {
            final UsbMidiDevice existingDevice = midiDeviceMap.get(midiOutputDevice.getDeviceAddress());
            if (existingDevice == null) {
                final UsbMidiDevice midiDevice = new UsbMidiDevice(null, midiOutputDevice);
                addedDevice = midiDevice;
                midiDeviceMap.put(midiOutputDevice.getDeviceAddress(), midiDevice);
                MidiSystem.addMidiDevice(midiDevice);
            } else {
                addedDevice = existingDevice;
                existingDevice.addMidiOutputDevice(midiOutputDevice);
                MidiSystem.addMidiDevice(existingDevice);
            }
        }

        synchronized (midiSynthesizerMap) {
            final UsbMidiSynthesizer existingSynthesizer = midiSynthesizerMap.get(midiOutputDevice.getDeviceAddress());
            if (existingSynthesizer == null) {
                final UsbMidiSynthesizer synthesizer = new UsbMidiSynthesizer(addedDevice);
                midiSynthesizerMap.put(midiOutputDevice.getDeviceAddress(), synthesizer);
                MidiSystem.addSynthesizer(synthesizer);
            } else {
                try {
                    existingSynthesizer.setReceiver(addedDevice.getReceiver());
                } catch (final MidiUnavailableException ignored) {
                }
            }
        }
    }

    @Override
    public void onDeviceDetached(@NonNull final UsbDevice usbDevice) {
        // deprecated method.
        // do nothing
    }

    @Override
    public void onMidiInputDeviceDetached(@NonNull final MidiInputDevice midiInputDevice) {
        String removedDeviceAddress = null;
        synchronized (midiDeviceMap) {
            final UsbMidiDevice existingDevice = midiDeviceMap.get(midiInputDevice.getDeviceAddress());
            if (existingDevice != null) {
                existingDevice.removeMidiInputDevice(midiInputDevice);

                if (existingDevice.getMidiOutputDevices().isEmpty() && existingDevice.getMidiInputDevices().isEmpty()) {
                    existingDevice.close();

                    // both of devices are disconnected
                    removedDeviceAddress = midiInputDevice.getDeviceAddress();
                    midiDeviceMap.remove(midiInputDevice.getDeviceAddress());
                    MidiSystem.removeMidiDevice(existingDevice);
                }
            }
        }

        if (removedDeviceAddress != null) {
            synchronized (midiSynthesizerMap) {
                MidiSystem.removeSynthesizer(midiSynthesizerMap.get(removedDeviceAddress));
                midiSynthesizerMap.remove(removedDeviceAddress);
            }
        }
    }

    @Override
    public void onMidiOutputDeviceDetached(@NonNull final MidiOutputDevice midiOutputDevice) {
        String removedDeviceAddress = null;
        synchronized (midiDeviceMap) {
            final UsbMidiDevice existingDevice = midiDeviceMap.get(midiOutputDevice.getDeviceAddress());
            if (existingDevice != null) {
                existingDevice.removeMidiOutputDevice(midiOutputDevice);

                if (existingDevice.getMidiOutputDevices().isEmpty() && existingDevice.getMidiInputDevices().isEmpty()) {
                    existingDevice.close();

                    // both of devices are disconnected
                    removedDeviceAddress = midiOutputDevice.getDeviceAddress();
                    midiDeviceMap.remove(midiOutputDevice.getDeviceAddress());
                    MidiSystem.removeMidiDevice(existingDevice);
                }
            }
        }

        if (removedDeviceAddress != null) {
            synchronized (midiSynthesizerMap) {
                MidiSystem.removeSynthesizer(midiSynthesizerMap.get(removedDeviceAddress));
                midiSynthesizerMap.remove(removedDeviceAddress);
            }
        }
    }
}
