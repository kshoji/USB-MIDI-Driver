package jp.kshoji.javax.sound.midi.usb;

import android.hardware.usb.UsbDevice;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.javax.sound.midi.MidiDevice;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.Transmitter;

/**
 * {@link jp.kshoji.javax.sound.midi.MidiDevice} implementation
 *
 * @author K.Shoji
 */
public final class UsbMidiDevice implements MidiDevice {

    private final Map<MidiOutputDevice, Receiver> receivers = new HashMap<MidiOutputDevice, Receiver>();
    private final Map<MidiInputDevice, Transmitter> transmitters = new HashMap<MidiInputDevice, Transmitter>();

    private boolean isOpened;

    private Info cachedInfo = null;

    /**
     * Constructor
     *
     * @param midiInputDevice  the MidiInputDevice
     * @param midiOutputDevice the MidiOutputDevice
     */
    public UsbMidiDevice(@Nullable final MidiInputDevice midiInputDevice, @Nullable final MidiOutputDevice midiOutputDevice) {
        if (midiInputDevice == null && midiOutputDevice == null) {
            throw new NullPointerException("Both of MidiInputDevice and MidiOutputDevice are null.");
        }

        if (midiOutputDevice != null) {
            receivers.put(midiOutputDevice, new UsbMidiReceiver(this, midiOutputDevice));
        }

        if (midiInputDevice != null) {
            transmitters.put(midiInputDevice, new UsbMidiTransmitter(this, midiInputDevice));
        }

        isOpened = false;

        try {
            open();
        } catch (final MidiUnavailableException e) {
            Log.e(Constants.TAG, e.getMessage(), e);
        }
    }

    @NonNull
    @Override
    public Info getDeviceInfo() {
        if (cachedInfo != null) {
            return cachedInfo;
        }
        UsbDevice usbDevice = null;

        for (final MidiInputDevice midiInputDevice : transmitters.keySet()) {
            usbDevice = midiInputDevice.getUsbDevice();
            break;
        }
        if (usbDevice == null) {
            for (final MidiOutputDevice midiOutputDevice : receivers.keySet()) {
                usbDevice = midiOutputDevice.getUsbDevice();
                break;
            }
        }

        if (usbDevice == null) {
            // XXX returns `null` information
            return cachedInfo = new Info("(null)", "(null)", "(null)", "(null)");
        }

        return cachedInfo = new Info(usbDevice.getDeviceName(), //
                String.format("vendorId: %x, productId: %x", usbDevice.getVendorId(), usbDevice.getProductId()), //
                "deviceId:" + usbDevice.getDeviceId(), //
                usbDevice.getDeviceName());
    }

    @Override
    public void open() throws MidiUnavailableException {
        if (isOpened) {
            return;
        }

        for (final Receiver receiver : receivers.values()) {
            if (receiver instanceof UsbMidiReceiver) {
                final UsbMidiReceiver usbMidiReceiver = (UsbMidiReceiver) receiver;
                // claimInterface will be called
                usbMidiReceiver.open();
            }
        }
        for (final Transmitter transmitter : transmitters.values()) {
            if (transmitter instanceof UsbMidiTransmitter) {
                final UsbMidiTransmitter usbMidiTransmitter = (UsbMidiTransmitter) transmitter;
                // claimInterface will be called
                usbMidiTransmitter.open();
            }
        }
        isOpened = true;
    }

    @Override
    public void close() {
        if (!isOpened) {
            return;
        }

        for (final Transmitter transmitter : transmitters.values()) {
            transmitter.close();
        }
        transmitters.clear();
        for (final Receiver receiver : receivers.values()) {
            receiver.close();
        }
        receivers.clear();

        isOpened = false;
    }

    @Override
    public boolean isOpen() {
        return isOpened;
    }

    @Override
    public long getMicrosecondPosition() {
        // time-stamping is not supported
        return -1;
    }

    @Override
    public int getMaxReceivers() {
        return receivers.size();
    }

    @Override
    public int getMaxTransmitters() {
        return transmitters.size();
    }

    @NonNull
    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        for (final Receiver receiver : receivers.values()) {
            // returns first one
            return receiver;
        }

        throw new MidiUnavailableException("Receiver not found");
    }

    @NonNull
    @Override
    public List<Receiver> getReceivers() {
        final List<Receiver> result = new ArrayList<>();
        result.addAll(receivers.values());

        return Collections.unmodifiableList(result);
    }

    @NonNull
    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        for (final Transmitter transmitter : transmitters.values()) {
            // returns first one
            return transmitter;
        }

        throw new MidiUnavailableException("Transmitter not found");
    }

    @NonNull
    @Override
    public List<Transmitter> getTransmitters() {
        final List<Transmitter> result = new ArrayList<>();
        result.addAll(transmitters.values());

        return Collections.unmodifiableList(result);
    }

    /**
     * Add a MidiInputDevice to this device
     *
     * @param midiInputDevice the MidiInputDevice to add
     */
    public void addMidiInputDevice(@NonNull final MidiInputDevice midiInputDevice) {
        if (!transmitters.containsKey(midiInputDevice)) {
            final UsbMidiTransmitter transmitter = new UsbMidiTransmitter(this, midiInputDevice);
            transmitters.put(midiInputDevice, transmitter);
            transmitter.open();
        }
    }

    /**
     * Remove a MidiInputDevice from this device
     *
     * @param midiInputDevice the MidiInputDevice to remove
     */
    public void removeMidiInputDevice(@NonNull final MidiInputDevice midiInputDevice) {
        if (transmitters.containsKey(midiInputDevice)) {
            final Transmitter transmitter = transmitters.remove(midiInputDevice);
            transmitter.close();
        }
    }

    /**
     * Get all connected MidiInputDevice
     *
     * @return the set of MidiInputDevice
     */
    @NonNull
    public Set<MidiInputDevice> getMidiInputDevices() {
        return Collections.unmodifiableSet(transmitters.keySet());
    }

    /**
     * Add a MidiOutputDevice to this device
     *
     * @param midiOutputDevice the MidiOutputDevice to add
     */
    public void addMidiOutputDevice(@NonNull final MidiOutputDevice midiOutputDevice) {
        if (!receivers.containsKey(midiOutputDevice)) {
            final UsbMidiReceiver receiver = new UsbMidiReceiver(this, midiOutputDevice);
            receivers.put(midiOutputDevice, receiver);
            receiver.open();
        }
    }

    /**
     * Remove a MidiOutputDevice from this device
     *
     * @param midiOutputDevice the MidiOutputDevice to remove
     */
    public void removeMidiOutputDevice(@NonNull final MidiOutputDevice midiOutputDevice) {
        if (receivers.containsKey(midiOutputDevice)) {
            final Receiver receiver = receivers.remove(midiOutputDevice);
            receiver.close();
        }
    }

    /**
     * Get all connected MidiOutputDevice
     *
     * @return the set of MidiOutputDevice
     */
    @NonNull
    public Set<MidiOutputDevice> getMidiOutputDevices() {
        return Collections.unmodifiableSet(receivers.keySet());
    }
}
