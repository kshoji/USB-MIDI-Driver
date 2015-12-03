package jp.kshoji.driver.midi.listener;

import android.support.annotation.NonNull;

import jp.kshoji.driver.midi.device.MidiInputDevice;

/**
 * Listener for MIDI events
 * For events' details, @see <a href="http://www.usb.org/developers/devclass_docs/midi10.pdf">Universal Serial Bus Device Class Definition for MIDI Devices</a>
 * 
 * @author K.Shoji
 */
public interface OnMidiInputEventListener {
	
	/**
	 * Miscellaneous function codes. Reserved for future extensions.
	 * Code Index Number : 0x0
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
	 * @param byte1 the first byte
	 * @param byte2 the second byte
	 * @param byte3 the third byte
	 */
	void onMidiMiscellaneousFunctionCodes(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3);
	
	/**
	 * Cable events. Reserved for future expansion.
	 * Code Index Number : 0x1
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
     * @param byte1 the first byte
     * @param byte2 the second byte
     * @param byte3 the third byte
	 */
	void onMidiCableEvents(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3);
	
	/**
	 * System Common messages, or SysEx ends with following single byte.
	 * Code Index Number : 0x2 0x3 0x5
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
	 * @param bytes bytes.length:1, 2, or 3
	 */
	void onMidiSystemCommonMessage(@NonNull MidiInputDevice sender, int cable, byte[] bytes);
	
	/**
	 * SysEx
	 * Code Index Number : 0x4, 0x5, 0x6, 0x7
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
	 * @param systemExclusive the SysEx message
	 */
	void onMidiSystemExclusive(@NonNull MidiInputDevice sender, int cable, byte[] systemExclusive);
	
	/**
	 * Note-off
	 * Code Index Number : 0x8
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
	 * @param channel 0-15
	 * @param note 0-127
	 * @param velocity 0-127
	 */
	void onMidiNoteOff(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity);
	
	/**
	 * Note-on
	 * Code Index Number : 0x9
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param note 0-127
	 * @param velocity 0-127
	 */
	void onMidiNoteOn(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity);
	
	/**
	 * Poly-KeyPress
	 * Code Index Number : 0xa
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param note 0-127
	 * @param pressure 0-127
	 */
	void onMidiPolyphonicAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int note, int pressure);
	
	/**
	 * Control Change
	 * Code Index Number : 0xb
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param function 0-127
	 * @param value 0-127
	 */
	void onMidiControlChange(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value);
	
	/**
	 * Program Change
	 * Code Index Number : 0xc
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param program 0-127
	 */
	void onMidiProgramChange(@NonNull MidiInputDevice sender, int cable, int channel, int program);
	
	/**
	 * Channel Pressure
	 * Code Index Number : 0xd
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param pressure 0-127
	 */
	void onMidiChannelAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int pressure);
	
	/**
	 * PitchBend Change
	 * Code Index Number : 0xe
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param amount 0(low)-8192(center)-16383(high)
	 */
	void onMidiPitchWheel(@NonNull MidiInputDevice sender, int cable, int channel, int amount);

    /**
	 * Single Byte
	 * Code Index Number : 0xf
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
	 * @param byte1 the first byte
	 */
	void onMidiSingleByte(@NonNull MidiInputDevice sender, int cable, int byte1);

    /**
     * MIDI Time Code(MTC) Quarter Frame
     *
     * @param sender the device sent this message
     * @param cable the cable ID 0-15
     * @param timing 0-16383
     */
    void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice sender, int cable, int timing);

    /**
     * Song Select
     *
     * @param sender the device sent this message
     * @param cable the cable ID 0-15
     * @param song 0-127
     */
    void onMidiSongSelect(@NonNull MidiInputDevice sender, int cable, int song);

    /**
     * Song Position Pointer
     *
     * @param sender the device sent this message
     * @param cable the cable ID 0-15
     * @param position 0-16383
     */
    void onMidiSongPositionPointer(@NonNull MidiInputDevice sender, int cable, int position);

    /**
     * Tune Request
     *
     * @param sender the device sent this message
     * @param cable the cable ID 0-15
     */
    void onMidiTuneRequest(@NonNull MidiInputDevice sender, int cable);

    /**
     * Timing Clock
     *
     * @param sender the device sent this message
     * @param cable the cable ID 0-15
     */
    void onMidiTimingClock(@NonNull MidiInputDevice sender, int cable);

    /**
     * Start Playing
     *
     * @param sender the device sent this message
     * @param cable the cable ID 0-15
     */
    void onMidiStart(@NonNull MidiInputDevice sender, int cable);

    /**
     * Continue Playing
     *
     * @param sender the device sent this message
     * @param cable the cable ID 0-15
     */
    void onMidiContinue(@NonNull MidiInputDevice sender, int cable);

    /**
     * Stop Playing
     *
     * @param sender the device sent this message
     * @param cable the cable ID 0-15
     */
    void onMidiStop(@NonNull MidiInputDevice sender, int cable);

    /**
     * Active Sensing
     *
     * @param sender the device sent this message
     * @param cable the cable ID 0-15
     */
    void onMidiActiveSensing(@NonNull MidiInputDevice sender, int cable);

    /**
     * Reset Device
     *
     * @param sender the device sent this message
     * @param cable the cable ID 0-15
     */
    void onMidiReset(@NonNull MidiInputDevice sender, int cable);

    /**
	 * RPN message, the value will be calculated as  `(valueMSB << 7) | valueLSB`
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param function 14bits
	 * @param valueMSB higher 7bits
	 * @param valueLSB lower 7bits. If you know the value has LSB for the NRPN function, observe this value to detect parameter changing.
	 */
	void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB);

	/**
	 * NRPN message, the value will be calculated as `(valueMSB << 7) | valueLSB`
	 *
     * @param sender the Object which the event sent
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param function 14bits
	 * @param valueMSB higher 7bits
	 * @param valueLSB lower 7bits. If you know the value has LSB for the NRPN function, observe this value to detect parameter changing.
	 */
	void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB);

	/**
	 * RPN message
	 *
	 * @param sender the Object which the event sent
	 * @param cable the cable ID 0-15
	 * @param channel the MIDI channel number 0-15
	 * @param function 14bits
	 * @param value 0-16383
	 */
	void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value);

	/**
	 * NRPN message
	 *
	 * @param sender the Object which the event sent
	 * @param cable the cable ID 0-15
	 * @param channel the MIDI channel number 0-15
	 * @param function 14bits
	 * @param value 0-16383
	 */
	void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value);
}
