package jp.kshoji.javax.sound.midi;

/**
 * {@link Exception} for invalid MIDI data.
 * 
 * @author K.Shoji
 */
public class InvalidMidiDataException extends Exception {
	private static final long serialVersionUID = 2780771756789932067L;

	public InvalidMidiDataException() {
		super();
	}

	public InvalidMidiDataException(String message) {
		super(message);
	}
}
