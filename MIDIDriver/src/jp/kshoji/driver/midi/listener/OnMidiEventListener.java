package jp.kshoji.driver.midi.listener;

/**
 * Listener for MIDI events
 * 
 * @author K.Shoji
 */
public interface OnMidiEventListener {
	
	/**
	 * Code Index Number : 0x0
	 * 
	 * @param cable
	 * @param byte1
	 */
	void onMidiMiscellaneousFunctionCodes(int cable, int byte1, int byte2, int byte3);
	
	/**
	 * Code Index Number : 0x1
	 * 
	 * @param cable
	 * @param byte1
	 */
	void onMidiCableEvents(int cable, int byte1, int byte2, int byte3);
	
	/**
	 * Code Index Number : 0x2 0x3 0x5
	 * 
	 * @param cable
	 * @param bytes bytes.length:1, 2, or 3
	 */
	void onMidiSystemCommonMessage(int cable, byte[] bytes);
	
	/**
	 * Code Index Number : 0x4, 0x5, 0x6, 0x7
	 * 
	 * @param cable
	 * @param systemExclusive
	 */
	void onMidiSystemExclusive(int cable, byte[] systemExclusive);
	
	/**
	 * Code Index Number : 0x8
	 * 
	 * @param cable
	 * @param channel
	 * @param note
	 * @param velocity
	 */
	void onMidiNoteOff(int cable, int channel, int note, int velocity);
	
	/**
	 * Code Index Number : 0x9
	 * 
	 * @param cable
	 * @param channel
	 * @param note
	 * @param velocity
	 */
	void onMidiNoteOn(int cable, int channel, int note, int velocity);
	
	/**
	 * Code Index Number : 0xa
	 * 
	 * @param cable
	 * @param channel
	 * @param note
	 * @param pressure
	 */
	void onMidiPolyphonicAftertouch(int cable, int channel, int note, int pressure);
	
	/**
	 * Code Index Number : 0xb
	 * 
	 * @param cable
	 * @param channel
	 * @param function
	 * @param value
	 */
	void onMidiControlChange(int cable, int channel, int function, int value);
	
	/**
	 * Code Index Number : 0xc
	 * 
	 * @param cable
	 * @param channel
	 * @param program
	 */
	void onMidiProgramChange(int cable, int channel, int program);
	
	/**
	 * Code Index Number : 0xd
	 * 
	 * @param cable
	 * @param channel
	 * @param pressure
	 */
	void onMidiChannelAftertouch(int cable, int channel, int pressure);
	
	/**
	 * Code Index Number : 0xe
	 * 
	 * @param cable
	 * @param channel
	 * @param lsb
	 * @param msb
	 */
	void onMidiPitchWheel(int cable, int channel, int amount);
	
	/**
	 * Code Index Number : 0xf
	 * 
	 * @param cable
	 * @param byte1
	 */
	void onMidiSingleByte(int cable, int byte1);
}
