package jp.kshoji.javax.sound.midi;

import java.util.Arrays;

public abstract class MidiMessage implements Cloneable {
	protected byte[] data;

	protected MidiMessage(byte[] data) {
		this.data = data;
	}

	/**
	 * 
	 * @param data
	 * @param length unused parameter. Use always data.length
	 * @throws InvalidMidiDataException
	 */
	protected void setMessage(byte[] data, int length) throws InvalidMidiDataException {
		if (this.data == null) {
			this.data = new byte[data.length];
		}
		System.arraycopy(data, 0, this.data, 0, data.length);
	}

	public byte[] getMessage() {
		if (data == null) {
			return null;
		}
		byte[] resultArray = new byte[data.length];
		System.arraycopy(data, 0, resultArray, 0, data.length);
		return resultArray;
	}

	public int getStatus() {
		if (data != null && data.length > 0) {
			return (data[0] & 0xff);
		}
		return 0;
	}

	public int getLength() {
		if (data == null) {
			return 0;
		}
		return data.length;
	}

	@Override
	public String toString() {
		return getClass().getName() + ":" + Arrays.toString(data);
	}
}
