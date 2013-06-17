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
	void onMidiMiscellaneousFunctionCodes(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3);
	
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
	void onMidiCableEvents(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3);
	
	/**
	 * System Common messages, or SysEx ends with following single byte.
	 * Code Index Number : 0x2 0x3 0x5
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param bytes bytes.length:1, 2, or 3
	 */
	void onMidiSystemCommonMessage(MidiInputDevice sender, int cable, byte[] bytes);
	
	/**
	 * SysEx
	 * Code Index Number : 0x4, 0x5, 0x6, 0x7
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param systemExclusive
	 */
	void onMidiSystemExclusive(MidiInputDevice sender, int cable, byte[] systemExclusive);
	
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
	void onMidiNoteOff(MidiInputDevice sender, int cable, int channel, int note, int velocity);
	
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
	void onMidiNoteOn(MidiInputDevice sender, int cable, int channel, int note, int velocity);
	
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
	void onMidiPolyphonicAftertouch(MidiInputDevice sender, int cable, int channel, int note, int pressure);
	
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
	void onMidiControlChange(MidiInputDevice sender, int cable, int channel, int function, int value);
	
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
	void onMidiProgramChange(MidiInputDevice sender, int cable, int channel, int program);
	
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
	void onMidiChannelAftertouch(MidiInputDevice sender, int cable, int channel, int pressure);
	
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
	void onMidiPitchWheel(MidiInputDevice sender, int cable, int channel, int amount);
	
	/**
	 * Single Byte
	 * Code Index Number : 0xf
	 * 
	 * @param senderDevice
	 * @param senderInterface
	 * @param cable 0-15
	 * @param byte1
	 */
	void onMidiSingleByte(MidiInputDevice sender, int cable, int byte1);

	/**
	 * RPN message
	 * 
	 * @param sender
	 * @param cable
	 * @param channel
	 * @param function 14bits
	 * @param valueMSB higher 7bits
	 * @param valueLSB lower 7bits. -1 if value has no LSB. If you know the function's parameter value have LSB, you must ignore when valueLSB < 0.
	 */
	void onMidiRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB);

	/**
	 * NRPN message
	 * 
	 * @param sender
	 * @param cable
	 * @param channel
	 * @param function 14bits
	 * @param valueMSB higher 7bits
	 * @param valueLSB lower 7bits. -1 if value has no LSB. If you know the function's parameter value have LSB, you must ignore when valueLSB < 0.
	 */
	void onMidiNRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB);
}
