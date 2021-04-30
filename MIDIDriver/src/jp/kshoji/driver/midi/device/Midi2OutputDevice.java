package jp.kshoji.driver.midi.device;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.LinkedList;
import java.util.Queue;

import jp.kshoji.driver.midi.util.MidiCapabilityNegotiator;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;

/**
 * MIDI 2.0 Output Device
 *
 * @author K.Shoji
 */
public final class Midi2OutputDevice {

    private static final int BUFFER_POOL_SIZE = 1024;
    final UsbDeviceConnection usbDeviceConnection;
    final UsbEndpoint outputEndpoint;
    final WaiterThread waiterThread;
    final LinkedList<byte[]> bufferPool4 = new LinkedList<>();
    final LinkedList<byte[]> bufferPool8 = new LinkedList<>();
    final LinkedList<byte[]> bufferPool16 = new LinkedList<>();
    private final UsbDevice usbDevice;
    private final UsbInterface usbInterface;
    private MidiCapabilityNegotiator.MidiProtocol midiProtocol;
    private MidiCapabilityNegotiator.MidiExtension midiExtension;

    /**
     * Constructor
     *
     * @param usbDevice           the UsbDevice
     * @param usbDeviceConnection the UsbDeviceConnection
     * @param usbInterface        the UsbInterface
     * @param usbEndpoint         the UsbEndpoint
     */
    public Midi2OutputDevice(@NonNull UsbDevice usbDevice, @NonNull UsbDeviceConnection usbDeviceConnection, @NonNull UsbInterface usbInterface, @NonNull UsbEndpoint usbEndpoint) {
        this.usbDevice = usbDevice;
        this.usbDeviceConnection = usbDeviceConnection;
        this.usbInterface = usbInterface;

        waiterThread = new WaiterThread();

        outputEndpoint = usbEndpoint;

        this.usbDeviceConnection.claimInterface(this.usbInterface, true);

        waiterThread.setName("MidiOutputDevice[" + usbDevice.getDeviceName() + "].Midi2OutputWaiterThread");
        waiterThread.start();

        for (int i = 0; i < BUFFER_POOL_SIZE; i++) {
            bufferPool4.addLast(new byte[4]);
            bufferPool8.addLast(new byte[8]);
            bufferPool16.addLast(new byte[16]);
        }
    }

    /**
     * stop to use this device.
     */
    void stop(Runnable onStopped) {
        usbDeviceConnection.releaseInterface(usbInterface);

        waiterThread.stopThread(onStopped);
    }

    /**
     * Suspends event sending
     */
    public void suspend() {
        waiterThread.suspendFlag = true;
        waiterThread.interrupt();
    }

    /**
     * Resumes event sending
     */
    public void resume() {
        waiterThread.suspendFlag = false;
        waiterThread.interrupt();
    }

    /**
     * Get the product name
     *
     * @return the product name. null if API Level < {@link Build.VERSION_CODES#HONEYCOMB_MR2 }, or the product name is truly null
     */
    @Nullable
    public String getProductName() {
        return UsbMidiDeviceUtils.getProductName(usbDevice, usbDeviceConnection);
    }

    /**
     * Get the manufacturer name
     *
     * @return the manufacturer name. null if API Level < {@link Build.VERSION_CODES#HONEYCOMB_MR2 }, or the manufacturer name is truly null
     */
    @Nullable
    public String getManufacturerName() {
        return UsbMidiDeviceUtils.getManufacturerName(usbDevice, usbDeviceConnection);
    }

    /**
     * Get the device name(linux device path)
     *
     * @return the device name(linux device path)
     */
    @NonNull
    public String getDeviceAddress() {
        return usbDevice.getDeviceName();
    }

    /**
     * @return the usbDevice
     */
    @NonNull
    public UsbDevice getUsbDevice() {
        return usbDevice;
    }

    /**
     * @return the usbInterface
     */
    @Deprecated
    @NonNull
    public UsbInterface getUsbInterface() {
        return usbInterface;
    }

    /**
     * @return the usbEndpoint
     */
    @Deprecated
    @NonNull
    public UsbEndpoint getUsbEndpoint() {
        return outputEndpoint;
    }

    /**
     * Sets capable MIDI Protocol, and Extension
     *
     * @param midiProtocol  the MidiProtocol
     * @param midiExtension the MidiExtension
     */
    public void setProtocolInformation(MidiCapabilityNegotiator.MidiProtocol midiProtocol, MidiCapabilityNegotiator.MidiExtension midiExtension) {
        this.midiProtocol = midiProtocol;
        this.midiExtension = midiExtension;
    }

    /**
     * Obtains using MIDI Protocol version
     * @return the MidiProtocol
     */
    public MidiCapabilityNegotiator.MidiProtocol getMidiProtocol() {
        return midiProtocol;
    }

    /**
     * Obtains using MIDI Extension specification
     * @return the MidiExtension
     */
    public MidiCapabilityNegotiator.MidiExtension getMidiExtension() {
        return midiExtension;
    }

    /**
     * Sends Universal MIDI Packet(32bits) to output device.
     *
     * @param messageType the message type 0, 1, or 2
     * @param group       the group ID 0-15
     * @param byte1       first byte 0-255
     * @param byte2       second byte 0-255
     * @param byte3       third byte 0-255
     */
    private void sendMidi2Message4bytes(int messageType, int group, int byte1, int byte2, int byte3) {
        while (bufferPool4.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {

            }
        }

        synchronized (waiterThread.queue) {
            byte[] writeBuffer = bufferPool4.removeFirst();

            writeBuffer[0] = (byte) (((messageType & 0xf) << 4) | (group & 0xf));
            writeBuffer[1] = (byte) byte1;
            writeBuffer[2] = (byte) byte2;
            writeBuffer[3] = (byte) byte3;

            waiterThread.queue.add(writeBuffer);
        }

        // message has been queued, so interrupt the waiter thread
        waiterThread.interrupt();
    }

    /**
     * NOOP
     *
     * @param group the group ID 0-15
     */
    public void sendMidiNoop(int group) {
        sendMidi2Message4bytes(0, group, 0, 0, 0);
    }

    // region messageType 0

    /**
     * JR Clock
     *
     * @param group           the group ID 0-15
     * @param senderClockTime 16-bit time value in clock ticks of 1/31250 of one second
     */
    public void sendMidiJitterReductionClock(int group, int senderClockTime) {
        sendMidi2Message4bytes(0, group, 0x10, (senderClockTime >> 8) & 0xff, senderClockTime & 0xff);
    }

    /**
     * JR Timestamp
     *
     * @param group                the group ID 0-15
     * @param senderClockTimestamp 16-bit time value in clock ticks of 1/31250 of one second
     */
    public void sendMidiJitterReductionTimestamp(int group, int senderClockTimestamp) {
        sendMidi2Message4bytes(0, group, 0x20, (senderClockTimestamp >> 8) & 0xff, senderClockTimestamp & 0xff);
    }

    /**
     * System Common messages
     *
     * @param group the group ID 0-15
     * @param bytes bytes.length:1, 2, or 3
     */
    private void sendMidiSystemCommonMessage(int group, byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return;
        }
        sendMidi2Message4bytes(1, group,
                bytes[0] & 0xff,
                bytes.length > 1 ? bytes[1] & 0xff : 0,
                bytes.length > 2 ? bytes[2] & 0xff : 0);
    }

    // endregion

    // region messageType 1

    /**
     * MIDI Time Code(MTC) Quarter Frame
     *
     * @param timing 0-127
     */
    public final void sendMidiTimeCodeQuarterFrame(int group, int timing) {
        sendMidiSystemCommonMessage(group, new byte[]{(byte) 0xf1, (byte) (timing & 0x7f)});
    }

    /**
     * Song Select
     *
     * @param song 0-127
     */
    public final void sendMidiSongSelect(int group, int song) {
        sendMidiSystemCommonMessage(group, new byte[]{(byte) 0xf3, (byte) (song & 0x7f)});
    }

    /**
     * Song Position Pointer
     *
     * @param position 0-16383
     */
    public final void sendMidiSongPositionPointer(int group, int position) {
        sendMidiSystemCommonMessage(group, new byte[]{(byte) 0xf2, (byte) (position & 0x7f), (byte) ((position >> 7) & 0x7f)});
    }

    /**
     * Tune Request
     */
    public final void sendMidiTuneRequest(int group) {
        sendMidi2Message4bytes(1, group, 0xf6, 0, 0);
    }

    /**
     * Timing Clock
     */
    public final void sendMidiTimingClock(int group) {
        sendMidi2Message4bytes(1, group, 0xf8, 0, 0);
    }

    /**
     * Start Playing
     */
    public final void sendMidiStart(int group) {
        sendMidi2Message4bytes(1, group, 0xfa, 0, 0);
    }

    /**
     * Continue Playing
     */
    public final void sendMidiContinue(int group) {
        sendMidi2Message4bytes(1, group, 0xfb, 0, 0);
    }

    /**
     * Stop Playing
     */
    public final void sendMidiStop(int group) {
        sendMidi2Message4bytes(1, group, 0xfc, 0, 0);
    }

    /**
     * Active Sensing
     */
    public final void sendMidiActiveSensing(int group) {
        sendMidi2Message4bytes(1, group, 0xfe, 0, 0);
    }

    /**
     * Reset Device
     */
    public final void sendMidiReset(int group) {
        sendMidi2Message4bytes(1, group, 0xff, 0, 0);
    }

    /**
     * Note-off
     *
     * @param group    0-15
     * @param channel  0-15
     * @param note     0-127
     * @param velocity 0-127
     */
    public void sendMidi1NoteOff(int group, int channel, int note, int velocity) {
        sendMidi2Message4bytes(2, group, 0x80 | (channel & 0xf), note & 0x7f, velocity & 0x7f);
    }

    // endregion

    // region messageType 2

    /**
     * Note-on
     *
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param note     0-127
     * @param velocity 0-127
     */
    public void sendMidi1NoteOn(int group, int channel, int note, int velocity) {
        sendMidi2Message4bytes(2, group, 0x90 | (channel & 0xf), note & 0x7f, velocity & 0x7f);
    }

    /**
     * Poly-KeyPress
     *
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param note     0-127
     * @param pressure 0-127
     */
    public void sendMidi1PolyphonicAftertouch(int group, int channel, int note, int pressure) {
        sendMidi2Message4bytes(2, group, 0xa0 | (channel & 0xf), note & 0x7f, pressure & 0x7f);
    }

    /**
     * Control Change
     *
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param function 0-127
     * @param value    0-127
     */
    public void sendMidi1ControlChange(int group, int channel, int function, int value) {
        sendMidi2Message4bytes(2, group, 0xb0 | (channel & 0xf), function & 0x7f, value & 0x7f);
    }

    /**
     * Program Change
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param program 0-127
     */
    public void sendMidi1ProgramChange(int group, int channel, int program) {
        sendMidi2Message4bytes(2, group, 0xc0 | (channel & 0xf), program & 0x7f, 0);
    }

    /**
     * Channel Pressure
     *
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param pressure 0-127
     */
    public void sendMidi1ChannelAftertouch(int group, int channel, int pressure) {
        sendMidi2Message4bytes(2, group, 0xd0 | (channel & 0xf), pressure & 0x7f, 0);
    }

    /**
     * PitchBend Change
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param amount  0(low)-8192(center)-16383(high)
     */
    public void sendMidi1PitchWheel(int group, int channel, int amount) {
        sendMidi2Message4bytes(2, group, 0xe0 | (channel & 0xf), amount & 0x7f, (amount >> 7) & 0x7f);
    }

    /**
     * SysEx
     *
     * @param group           the group ID 0-15
     * @param systemExclusive raw data. Note: Status values 0xF0 and 0xF7 are not used for UMP System Exclusive
     */
    public void sendMidi1SystemExclusive(int group, @NonNull byte[] systemExclusive) {
        // remove first F0, and last F7
        int removeLength = 0;
        if ((systemExclusive[0] & 0xff) == 0xf0) {
            removeLength++;
        }
        if ((systemExclusive[systemExclusive.length - 1] & 0xff) == 0xf7) {
            removeLength++;
        }

        if ((systemExclusive.length - removeLength) > 6) {
            int sysexEnd = systemExclusive.length - ((systemExclusive[systemExclusive.length - 1] & 0xff) == 0xf7 ? 1 : 0);
            int sysexLength;
            for (int sysexIndex = (systemExclusive[0] & 0xff) == 0xf0 ? 1 : 0; sysexIndex < sysexEnd; sysexIndex += 6) {
                while (bufferPool8.isEmpty()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {

                    }
                }

                synchronized (waiterThread.queue) {
                    byte[] writeBuffer = bufferPool8.removeFirst();

                    writeBuffer[0] = (byte) (0x30 | (group & 0xf));
                    sysexLength = Math.min(sysexEnd - sysexIndex, 6);

                    if (sysexIndex == 0) {
                        // sysex starts
                        writeBuffer[1] = (byte) (0x10 | (sysexLength & 0xf));
                    } else if ((sysexIndex + 6 < sysexEnd)) {
                        // sysex continues
                        writeBuffer[1] = (byte) (0x20 | (sysexLength & 0xf));
                    } else {
                        // sysex ends
                        writeBuffer[1] = (byte) (0x30 | (sysexLength & 0xf));
                    }

                    writeBuffer[2] = systemExclusive[sysexIndex];
                    writeBuffer[3] = (sysexIndex + 1) < sysexEnd ? systemExclusive[(sysexIndex + 1)] : 0;
                    writeBuffer[4] = (sysexIndex + 2) < sysexEnd ? systemExclusive[(sysexIndex + 2)] : 0;
                    writeBuffer[5] = (sysexIndex + 3) < sysexEnd ? systemExclusive[(sysexIndex + 3)] : 0;
                    writeBuffer[6] = (sysexIndex + 4) < sysexEnd ? systemExclusive[(sysexIndex + 4)] : 0;
                    writeBuffer[7] = (sysexIndex + 5) < sysexEnd ? systemExclusive[(sysexIndex + 5)] : 0;

                    waiterThread.queue.add(writeBuffer);
                }

                // message has been queued, so interrupt the waiter thread
                waiterThread.interrupt();
            }

        } else {
            // Complete System Exclusive Message in one UMP
            int sysexIndex = (systemExclusive[0] & 0xff) == 0xf0 ? 1 : 0;
            int sysexLength = systemExclusive.length - removeLength;
            int sysexEnd = systemExclusive.length - ((systemExclusive[systemExclusive.length - 1] & 0xff) == 0xf7 ? 1 : 0);

            while (bufferPool8.isEmpty()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {

                }
            }

            synchronized (waiterThread.queue) {
                byte[] writeBuffer = bufferPool8.removeFirst();

                writeBuffer[0] = (byte) (group & 0xf);
                writeBuffer[1] = (byte) (sysexLength & 0xf);
                writeBuffer[2] = sysexIndex < sysexEnd ? systemExclusive[sysexIndex] : 0;
                writeBuffer[3] = (sysexIndex + 1) < sysexEnd ? systemExclusive[(sysexIndex + 1)] : 0;
                writeBuffer[4] = (sysexIndex + 2) < sysexEnd ? systemExclusive[(sysexIndex + 2)] : 0;
                writeBuffer[5] = (sysexIndex + 3) < sysexEnd ? systemExclusive[(sysexIndex + 3)] : 0;
                writeBuffer[6] = (sysexIndex + 4) < sysexEnd ? systemExclusive[(sysexIndex + 4)] : 0;
                writeBuffer[7] = (sysexIndex + 5) < sysexEnd ? systemExclusive[(sysexIndex + 5)] : 0;

                waiterThread.queue.add(writeBuffer);
            }

            // message has been queued, so interrupt the waiter thread
            waiterThread.interrupt();
        }
    }

    // endregion

    // region messageType 3

    /**
     * Sends Universal MIDI Packet(64bits) to output device.
     *
     * @param group the group ID 0-15
     * @param byte1 first byte 0-255
     * @param byte2 second byte 0-255
     * @param byte3 third byte 0-255
     * @param data  the data 0-4294967295
     */
    private void sendMidi2MessageType4(int group, int byte1, int byte2, int byte3, long data) {
        while (bufferPool8.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {

            }
        }

        synchronized (waiterThread.queue) {
            byte[] writeBuffer = bufferPool8.removeFirst();

            writeBuffer[0] = (byte) (0x40 | (group & 0xf));
            writeBuffer[1] = (byte) byte1;
            writeBuffer[2] = (byte) byte2;
            writeBuffer[3] = (byte) byte3;
            writeBuffer[4] = (byte) ((data >> 24) & 0xff);
            writeBuffer[5] = (byte) ((data >> 16) & 0xff);
            writeBuffer[6] = (byte) ((data >> 8) & 0xff);
            writeBuffer[7] = (byte) (data & 0xff);

            waiterThread.queue.add(writeBuffer);
        }

        // message has been queued, so interrupt the waiter thread
        waiterThread.interrupt();
    }

    // endregion

    // region messageType 4

    /**
     * Note-off
     *
     * @param group         0-15
     * @param channel       0-15
     * @param note          0-127
     * @param velocity      0-65535
     * @param attributeType 0-255
     * @param attributeData 0-65535
     */
    public void sendMidi2NoteOff(int group, int channel, int note, int velocity, int attributeType, int attributeData) {
        while (bufferPool8.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {

            }
        }

        synchronized (waiterThread.queue) {
            byte[] writeBuffer = bufferPool8.removeFirst();

            writeBuffer[0] = (byte) (0x40 | (group & 0xf));
            writeBuffer[1] = (byte) (0x80 | (channel & 0xf));
            writeBuffer[2] = (byte) (note & 0x7f);
            writeBuffer[3] = (byte) (attributeType & 0xff);
            writeBuffer[4] = (byte) ((velocity >> 8) & 0xff);
            writeBuffer[5] = (byte) (velocity & 0xff);
            writeBuffer[6] = (byte) ((attributeData >> 8) & 0xff);
            writeBuffer[7] = (byte) (attributeData & 0xff);

            waiterThread.queue.add(writeBuffer);
        }

        // message has been queued, so interrupt the waiter thread
        waiterThread.interrupt();
    }

    /**
     * Note-on
     *
     * @param group         the group ID 0-15
     * @param channel       the MIDI channel number 0-15
     * @param note          0-127
     * @param velocity      0-65535
     * @param attributeType 0-255
     * @param attributeData 0-65535
     */
    public void sendMidi2NoteOn(int group, int channel, int note, int velocity, int attributeType, int attributeData) {
        while (bufferPool8.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {

            }
        }

        synchronized (waiterThread.queue) {
            byte[] writeBuffer = bufferPool8.removeFirst();

            writeBuffer[0] = (byte) (0x40 | (group & 0xf));
            writeBuffer[1] = (byte) (0x90 | (channel & 0xf));
            writeBuffer[2] = (byte) (note & 0x7f);
            writeBuffer[3] = (byte) (attributeType & 0xff);
            writeBuffer[4] = (byte) ((velocity >> 8) & 0xff);
            writeBuffer[5] = (byte) (velocity & 0xff);
            writeBuffer[6] = (byte) ((attributeData >> 8) & 0xff);
            writeBuffer[7] = (byte) (attributeData & 0xff);

            waiterThread.queue.add(writeBuffer);
        }

        // message has been queued, so interrupt the waiter thread
        waiterThread.interrupt();
    }

    /**
     * Note-on
     *
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param note     0-127
     * @param velocity 0-65535
     */
    public void sendMidi2NoteOn(int group, int channel, int note, int velocity) {
        sendMidi2NoteOn(group, channel, note, velocity, 0, 0);
    }

    /**
     * Poly-KeyPress
     *
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param note     0-127
     * @param pressure 0-4294967295
     */
    public void sendMidi2PolyphonicAftertouch(int group, int channel, int note, long pressure) {
        sendMidi2MessageType4(group, 0xa0 | (channel & 0xf), note, 0, pressure & 0xffff);
    }

    /**
     * Control Change
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param index   0-127
     * @param value   0-4294967295
     */
    public void sendMidi2ControlChange(int group, int channel, int index, long value) {
        sendMidi2MessageType4(group, 0xb0 | (channel & 0xf), index, 0, value & 0xffff);
    }

    /**
     * Program Change
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param program 0-127
     * @param bank    0-16383
     */
    public void sendMidi2ProgramChange(int group, int channel, int optionFlags, int program, int bank) {
        while (bufferPool8.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {

            }
        }

        synchronized (waiterThread.queue) {
            byte[] writeBuffer = bufferPool8.removeFirst();

            writeBuffer[0] = (byte) (0x40 | (group & 0xf));
            writeBuffer[1] = (byte) (0xc0 | (channel & 0xf));
            writeBuffer[2] = 0;
            writeBuffer[3] = (byte) (optionFlags & 0xff);
            writeBuffer[4] = (byte) (program & 0x7f);
            writeBuffer[5] = 0;
            writeBuffer[6] = (byte) ((bank >> 7) & 0x7f);
            writeBuffer[7] = (byte) (bank & 0x7f);

            waiterThread.queue.add(writeBuffer);
        }

        // message has been queued, so interrupt the waiter thread
        waiterThread.interrupt();
    }

    /**
     * Channel Pressure
     *
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param pressure 0-4294967295
     */
    public void sendMidi2ChannelAftertouch(int group, int channel, long pressure) {
        sendMidi2MessageType4(group, 0xd0 | (channel & 0xf), 0, 0, pressure & 0xffff);
    }

    /**
     * PitchBend Change
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param amount  0(low)-2147483648(center)-4294967295(high)
     */
    public void sendMidi2PitchWheel(int group, int channel, long amount) {
        sendMidi2MessageType4(group, 0xe0 | (channel & 0xf), 0, 0, amount & 0xffff);
    }

    /**
     * Per Note PitchBend Change
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param note    0-127
     * @param amount  0(low)-2147483648(center)-4294967295(high)
     */
    public void sendMidiPerNotePitchWheel(int group, int channel, int note, long amount) {
        sendMidi2MessageType4(group, 0x60 | (channel & 0xf), note & 0x7f, 0, amount & 0xffff);
    }

    /**
     * Per Note Management
     *
     * @param group       the group ID 0-15
     * @param channel     the MIDI channel number 0-15
     * @param optionFlags 0-255
     */
    public void sendMidiPerNoteManagement(int group, int channel, int note, int optionFlags) {
        sendMidi2MessageType4(group, 0xf0 | (channel & 0xf), note & 0x7f, optionFlags & 0xff, 0L);
    }

    /**
     * Registered Per Note Controller
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param note    0-127
     * @param index   0-255
     * @param data    0-4294967295
     */
    public void sendMidiRegisteredPerNoteController(int group, int channel, int note, int index, long data) {
        sendMidi2MessageType4(group, channel & 0xf, note & 0x7f, index & 0xff, data & 0xffff);
    }

    /**
     * Assignable Per Note Controller
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param note    0-127
     * @param index   0-255
     * @param data    0-4294967295
     */
    public void sendMidiAssignablePerNoteController(int group, int channel, int note, int index, long data) {
        sendMidi2MessageType4(group, 0x10 | (channel & 0xf), note & 0x7f, index & 0xff, data & 0xffff);
    }

    /**
     * Registered Controller (RPN)
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param bank    0-127
     * @param index   0-127
     * @param data    0-4294967295
     */
    public void sendMidiRegisteredController(int group, int channel, int bank, int index, long data) {
        sendMidi2MessageType4(group, 0x20 | (channel & 0xf), bank & 0x7f, index & 0x7f, data & 0xffff);
    }

    /**
     * Assignable Controller (NRPN)
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param bank    0-127
     * @param index   0-127
     * @param data    0-4294967295
     */
    public void sendMidiAssignableController(int group, int channel, int bank, int index, long data) {
        sendMidi2MessageType4(group, 0x30 | (channel & 0xf), bank & 0x7f, index & 0x7f, data & 0xffff);
    }

    /**
     * Relative Registered Controller
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param bank    0-127
     * @param index   0-127
     * @param data    0-4294967295
     */
    public void sendMidiRelativeRegisteredController(int group, int channel, int bank, int index, long data) {
        sendMidi2MessageType4(group, 0x40 | (channel & 0xf), bank & 0x7f, index & 0x7f, data & 0xffff);
    }

    /**
     * Relative Assignable Controller
     *
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param bank    0-127
     * @param index   0-127
     * @param data    0-4294967295
     */
    public void sendMidiRelativeAssignableController(int group, int channel, int bank, int index, long data) {
        sendMidi2MessageType4(group, 0x50 | (channel & 0xf), bank & 0x7f, index & 0x7f, data & 0xffff);
    }

    /**
     * SysEx
     *
     * @param group           the group ID 0-15
     * @param streamId        the stream ID 0-255
     * @param systemExclusive raw data. Note: Status values 0xF0 and 0xF7 are not used for UMP System Exclusive
     */
    public void sendMidi2SystemExclusive(int group, int streamId, @NonNull final byte[] systemExclusive) {
        if (systemExclusive.length > 13) {
            int sysexLength;
            for (int sysexIndex = 0; sysexIndex < systemExclusive.length; sysexIndex += 13) {
                while (bufferPool16.isEmpty()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {

                    }
                }

                synchronized (waiterThread.queue) {
                    byte[] writeBuffer = bufferPool16.removeFirst();

                    writeBuffer[0] = (byte) (0x50 | (group & 0xf));
                    sysexLength = Math.min(systemExclusive.length - sysexIndex, 13);

                    if (sysexIndex == 0) {
                        // sysex starts
                        writeBuffer[1] = (byte) (0x10 | (sysexLength & 0xf));
                    } else if (sysexIndex + 13 < systemExclusive.length) {
                        // sysex continues
                        writeBuffer[1] = (byte) (0x20 | (sysexLength & 0xf));
                    } else {
                        // sysex ends
                        writeBuffer[1] = (byte) (0x30 | (sysexLength & 0xf));
                    }

                    // stream ID
                    writeBuffer[2] = (byte) (streamId & 0xff);

                    for (int i = 0; i < 13; i++) {
                        writeBuffer[3 + i] = sysexIndex + i < systemExclusive.length ? systemExclusive[sysexIndex + i] : 0;
                    }

                    waiterThread.queue.add(writeBuffer);
                }

                // message has been queued, so interrupt the waiter thread
                waiterThread.interrupt();
            }

        } else {
            int sysexLength = systemExclusive.length;

            while (bufferPool16.isEmpty()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {

                }
            }

            synchronized (waiterThread.queue) {
                byte[] writeBuffer = bufferPool16.removeFirst();

                writeBuffer[0] = (byte) (0x50 | (group & 0xf));

                // Complete System Exclusive Message in one UMP
                writeBuffer[1] = (byte) (sysexLength & 0xf);

                // stream ID
                writeBuffer[2] = (byte) (streamId & 0xff);

                for (int i = 0; i < 13; i++) {
                    writeBuffer[3 + i] = i < sysexLength ? systemExclusive[i] : 0;
                }

                waiterThread.queue.add(writeBuffer);
            }

            // message has been queued, so interrupt the waiter thread
            waiterThread.interrupt();
        }
    }

    // endregion

    // region messageType 5

    /**
     * Mixed Data Set Header
     *
     * @param group   the group ID 0-15
     * @param mdsId   mixed data set ID 0-255
     * @param headers mixed data set header data
     */
    public void sendMidiMixedDataSetHeader(int group, int mdsId, @NonNull final byte[] headers) {
        while (bufferPool16.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {

            }
        }

        synchronized (waiterThread.queue) {
            byte[] writeBuffer = bufferPool16.removeFirst();

            writeBuffer[0] = (byte) (0x50 | (group & 0xf));
            writeBuffer[1] = (byte) (0x80 | (mdsId & 0xf));
            for (int i = 0; i < 14; i++) {
                writeBuffer[2 + i] = i < headers.length ? headers[i] : 0;
            }

            waiterThread.queue.add(writeBuffer);
        }

        // message has been queued, so interrupt the waiter thread
        waiterThread.interrupt();
    }

    /**
     * Mixed Data Set Payload
     *
     * @param group    the group ID 0-15
     * @param mdsId    mixed data set ID 0-255
     * @param payloads mixed data set payload data
     */
    public void sendMidiMixedDataSetPayload(int group, int mdsId, @NonNull final byte[] payloads) {
        while (bufferPool16.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {

            }
        }

        synchronized (waiterThread.queue) {
            byte[] writeBuffer = bufferPool16.removeFirst();

            writeBuffer[0] = (byte) (0x50 | (group & 0xf));
            writeBuffer[1] = (byte) (0x90 | (mdsId & 0xf));
            for (int i = 0; i < 14; i++) {
                writeBuffer[2 + i] = i < payloads.length ? payloads[i] : 0;
            }

            waiterThread.queue.add(writeBuffer);
        }

        // message has been queued, so interrupt the waiter thread
        waiterThread.interrupt();
    }

    /**
     * Sending thread for output data. Loops infinitely while stopFlag == false.
     *
     * @author K.Shoji
     */
    @SuppressLint("NewApi")
    private final class WaiterThread extends Thread {
        final Queue<byte[]> queue = new LinkedList<>();

        private volatile boolean stopFlag;
        private Runnable onStopped = null;
        volatile boolean suspendFlag;

        /**
         * Constructor
         */
        WaiterThread() {
            stopFlag = false;
            suspendFlag = false;
        }

        void stopThread(Runnable onStopped) {
            this.onStopped = onStopped;
            stopFlag = true;
            Midi2OutputDevice.this.resume();
        }

        @Override
        public void run() {
            byte[] dequedDataBuffer;
            int dequedDataBufferLength;
            int queueSize;
            final int maxPacketSize = outputEndpoint.getMaxPacketSize();
            byte[] endpointBuffer = new byte[maxPacketSize];
            int endpointBufferLength;
            int bufferPosition;
            int usbRequestFailCount;
            int bytesWritten;

            while (!stopFlag) {
                dequedDataBuffer = null;
                synchronized (queue) {
                    queueSize = queue.size();
                    if (queueSize > 0) {
                        dequedDataBuffer = queue.poll();
                    }
                }

                if (suspendFlag) {
                    try {
                        // sleep until interrupted
                        sleep(500);
                    } catch (InterruptedException e) {
                        // interrupted: event queued, or stopFlag/suspendFlag changed.
                    }
                    continue;
                }

                if (dequedDataBuffer != null) {
                    dequedDataBufferLength = dequedDataBuffer.length;

                    synchronized (usbDeviceConnection) {
                        // usb can't send data larger than maxPacketSize. split the data.
                        for (bufferPosition = 0; bufferPosition < dequedDataBufferLength; bufferPosition += maxPacketSize) {
                            endpointBufferLength = dequedDataBufferLength - bufferPosition;
                            if (endpointBufferLength > maxPacketSize) {
                                endpointBufferLength = maxPacketSize;
                            }

                            usbRequestFailCount = 0;
                            // if device disconnected, usbDeviceConnection.bulkTransfer returns negative value
                            while (true) {
                                // loop until transfer completed
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                    // JELLY_BEAN_MR2 supports bulkTransfer with offset
                                    bytesWritten = usbDeviceConnection.bulkTransfer(outputEndpoint, dequedDataBuffer, bufferPosition, endpointBufferLength, 10);
                                } else {
                                    if (bufferPosition > 0) {
                                        // copy the fragment to the endpointBuffer before transfer
                                        System.arraycopy(dequedDataBuffer, bufferPosition, endpointBuffer, 0, endpointBufferLength);
                                        bytesWritten = usbDeviceConnection.bulkTransfer(outputEndpoint, endpointBuffer, endpointBufferLength, 10);
                                    } else {
                                        // it's the first fragment.. copy is not required
                                        bytesWritten = usbDeviceConnection.bulkTransfer(outputEndpoint, dequedDataBuffer, endpointBufferLength, 10);
                                    }
                                }

                                if (bytesWritten < 0) {
                                    usbRequestFailCount++;
                                } else {
                                    break;
                                }

                                if (usbRequestFailCount > 10) {
                                    // maybe disconnected
                                    stopFlag = true;
                                    break;
                                }
                            }

                            if (stopFlag) {
                                break;
                            }
                        }
                    }

                    // returns buffer to pool
                    switch (dequedDataBufferLength) {
                        case 4:
                            synchronized (queue) {
                                bufferPool4.addLast(dequedDataBuffer);
                            }
                            break;
                        case 8:
                            synchronized (queue) {
                                bufferPool8.addLast(dequedDataBuffer);
                            }
                            break;
                        case 16:
                            synchronized (queue) {
                                bufferPool16.addLast(dequedDataBuffer);
                            }
                            break;
                    }
                }

                // no more data in queue, sleep.
                if (queueSize == 0 && !interrupted()) {
                    try {
                        // sleep until interrupted
                        sleep(500);
                    } catch (InterruptedException e) {
                        // interrupted: event queued, or stopFlag changed.
                    }
                }
            }
            if (onStopped != null) {
                onStopped.run();
            }
        }
    }

    // endregion
}
