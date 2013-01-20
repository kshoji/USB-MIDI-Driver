package jp.kshoji.driver.midi.handler;

import java.io.ByteArrayOutputStream;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import android.os.Handler.Callback;
import android.os.Message;

/**
 * USB MIDI Message parser
 * 
 * @author K.Shoji
 */
public final class MidiMessageCallback implements Callback {

	private final OnMidiInputEventListener midiEventListener;
	private final MidiInputDevice sender;
	private ByteArrayOutputStream systemExclusive = null;

	/**
	 * constructor
	 * 
	 * @param device
	 * @param midiEventListener
	 */
	public MidiMessageCallback(final MidiInputDevice device, final OnMidiInputEventListener midiEventListener) {
		this.midiEventListener = midiEventListener;
		sender = device;
	}

	/*
	 * (non-Javadoc)
	 * @see android.os.Handler.Callback#handleMessage(android.os.Message)
	 */
	@Override
	public boolean handleMessage(final Message msg) {
		if (midiEventListener == null) {
			return false;
		}
		byte[] read = (byte[]) msg.obj;
		int cable;
		int codeIndexNumber;
		int byte1;
		int byte2;
		int byte3;
		for (int i = 0; i < read.length; i += 4) {
			cable = (read[i + 0] >> 4) & 0xf;
			codeIndexNumber = read[i + 0] & 0xf;
			byte1 = read[i + 1] & 0xff;
			byte2 = read[i + 2] & 0xff;
			byte3 = read[i + 3] & 0xff;

			switch (codeIndexNumber) {
				case 0:
					midiEventListener.onMidiMiscellaneousFunctionCodes(sender, cable, byte1, byte2, byte3);
					break;
				case 1:
					midiEventListener.onMidiCableEvents(sender, cable, byte1, byte2, byte3);
					break;
				case 2:
				// system common message with 2 bytes
				{
					byte[] bytes = new byte[]{(byte) byte1, (byte) byte2};
					midiEventListener.onMidiSystemCommonMessage(sender, cable, bytes);
				}
					break;
				case 3:
				// system common message with 3 bytes
				{
					byte[] bytes = new byte[]{(byte) byte1, (byte) byte2, (byte) byte3};
					midiEventListener.onMidiSystemCommonMessage(sender, cable, bytes);
				}
					break;
				case 4:
					// sysex starts, and has next
					synchronized (this) {
						if (systemExclusive == null) {
							systemExclusive = new ByteArrayOutputStream();
						}
					}
					synchronized (systemExclusive) {
						systemExclusive.write(byte1);
						systemExclusive.write(byte2);
						systemExclusive.write(byte3);
					}
					break;
				case 5:
					// system common message with 1byte
					// sysex end with 1 byte
					if (systemExclusive == null) {
						byte[] bytes = new byte[]{(byte) byte1};
						midiEventListener.onMidiSystemCommonMessage(sender, cable, bytes);
					} else {
						synchronized (systemExclusive) {
							systemExclusive.write(byte1);
							midiEventListener.onMidiSystemExclusive(sender, cable, systemExclusive.toByteArray());
						}
						synchronized (this) {
							systemExclusive = null;
						}
					}
					break;
				case 6:
					// sysex end with 2 bytes
					if (systemExclusive != null) {
						synchronized (systemExclusive) {
							systemExclusive.write(byte1);
							systemExclusive.write(byte2);
							midiEventListener.onMidiSystemExclusive(sender, cable, systemExclusive.toByteArray());
						}
						synchronized (this) {
							systemExclusive = null;
						}
					}
					break;
				case 7:
					// sysex end with 3 bytes
					if (systemExclusive != null) {
						synchronized (systemExclusive) {
							systemExclusive.write(byte1);
							systemExclusive.write(byte2);
							systemExclusive.write(byte3);
							midiEventListener.onMidiSystemExclusive(sender, cable, systemExclusive.toByteArray());
						}
						synchronized (this) {
							systemExclusive = null;
						}
					}
					break;
				case 8:
					midiEventListener.onMidiNoteOff(sender, cable, byte1 & 0xf, byte2, byte3);
					break;
				case 9:
					midiEventListener.onMidiNoteOn(sender, cable, byte1 & 0xf, byte2, byte3);
					break;
				case 10:
					// poly key press
					midiEventListener.onMidiPolyphonicAftertouch(sender, cable, byte1 & 0xf, byte2, byte3);
					break;
				case 11:
					// control change
					midiEventListener.onMidiControlChange(sender, cable, byte1 & 0xf, byte2, byte3);
					break;
				case 12:
					// program change
					midiEventListener.onMidiProgramChange(sender, cable, byte1 & 0xf, byte2);
					break;
				case 13:
					// channel pressure
					midiEventListener.onMidiChannelAftertouch(sender, cable, byte1 & 0xf, byte2);
					break;
				case 14:
					// pitch bend
					midiEventListener.onMidiPitchWheel(sender, cable, byte1 & 0xf, byte2 | (byte3 << 8));
					break;
				case 15:
					// single byte
					midiEventListener.onMidiSingleByte(sender, cable, byte1);
					break;
				default:
					// do nothing.
					break;
			}
		}
		return false;
	}
}
