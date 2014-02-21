package jp.kshoji.javax.sound.midi;

/**
 * Represents MIDI SysEx Message
 * 
 * @author K.Shoji
 */
public class SysexMessage extends MidiMessage {
	/**
	 * Default constructor.
	 */
	public SysexMessage() {
		this(new byte[] { (byte) (ShortMessage.START_OF_EXCLUSIVE & 0xff), (byte) (ShortMessage.END_OF_EXCLUSIVE & 0xff) });
	}

	/**
	 * Constructor with raw data.
	 * 
	 * @param data
	 */
	protected SysexMessage(byte[] data) {
		super(data);
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.javax.sound.midi.MidiMessage#setMessage(byte[], int)
	 */
	@Override
	public void setMessage(byte[] data, int length) throws InvalidMidiDataException {
		int status = (data[0] & 0xff);
		if ((status != ShortMessage.START_OF_EXCLUSIVE) && (status != ShortMessage.END_OF_EXCLUSIVE)) {
			throw new InvalidMidiDataException("Invalid status byte for SysexMessage: 0x" + Integer.toHexString(status));
		}
		super.setMessage(data, length);
	}

	/**
	 * Set the entire informations of message.
	 * 
	 * @param status must be ShortMessage.START_OF_EXCLUSIVE or ShortMessage.END_OF_EXCLUSIVE
	 * @param data
	 * @param length unused parameter. Use always data.length
	 * @throws InvalidMidiDataException
	 */
	public void setMessage(int status, byte[] data, int length) throws InvalidMidiDataException {
		if ((status != ShortMessage.START_OF_EXCLUSIVE) && (status != ShortMessage.END_OF_EXCLUSIVE)) {
			throw new InvalidMidiDataException("Invalid status byte for SysexMessage: 0x" + Integer.toHexString(status));
		}

		if (this.data == null || this.data.length < data.length + 1) {
			// extend 1 byte
			this.data = new byte[data.length + 1];
		}

		this.data[0] = (byte) (status & 0xff);
		if (data.length > 0) {
			System.arraycopy(data, 0, this.data, 1, data.length);
		}
	}

	/**
	 * Get the SysEx data.
	 * 
	 * @return
	 */
	public byte[] getData() {
		byte[] result = new byte[data.length];
		System.arraycopy(data, 0, result, 0, result.length);
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		return new SysexMessage(getData());
	}
}
