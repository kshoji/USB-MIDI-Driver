package jp.kshoji.driver.midi.fragment;

import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import android.app.Fragment;

/**
 * Base {@link Fragment} for using USB MIDI interface.
 * 
 * @author K.Shoji
 */
public abstract class AbstractMidiFragment extends Fragment implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {
}