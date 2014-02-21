package jp.kshoji.javax.sound.midi;

/**
 * {@link MidiMessage} transmitter.
 * 
 * @author K.Shoji
 */
public interface Transmitter {
	/**
	 * Set the {@link Receiver} for this {@link Transmitter}
	 * @param receiver
	 */
	void setReceiver(Receiver receiver);

	/**
	 * Get the {@link Receiver} for this {@link Transmitter}
	 * @return
	 */
	Receiver getReceiver();

	/**
	 * Close the {@link Transmitter}
	 */
	void close();
}
