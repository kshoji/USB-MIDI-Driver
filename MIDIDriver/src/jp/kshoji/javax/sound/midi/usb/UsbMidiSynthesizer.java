package jp.kshoji.javax.sound.midi.usb;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

import jp.kshoji.javax.sound.midi.Instrument;
import jp.kshoji.javax.sound.midi.MidiChannel;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Patch;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.Soundbank;
import jp.kshoji.javax.sound.midi.Synthesizer;
import jp.kshoji.javax.sound.midi.Transmitter;
import jp.kshoji.javax.sound.midi.VoiceStatus;
import jp.kshoji.javax.sound.midi.impl.MidiChannelImpl;

/**
 * {@link jp.kshoji.javax.sound.midi.Synthesizer} implementation
 *
 * @author K.Shoji
 */
public class UsbMidiSynthesizer implements Synthesizer {
    private final UsbMidiDevice usbMidiDevice;
    private MidiChannel[] channels;
    private VoiceStatus[] voiceStatuses;

    /**
     * Constructor
     *
     * @param usbMidiDevice the device
     */
    public UsbMidiSynthesizer(final UsbMidiDevice usbMidiDevice) {
        this.usbMidiDevice = usbMidiDevice;

        Receiver receiver = null;
        try {
            receiver = this.usbMidiDevice.getReceiver();
        } catch (final MidiUnavailableException ignored) {
        }

        if (receiver == null) {
            // empty
            channels = new MidiChannel[0];
            voiceStatuses = new VoiceStatus[0];
        } else {
            // 16 channels
            voiceStatuses = new VoiceStatus[16];
            channels = new MidiChannel[16];
            for (int channel = 0; channel < 16; channel++) {
                voiceStatuses[channel] = new VoiceStatus();
                channels[channel] = new MidiChannelImpl(channel, receiver, voiceStatuses[channel]);
            }
        }
    }

    @NonNull
    @Override
    public MidiChannel[] getChannels() {
        return channels;
    }

    @Override
    public long getLatency() {
        return 0;
    }

    @Override
    public int getMaxPolyphony() {
        return 127;
    }

    @NonNull
    @Override
    public VoiceStatus[] getVoiceStatus() {
        return voiceStatuses;
    }

    @Nullable
    @Override
    public Soundbank getDefaultSoundbank() {
        return null;
    }

    @Override
    public boolean isSoundbankSupported(@NonNull final Soundbank soundbank) {
        return false;
    }

    @NonNull
    @Override
    public Instrument[] getAvailableInstruments() {
        return new Instrument[0];
    }

    @NonNull
    @Override
    public Instrument[] getLoadedInstruments() {
        return new Instrument[0];
    }

    @Override
    public boolean remapInstrument(@NonNull final Instrument from, @NonNull final Instrument to) {
        return false;
    }

    @Override
    public boolean loadAllInstruments(@NonNull final Soundbank soundbank) {
        return false;
    }

    @Override
    public void unloadAllInstruments(@NonNull final Soundbank soundbank) {

    }

    @Override
    public boolean loadInstrument(@NonNull final Instrument instrument) {
        return false;
    }

    @Override
    public void unloadInstrument(@NonNull final Instrument instrument) {

    }

    @Override
    public boolean loadInstruments(@NonNull final Soundbank soundbank, @NonNull final Patch[] patchList) {
        return false;
    }

    @Override
    public void unloadInstruments(@NonNull final Soundbank soundbank, @NonNull final Patch[] patchList) {

    }

    @NonNull
    @Override
    public Info getDeviceInfo() {
        return usbMidiDevice.getDeviceInfo();
    }

    @Override
    public void open() throws MidiUnavailableException {
        usbMidiDevice.open();
    }

    @Override
    public void close() {
        usbMidiDevice.close();
    }

    @Override
    public boolean isOpen() {
        return usbMidiDevice.isOpen();
    }

    @Override
    public long getMicrosecondPosition() {
        return -1;
    }

    @Override
    public int getMaxReceivers() {
        return usbMidiDevice.getMaxReceivers();
    }

    @Override
    public int getMaxTransmitters() {
        return usbMidiDevice.getMaxTransmitters();
    }

    @NonNull
    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return usbMidiDevice.getReceiver();
    }

    @NonNull
    @Override
    public List<Receiver> getReceivers() {
        return usbMidiDevice.getReceivers();
    }

    @NonNull
    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        return usbMidiDevice.getTransmitter();
    }

    @NonNull
    @Override
    public List<Transmitter> getTransmitters() {
        return usbMidiDevice.getTransmitters();
    }

    public void setReceiver(final Receiver receiver) {
        // 16 channels
        voiceStatuses = new VoiceStatus[16];
        channels = new MidiChannel[16];
        for (int channel = 0; channel < 16; channel++) {
            voiceStatuses[channel] = new VoiceStatus();
            channels[channel] = new MidiChannelImpl(channel, receiver, voiceStatuses[channel]);
        }
    }
}
