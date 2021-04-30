package jp.kshoji.driver.midi.util;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jp.kshoji.driver.midi.device.Midi2InputDevice;
import jp.kshoji.driver.midi.device.Midi2OutputDevice;
import jp.kshoji.driver.midi.device.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidi2InputEventListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;

/**
 * Driver for USB MIDI devices.
 *
 * @author K.Shoji
 */
public abstract class UsbMidiDriver implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener, OnMidi2InputEventListener {
    private boolean isOpen = false;

    Set<Midi2InputDevice> midi2InputDevices = null;
    Set<Midi2OutputDevice> midi2OutputDevices = null;

    Set<UsbDevice> connectedUsbDevices = null;
    Set<MidiInputDevice> midiInputDevices = null;
    Set<MidiOutputDevice> midiOutputDevices = null;

    /**
     * Get MIDI 2.0 output device for specified UsbDevice, if available.
     *
     * @param usbDevice the UsbDevice
     * @return {@link Set<Midi2OutputDevice>}
     */
    @NonNull
    public final Set<Midi2OutputDevice> getMidi2OutputDevices(@NonNull UsbDevice usbDevice) {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }

        Set<Midi2OutputDevice> result = new HashSet<>();
        for (Midi2OutputDevice midi2OutputDevice : midi2OutputDevices) {
            if (midi2OutputDevice.getUsbDevice().equals(usbDevice)) {
                result.add(midi2OutputDevice);
            }
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Implementation for multiple device connections.
     *
     * @author K.Shoji
     */
    final class OnMidiDeviceAttachedListenerImpl implements OnMidiDeviceAttachedListener {

        @Override
        public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
            connectedUsbDevices.add(usbDevice);
            UsbMidiDriver.this.onDeviceAttached(usbDevice);
        }

        @Override
        public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {
            if (midiInputDevices != null) {
                midiInputDevices.add(midiInputDevice);
            }
            midiInputDevice.setMidiEventListener(UsbMidiDriver.this);

            UsbMidiDriver.this.onMidiInputDeviceAttached(midiInputDevice);
        }

        @Override
        public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {
            if (midiOutputDevices != null) {
                midiOutputDevices.add(midiOutputDevice);
            }

            UsbMidiDriver.this.onMidiOutputDeviceAttached(midiOutputDevice);
        }

        @Override
        public void onMidi2InputDeviceAttached(@NonNull Midi2InputDevice midi2InputDevice) {
            if (midi2InputDevices != null) {
                midi2InputDevices.add(midi2InputDevice);
            }
            midi2InputDevice.setMidiEventListener(UsbMidiDriver.this);

            UsbMidiDriver.this.onMidi2InputDeviceAttached(midi2InputDevice);
        }

        @Override
        public void onMidi2OutputDeviceAttached(@NonNull Midi2OutputDevice midi2OutputDevice) {
            if (midi2OutputDevices != null) {
                midi2OutputDevices.add(midi2OutputDevice);
            }

            UsbMidiDriver.this.onMidi2OutputDeviceAttached(midi2OutputDevice);
        }
    }
    OnMidiDeviceAttachedListener deviceAttachedListener = null;
    OnMidiDeviceDetachedListener deviceDetachedListener = null;
    MidiDeviceConnectionWatcher deviceConnectionWatcher = null;

    private final Context context;

    /**
     * Constructor
     *
     * @param context Activity context
     */
    protected UsbMidiDriver(@NonNull Context context) {
        this.context = context;
    }

    /**
     * Starts using UsbMidiDriver.
     *
     * Starts the USB device watching and communicating thread.
     */
    public final void open() {
        if (isOpen) {
            // already opened
            return;
        }
        isOpen = true;

        connectedUsbDevices = new HashSet<>();
        midiInputDevices = new HashSet<>();
        midiOutputDevices = new HashSet<>();
        midi2InputDevices = new HashSet<>();
        midi2OutputDevices = new HashSet<>();

        UsbManager usbManager = (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE);
        deviceAttachedListener = new OnMidiDeviceAttachedListenerImpl();
        deviceDetachedListener = new OnMidiDeviceDetachedListenerImpl();

        deviceConnectionWatcher = new MidiDeviceConnectionWatcher(context.getApplicationContext(), usbManager, deviceAttachedListener, deviceDetachedListener);
    }

    /**
     * Stops using UsbMidiDriver.
     *
     * Shutdown the USB device communicating thread.
     * The all connected devices will be closed.
     */
    public final void close() {
        if (!isOpen) {
            // already closed
            return;
        }
        isOpen = false;

        deviceConnectionWatcher.stop(new Runnable() {
            @Override
            public void run() {
                deviceConnectionWatcher = null;

                if (midiInputDevices != null) {
                    midiInputDevices.clear();
                }
                midiInputDevices = null;

                if (midiOutputDevices != null) {
                    midiOutputDevices.clear();
                }
                midiOutputDevices = null;

                if (midi2InputDevices != null) {
                    midi2InputDevices.clear();
                }
                midi2InputDevices = null;

                if (midi2OutputDevices != null) {
                    midi2OutputDevices.clear();
                }
                midi2OutputDevices = null;

                if (connectedUsbDevices != null) {
                    connectedUsbDevices.clear();
                }
                connectedUsbDevices = null;
            }
        });
    }

    /**
     * Suspends receiving/transmitting MIDI messages.
     * All events will be discarded until the devices being resumed.
     */
    protected final void suspend() {
        if (midiInputDevices != null) {
            for (MidiInputDevice inputDevice : midiInputDevices) {
                if (inputDevice != null) {
                    inputDevice.suspend();
                }
            }
        }

        if (midiOutputDevices != null) {
            for (MidiOutputDevice outputDevice : midiOutputDevices) {
                if (outputDevice != null) {
                    outputDevice.suspend();
                }
            }
        }
    }

    /**
     * Resumes from {@link #suspend()}
     */
    protected final void resume() {
        if (midiInputDevices != null) {
            for (MidiInputDevice inputDevice : midiInputDevices) {
                if (inputDevice != null) {
                    inputDevice.resume();
                }
            }
        }

        if (midiOutputDevices != null) {
            for (MidiOutputDevice outputDevice : midiOutputDevices) {
                if (outputDevice != null) {
                    outputDevice.resume();
                }
            }
        }
    }

    /**
     * Get connected USB MIDI devices.
     *
     * @return connected UsbDevice set
     */
    @NonNull
    public final Set<UsbDevice> getConnectedUsbDevices() {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }
        if (connectedUsbDevices != null) {
            return Collections.unmodifiableSet(connectedUsbDevices);
        }

        return Collections.unmodifiableSet(new HashSet<UsbDevice>());
    }

    /**
     * Get MIDI output device for specified UsbDevice, if available.
     *
     * @param usbDevice the UsbDevice
     * @return {@link Set<MidiOutputDevice>}
     */
    @NonNull
    public final Set<MidiOutputDevice> getMidiOutputDevices(@NonNull UsbDevice usbDevice) {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }

        Set<MidiOutputDevice> result = new HashSet<>();
        for (MidiOutputDevice midiOutputDevice : midiOutputDevices) {
            if (midiOutputDevice.getUsbDevice().equals(usbDevice)) {
                result.add(midiOutputDevice);
            }
        }

        return Collections.unmodifiableSet(result);
    }

    /**
     * Implementation for multiple device connections.
     *
     * @author K.Shoji
     */
    final class OnMidiDeviceDetachedListenerImpl implements OnMidiDeviceDetachedListener {

        @Override
        public void onDeviceDetached(@NonNull UsbDevice usbDevice) {
            connectedUsbDevices.remove(usbDevice);
            UsbMidiDriver.this.onDeviceDetached(usbDevice);
        }

        @Override
        public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {
            if (midiInputDevices != null) {
                midiInputDevices.remove(midiInputDevice);
            }
            midiInputDevice.setMidiEventListener(null);

            UsbMidiDriver.this.onMidiInputDeviceDetached(midiInputDevice);
        }

        @Override
        public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {
            if (midiOutputDevices != null) {
                midiOutputDevices.remove(midiOutputDevice);
            }

            UsbMidiDriver.this.onMidiOutputDeviceDetached(midiOutputDevice);
        }

        @Override
        public void onMidi2InputDeviceDetached(@NonNull Midi2InputDevice midiInputDevice) {
            if (midi2InputDevices != null) {
                midi2InputDevices.remove(midiInputDevice);
            }
            midiInputDevice.setMidiEventListener(null);

            UsbMidiDriver.this.onMidi2InputDeviceDetached(midiInputDevice);
        }

        @Override
        public void onMidi2OutputDeviceDetached(@NonNull Midi2OutputDevice midiOutputDevice) {
            if (midi2OutputDevices != null) {
                midi2OutputDevices.remove(midiOutputDevice);
            }

            UsbMidiDriver.this.onMidi2OutputDeviceDetached(midiOutputDevice);
        }
    }

    /**
     * Get the all MIDI output devices.
     *
     * @return {@link Set<MidiOutputDevice>}
     */
    @NonNull
    public final Set<MidiOutputDevice> getMidiOutputDevices() {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }

        return Collections.unmodifiableSet(midiOutputDevices);
    }

    @Override
    public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
        // do nothing in this implementation
    }

    @Override
    public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
        // do nothing in this implementation
    }

    @Override
    public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
        // do nothing in this implementation
    }

    @Override
    public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
        // do nothing in this implementation
    }
}
