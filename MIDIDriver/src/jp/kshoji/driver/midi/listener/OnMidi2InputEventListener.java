package jp.kshoji.driver.midi.listener;

import android.support.annotation.NonNull;

import jp.kshoji.driver.midi.device.Midi2InputDevice;

/**
 * Listener for MIDI 2.0 events
 * For events' details, @see <a href="https://www.usb.org/sites/default/files/USB%20MIDI%20v2_0.pdf">USB Class Definition for MIDI Devices v2.0</a>
 *
 * @author K.Shoji
 */
public interface OnMidi2InputEventListener {
    // region messageType 0

    /**
     * NOOP
     *
     * @param sender the Object which the event sent
     * @param group  the group ID 0-15
     */
    void onMidiNoop(Midi2InputDevice sender, int group);

    /**
     * JR Clock
     *
     * @param sender          the Object which the event sent
     * @param group           the group ID 0-15
     * @param senderClockTime 16-bit time value in clock ticks of 1/31250 of one second
     */
    void onMidiJitterReductionClock(Midi2InputDevice sender, int group, int senderClockTime);

    /**
     * JR Timestamp
     *
     * @param sender               the Object which the event sent
     * @param group                the group ID 0-15
     * @param senderClockTimestamp 16-bit time value in clock ticks of 1/31250 of one second
     */
    void onMidiJitterReductionTimestamp(Midi2InputDevice sender, int group, int senderClockTimestamp);
    // endregion

    // region messageType 1

    /**
     * MIDI Time Code(MTC) Quarter Frame
     *
     * @param sender the Object which the event sent
     * @param group  the group ID 0-15
     * @param timing 0-127
     */
    void onMidiTimeCodeQuarterFrame(Midi2InputDevice sender, int group, int timing);

    /**
     * Song Select
     *
     * @param sender the Object which the event sent
     * @param group  the group ID 0-15
     * @param song   0-127
     */
    void onMidiSongSelect(Midi2InputDevice sender, int group, int song);

    /**
     * Song Position Pointer
     *
     * @param sender   the Object which the event sent
     * @param group    the group ID 0-15
     * @param position 0-16383
     */
    void onMidiSongPositionPointer(Midi2InputDevice sender, int group, int position);

    /**
     * Tune Request
     *
     * @param sender the Object which the event sent
     * @param group  the group ID 0-15
     */
    void onMidiTuneRequest(Midi2InputDevice sender, int group);

    /**
     * Timing Clock
     *
     * @param sender the Object which the event sent
     * @param group  the group ID 0-15
     */
    void onMidiTimingClock(Midi2InputDevice sender, int group);

    /**
     * Start Playing
     *
     * @param sender the Object which the event sent
     * @param group  the group ID 0-15
     */
    void onMidiStart(Midi2InputDevice sender, int group);

    /**
     * Continue Playing
     *
     * @param sender the Object which the event sent
     * @param group  the group ID 0-15
     */
    void onMidiContinue(Midi2InputDevice sender, int group);

    /**
     * Stop Playing
     *
     * @param sender the Object which the event sent
     * @param group  the group ID 0-15
     */
    void onMidiStop(Midi2InputDevice sender, int group);

    /**
     * Active Sensing
     *
     * @param sender the Object which the event sent
     * @param group  the group ID 0-15
     */
    void onMidiActiveSensing(Midi2InputDevice sender, int group);

    /**
     * Reset Device
     *
     * @param sender the Object which the event sent
     * @param group  the group ID 0-15
     */
    void onMidiReset(Midi2InputDevice sender, int group);
    // endregion

    // region messageType 2

    /**
     * Note-off
     *
     * @param sender   the Object which the event sent
     * @param group    0-15
     * @param channel  0-15
     * @param note     0-127
     * @param velocity 0-127
     */
    void onMidi1NoteOff(Midi2InputDevice sender, int group, int channel, int note, int velocity);

    /**
     * Note-on
     *
     * @param sender   the Object which the event sent
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param note     0-127
     * @param velocity 0-127
     */
    void onMidi1NoteOn(Midi2InputDevice sender, int group, int channel, int note, int velocity);

    /**
     * Poly-KeyPress
     *
     * @param sender   the Object which the event sent
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param note     0-127
     * @param pressure 0-127
     */
    void onMidi1PolyphonicAftertouch(Midi2InputDevice sender, int group, int channel, int note, int pressure);

    /**
     * Control Change
     *
     * @param sender   the Object which the event sent
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param function 0-127
     * @param value    0-127
     */
    void onMidi1ControlChange(Midi2InputDevice sender, int group, int channel, int function, int value);

    /**
     * Program Change
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param program 0-127
     */
    void onMidi1ProgramChange(Midi2InputDevice sender, int group, int channel, int program);

    /**
     * Channel Pressure
     *
     * @param sender   the Object which the event sent
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param pressure 0-127
     */
    void onMidi1ChannelAftertouch(Midi2InputDevice sender, int group, int channel, int pressure);

    /**
     * PitchBend Change
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param amount  0(low)-8192(center)-16383(high)
     */
    void onMidi1PitchWheel(Midi2InputDevice sender, int group, int channel, int amount);
    // endregion

    // region messageType 3

    /**
     * SysEx
     *
     * @param sender          the Object which the event sent
     * @param group           the group ID 0-15
     * @param systemExclusive raw data. Note: Status values 0xF0 and 0xF7 are not used for UMP System Exclusive
     */
    void onMidi1SystemExclusive(Midi2InputDevice sender, int group, @NonNull byte[] systemExclusive);
    // endregion

    // region messageType 4

    /**
     * Note-off
     *
     * @param sender        the Object which the event sent
     * @param group         0-15
     * @param channel       0-15
     * @param note          0-127
     * @param velocity      0-65535
     * @param attributeType 0-255
     * @param attributeData 0-65535
     */
    void onMidi2NoteOff(Midi2InputDevice sender, int group, int channel, int note, int velocity, int attributeType, int attributeData);

    /**
     * Note-on
     *
     * @param sender        the Object which the event sent
     * @param group         the group ID 0-15
     * @param channel       the MIDI channel number 0-15
     * @param note          0-127
     * @param velocity      0-65535
     * @param attributeType 0-255
     * @param attributeData 0-65535
     */
    void onMidi2NoteOn(Midi2InputDevice sender, int group, int channel, int note, int velocity, int attributeType, int attributeData);

    /**
     * Poly-KeyPress
     *
     * @param sender   the Object which the event sent
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param note     0-127
     * @param pressure 0-4294967295
     */
    void onMidi2PolyphonicAftertouch(Midi2InputDevice sender, int group, int channel, int note, long pressure);

    /**
     * Control Change
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param index   0-127
     * @param value   0-4294967295
     */
    void onMidi2ControlChange(Midi2InputDevice sender, int group, int channel, int index, long value);

    /**
     * Program Change
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param program 0-127
     * @param bank    0-16383
     */
    void onMidi2ProgramChange(Midi2InputDevice sender, int group, int channel, int optionFlags, int program, int bank);

    /**
     * Channel Pressure
     *
     * @param sender   the Object which the event sent
     * @param group    the group ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param pressure 0-4294967295
     */
    void onMidi2ChannelAftertouch(Midi2InputDevice sender, int group, int channel, long pressure);

    /**
     * PitchBend Change
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param amount  0(low)-2147483648(center)-4294967295(high)
     */
    void onMidi2PitchWheel(Midi2InputDevice sender, int group, int channel, long amount);

    /**
     * Per Note PitchBend Change
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param note    0-127
     * @param amount  0(low)-2147483648(center)-4294967295(high)
     */
    void onMidiPerNotePitchWheel(Midi2InputDevice sender, int group, int channel, int note, long amount);

    /**
     * Per Note Management
     *
     * @param sender      the Object which the event sent
     * @param group       the group ID 0-15
     * @param channel     the MIDI channel number 0-15
     * @param optionFlags 0-255
     */
    void onMidiPerNoteManagement(Midi2InputDevice sender, int group, int channel, int note, int optionFlags);

    /**
     * Registered Per Note Controller
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param note    0-127
     * @param index   0-255
     * @param data    0-4294967295
     */
    void onMidiRegisteredPerNoteController(Midi2InputDevice sender, int group, int channel, int note, int index, long data);

    /**
     * Assignable Per Note Controller
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param note    0-127
     * @param index   0-255
     * @param data    0-4294967295
     */
    void onMidiAssignablePerNoteController(Midi2InputDevice sender, int group, int channel, int note, int index, long data);

    /**
     * Registered Controller (RPN)
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param bank    0-127
     * @param index   0-127
     * @param data    0-4294967295
     */
    void onMidiRegisteredController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data);

    /**
     * Assignable Controller (NRPN)
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param bank    0-127
     * @param index   0-127
     * @param data    0-4294967295
     */
    void onMidiAssignableController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data);

    /**
     * Relative Registered Controller
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param bank    0-127
     * @param index   0-127
     * @param data    0-4294967295
     */
    void onMidiRelativeRegisteredController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data);

    /**
     * Relative Assignable Controller
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param bank    0-127
     * @param index   0-127
     * @param data    0-4294967295
     */
    void onMidiRelativeAssignableController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data);
    // endregion

    // region messageType 5

    /**
     * SysEx
     *
     * @param sender          the Object which the event sent
     * @param group           the group ID 0-15
     * @param streamId        the stream ID 0-255
     * @param systemExclusive raw data. Note: Status values 0xF0 and 0xF7 are not used for UMP System Exclusive
     */
    void onMidi2SystemExclusive(Midi2InputDevice sender, int group, int streamId, @NonNull byte[] systemExclusive);

    /**
     * Mixed Data Set Header
     *
     * @param sender  the Object which the event sent
     * @param group   the group ID 0-15
     * @param mdsId   mixed data set ID 0-255
     * @param headers mixed data set header data
     */
    void onMidiMixedDataSetHeader(Midi2InputDevice sender, int group, int mdsId, @NonNull byte[] headers);

    /**
     * Mixed Data Set Payload
     *
     * @param sender   the Object which the event sent
     * @param group    the group ID 0-15
     * @param mdsId    mixed data set ID 0-255
     * @param payloads mixed data set payload data
     */
    void onMidiMixedDataSetPayload(Midi2InputDevice sender, int group, int mdsId, @NonNull byte[] payloads);
    // endregion
}
