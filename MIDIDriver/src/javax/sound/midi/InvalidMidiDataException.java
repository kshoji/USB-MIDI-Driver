package javax.sound.midi;

public class InvalidMidiDataException extends Exception {
	private static final long serialVersionUID = 2780771756789932067L;

	public InvalidMidiDataException() {
		super();
	}

	public InvalidMidiDataException(String message) {
		super(message);
	}
}
