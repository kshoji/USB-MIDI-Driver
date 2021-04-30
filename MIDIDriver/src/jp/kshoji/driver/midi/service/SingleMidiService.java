package jp.kshoji.driver.midi.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import jp.kshoji.driver.midi.device.Midi2InputDevice;
import jp.kshoji.driver.midi.device.Midi2OutputDevice;
import jp.kshoji.driver.midi.device.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidi2InputEventListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.util.Constants;

/**
 * Service for single MIDI connection
 *
 * @author <a href="https://github.com/akapelrud">akapelrud</a>
 * @author K.Shoji
 */
public final class SingleMidiService extends Service {

    /**
     * Binder for this Service
     */
    public class LocalBinder extends Binder {

        /**
         * Get the Service
         *
         * @return the Service
         */
        @NonNull
        public SingleMidiService getService() {
            return SingleMidiService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    private MidiInputDevice midiInputDevice = null;
    private MidiOutputDevice midiOutputDevice = null;
    private final OnMidi2InputEventListener midi2InputEventListener = null;
    private final OnMidi2InputEventListener serviceMidi2InputEventListener = new OnMidi2InputEventListener() {

        @Override
        public void onMidiNoop(Midi2InputDevice sender, int group) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiNoop(sender, group);
            }
        }

        @Override
        public void onMidiJitterReductionClock(Midi2InputDevice sender, int group, int senderClockTime) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiJitterReductionClock(sender, group, senderClockTime);
            }
        }

        @Override
        public void onMidiJitterReductionTimestamp(Midi2InputDevice sender, int group, int senderClockTimestamp) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiJitterReductionTimestamp(sender, group, senderClockTimestamp);
            }
        }

        @Override
        public void onMidiTimeCodeQuarterFrame(@NonNull Midi2InputDevice sender, int group, int timing) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiTimeCodeQuarterFrame(sender, group, timing);
            }
        }

        @Override
        public void onMidiSongSelect(@NonNull Midi2InputDevice sender, int group, int song) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiSongSelect(sender, group, song);
            }
        }

        @Override
        public void onMidiSongPositionPointer(@NonNull Midi2InputDevice sender, int group, int position) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiSongPositionPointer(sender, group, position);
            }
        }

        @Override
        public void onMidiTuneRequest(@NonNull Midi2InputDevice sender, int group) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiTuneRequest(sender, group);
            }
        }

        @Override
        public void onMidiTimingClock(@NonNull Midi2InputDevice sender, int group) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiTimingClock(sender, group);
            }
        }

        @Override
        public void onMidiStart(@NonNull Midi2InputDevice sender, int group) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiStart(sender, group);
            }
        }

        @Override
        public void onMidiContinue(@NonNull Midi2InputDevice sender, int group) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiContinue(sender, group);
            }
        }

        @Override
        public void onMidiStop(@NonNull Midi2InputDevice sender, int group) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiStop(sender, group);
            }
        }

        @Override
        public void onMidiActiveSensing(@NonNull Midi2InputDevice sender, int group) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiActiveSensing(sender, group);
            }
        }

        @Override
        public void onMidiReset(@NonNull Midi2InputDevice sender, int group) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiReset(sender, group);
            }
        }

        @Override
        public void onMidi1NoteOff(Midi2InputDevice sender, int group, int channel, int note, int velocity) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi1NoteOff(sender, group, channel, note, velocity);
            }
        }

        @Override
        public void onMidi1NoteOn(Midi2InputDevice sender, int group, int channel, int note, int velocity) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi1NoteOn(sender, group, channel, note, velocity);
            }
        }

        @Override
        public void onMidi1PolyphonicAftertouch(Midi2InputDevice sender, int group, int channel, int note, int pressure) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi1PolyphonicAftertouch(sender, group, channel, note, pressure);
            }
        }

        @Override
        public void onMidi1ControlChange(Midi2InputDevice sender, int group, int channel, int function, int value) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi1ControlChange(sender, group, channel, function, value);
            }
        }

        @Override
        public void onMidi1ProgramChange(Midi2InputDevice sender, int group, int channel, int program) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi1ProgramChange(sender, group, channel, program);
            }
        }

        @Override
        public void onMidi1ChannelAftertouch(Midi2InputDevice sender, int group, int channel, int pressure) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi1ChannelAftertouch(sender, group, channel, pressure);
            }
        }

        @Override
        public void onMidi1PitchWheel(Midi2InputDevice sender, int group, int channel, int amount) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi1PitchWheel(sender, group, channel, amount);
            }
        }

        @Override
        public void onMidi1SystemExclusive(Midi2InputDevice sender, int group, @NonNull byte[] systemExclusive) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi1SystemExclusive(sender, group, systemExclusive);
            }
        }

        @Override
        public void onMidi2NoteOff(Midi2InputDevice sender, int group, int channel, int note, int velocity, int attributeType, int attributeData) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi2NoteOff(sender, group, channel, note, velocity, attributeType, attributeData);
            }
        }

        @Override
        public void onMidi2NoteOn(Midi2InputDevice sender, int group, int channel, int note, int velocity, int attributeType, int attributeData) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi2NoteOn(sender, group, channel, note, velocity, attributeType, attributeData);
            }
        }

        @Override
        public void onMidi2PolyphonicAftertouch(Midi2InputDevice sender, int group, int channel, int note, long pressure) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi2PolyphonicAftertouch(sender, group, channel, note, pressure);
            }
        }

        @Override
        public void onMidi2ControlChange(Midi2InputDevice sender, int group, int channel, int index, long value) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi2ControlChange(sender, group, channel, index, value);
            }
        }

        @Override
        public void onMidi2ProgramChange(Midi2InputDevice sender, int group, int channel, int optionFlags, int program, int bank) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi2ProgramChange(sender, group, channel, optionFlags, program, bank);
            }
        }

        @Override
        public void onMidi2ChannelAftertouch(Midi2InputDevice sender, int group, int channel, long pressure) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi2ChannelAftertouch(sender, group, channel, pressure);
            }
        }

        @Override
        public void onMidi2PitchWheel(Midi2InputDevice sender, int group, int channel, long amount) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi2PitchWheel(sender, group, channel, amount);
            }
        }

        @Override
        public void onMidiPerNotePitchWheel(Midi2InputDevice sender, int group, int channel, int note, long amount) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiPerNotePitchWheel(sender, group, channel, note, amount);
            }
        }

        @Override
        public void onMidiPerNoteManagement(Midi2InputDevice sender, int group, int channel, int note, int optionFlags) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiPerNoteManagement(sender, group, channel, note, optionFlags);
            }
        }

        @Override
        public void onMidiRegisteredPerNoteController(Midi2InputDevice sender, int group, int channel, int note, int index, long data) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiRegisteredPerNoteController(sender, group, channel, note, index, data);
            }
        }

        @Override
        public void onMidiAssignablePerNoteController(Midi2InputDevice sender, int group, int channel, int note, int index, long data) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiAssignablePerNoteController(sender, group, channel, note, index, data);
            }
        }

        @Override
        public void onMidiRegisteredController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiRegisteredController(sender, group, channel, bank, index, data);
            }
        }

        @Override
        public void onMidiAssignableController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiAssignableController(sender, group, channel, bank, index, data);
            }
        }

        @Override
        public void onMidiRelativeRegisteredController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiRelativeRegisteredController(sender, group, channel, bank, index, data);
            }
        }

        @Override
        public void onMidiRelativeAssignableController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiRelativeAssignableController(sender, group, channel, bank, index, data);
            }
        }

        @Override
        public void onMidi2SystemExclusive(Midi2InputDevice sender, int group, int streamId, @NonNull byte[] systemExclusive) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidi2SystemExclusive(sender, group, streamId, systemExclusive);
            }
        }

        @Override
        public void onMidiMixedDataSetHeader(Midi2InputDevice sender, int group, int mdsId, @NonNull byte[] headers) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiMixedDataSetHeader(sender, group, mdsId, headers);
            }
        }

        @Override
        public void onMidiMixedDataSetPayload(Midi2InputDevice sender, int group, int mdsId, @NonNull byte[] payloads) {
            if (midi2InputEventListener != null) {
                midi2InputEventListener.onMidiMixedDataSetPayload(sender, group, mdsId, payloads);
            }
        }
    };
    private MidiDeviceConnectionWatcher deviceConnectionWatcher = null;
    private OnMidiInputEventListener midiInputEventListener = null;
    private Midi2InputDevice midi2InputDevice = null;

    private boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            Log.d(Constants.TAG, "MIDI service starting.");

            UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            // runs in separate thread
            deviceConnectionWatcher = new MidiDeviceConnectionWatcher(this, usbManager, midiDeviceAttachedListener, midiDeviceDetachedListener);

            isRunning = true;
        }

        return START_REDELIVER_INTENT; // must be restarted if stopped by the system, We must respond to midi events(!)
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private Midi2OutputDevice midi2OutputDevice = null;
    private final OnMidiDeviceAttachedListener midiDeviceAttachedListener = new OnMidiDeviceAttachedListener() {

        @Override
        public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
            Log.d(Constants.TAG, "USB MIDI Device " + usbDevice.getDeviceName() + " has been attached.");
        }

        @Override
        public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {
            if (SingleMidiService.this.midiInputDevice != null) {
                return;
            }

            SingleMidiService.this.midiInputDevice = midiInputDevice;
            SingleMidiService.this.midiInputDevice.setMidiEventListener(serviceMidiInputEventListener);
        }

        @Override
        public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {
            if (SingleMidiService.this.midiOutputDevice != null) {
                return;
            }

            SingleMidiService.this.midiOutputDevice = midiOutputDevice;
        }

        @Override
        public void onMidi2InputDeviceAttached(@NonNull Midi2InputDevice midi2InputDevice) {
            if (SingleMidiService.this.midi2InputDevice != null) {
                return;
            }

            SingleMidiService.this.midi2InputDevice = midi2InputDevice;
            SingleMidiService.this.midi2InputDevice.setMidiEventListener(serviceMidi2InputEventListener);
        }

        @Override
        public void onMidi2OutputDeviceAttached(@NonNull Midi2OutputDevice midi2OutputDevice) {
            if (SingleMidiService.this.midi2OutputDevice != null) {
                return;
            }

            SingleMidiService.this.midi2OutputDevice = midi2OutputDevice;
        }
    };
    private final OnMidiDeviceDetachedListener midiDeviceDetachedListener = new OnMidiDeviceDetachedListener() {
        @Override
        public void onDeviceDetached(@NonNull UsbDevice detachedDevice) {
            Log.d(Constants.TAG, "USB MIDI Device " + detachedDevice.getDeviceName() + " has been detached.");
        }

        @Override
        public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {
            if (SingleMidiService.this.midiInputDevice == midiInputDevice) {
                SingleMidiService.this.midiInputDevice.setMidiEventListener(null);
                SingleMidiService.this.midiInputDevice = null;
            }
        }

        @Override
        public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {
            if (SingleMidiService.this.midiOutputDevice == midiOutputDevice) {
                SingleMidiService.this.midiOutputDevice = null;
            }
        }

        @Override
        public void onMidi2InputDeviceDetached(@NonNull Midi2InputDevice midiInputDevice) {
            if (SingleMidiService.this.midi2InputDevice == midiInputDevice) {
                SingleMidiService.this.midi2InputDevice.setMidiEventListener(null);
                SingleMidiService.this.midi2InputDevice = null;
            }
        }

        @Override
        public void onMidi2OutputDeviceDetached(@NonNull Midi2OutputDevice midiOutputDevice) {
            if (SingleMidiService.this.midi2OutputDevice == midiOutputDevice) {
                SingleMidiService.this.midi2OutputDevice = null;
            }
        }
    };

    /**
     * Set the {@link OnMidiInputEventListener} to receive MIDI events.
     *
     * @param midiInputEventListener the listener
     */
    public void setOnMidiInputEventListener(@Nullable OnMidiInputEventListener midiInputEventListener) {
        this.midiInputEventListener = midiInputEventListener;
    }

    /**
     * Get {@link MidiOutputDevice} to send MIDI events.
     *
     * @return the MidiOutputDevice, null if not available
     */
    @Nullable
    public MidiOutputDevice getMidiOutputDevice() {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }

        return midiOutputDevice;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.stop(new Runnable() {
                @Override
                public void run() {
                    deviceConnectionWatcher = null;

                    midiInputDevice = null;
                    midi2InputDevice = null;

                    midiOutputDevice = null;
                    midi2OutputDevice = null;

                    Log.d(Constants.TAG, "MIDI service stopped.");
                }
            });
        } else {
            midiInputDevice = null;
            midi2InputDevice = null;

            midiOutputDevice = null;
            midi2OutputDevice = null;

            Log.d(Constants.TAG, "MIDI service stopped.");
        }
    }

    /**
     * Suspends event listening / sending
     */
    public void suspend() {
        if (midiInputDevice != null) {
            midiInputDevice.suspend();
        }

        if (midiOutputDevice != null) {
            midiOutputDevice.suspend();
        }

        if (midi2InputDevice != null) {
            midi2InputDevice.suspend();
        }

        if (midi2OutputDevice != null) {
            midi2OutputDevice.suspend();
        }
    }

    private final OnMidiInputEventListener serviceMidiInputEventListener = new OnMidiInputEventListener() {

        @Override
        public void onMidiMiscellaneousFunctionCodes(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiMiscellaneousFunctionCodes(sender, cable, byte1, byte2, byte3);
            }
        }

        @Override
        public void onMidiCableEvents(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiCableEvents(sender, cable, byte1, byte2, byte3);
            }
        }

        @Override
        public void onMidiSystemCommonMessage(@NonNull MidiInputDevice sender, int cable, byte[] bytes) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiSystemCommonMessage(sender, cable, bytes);
            }
        }

        @Override
        public void onMidiSystemExclusive(@NonNull MidiInputDevice sender, int cable, byte[] systemExclusive) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiSystemExclusive(sender, cable, systemExclusive);
            }
        }

        @Override
        public void onMidiNoteOff(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiNoteOff(sender, cable, channel, note, velocity);
            }
        }

        @Override
        public void onMidiNoteOn(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiNoteOn(sender, cable, channel, note, velocity);
            }
        }

        @Override
        public void onMidiPolyphonicAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int note, int pressure) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiPolyphonicAftertouch(sender, cable, channel, note, pressure);
            }
        }

        @Override
        public void onMidiControlChange(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiControlChange(sender, cable, channel, function, value);
            }
        }

        @Override
        public void onMidiProgramChange(@NonNull MidiInputDevice sender, int cable, int channel, int program) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiProgramChange(sender, cable, channel, program);
            }
        }

        @Override
        public void onMidiChannelAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int pressure) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiChannelAftertouch(sender, cable, channel, pressure);
            }
        }

        @Override
        public void onMidiPitchWheel(@NonNull MidiInputDevice sender, int cable, int channel, int amount) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiPitchWheel(sender, cable, channel, amount);
            }
        }

        @Override
        public void onMidiSingleByte(@NonNull MidiInputDevice sender, int cable, int byte1) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiSingleByte(sender, cable, byte1);
            }
        }

        @Override
        public void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice sender, int cable, int timing) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiTimeCodeQuarterFrame(sender, cable, timing);
            }
        }

        @Override
        public void onMidiSongSelect(@NonNull MidiInputDevice sender, int cable, int song) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiSongSelect(sender, cable, song);
            }
        }

        @Override
        public void onMidiSongPositionPointer(@NonNull MidiInputDevice sender, int cable, int position) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiSongPositionPointer(sender, cable, position);
            }
        }

        @Override
        public void onMidiTuneRequest(@NonNull MidiInputDevice sender, int cable) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiTuneRequest(sender, cable);
            }
        }

        @Override
        public void onMidiTimingClock(@NonNull MidiInputDevice sender, int cable) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiTimingClock(sender, cable);
            }
        }

        @Override
        public void onMidiStart(@NonNull MidiInputDevice sender, int cable) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiStart(sender, cable);
            }
        }

        @Override
        public void onMidiContinue(@NonNull MidiInputDevice sender, int cable) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiContinue(sender, cable);
            }
        }

        @Override
        public void onMidiStop(@NonNull MidiInputDevice sender, int cable) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiStop(sender, cable);
            }
        }

        @Override
        public void onMidiActiveSensing(@NonNull MidiInputDevice sender, int cable) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiActiveSensing(sender, cable);
            }
        }

        @Override
        public void onMidiReset(@NonNull MidiInputDevice sender, int cable) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiReset(sender, cable);
            }
        }

        @Override
        public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiRPNReceived(sender, cable, channel, function, valueMSB, valueLSB);
            }
        }

        @Override
        public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiNRPNReceived(sender, cable, channel, function, valueMSB, valueLSB);
            }
        }

        @Override
        public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiRPNReceived(sender, cable, channel, function, value);
            }
        }

        @Override
        public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
            if (midiInputEventListener != null) {
                midiInputEventListener.onMidiNRPNReceived(sender, cable, channel, function, value);
            }
        }
    };

    /**
     * Resumes from {@link #suspend()}
     */
    public void resume() {
        if (midiInputDevice != null) {
            midiInputDevice.resume();
        }

        if (midiOutputDevice != null) {
            midiOutputDevice.resume();
        }

        if (midi2InputDevice != null) {
            midi2InputDevice.resume();
        }

        if (midi2OutputDevice != null) {
            midi2OutputDevice.resume();
        }
    }
}
