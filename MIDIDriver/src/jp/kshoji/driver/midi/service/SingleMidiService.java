package jp.kshoji.driver.midi.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;
import jp.kshoji.driver.usb.util.DeviceFilter;

/**
 * Created by André on 17.10.13.
 */
public class SingleMidiService extends Service
    implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener{

    public class LocalBinder extends Binder {
        public SingleMidiService getService() {
            return SingleMidiService.this;
        }
    }

    private final IBinder mBinder = new LocalBinder();

    UsbDevice device = null;
    UsbDeviceConnection deviceConnection = null;
    MidiInputDevice midiInputDevice = null;
    MidiOutputDevice midiOutputDevice = null;
    OnMidiDeviceAttachedListener deviceAttachedListener = null;
    OnMidiDeviceDetachedListener deviceDetachedListener = null;
    Handler deviceDetachedHandler = null;
    private MidiDeviceConnectionWatcher deviceConnectionWatcher = null;

    private boolean mRunning = false;
    private boolean mLoopback = false;

    /**
     * Implementation for single device connections.
     *
     * @author K.Shoji
     */
    final class OnMidiDeviceAttachedListenerImpl implements OnMidiDeviceAttachedListener {
        private final UsbManager usbManager;

        /**
         * constructor
         *
         * @param usbManager
         */
        public OnMidiDeviceAttachedListenerImpl(UsbManager usbManager) {
            this.usbManager = usbManager;
        }

        /*
         * (non-Javadoc)
         * @see jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener#onDeviceAttached(android.hardware.usb.UsbDevice, android.hardware.usb.UsbInterface)
         */
        @Override
        public synchronized void onDeviceAttached(final UsbDevice attachedDevice) {
            if (device != null) {
                // already one device has been connected
                return;
            }

            deviceConnection = usbManager.openDevice(attachedDevice);
            if (deviceConnection == null) {
                return;
            }

            List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(getApplicationContext());

            Set<MidiInputDevice> foundInputDevices = UsbMidiDeviceUtils.findMidiInputDevices(attachedDevice, deviceConnection, deviceFilters);
            if (foundInputDevices.size() > 0) {
                midiInputDevice = (MidiInputDevice) foundInputDevices.toArray()[0];
            }

            Set<MidiOutputDevice> foundOutputDevices = UsbMidiDeviceUtils.findMidiOutputDevices(attachedDevice, deviceConnection, deviceFilters);
            if (foundOutputDevices.size() > 0) {
                midiOutputDevice = (MidiOutputDevice) foundOutputDevices.toArray()[0];
            }

            Log.d(Constants.TAG, "Device " + attachedDevice.getDeviceName() + " has been attached.");

            SingleMidiService.this.onDeviceAttached(attachedDevice);
        }

        @Override
        public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {

        }

        @Override
        public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {

        }
    }

    /**
     * Implementation for single device connections.
     *
     * @author K.Shoji
     */
    final class OnMidiDeviceDetachedListenerImpl implements OnMidiDeviceDetachedListener {
        /*
         * (non-Javadoc)
         * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
         */
        @Override
        public synchronized void onDeviceDetached(final UsbDevice detachedDevice) {
            if (midiInputDevice != null) {
                midiInputDevice = null;
            }

            if (midiOutputDevice != null) {
                midiOutputDevice = null;
            }

            if (deviceConnection != null) {
                deviceConnection.close();
                deviceConnection = null;
            }
            device = null;

            Log.d(Constants.TAG, "Device " + detachedDevice.getDeviceName() + " has been detached.");

            Message message = new Message();
            message.obj = detachedDevice;
            deviceDetachedHandler.sendMessage(message);
        }

        @Override
        public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {

        }

        @Override
        public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {

        }
    }


    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate() {

        Log.d(Constants.TAG, "Creating MIDI service.");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!mRunning) {
            Log.d(Constants.TAG, "MIDI service starting.");
            Toast.makeText(this, "MIDI Service starting", Toast.LENGTH_SHORT).show();

            /*Notification notification = new Notification.Builder(this)
                    .setContentTitle("MIDI service")
                    .setContentText("Running")
                    .build();*/
            //startForeground(1, notification);

            UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);
            deviceAttachedListener = new OnMidiDeviceAttachedListenerImpl(usbManager);
            deviceDetachedListener = new OnMidiDeviceDetachedListenerImpl();

            deviceDetachedHandler = new Handler(new Handler.Callback() {
                @Override
                public boolean handleMessage(Message msg) {
                    UsbDevice usbDevice = (UsbDevice) msg.obj;
                    onDeviceDetached(usbDevice);
                    return true;
                }
            });

            // runs in separate thread
            deviceConnectionWatcher = new MidiDeviceConnectionWatcher(
                    this, usbManager,
                    deviceAttachedListener, deviceDetachedListener);

            mRunning = true;
        }

        return START_REDELIVER_INTENT;
            // must be restarted if stopped by the system, We must respond to midi events(!)
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onDestroy()
     */
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.stop();
        }
        deviceConnectionWatcher = null;

        midiInputDevice = null;

        midiOutputDevice = null;

        deviceConnection = null;

        //stopForeground(true);
        Toast.makeText(this, "MIDI Service stopped", Toast.LENGTH_SHORT).show();
    }

    /**
     * Get MIDI output device, if available.
     *
     * @return MidiOutputDevice, null if not available
     */
    public final MidiOutputDevice getMidiOutputDevice() {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }

        return midiOutputDevice;
    }

    @Override
    public void onDeviceAttached(UsbDevice usbDevice) {
        Toast.makeText(this,
                "USB MIDI Device " + usbDevice.getDeviceName() + " has been attached.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {

    }

    @Override
    public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {

    }

    @Override
    public void onDeviceDetached(UsbDevice usbDevice) {
        Toast.makeText(this,
                "USB MIDI Device " + usbDevice.getDeviceName() + " has been detached.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {

    }

    @Override
    public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {

    }


    public boolean enableLoopback(boolean enable) {
        if(mLoopback != enable)
            Toast.makeText(this, (enable?"Enabling":"Disabling")+" MIDI Loopback.", Toast.LENGTH_SHORT).show();
        mLoopback = enable;

        return (getMidiOutputDevice() != null);
    }


    @Override
    public void onMidiMiscellaneousFunctionCodes(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiMiscellaneousFunctionCodes(cable, byte1, byte2, byte3);
    }

    @Override
    public void onMidiCableEvents(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiCableEvents(cable, byte1, byte2, byte3);
    }

    @Override
    public void onMidiSystemCommonMessage(MidiInputDevice sender, int cable, byte[] bytes) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiSystemCommonMessage(cable, bytes);
    }

    @Override
    public void onMidiSystemExclusive(MidiInputDevice sender, int cable, byte[] systemExclusive) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiSystemExclusive(cable, systemExclusive);
    }

    @Override
    public void onMidiNoteOff(MidiInputDevice sender, int cable, int channel, int note, int velocity) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiNoteOff(cable, channel, note, velocity);
    }

    @Override
    public void onMidiNoteOn(MidiInputDevice sender, int cable, int channel, int note, int velocity) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiNoteOn(cable, channel, note, velocity);
    }

    @Override
    public void onMidiPolyphonicAftertouch(MidiInputDevice sender, int cable, int channel, int note, int pressure) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiPolyphonicAftertouch(cable, channel, note, pressure);
    }

    @Override
    public void onMidiControlChange(MidiInputDevice sender, int cable, int channel, int function, int value) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiControlChange(cable, channel, function, value);
    }

    @Override
    public void onMidiProgramChange(MidiInputDevice sender, int cable, int channel, int program) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiProgramChange(cable, channel, program);
    }

    @Override
    public void onMidiChannelAftertouch(MidiInputDevice sender, int cable, int channel, int pressure) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiChannelAftertouch(cable, channel, pressure);
    }

    @Override
    public void onMidiPitchWheel(MidiInputDevice sender, int cable, int channel, int amount) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiPitchWheel(cable, channel, amount);
    }

    @Override
    public void onMidiSingleByte(MidiInputDevice sender, int cable, int byte1) {
        if(mLoopback && getMidiOutputDevice() != null)
            getMidiOutputDevice().sendMidiSingleByte(cable, byte1);
    }

    /**
     * RPN message
     * This method is just the utility method, do not need to be implemented necessarily by subclass.
     *
     * @param sender
     * @param cable
     * @param channel
     * @param function 14bits
     * @param valueMSB higher 7bits
     * @param valueLSB lower 7bits. -1 if value has no LSB. If you know the function's parameter value have LSB, you must ignore when valueLSB < 0.
     */
    public void onMidiRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
        // do nothing in this implementation
    }

    /**
     * NRPN message
     * This method is just the utility method, do not need to be implemented necessarily by subclass.
     *
     * @param sender
     * @param cable
     * @param channel
     * @param function 14bits
     * @param valueMSB higher 7bits
     * @param valueLSB lower 7bits. -1 if value has no LSB. If you know the function's parameter value have LSB, you must ignore when valueLSB < 0.
     */
    public void onMidiNRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
        // do nothing in this implementation
    }
}
