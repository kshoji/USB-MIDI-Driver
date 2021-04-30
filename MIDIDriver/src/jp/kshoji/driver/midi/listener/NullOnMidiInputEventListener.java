package jp.kshoji.driver.midi.listener;

import android.support.annotation.NonNull;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;

/**
 * MIDI events Listener without behaviour
 *
 * @author K.Shoji
 */
public class NullOnMidiInputEventListener implements OnMidiInputEventListener {
    @Override
    public void onMidiMiscellaneousFunctionCodes(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {

    }

    @Override
    public void onMidiCableEvents(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {

    }

    @Override
    public void onMidiSystemCommonMessage(@NonNull MidiInputDevice sender, int cable, byte[] bytes) {

    }

    @Override
    public void onMidiSystemExclusive(@NonNull MidiInputDevice sender, int cable, byte[] systemExclusive) {

    }

    @Override
    public void onMidiNoteOff(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {

    }

    @Override
    public void onMidiNoteOn(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {

    }

    @Override
    public void onMidiPolyphonicAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int note, int pressure) {

    }

    @Override
    public void onMidiControlChange(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {

    }

    @Override
    public void onMidiProgramChange(@NonNull MidiInputDevice sender, int cable, int channel, int program) {

    }

    @Override
    public void onMidiChannelAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int pressure) {

    }

    @Override
    public void onMidiPitchWheel(@NonNull MidiInputDevice sender, int cable, int channel, int amount) {

    }

    @Override
    public void onMidiSingleByte(@NonNull MidiInputDevice sender, int cable, int byte1) {

    }

    @Override
    public void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice sender, int cable, int timing) {

    }

    @Override
    public void onMidiSongSelect(@NonNull MidiInputDevice sender, int cable, int song) {

    }

    @Override
    public void onMidiSongPositionPointer(@NonNull MidiInputDevice sender, int cable, int position) {

    }

    @Override
    public void onMidiTuneRequest(@NonNull MidiInputDevice sender, int cable) {

    }

    @Override
    public void onMidiTimingClock(@NonNull MidiInputDevice sender, int cable) {

    }

    @Override
    public void onMidiStart(@NonNull MidiInputDevice sender, int cable) {

    }

    @Override
    public void onMidiContinue(@NonNull MidiInputDevice sender, int cable) {

    }

    @Override
    public void onMidiStop(@NonNull MidiInputDevice sender, int cable) {

    }

    @Override
    public void onMidiActiveSensing(@NonNull MidiInputDevice sender, int cable) {

    }

    @Override
    public void onMidiReset(@NonNull MidiInputDevice sender, int cable) {

    }

    @Override
    public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {

    }

    @Override
    public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {

    }

    @Override
    public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {

    }

    @Override
    public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {

    }
}
