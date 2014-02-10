package jp.kshoji.javax.sound.midi;

import java.util.EventListener;

public interface ControllerEventListener extends EventListener {
	/**
	 * Called at {@link ShortMessage} event has fired
	 * 
	 * @param event
	 */
	void controlChange(ShortMessage event);
}
