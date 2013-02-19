package javax.sound.midi;

public interface Transmitter {
	void setReceiver(Receiver receiver);

	Receiver getReceiver();

	void close();
}
