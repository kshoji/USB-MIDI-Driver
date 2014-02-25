package jp.kshoji.javax.sound.midi;

import java.util.EventListener;

/**
 * {@link EventListener} for MIDI Control Change messages.
 * 
 * @author K.Shoji
 */
public interface ControllerEventListener extends EventListener {
	/**
	 * Called at {@link ShortMessage} event has fired
	 * 
	 * @param event
	 */
	void controlChange(ShortMessage event);
}
