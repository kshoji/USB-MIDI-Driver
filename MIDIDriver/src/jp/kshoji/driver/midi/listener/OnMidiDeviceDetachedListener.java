package jp.kshoji.driver.midi.listener;

import android.hardware.usb.UsbDevice;
import android.support.annotation.NonNull;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;

/**
 * Listener for MIDI detached events
 * 
 * @author K.Shoji
 */
public interface OnMidiDeviceDetachedListener {

    /**
     * device has been detached
     *
     * @param usbDevice the detached UsbDevice
     */
    @Deprecated
    void onDeviceDetached(@NonNull UsbDevice usbDevice);

    /**
     * MIDI input device has been detached
     *
     * @param midiInputDevice detached MIDI Input device
     */
    void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice);

    /**
     * MIDI output device has been detached
     *
     * @param midiOutputDevice detached MIDI Output device
     */
    void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice);
}
