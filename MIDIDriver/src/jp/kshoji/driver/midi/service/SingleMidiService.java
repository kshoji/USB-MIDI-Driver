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

import jp.kshoji.driver.midi.device.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
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
public final class SingleMidiService extends Service implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {

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
    private MidiDeviceConnectionWatcher deviceConnectionWatcher = null;
    private OnMidiInputEventListener midiInputEventListener = null;

    private boolean isRunning = false;
    private boolean isLoopbackEnabled = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            Log.d(Constants.TAG, "MIDI service starting.");

            UsbManager usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);

            // runs in separate thread
            deviceConnectionWatcher = new MidiDeviceConnectionWatcher(this, usbManager, this, this);

            isRunning = true;
        }

        return START_REDELIVER_INTENT; // must be restarted if stopped by the system, We must respond to midi events(!)
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.stop();
        }
        deviceConnectionWatcher = null;

        midiInputDevice = null;

        midiOutputDevice = null;

        Log.d(Constants.TAG, "MIDI service stopped.");
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
    }

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
    }

    /**
     * Enables loopback mode, and checks MIDI Output availability
     *
     * @param enable true: enabled
     * @return true: the MIDI output is available
     */
    public boolean enableLoopback(boolean enable) {
        if (isLoopbackEnabled != enable) {
            Log.d(Constants.TAG, (enable ? "Enabling" : "Disabling") + " MIDI Loopback.");
        }
        isLoopbackEnabled = enable;

        return getMidiOutputDevice() != null;
    }

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
    public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
        Log.d(Constants.TAG, "USB MIDI Device " + usbDevice.getDeviceName() + " has been attached.");
    }

    @Override
    public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {
        if (this.midiInputDevice != null) {
            return;
        }

        this.midiInputDevice = midiInputDevice;
        this.midiInputDevice.setMidiEventListener(this);
    }

    @Override
    public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {
        if (this.midiOutputDevice != null) {
            return;
        }

        this.midiOutputDevice = midiOutputDevice;
    }

    @Override
    public void onDeviceDetached(@NonNull UsbDevice detachedDevice) {
        Log.d(Constants.TAG, "USB MIDI Device " + detachedDevice.getDeviceName() + " has been detached.");
    }

    @Override
    public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {
        if (this.midiInputDevice != null && this.midiInputDevice == midiInputDevice) {
            this.midiInputDevice.setMidiEventListener(null);
            this.midiInputDevice = null;
        }
    }

    @Override
    public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {
        if (this.midiOutputDevice != null && this.midiOutputDevice == midiOutputDevice) {
            this.midiOutputDevice = null;
        }
    }

    @Override
    public void onMidiMiscellaneousFunctionCodes(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiMiscellaneousFunctionCodes(sender, cable, byte1, byte2, byte3);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiMiscellaneousFunctionCodes(cable, byte1, byte2, byte3);
        }
    }

    @Override
    public void onMidiCableEvents(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiCableEvents(sender, cable, byte1, byte2, byte3);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiCableEvents(cable, byte1, byte2, byte3);
        }
    }

    @Override
    public void onMidiSystemCommonMessage(@NonNull MidiInputDevice sender, int cable, byte[] bytes) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiSystemCommonMessage(sender, cable, bytes);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiSystemCommonMessage(cable, bytes);
        }
    }

    @Override
    public void onMidiSystemExclusive(@NonNull MidiInputDevice sender, int cable, byte[] systemExclusive) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiSystemExclusive(sender, cable, systemExclusive);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiSystemExclusive(cable, systemExclusive);
        }
    }

    @Override
    public void onMidiNoteOff(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiNoteOff(sender, cable, channel, note, velocity);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiNoteOff(cable, channel, note, velocity);
        }
    }

    @Override
    public void onMidiNoteOn(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiNoteOn(sender, cable, channel, note, velocity);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiNoteOn(cable, channel, note, velocity);
        }
    }

    @Override
    public void onMidiPolyphonicAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int note, int pressure) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiPolyphonicAftertouch(sender, cable, channel, note, pressure);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiPolyphonicAftertouch(cable, channel, note, pressure);
        }
    }

    @Override
    public void onMidiControlChange(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiControlChange(sender, cable, channel, function, value);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiControlChange(cable, channel, function, value);
        }
    }

    @Override
    public void onMidiProgramChange(@NonNull MidiInputDevice sender, int cable, int channel, int program) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiProgramChange(sender, cable, channel, program);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiProgramChange(cable, channel, program);
        }
    }

    @Override
    public void onMidiChannelAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int pressure) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiChannelAftertouch(sender, cable, channel, pressure);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiChannelAftertouch(cable, channel, pressure);
        }
    }

    @Override
    public void onMidiPitchWheel(@NonNull MidiInputDevice sender, int cable, int channel, int amount) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiPitchWheel(sender, cable, channel, amount);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiPitchWheel(cable, channel, amount);
        }
    }

    @Override
    public void onMidiSingleByte(@NonNull MidiInputDevice sender, int cable, int byte1) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiSingleByte(sender, cable, byte1);
        }

        if (isLoopbackEnabled && getMidiOutputDevice() != null) {
            getMidiOutputDevice().sendMidiSingleByte(cable, byte1);
        }
    }

    /**
     * RPN message
     * This method is just the utility method, do not need to be implemented necessarily by subclass.
     *
     * @param sender the sender
     * @param cable the cable ID 0-15
     * @param channel the channel 0-15
     * @param function 14bits
     * @param valueMSB higher 7bits
     * @param valueLSB lower 7bits. -1 if value has no LSB. If you know the function's parameter value have LSB, you must ignore when valueLSB < 0.
     */
    public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiRPNReceived(sender, cable, channel, function, valueMSB, valueLSB);
        }
    }

    /**
     * NRPN message
     * This method is just the utility method, do not need to be implemented necessarily by subclass.
     *
     * @param sender the sender
     * @param cable the cable ID 0-15
     * @param channel the channel 0-15
     * @param function 14bits
     * @param valueMSB higher 7bits
     * @param valueLSB lower 7bits. -1 if value has no LSB. If you know the function's parameter value have LSB, you must ignore when valueLSB < 0.
     */
    public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
        if (midiInputEventListener != null) {
            midiInputEventListener.onMidiNRPNReceived(sender, cable, channel, function, valueMSB, valueLSB);
        }
    }
}
