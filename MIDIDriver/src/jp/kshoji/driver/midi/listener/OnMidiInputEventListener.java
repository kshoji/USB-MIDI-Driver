package jp.kshoji.driver.midi.listener;

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
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param byte1
	 * @param byte2
	 * @param byte3
	 */
	void onMidiMiscellaneousFunctionCodes(final MidiInputDevice sender, int cable, int byte1, int byte2, int byte3);
	
	/**
	 * Cable events. Reserved for future expansion.
	 * Code Index Number : 0x1
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param byte1
	 * @param byte2
	 * @param byte3
	 */
	void onMidiCableEvents(final MidiInputDevice sender, int cable, int byte1, int byte2, int byte3);
	
	/**
	 * System Common messages, or SysEx ends with following single byte.
	 * Code Index Number : 0x2 0x3 0x5
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param bytes bytes.length:1, 2, or 3
	 */
	void onMidiSystemCommonMessage(final MidiInputDevice sender, int cable, byte[] bytes);
	
	/**
	 * SysEx
	 * Code Index Number : 0x4, 0x5, 0x6, 0x7
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param systemExclusive
	 */
	void onMidiSystemExclusive(final MidiInputDevice sender, int cable, byte[] systemExclusive);
	
	/**
	 * Note-off
	 * Code Index Number : 0x8
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param note 0-127
	 * @param velocity 0-127
	 */
	void onMidiNoteOff(final MidiInputDevice sender, int cable, int channel, int note, int velocity);
	
	/**
	 * Note-on
	 * Code Index Number : 0x9
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param note 0-127
	 * @param velocity 0-127
	 */
	void onMidiNoteOn(final MidiInputDevice sender, int cable, int channel, int note, int velocity);
	
	/**
	 * Poly-KeyPress
	 * Code Index Number : 0xa
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param note 0-127
	 * @param pressure 0-127
	 */
	void onMidiPolyphonicAftertouch(final MidiInputDevice sender, int cable, int channel, int note, int pressure);
	
	/**
	 * Control Change
	 * Code Index Number : 0xb
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param function 0-127
	 * @param value 0-127
	 */
	void onMidiControlChange(final MidiInputDevice sender, int cable, int channel, int function, int value);
	
	/**
	 * Program Change
	 * Code Index Number : 0xc
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param program 0-127
	 */
	void onMidiProgramChange(final MidiInputDevice sender, int cable, int channel, int program);
	
	/**
	 * Channel Pressure
	 * Code Index Number : 0xd
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param pressure 0-127
	 */
	void onMidiChannelAftertouch(final MidiInputDevice sender, int cable, int channel, int pressure);
	
	/**
	 * PitchBend Change
	 * Code Index Number : 0xe
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param amount 0(low)-8192(center)-16383(high)
	 */
	void onMidiPitchWheel(final MidiInputDevice sender, int cable, int channel, int amount);
	
	/**
	 * ￼Single Byte
	 * Code Index Number : 0xf
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param byte1
	 */
	void onMidiSingleByte(final MidiInputDevice sender, int cable, int byte1);
}
