package jp.kshoji.driver.midi.listener;

import android.hardware.usb.UsbDevice;
import android.support.annotation.NonNull;

import jp.kshoji.driver.midi.device.Midi2InputDevice;
import jp.kshoji.driver.midi.device.Midi2OutputDevice;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;

/**
 * Listener for MIDI attached events
 * 
 * @author K.Shoji
 */
public interface OnMidiDeviceAttachedListener {

    /**
     * device has been attached
     *
     * @param usbDevice the attached UsbDevice
     */
    @Deprecated
    void onDeviceAttached(@NonNull UsbDevice usbDevice);

    /**
     * MIDI input device has been attached
     *
     * @param midiInputDevice attached MIDI Input device
     */
    void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice);

    /**
     * MIDI output device has been attached
     *
     * @param midiOutputDevice attached MIDI Output device
     */
    void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice);

    /**
     * MIDI 2.0 input device has been attached
     *
     * @param midiInputDevice attached MIDI Input device
     */
    void onMidi2InputDeviceAttached(@NonNull Midi2InputDevice midiInputDevice);

    /**
     * MIDI 2.0 output device has been attached
     *
     * @param midiOutputDevice attached MIDI Output device
     */
    void onMidi2OutputDeviceAttached(@NonNull Midi2OutputDevice midiOutputDevice);
}
