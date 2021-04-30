package jp.kshoji.driver.midi.activity;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jp.kshoji.driver.midi.device.Midi2InputDevice;
import jp.kshoji.driver.midi.device.Midi2OutputDevice;
import jp.kshoji.driver.midi.device.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.fragment.AbstractMidiFragment;
import jp.kshoji.driver.midi.listener.OnMidi2InputEventListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;

/**
 * base Activity for using {@link AbstractMidiFragment}s.
 * In this implement, each devices will be detected on connect.
 * launchMode must be "singleTask" or "singleInstance".
 *
 * @author K.Shoji
 */
public class MidiFragmentHostActivity extends Activity implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener, OnMidi2InputEventListener {
    Set<MidiInputDevice> midiInputDevices = null;
    Set<MidiOutputDevice> midiOutputDevices = null;
    OnMidiDeviceAttachedListener deviceAttachedListener = null;
    OnMidiDeviceDetachedListener deviceDetachedListener = null;
    MidiDeviceConnectionWatcher deviceConnectionWatcher = null;
    Set<Midi2InputDevice> midi2InputDevices = null;
    Set<Midi2OutputDevice> midi2OutputDevices = null;
    final List<WeakReference<Fragment>> attachedFragments = new ArrayList<>();

    @Override
    public void onMidiNoop(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiJitterReductionClock(Midi2InputDevice sender, int group, int senderClockTime) {

    }

    @Override
    public void onMidiJitterReductionTimestamp(Midi2InputDevice sender, int group, int senderClockTimestamp) {

    }

    @Override
    public void onMidiTimeCodeQuarterFrame(Midi2InputDevice sender, int group, int timing) {

    }

    @Override
    public void onMidiSongSelect(Midi2InputDevice sender, int group, int song) {

    }

    @Override
    public void onMidiSongPositionPointer(Midi2InputDevice sender, int group, int position) {

    }

    @Override
    public void onMidiTuneRequest(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiTimingClock(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiStart(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiContinue(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiStop(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiActiveSensing(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidiReset(Midi2InputDevice sender, int group) {

    }

    @Override
    public void onMidi1NoteOff(Midi2InputDevice sender, int group, int channel, int note, int velocity) {

    }

    @Override
    public void onMidi1NoteOn(Midi2InputDevice sender, int group, int channel, int note, int velocity) {

    }

    @Override
    public void onMidi1PolyphonicAftertouch(Midi2InputDevice sender, int group, int channel, int note, int pressure) {

    }

    @Override
    public void onMidi1ControlChange(Midi2InputDevice sender, int group, int channel, int function, int value) {

    }

    @Override
    public void onMidi1ProgramChange(Midi2InputDevice sender, int group, int channel, int program) {

    }

    @Override
    public void onMidi1ChannelAftertouch(Midi2InputDevice sender, int group, int channel, int pressure) {

    }

    @Override
    public void onMidi1PitchWheel(Midi2InputDevice sender, int group, int channel, int amount) {

    }

    @Override
    public void onMidi1SystemExclusive(Midi2InputDevice sender, int group, @NonNull byte[] systemExclusive) {

    }

    @Override
    public void onMidi2NoteOff(Midi2InputDevice sender, int group, int channel, int note, int velocity, int attributeType, int attributeData) {

    }

    @Override
    public void onMidi2NoteOn(Midi2InputDevice sender, int group, int channel, int note, int velocity, int attributeType, int attributeData) {

    }

    @Override
    public void onMidi2PolyphonicAftertouch(Midi2InputDevice sender, int group, int channel, int note, long pressure) {

    }

    @Override
    public void onMidi2ControlChange(Midi2InputDevice sender, int group, int channel, int index, long value) {

    }

    @Override
    public void onMidi2ProgramChange(Midi2InputDevice sender, int group, int channel, int optionFlags, int program, int bank) {

    }

    @Override
    public void onMidi2ChannelAftertouch(Midi2InputDevice sender, int group, int channel, long pressure) {

    }

    @Override
    public void onMidi2PitchWheel(Midi2InputDevice sender, int group, int channel, long amount) {

    }

    @Override
    public void onMidiPerNotePitchWheel(Midi2InputDevice sender, int group, int channel, int note, long amount) {

    }

    @Override
    public void onMidiPerNoteManagement(Midi2InputDevice sender, int group, int channel, int note, int optionFlags) {

    }

    @Override
    public void onMidiRegisteredPerNoteController(Midi2InputDevice sender, int group, int channel, int note, int index, long data) {

    }

    @Override
    public void onMidiAssignablePerNoteController(Midi2InputDevice sender, int group, int channel, int note, int index, long data) {

    }

    @Override
    public void onMidiRegisteredController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {

    }

    @Override
    public void onMidiAssignableController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {

    }

    @Override
    public void onMidiRelativeRegisteredController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {

    }

    @Override
    public void onMidiRelativeAssignableController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {

    }

    @Override
    public void onMidi2SystemExclusive(Midi2InputDevice sender, int group, int streamId, @NonNull byte[] systemExclusive) {

    }

    @Override
    public void onMidiMixedDataSetHeader(Midi2InputDevice sender, int group, int mdsId, @NonNull byte[] headers) {

    }

    @Override
    public void onMidiMixedDataSetPayload(Midi2InputDevice sender, int group, int mdsId, @NonNull byte[] payloads) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        midiInputDevices = new HashSet<>();
        midiOutputDevices = new HashSet<>();

        midi2InputDevices = new HashSet<>();
        midi2OutputDevices = new HashSet<>();

        UsbManager usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
        deviceAttachedListener = new OnMidiDeviceAttachedListenerImpl();
        deviceDetachedListener = new OnMidiDeviceDetachedListenerImpl();

        deviceConnectionWatcher = new MidiDeviceConnectionWatcher(getApplicationContext(), usbManager, deviceAttachedListener, deviceDetachedListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        deviceConnectionWatcher.stop(new Runnable() {
            @Override
            public void run() {
                deviceConnectionWatcher = null;

                if (midiInputDevices != null) {
                    midiInputDevices.clear();
                }
                midiInputDevices = null;

                if (midiOutputDevices != null) {
                    midiOutputDevices.clear();
                }
                midiOutputDevices = null;
            }
        });
    }

    /**
     * Suspends receiving/transmitting MIDI messages.
     * All events will be discarded until the devices being resumed.
     */
    public final void suspendMidiDevices() {
        if (midiInputDevices != null) {
            for (MidiInputDevice inputDevice : midiInputDevices) {
                if (inputDevice != null) {
                    inputDevice.suspend();
                }
            }
        }

        if (midiOutputDevices != null) {
            for (MidiOutputDevice outputDevice : midiOutputDevices) {
                if (outputDevice != null) {
                    outputDevice.suspend();
                }
            }
        }
    }

    /**
     * Resumes from {@link #suspendMidiDevices()}
     */
    public final void resumeMidiDevices() {
        if (midiInputDevices != null) {
            for (MidiInputDevice inputDevice : midiInputDevices) {
                if (inputDevice != null) {
                    inputDevice.resume();
                }
            }
        }

        if (midiOutputDevices != null) {
            for (MidiOutputDevice outputDevice : midiOutputDevices) {
                if (outputDevice != null) {
                    outputDevice.resume();
                }
            }
        }
    }

    /**
     * Get MIDI output device, if available.
     *
     * @return {@link Set<MidiOutputDevice>}
     */
    @NonNull
    public final Set<MidiOutputDevice> getMidiOutputDevices() {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }

        return Collections.unmodifiableSet(midiOutputDevices);
    }

    @Override
    public void onAttachFragment(Fragment fragment) {
        super.onAttachFragment(fragment);
        attachedFragments.add(new WeakReference<>(fragment));
    }

    /**
     * Get attached {@link AbstractMidiFragment}s. Invisible Fragments are also included.
     *
     * @return {@link AbstractMidiFragment}s attached with this Activity
     */
    @NonNull
    private List<AbstractMidiFragment> getMidiFragments() {
        ArrayList<AbstractMidiFragment> midiFragments = new ArrayList<>();

        for (WeakReference<Fragment> reference : attachedFragments) {
            Fragment fragment = reference.get();
            if (fragment instanceof AbstractMidiFragment) {
                midiFragments.add((AbstractMidiFragment) fragment);
            }
        }

        return midiFragments;
    }

    @Override
    public void onMidiMiscellaneousFunctionCodes(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiMiscellaneousFunctionCodes(sender, cable, byte1, byte2, byte3);
        }
    }

    @Override
    public void onMidiCableEvents(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiCableEvents(sender, cable, byte1, byte2, byte3);
        }
    }

    @Override
    public void onMidiSystemCommonMessage(@NonNull MidiInputDevice sender, int cable, byte[] bytes) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiSystemCommonMessage(sender, cable, bytes);
        }
    }

    @Override
    public void onMidiSystemExclusive(@NonNull MidiInputDevice sender, int cable, byte[] systemExclusive) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiSystemExclusive(sender, cable, systemExclusive);
        }
    }

    @Override
    public void onMidiNoteOff(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiNoteOff(sender, cable, channel, note, velocity);
        }
    }

    @Override
    public void onMidiNoteOn(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiNoteOn(sender, cable, channel, note, velocity);
        }
    }

    @Override
    public void onMidiPolyphonicAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int note, int pressure) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiPolyphonicAftertouch(sender, cable, channel, note, pressure);
        }
    }

    @Override
    public void onMidiControlChange(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiControlChange(sender, cable, channel, function, value);
        }
    }

    @Override
    public void onMidiProgramChange(@NonNull MidiInputDevice sender, int cable, int channel, int program) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiProgramChange(sender, cable, channel, program);
        }
    }

    @Override
    public void onMidiChannelAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int pressure) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiChannelAftertouch(sender, cable, channel, pressure);
        }
    }

    @Override
    public void onMidiPitchWheel(@NonNull MidiInputDevice sender, int cable, int channel, int amount) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiPitchWheel(sender, cable, channel, amount);
        }
    }

    @Override
    public void onMidiSingleByte(@NonNull MidiInputDevice sender, int cable, int byte1) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiSingleByte(sender, cable, byte1);
        }
    }

    @Override
    public void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice sender, int cable, int timing) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiTimeCodeQuarterFrame(sender, cable, timing);
        }
    }

    @Override
    public void onMidiSongSelect(@NonNull MidiInputDevice sender, int cable, int song) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiSongSelect(sender, cable, song);
        }
    }

    @Override
    public void onMidiSongPositionPointer(@NonNull MidiInputDevice sender, int cable, int position) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiSongPositionPointer(sender, cable, position);
        }
    }

    @Override
    public void onMidiTuneRequest(@NonNull MidiInputDevice sender, int cable) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiTuneRequest(sender, cable);
        }
    }

    @Override
    public void onMidiTimingClock(@NonNull MidiInputDevice sender, int cable) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiTimingClock(sender, cable);
        }
    }

    @Override
    public void onMidiStart(@NonNull MidiInputDevice sender, int cable) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiStart(sender, cable);
        }
    }

    @Override
    public void onMidiContinue(@NonNull MidiInputDevice sender, int cable) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiContinue(sender, cable);
        }
    }

    @Override
    public void onMidiStop(@NonNull MidiInputDevice sender, int cable) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiStop(sender, cable);
        }
    }

    @Override
    public void onMidiActiveSensing(@NonNull MidiInputDevice sender, int cable) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiActiveSensing(sender, cable);
        }
    }

    @Override
    public void onMidiReset(@NonNull MidiInputDevice sender, int cable) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiReset(sender, cable);
        }
    }

    @Override
    public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
        // deprecated method.
        // do nothing
    }

    @Override
    public void onMidi2InputDeviceAttached(@NonNull Midi2InputDevice midiInputDevice) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidi2InputDeviceAttached(midiInputDevice);
        }
    }

    @Override
    public void onMidi2OutputDeviceAttached(@NonNull Midi2OutputDevice midiOutputDevice) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidi2OutputDeviceAttached(midiOutputDevice);
        }
    }

    @Override
    public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiInputDeviceAttached(midiInputDevice);
        }
    }

    @Override
    public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiOutputDeviceAttached(midiOutputDevice);
        }
    }

    @Override
    public void onMidi2InputDeviceDetached(@NonNull Midi2InputDevice midiInputDevice) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidi2InputDeviceDetached(midiInputDevice);
        }
    }

    @Override
    public void onMidi2OutputDeviceDetached(@NonNull Midi2OutputDevice midiOutputDevice) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidi2OutputDeviceDetached(midiOutputDevice);
        }
    }

    @Override
    public void onDeviceDetached(@NonNull UsbDevice usbDevice) {
        // deprecated method.
        // do nothing
    }

    @Override
    public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiInputDeviceDetached(midiInputDevice);
        }
    }

    @Override
    public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiOutputDeviceDetached(midiOutputDevice);
        }
    }

    @Override
    public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiRPNReceived(sender, cable, channel, function, valueMSB, valueLSB);
        }
    }

    @Override
    public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiNRPNReceived(sender, cable, channel, function, valueMSB, valueLSB);
        }
    }

    @Override
    public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiRPNReceived(sender, cable, channel, function, value);
        }
    }

    @Override
    public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
        List<AbstractMidiFragment> midiFragments = getMidiFragments();
        for (AbstractMidiFragment fragment : midiFragments) {
            fragment.onMidiNRPNReceived(sender, cable, channel, function, value);
        }
    }

    /**
     * Implementation for multiple device connections.
     *
     * @author K.Shoji
     */
    final class OnMidiDeviceAttachedListenerImpl implements OnMidiDeviceAttachedListener {

        @Override
        public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
            // deprecated method.
            // do nothing
        }

        @Override
        public void onMidiInputDeviceAttached(@NonNull final MidiInputDevice midiInputDevice) {
            if (midiInputDevices != null) {
                midiInputDevice.setMidiEventListener(MidiFragmentHostActivity.this);
                midiInputDevices.add(midiInputDevice);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<AbstractMidiFragment> midiFragments = getMidiFragments();
                    for (AbstractMidiFragment midiFragment : midiFragments) {
                        midiFragment.onMidiInputDeviceAttached(midiInputDevice);
                    }
                }
            });
        }

        @Override
        public void onMidiOutputDeviceAttached(@NonNull final MidiOutputDevice midiOutputDevice) {
            if (midiOutputDevices != null) {
                midiOutputDevices.add(midiOutputDevice);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<AbstractMidiFragment> midiFragments = getMidiFragments();
                    for (AbstractMidiFragment midiFragment : midiFragments) {
                        midiFragment.onMidiOutputDeviceAttached(midiOutputDevice);
                    }
                }
            });
        }

        @Override
        public void onMidi2InputDeviceAttached(@NonNull final Midi2InputDevice midiInputDevice) {
            if (midi2InputDevices != null) {
                midiInputDevice.setMidiEventListener(MidiFragmentHostActivity.this);
                midi2InputDevices.add(midiInputDevice);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<AbstractMidiFragment> midiFragments = getMidiFragments();
                    for (AbstractMidiFragment midiFragment : midiFragments) {
                        midiFragment.onMidi2InputDeviceAttached(midiInputDevice);
                    }
                }
            });
        }

        @Override
        public void onMidi2OutputDeviceAttached(@NonNull final Midi2OutputDevice midiOutputDevice) {
            if (midi2OutputDevices != null) {
                midi2OutputDevices.add(midiOutputDevice);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<AbstractMidiFragment> midiFragments = getMidiFragments();
                    for (AbstractMidiFragment midiFragment : midiFragments) {
                        midiFragment.onMidi2OutputDeviceAttached(midiOutputDevice);
                    }
                }
            });
        }
    }

    /**
     * Implementation for multiple device connections.
     *
     * @author K.Shoji
     */
    final class OnMidiDeviceDetachedListenerImpl implements OnMidiDeviceDetachedListener {

        @Override
        public void onDeviceDetached(@NonNull UsbDevice usbDevice) {
            // deprecated method.
            // do nothing
        }

        @Override
        public void onMidiInputDeviceDetached(@NonNull final MidiInputDevice midiInputDevice) {
            if (midiInputDevices != null) {
                midiInputDevices.remove(midiInputDevice);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<AbstractMidiFragment> midiFragments = getMidiFragments();
                    for (AbstractMidiFragment midiFragment : midiFragments) {
                        midiFragment.onMidiInputDeviceDetached(midiInputDevice);
                    }
                }
            });
        }

        @Override
        public void onMidiOutputDeviceDetached(@NonNull final MidiOutputDevice midiOutputDevice) {
            if (midiOutputDevices != null) {
                midiOutputDevices.remove(midiOutputDevice);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<AbstractMidiFragment> midiFragments = getMidiFragments();
                    for (AbstractMidiFragment midiFragment : midiFragments) {
                        midiFragment.onMidiOutputDeviceDetached(midiOutputDevice);
                    }
                }
            });
        }

        @Override
        public void onMidi2InputDeviceDetached(@NonNull final Midi2InputDevice midiInputDevice) {
            if (midi2InputDevices != null) {
                midi2InputDevices.remove(midiInputDevice);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<AbstractMidiFragment> midiFragments = getMidiFragments();
                    for (AbstractMidiFragment midiFragment : midiFragments) {
                        midiFragment.onMidi2InputDeviceDetached(midiInputDevice);
                    }
                }
            });
        }

        @Override
        public void onMidi2OutputDeviceDetached(@NonNull final Midi2OutputDevice midiOutputDevice) {
            if (midi2OutputDevices != null) {
                midi2OutputDevices.remove(midiOutputDevice);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    List<AbstractMidiFragment> midiFragments = getMidiFragments();
                    for (AbstractMidiFragment midiFragment : midiFragments) {
                        midiFragment.onMidi2OutputDeviceDetached(midiOutputDevice);
                    }
                }
            });
        }
    }
}
