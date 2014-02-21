package jp.kshoji.javax.sound.midi;

import java.util.Arrays;

/**
 * Represents MIDI Meta Message
 * 
 * @author K.Shoji
 */
public class MetaMessage extends MidiMessage {
	public static final int META = 0xff;
	
	public static final int TYPE_END_OF_TRACK = 0x2f;
	
	private static final byte[] defaultMessage = { (byte) META, 0 };

	private int dataLength = 0;

	/**
	 * Default constructor.
	 */
	public MetaMessage() {
		this(defaultMessage);
	}

	/**
	 * Constructor with raw data.
	 * 
	 * @param data
	 * @throws NegativeArraySizeException MUST be caught. We can't throw {@link InvalidMidiDataException} because of API compatibility.
	 */
	protected MetaMessage(byte[] data) throws NegativeArraySizeException {
		super(data);

		if (data.length >= 3) {
			dataLength = data.length - 3;
			int pos = 2;
			while (pos < data.length && (data[pos] & 0x80) != 0) {
				dataLength--;
				pos++;
			}
		}

		if (dataLength < 0) {
			// 'dataLength' may negative value. Negative 'dataLength' will throw NegativeArraySizeException when getData() called.
			throw new NegativeArraySizeException("Invalid meta event. data: " + Arrays.toString(data));
		}
	}

	/**
	 * Set the entire informations of message.
	 * 
	 * @param type
	 * @param data
	 * @param length unused parameter. Use always data.length
	 * @throws InvalidMidiDataException
	 */
	public void setMessage(int type, byte[] data, int length) throws InvalidMidiDataException {
		if (type >= 128 || type < 0) {
			throw new InvalidMidiDataException("Invalid meta event. type: " + type);
		}

		this.dataLength = data.length;
		this.data = new byte[2 + getMidiValuesLength(data.length) + data.length];
		this.data[0] = (byte) META;
		this.data[1] = (byte) type;
		writeMidiValues(this.data, 2, data.length);
		if (this.data.length > 0) {
			System.arraycopy(data, 0, this.data, this.data.length - this.dataLength, this.dataLength);
		}
	}

	/**
	 * Get the type of {@link MetaMessage}
	 * 
	 * @return
	 */
	public int getType() {
		if (data.length >= 2) {
			return data[1] & 0xff;
		}
		return 0;
	}

	/**
	 * Get the data of {@link MetaMessage}
	 * 
	 * @return
	 */
	public byte[] getData() {
		byte[] returnedArray = new byte[dataLength];
		System.arraycopy(data, (data.length - dataLength), returnedArray, 0, dataLength);
		return returnedArray;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
	public Object clone() {
		byte[] result = new byte[data.length];
		System.arraycopy(data, 0, result, 0, data.length);
		return new MetaMessage(result);
	}

	private static int getMidiValuesLength(long value) {
		int length = 0;
		long currentValue = value;
		do {
			currentValue = currentValue >> 7;
			length++;
		} while (currentValue > 0);
		return length;
	}

	private static void writeMidiValues(byte[] data, int off, long value) {
		int shift = 63;
		while ((shift > 0) && ((value & (0x7f << shift)) == 0)) {
			shift -= 7;
		}
		int currentOff = off;
		while (shift > 0) {
			data[currentOff++] = (byte) (((value & (0x7f << shift)) >> shift) | 0x80);
			shift -= 7;
		}
		data[currentOff] = (byte) (value & 0x7f);
	}
}
