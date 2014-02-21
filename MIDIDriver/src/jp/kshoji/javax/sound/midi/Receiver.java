package jp.kshoji.javax.sound.midi;

/**
 * {@link MidiMessage} receiver.
 * 
 * @author K.Shoji
 */
public interface Receiver {
	/**
	 * Called at {@link MidiMessage} receiving
	 * 
	 * @param message
	 * @param timeStamp -1 if the timeStamp information is not available
	 */
	void send(MidiMessage message, long timeStamp);

	/**
	 * Closes the {@link Receiver}
	 */
	void close();
}
