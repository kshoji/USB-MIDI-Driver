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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.util.Constants;

/**
 * Service for multiple MIDI connection
 *
 * @author K.Shoji
 */
public final class MultipleMidiService extends Service {

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
        public MultipleMidiService getService() {
            return MultipleMidiService.this;
        }
    }

    private final IBinder binder = new LocalBinder();

    private final Set<MidiInputDevice> midiInputDevices = new HashSet<>();
    private final Set<MidiOutputDevice> midiOutputDevices = new HashSet<>();
    private MidiDeviceConnectionWatcher deviceConnectionWatcher = null;

    private OnMidiDeviceAttachedListener midiDeviceAttachedListener = null;
    private OnMidiDeviceDetachedListener midiDeviceDetachedListener = null;

    private boolean isRunning = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isRunning) {
            Log.d(Constants.TAG, "MIDI service starting.");

            UsbManager usbManager = (UsbManager)getSystemService(Context.USB_SERVICE);

            // runs in separate thread
            deviceConnectionWatcher = new MidiDeviceConnectionWatcher(this, usbManager, serviceMidiDeviceAttachedListener, serviceMidiDeviceDetachedListener);

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

        midiInputDevices.clear();

        midiOutputDevices.clear();

        Log.d(Constants.TAG, "MIDI service stopped.");
    }

    /**
     * Set {@link jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener} to listen MIDI devices have been connected
     *
     * @param midiDeviceAttachedListener the event listener
     */
    public void setOnMidiDeviceAttachedListener(@Nullable OnMidiDeviceAttachedListener midiDeviceAttachedListener) {
        this.midiDeviceAttachedListener = midiDeviceAttachedListener;
    }

    /**
     * Set {@link jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener} to listen MIDI devices have been disconnected
     *
     * @param midiDeviceDetachedListener the event listener
     */
    public void setOnMidiDeviceDetachedListener(@Nullable OnMidiDeviceDetachedListener midiDeviceDetachedListener) {
        this.midiDeviceDetachedListener = midiDeviceDetachedListener;
    }

    /**
     * Suspends event listening / sending
     */
    public void suspend() {
        for (MidiInputDevice midiInputDevice : midiInputDevices) {
            midiInputDevice.suspend();
        }

        for (MidiOutputDevice midiOutputDevice : midiOutputDevices) {
            midiOutputDevice.suspend();
        }
    }

    /**
     * Resumes from {@link #suspend()}
     */
    public void resume() {
        for (MidiInputDevice midiInputDevice : midiInputDevices) {
            midiInputDevice.resume();
        }

        for (MidiOutputDevice midiOutputDevice : midiOutputDevices) {
            midiOutputDevice.resume();
        }
    }

    /**
     * Get {@link Set} of{@link jp.kshoji.driver.midi.device.MidiInputDevice} to send MIDI events.
     *
     * @return the Set of MidiInputDevice
     */
    @NonNull
    public Set<MidiInputDevice> getMidiInputDevices() {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }

        return Collections.unmodifiableSet(midiInputDevices);
    }

    /**
     * Get {@link Set} of{@link jp.kshoji.driver.midi.device.MidiOutputDevice} to send MIDI events.
     *
     * @return the Set of MidiOutputDevice
     */
    @NonNull
    public Set<MidiOutputDevice> getMidiOutputDevices() {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }

        return Collections.unmodifiableSet(midiOutputDevices);
    }

    private OnMidiDeviceAttachedListener serviceMidiDeviceAttachedListener = new OnMidiDeviceAttachedListener() {

        @Override
        public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
            Log.d(Constants.TAG, "USB MIDI Device " + usbDevice.getDeviceName() + " has been attached.");
            if (midiDeviceAttachedListener != null) {
                midiDeviceAttachedListener.onDeviceAttached(usbDevice);
            }
        }

        @Override
        public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {
            midiInputDevices.add(midiInputDevice);

            if (midiDeviceAttachedListener != null) {
                midiDeviceAttachedListener.onMidiInputDeviceAttached(midiInputDevice);
            }
        }

        @Override
        public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {
            midiOutputDevices.add(midiOutputDevice);

            if (midiDeviceAttachedListener != null) {
                midiDeviceAttachedListener.onMidiOutputDeviceAttached(midiOutputDevice);
            }
        }
    };

    private OnMidiDeviceDetachedListener serviceMidiDeviceDetachedListener = new OnMidiDeviceDetachedListener() {
        @Override
        public void onDeviceDetached(@NonNull UsbDevice detachedDevice) {
            Log.d(Constants.TAG, "USB MIDI Device " + detachedDevice.getDeviceName() + " has been detached.");
            if (midiDeviceDetachedListener != null) {
                midiDeviceDetachedListener.onDeviceDetached(detachedDevice);
            }
        }

        @Override
        public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {
            midiInputDevice.setMidiEventListener(null);
            midiInputDevices.remove(midiInputDevice);

            if (midiDeviceDetachedListener != null) {
                midiDeviceDetachedListener.onMidiInputDeviceDetached(midiInputDevice);
            }
        }

        @Override
        public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {
            midiOutputDevices.remove(midiOutputDevice);

            if (midiDeviceDetachedListener != null) {
                midiDeviceDetachedListener.onMidiOutputDeviceDetached(midiOutputDevice);
            }
        }
    };
}
