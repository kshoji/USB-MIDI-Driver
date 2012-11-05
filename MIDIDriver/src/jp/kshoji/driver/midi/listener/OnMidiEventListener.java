package jp.kshoji.driver.midi.listener;

/**
 * Listener for MIDI events
 * For events' details, @see <a href="http://www.usb.org/developers/devclass_docs/midi10.pdf">Universal Serial Bus Device Class Definition for MIDI Devices</a>
 * 
 * @author K.Shoji
 */
public interface OnMidiEventListener {
	
	/**
	 * Miscellaneous function codes. Reserved for future extensions.
	 * Code Index Number : 0x0
	 * 
	 * @param cable 0-15
	 * @param byte1
	 * @param byte2
	 * @param byte3
	 */
	void onMidiMiscellaneousFunctionCodes(int cable, int byte1, int byte2, int byte3);
	
	/**
	 * Cable events. Reserved for future expansion.
	 * Code Index Number : 0x1
	 * 
	 * @param cable 0-15
	 * @param byte1
	 * @param byte2
	 * @param byte3
	 */
	void onMidiCableEvents(int cable, int byte1, int byte2, int byte3);
	
	/**
	 * System Common messages, or SysEx ends with following single byte.
	 * Code Index Number : 0x2 0x3 0x5
	 * 
	 * @param cable 0-15
	 * @param bytes bytes.length:1, 2, or 3
	 */
	void onMidiSystemCommonMessage(int cable, byte[] bytes);
	
	/**
	 * SysEx
	 * Code Index Number : 0x4, 0x5, 0x6, 0x7
	 * 
	 * @param cable 0-15
	 * @param systemExclusive
	 */
	void onMidiSystemExclusive(int cable, byte[] systemExclusive);
	
	/**
	 * Note-off
	 * Code Index Number : 0x8
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param note 0-127
	 * @param velocity 0-127
	 */
	void onMidiNoteOff(int cable, int channel, int note, int velocity);
	
	/**
	 * Note-on
	 * Code Index Number : 0x9
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param note 0-127
	 * @param velocity 0-127
	 */
	void onMidiNoteOn(int cable, int channel, int note, int velocity);
	
	/**
	 * Poly-KeyPress
	 * Code Index Number : 0xa
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param note 0-127
	 * @param pressure 0-127
	 */
	void onMidiPolyphonicAftertouch(int cable, int channel, int note, int pressure);
	
	/**
	 * Control Change
	 * Code Index Number : 0xb
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param function 0-127
	 * @param value 0-127
	 */
	void onMidiControlChange(int cable, int channel, int function, int value);
	
	/**
	 * Program Change
	 * Code Index Number : 0xc
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param program 0-127
	 */
	void onMidiProgramChange(int cable, int channel, int program);
	
	/**
	 * Channel Pressure
	 * Code Index Number : 0xd
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param pressure 0-127
	 */
	void onMidiChannelAftertouch(int cable, int channel, int pressure);
	
	/**
	 * PitchBend Change
	 * Code Index Number : 0xe
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param amount 0(low)-8192(center)-16383(high)
	 */
	void onMidiPitchWheel(int cable, int channel, int amount);
	
	/**
	 * ￼Single Byte
	 * Code Index Number : 0xf
	 * 
	 * @param cable 0-15
	 * @param byte1
	 */
	void onMidiSingleByte(int cable, int byte1);
}
