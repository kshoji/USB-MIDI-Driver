package jp.kshoji.javax.sound.midi;

public class SysexMessage extends MidiMessage {
	public SysexMessage() {
		this(new byte[] { (byte) (ShortMessage.START_OF_EXCLUSIVE & 0xff), (byte) (ShortMessage.END_OF_EXCLUSIVE & 0xff) });
	}

	protected SysexMessage(byte[] data) {
		super(data);
	}

	@Override
	public void setMessage(byte[] data, int length) throws InvalidMidiDataException {
		int status = (data[0] & 0xff);
		if ((status != ShortMessage.START_OF_EXCLUSIVE) && (status != ShortMessage.END_OF_EXCLUSIVE)) {
			throw new InvalidMidiDataException("Invalid status byte for SysexMessage: 0x" + Integer.toHexString(status));
		}
		super.setMessage(data, length);
	}

	/**
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
			this.data = new byte[data.length + 1];
		}

		this.data[0] = (byte) (status & 0xff);
		if (data.length > 0) {
			System.arraycopy(data, 0, this.data, 1, data.length);
		}
	}

	public byte[] getData() {
		byte[] returnedArray = new byte[data.length - 1];
		System.arraycopy(data, 1, returnedArray, 0, (data.length - 1));
		return returnedArray;
	}

	@Override
	public Object clone() {
		byte[] result = new byte[data.length];
		System.arraycopy(data, 0, result, 0, result.length);
		return new SysexMessage(result);
	}
}
