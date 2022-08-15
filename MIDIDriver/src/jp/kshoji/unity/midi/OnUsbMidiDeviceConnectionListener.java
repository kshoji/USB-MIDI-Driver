package jp.kshoji.unity.midi;

/**
 * Listener for MIDI connection events
 * 
 * @author K.Shoji
 */
public interface OnUsbMidiDeviceConnectionListener {

    /**
     * MIDI input device has been attached
     *
     * @param deviceId attached MIDI Input device ID
     */
    void onMidiInputDeviceAttached(String deviceId);

    /**
     * MIDI output device has been attached
     *
     * @param deviceId attached MIDI Output device ID
     */
    void onMidiOutputDeviceAttached(String deviceId);

    /**
     * MIDI input device has been detached
     *
     * @param deviceId detached MIDI Input device ID
     */
    void onMidiInputDeviceDetached(String deviceId);

    /**
     * MIDI output device has been detached
     *
     * @param deviceId detached MIDI Output device ID
     */
    void onMidiOutputDeviceDetached(String deviceId);
}
