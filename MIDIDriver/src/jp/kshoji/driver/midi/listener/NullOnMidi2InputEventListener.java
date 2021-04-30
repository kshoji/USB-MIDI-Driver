package jp.kshoji.driver.midi.listener;

import android.support.annotation.NonNull;

import jp.kshoji.driver.midi.device.Midi2InputDevice;
import jp.kshoji.driver.midi.listener.OnMidi2InputEventListener;

/**
 * MIDI 2.0 events Listener without behaviour
 *
 * @author K.Shoji
 */
public class NullOnMidi2InputEventListener implements OnMidi2InputEventListener {
    @Override
    public void onMidiNoop(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiJitterReductionClock(Midi2InputDevice sender, int group, int senderClockTime) {

    }

    @Override
    public void onMidiJitterReductionTimestamp(Midi2InputDevice sender, int group, int senderClockTimestamp) {

    }

    @Override
    public void onMidiTimeCodeQuarterFrame(Midi2InputDevice sender, int group, int timing) {

    }

    @Override
    public void onMidiSongSelect(Midi2InputDevice sender, int group, int song) {

    }

    @Override
    public void onMidiSongPositionPointer(Midi2InputDevice sender, int group, int position) {

    }

    @Override
    public void onMidiTuneRequest(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiTimingClock(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiStart(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiContinue(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiStop(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiActiveSensing(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiReset(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidi1NoteOff(Midi2InputDevice sender, int group, int channel, int note, int velocity) {

    }

    @Override
    public void onMidi1NoteOn(Midi2InputDevice sender, int group, int channel, int note, int velocity) {

    }

    @Override
    public void onMidi1PolyphonicAftertouch(Midi2InputDevice sender, int group, int channel, int note, int pressure) {

    }

    @Override
    public void onMidi1ControlChange(Midi2InputDevice sender, int group, int channel, int function, int value) {

    }

    @Override
    public void onMidi1ProgramChange(Midi2InputDevice sender, int group, int channel, int program) {

    }

    @Override
    public void onMidi1ChannelAftertouch(Midi2InputDevice sender, int group, int channel, int pressure) {

    }

    @Override
    public void onMidi1PitchWheel(Midi2InputDevice sender, int group, int channel, int amount) {

    }

    @Override
    public void onMidi1SystemExclusive(Midi2InputDevice sender, int group, @NonNull byte[] systemExclusive) {

    }

    @Override
    public void onMidi2NoteOff(Midi2InputDevice sender, int group, int channel, int note, int velocity, int attributeType, int attributeData) {

    }

    @Override
    public void onMidi2NoteOn(Midi2InputDevice sender, int group, int channel, int note, int velocity, int attributeType, int attributeData) {

    }

    @Override
    public void onMidi2PolyphonicAftertouch(Midi2InputDevice sender, int group, int channel, int note, long pressure) {

    }

    @Override
    public void onMidi2ControlChange(Midi2InputDevice sender, int group, int channel, int index, long value) {

    }

    @Override
    public void onMidi2ProgramChange(Midi2InputDevice sender, int group, int channel, int optionFlags, int program, int bank) {

    }

    @Override
    public void onMidi2ChannelAftertouch(Midi2InputDevice sender, int group, int channel, long pressure) {

    }

    @Override
    public void onMidi2PitchWheel(Midi2InputDevice sender, int group, int channel, long amount) {

    }

    @Override
    public void onMidiPerNotePitchWheel(Midi2InputDevice sender, int group, int channel, int note, long amount) {

    }

    @Override
    public void onMidiPerNoteManagement(Midi2InputDevice sender, int group, int channel, int note, int optionFlags) {

    }

    @Override
    public void onMidiRegisteredPerNoteController(Midi2InputDevice sender, int group, int channel, int note, int index, long data) {

    }

    @Override
    public void onMidiAssignablePerNoteController(Midi2InputDevice sender, int group, int channel, int note, int index, long data) {

    }

    @Override
    public void onMidiRegisteredController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {

    }

    @Override
    public void onMidiAssignableController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {

    }

    @Override
    public void onMidiRelativeRegisteredController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {

    }

    @Override
    public void onMidiRelativeAssignableController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {

    }

    @Override
    public void onMidi2SystemExclusive(Midi2InputDevice sender, int group, int streamId, @NonNull byte[] systemExclusive) {

    }

    @Override
    public void onMidiMixedDataSetHeader(Midi2InputDevice sender, int group, int mdsId, @NonNull byte[] headers) {

    }

    @Override
    public void onMidiMixedDataSetPayload(Midi2InputDevice sender, int group, int mdsId, @NonNull byte[] payloads) {

    }
}
