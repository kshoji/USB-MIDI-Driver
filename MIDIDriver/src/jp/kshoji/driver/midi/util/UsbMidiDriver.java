package jp.kshoji.driver.midi.util;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.thread.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.usb.util.DeviceFilter;

/**
 * Driver for USB MIDI devices.
 *
 * @author K.Shoji
 */
public abstract class UsbMidiDriver implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {
    private boolean isOpen = false;

    /**
     * Implementation for multiple device connections.
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
         * @see jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener#onDeviceAttached(android.hardware.usb.UsbDevice)
         */
        @Override
        public synchronized void onDeviceAttached(UsbDevice attachedDevice) {
            // these fields are null; when this event fired while Activity destroying.
            if (midiInputDevices == null || midiOutputDevices == null || deviceConnections == null) {
                // nothing to do
                return;
            }

            deviceConnectionWatcher.notifyDeviceGranted();

            UsbDeviceConnection deviceConnection = usbManager.openDevice(attachedDevice);
            if (deviceConnection == null) {
                return;
            }

            deviceConnections.put(attachedDevice, deviceConnection);

            List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(context.getApplicationContext());

            Set<MidiInputDevice> foundInputDevices = UsbMidiDeviceUtils.findMidiInputDevices(attachedDevice, deviceConnection, deviceFilters, UsbMidiDriver.this);
            for (MidiInputDevice midiInputDevice : foundInputDevices) {
                try {
                    Set<MidiInputDevice> inputDevices = midiInputDevices.get(attachedDevice);
                    if (inputDevices == null) {
                        inputDevices = new HashSet<MidiInputDevice>();
                    }
                    inputDevices.add(midiInputDevice);
                    midiInputDevices.put(attachedDevice, inputDevices);
                } catch (IllegalArgumentException iae) {
                    Log.d(Constants.TAG, "This device didn't have any input endpoints.", iae);
                }
            }

            Set<MidiOutputDevice> foundOutputDevices = UsbMidiDeviceUtils.findMidiOutputDevices(attachedDevice, deviceConnection, deviceFilters);
            for (MidiOutputDevice midiOutputDevice : foundOutputDevices) {
                try {
                    Set<MidiOutputDevice> outputDevices = midiOutputDevices.get(attachedDevice);
                    if (outputDevices == null) {
                        outputDevices = new HashSet<MidiOutputDevice>();
                    }
                    outputDevices.add(midiOutputDevice);
                    midiOutputDevices.put(attachedDevice, outputDevices);
                } catch (IllegalArgumentException iae) {
                    Log.d(Constants.TAG, "This device didn't have any output endpoints.", iae);
                }
            }

            Log.d(Constants.TAG, "Device " + attachedDevice.getDeviceName() + " has been attached.");

            UsbMidiDriver.this.onDeviceAttached(attachedDevice);
        }
    }

    /**
     * Implementation for multiple device connections.
     *
     * @author K.Shoji
     */
    final class OnMidiDeviceDetachedListenerImpl implements OnMidiDeviceDetachedListener {
        /*
         * (non-Javadoc)
         * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
         */
        @Override
        public synchronized void onDeviceDetached(UsbDevice detachedDevice) {
            // these fields are null; when this event fired while Activity destroying.
            if (midiInputDevices == null || midiOutputDevices == null || deviceConnections == null) {
                // nothing to do
                return;
            }

            AsyncTask<UsbDevice, Void, Void> task = new AsyncTask<UsbDevice, Void, Void>() {

                @Override
                protected Void doInBackground(UsbDevice... params) {
                    if (params == null || params.length < 1) {
                        return null;
                    }

                    UsbDevice usbDevice = params[0];

                    // Stop input device's thread.
                    Set<MidiInputDevice> inputDevices = midiInputDevices.get(usbDevice);
                    if (inputDevices != null && inputDevices.size() > 0) {
                        for (MidiInputDevice inputDevice : inputDevices) {
                            if (inputDevice != null) {
                                inputDevice.stop();
                            }
                        }
                        midiInputDevices.remove(usbDevice);
                    }

                    Set<MidiOutputDevice> outputDevices = midiOutputDevices.get(usbDevice);
                    if (outputDevices != null) {
                        for (MidiOutputDevice outputDevice : outputDevices) {
                            if (outputDevice != null) {
                                outputDevice.stop();
                            }
                        }
                        midiOutputDevices.remove(usbDevice);
                    }

                    UsbDeviceConnection deviceConnection = deviceConnections.get(usbDevice);
                    if (deviceConnection != null) {
                        deviceConnection.close();

                        deviceConnections.remove(usbDevice);
                    }

                    Log.d(Constants.TAG, "Device " + usbDevice.getDeviceName() + " has been detached.");

                    Message message = Message.obtain(deviceDetachedHandler);
                    message.obj = usbDevice;
                    deviceDetachedHandler.sendMessage(message);

                    return null;
                }

            };
            task.execute(detachedDevice);
        }
    }

    Map<UsbDevice, UsbDeviceConnection> deviceConnections = null;
    Map<UsbDevice, Set<MidiInputDevice>> midiInputDevices = null;
    Map<UsbDevice, Set<MidiOutputDevice>> midiOutputDevices = null;
    OnMidiDeviceAttachedListener deviceAttachedListener = null;
    OnMidiDeviceDetachedListener deviceDetachedListener = null;
    Handler deviceDetachedHandler = null;
    MidiDeviceConnectionWatcher deviceConnectionWatcher = null;

    private final Context context;

    /**
     * Constructor
     *
     * @param context Activity context
     */
    protected UsbMidiDriver(Context context) {
        this.context = context;
    }

    /**
     * Starts using UsbMidiDriver.
     *
     * Starts the USB device watching and communicating thread.
     */
    public final void open() {
        if (isOpen) {
            // already opened
            return;
        }
        isOpen = true;

        deviceConnections = new HashMap<UsbDevice, UsbDeviceConnection>();
        midiInputDevices = new HashMap<UsbDevice, Set<MidiInputDevice>>();
        midiOutputDevices = new HashMap<UsbDevice, Set<MidiOutputDevice>>();

        UsbManager usbManager = (UsbManager) context.getApplicationContext().getSystemService(Context.USB_SERVICE);
        deviceAttachedListener = new OnMidiDeviceAttachedListenerImpl(usbManager);
        deviceDetachedListener = new OnMidiDeviceDetachedListenerImpl();

        deviceDetachedHandler = new Handler(new Handler.Callback() {
            /*
             * (non-Javadoc)
             * @see android.os.Handler.Callback#handleMessage(android.os.Message)
             */
            @Override
            public boolean handleMessage(Message msg) {
                Log.d(Constants.TAG, "(handleMessage) detached device:" + msg.obj);
                UsbDevice usbDevice = (UsbDevice) msg.obj;
                onDeviceDetached(usbDevice);
                return true;
            }
        });

        deviceConnectionWatcher = new MidiDeviceConnectionWatcher(context.getApplicationContext(), usbManager, deviceAttachedListener, deviceDetachedListener);
    }

    /**
     * Stops using UsbMidiDriver.
     *
     * Shutdown the USB device communicating thread.
     * The all connected devices will be closed.
     */
    public final void close() {
        if (!isOpen) {
            // already closed
            return;
        }
        isOpen = false;

        deviceConnectionWatcher.stop();
        deviceConnectionWatcher = null;

        if (midiInputDevices != null) {
            for (Set<MidiInputDevice> inputDevices : midiInputDevices.values()) {
                if (inputDevices != null) {
                    for (MidiInputDevice inputDevice : inputDevices) {
                        if (inputDevice != null) {
                            inputDevice.stop();
                        }
                    }
                }
            }

            midiInputDevices.clear();
        }
        midiInputDevices = null;

        if (midiOutputDevices != null) {
            midiOutputDevices.clear();
        }
        midiOutputDevices = null;

        deviceConnections = null;
    }

    /**
     * Suspends receiving/transmitting MIDI messages.
     * All events will be discarded until the devices being resumed.
     */
    protected final void suspend() {
        if (midiInputDevices != null) {
            for (Set<MidiInputDevice> inputDevices : midiInputDevices.values()) {
                if (inputDevices != null) {
                    for (MidiInputDevice inputDevice : inputDevices) {
                        if (inputDevice != null) {
                            inputDevice.suspend();
                        }
                    }
                }
            }
        }

        if (midiOutputDevices != null) {
            for (Set<MidiOutputDevice> outputDevices : midiOutputDevices.values()) {
                if (outputDevices != null) {
                    for (MidiOutputDevice outputDevice : outputDevices) {
                        if (outputDevice != null) {
                            outputDevice.suspend();
                        }
                    }
                }
            }
        }
    }

    /**
     * Resumes from {@link #suspend()}
     */
    protected final void resume() {
        if (midiInputDevices != null) {
            for (Set<MidiInputDevice> inputDevices : midiInputDevices.values()) {
                if (inputDevices != null) {
                    for (MidiInputDevice inputDevice : inputDevices) {
                        if (inputDevice != null) {
                            inputDevice.resume();
                        }
                    }
                }
            }
        }

        if (midiOutputDevices != null) {
            for (Set<MidiOutputDevice> outputDevices : midiOutputDevices.values()) {
                if (outputDevices != null) {
                    for (MidiOutputDevice outputDevice : outputDevices) {
                        if (outputDevice != null) {
                            outputDevice.resume();
                        }
                    }
                }
            }
        }
    }

    /**
     * Get connected USB MIDI devices.
     *
     * @return connected UsbDevice set
     */
    public final Set<UsbDevice> getConnectedUsbDevices() {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }
        if (deviceConnections != null) {
            return Collections.unmodifiableSet(deviceConnections.keySet());
        }

        return Collections.unmodifiableSet(new HashSet<UsbDevice>());
    }

    /**
     * Get MIDI output device, if available.
     *
     * @param usbDevice
     * @return {@link Set<MidiOutputDevice>}
     */
    public final Set<MidiOutputDevice> getMidiOutputDevices(UsbDevice usbDevice) {
        if (deviceConnectionWatcher != null) {
            deviceConnectionWatcher.checkConnectedDevicesImmediately();
        }
        if (midiOutputDevices != null && midiOutputDevices.get(usbDevice) != null) {
            return Collections.unmodifiableSet(midiOutputDevices.get(usbDevice));
        }

        return Collections.unmodifiableSet(new HashSet<MidiOutputDevice>());
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
    @Override
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
    @Override
    public void onMidiNRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
        // do nothing in this implementation
    }
}
