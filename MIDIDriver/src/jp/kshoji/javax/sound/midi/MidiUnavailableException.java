package jp.kshoji.javax.sound.midi;

public class MidiUnavailableException extends Exception {
	private static final long serialVersionUID = 6093809578628944323L;

	public MidiUnavailableException() {
		super();
	}

	public MidiUnavailableException(String message) {
		super(message);
	}
}
