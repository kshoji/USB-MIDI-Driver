package jp.kshoji.driver.midi.device;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;
import jp.kshoji.driver.usb.util.DeviceFilter;

/**
 * Detects USB MIDI Device Connected
 * stop() method must be called when the application will be destroyed.
 * 
 * @author K.Shoji
 */
public final class MidiDeviceConnectionWatcher {
	private final MidiDeviceConnectionWatchThread thread;
	final Context context;
    final UsbManager usbManager;

    final Handler deviceDetachedHandler;
    final OnMidiDeviceDetachedListener deviceDetachedListener;

    volatile boolean isGranting;
    volatile UsbDevice grantingDevice;
    final Queue<UsbDevice> deviceGrantQueue = new LinkedList<>();
    final HashSet<UsbDevice> grantedDevices = new HashSet<>();

    Map<UsbDevice, UsbDeviceConnection> deviceConnections = new HashMap<>();
    Map<UsbDevice, Set<MidiInputDevice>> midiInputDevices = new HashMap<>();
    Map<UsbDevice, Set<MidiOutputDevice>> midiOutputDevices = new HashMap<>();

    /**
	 * Constructor
	 *
     * @param context the Context
	 * @param usbManager the UsbManager
     * @param deviceAttachedListener the OnMidiDeviceAttachedListener
     */
	public MidiDeviceConnectionWatcher(@NonNull Context context, @NonNull UsbManager usbManager, @NonNull OnMidiDeviceAttachedListener deviceAttachedListener, @NonNull final OnMidiDeviceDetachedListener deviceDetachedListener) {
		this.context = context;
        this.usbManager = usbManager;
		isGranting = false;
        grantingDevice = null;
        this.deviceDetachedListener = deviceDetachedListener;

        deviceDetachedHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(Message message) {
                AsyncTask<UsbDevice, Void, Void> task = new AsyncTask<UsbDevice, Void, Void>() {

                    @Override
                    protected Void doInBackground(UsbDevice... params) {
                        if (params == null || params.length < 1) {
                            return null;
                        }

                        UsbDevice usbDevice = params[0];
                        onDeviceDetached(usbDevice);

                        return null;
                    }
                };

                UsbDevice detachedDevice = (UsbDevice) message.obj;
                task.execute(detachedDevice);

                return true;
            }
        });

		thread = new MidiDeviceConnectionWatchThread(usbManager, deviceAttachedListener, deviceDetachedHandler);
        thread.setName("MidiDeviceConnectionWatchThread");
		thread.start();
	}

    /**
     * Notify the specified device has been detached
     *
     * @param detachedDevice the USB MIDI device
     */
    private void onDeviceDetached(@NonNull UsbDevice detachedDevice) {
        deviceDetachedListener.onDeviceDetached(detachedDevice);

        // Stop input device's thread.
        Set<MidiInputDevice> inputDevices = midiInputDevices.get(detachedDevice);
        if (inputDevices != null && inputDevices.size() > 0) {
            for (MidiInputDevice inputDevice : inputDevices) {
                if (inputDevice != null) {
                    inputDevice.stop();

                    deviceDetachedListener.onMidiInputDeviceDetached(inputDevice);
                }
            }
            midiInputDevices.remove(detachedDevice);
        }

        Set<MidiOutputDevice> outputDevices = midiOutputDevices.get(detachedDevice);
        if (outputDevices != null) {
            for (MidiOutputDevice outputDevice : outputDevices) {
                if (outputDevice != null) {
                    outputDevice.stop();

                    deviceDetachedListener.onMidiOutputDeviceDetached(outputDevice);
                }
            }
            midiOutputDevices.remove(detachedDevice);
        }

        UsbDeviceConnection deviceConnection = deviceConnections.get(detachedDevice);
        if (deviceConnection != null) {
            deviceConnection.close();

            deviceConnections.remove(detachedDevice);
        }
    }

    /**
     * Checks the connected USB MIDI devices
     */
    public void checkConnectedDevicesImmediately() {
		thread.checkConnectedDevices();
	}
	
	/**
	 * Stops the watching thread <br />
	 * <br />
	 * Note: Takes one second until the thread stops.
	 * The device attached / detached events will be noticed until the thread will completely stops.
	 */
	public void stop() {
		thread.stopFlag = true;
        thread.interrupt();

		// blocks while the thread will stop
		while (thread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}
	
	/**
	 * Broadcast receiver for MIDI device connection granted
	 * 
	 * @author K.Shoji
	 */
	private final class UsbMidiGrantedReceiver extends BroadcastReceiver {
		private static final String USB_PERMISSION_GRANTED_ACTION = "jp.kshoji.driver.midi.USB_PERMISSION_GRANTED_ACTION";
		
		private final UsbDevice device;
		private final OnMidiDeviceAttachedListener deviceAttachedListener;
		
		/**
		 * @param device the UsbDevice
		 * @param deviceAttachedListener the OnMidiDeviceAttachedListener
		 */
		public UsbMidiGrantedReceiver(@NonNull UsbDevice device, @NonNull OnMidiDeviceAttachedListener deviceAttachedListener) {
			this.device = device;
			this.deviceAttachedListener = deviceAttachedListener;
		}
		
		@Override
		public void onReceive(Context receiverContext, Intent intent) {
			String action = intent.getAction();
			if (USB_PERMISSION_GRANTED_ACTION.equals(action)) {
				boolean granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
				if (granted) {
                    grantedDevices.add(device);
                    deviceAttachedListener.onDeviceAttached(device);

                    UsbDeviceConnection deviceConnection = usbManager.openDevice(device);
                    if (deviceConnection == null) {
                        return;
                    }

                    deviceConnections.put(device, deviceConnection);

                    List<DeviceFilter> deviceFilters = DeviceFilter.getDeviceFilters(context.getApplicationContext());

                    Set<MidiInputDevice> foundInputDevices = UsbMidiDeviceUtils.findMidiInputDevices(device, deviceConnection, deviceFilters);
                    for (MidiInputDevice midiInputDevice : foundInputDevices) {
                        try {
                            Set<MidiInputDevice> inputDevices = midiInputDevices.get(device);
                            if (inputDevices == null) {
                                inputDevices = new HashSet<>();
                            }
                            inputDevices.add(midiInputDevice);
                            midiInputDevices.put(device, inputDevices);

                            deviceAttachedListener.onMidiInputDeviceAttached(midiInputDevice);
                        } catch (IllegalArgumentException iae) {
                            Log.d(Constants.TAG, "This device didn't have any input endpoints.", iae);
                        }
                    }

                    Set<MidiOutputDevice> foundOutputDevices = UsbMidiDeviceUtils.findMidiOutputDevices(device, deviceConnection, deviceFilters);
                    for (MidiOutputDevice midiOutputDevice : foundOutputDevices) {
                        try {
                            Set<MidiOutputDevice> outputDevices = midiOutputDevices.get(device);
                            if (outputDevices == null) {
                                outputDevices = new HashSet<>();
                            }
                            outputDevices.add(midiOutputDevice);
                            midiOutputDevices.put(device, outputDevices);

                            deviceAttachedListener.onMidiOutputDeviceAttached(midiOutputDevice);
                        } catch (IllegalArgumentException iae) {
                            Log.d(Constants.TAG, "This device didn't have any output endpoints.", iae);
                        }
                    }

                    Log.d(Constants.TAG, "Device " + device.getDeviceName() + " has been attached.");
				}

                // reset the 'isGranting' to false
                isGranting = false;
                grantingDevice = null;
			}
            context.unregisterReceiver(this);
		}
	}
	
	/**
	 * USB Device polling thread
	 * 
	 * @author K.Shoji
	 */
	private final class MidiDeviceConnectionWatchThread extends Thread {
		private UsbManager usbManager;
		private OnMidiDeviceAttachedListener deviceAttachedListener;
		private Handler deviceDetachedHandler;
		private Set<UsbDevice> connectedDevices;
		boolean stopFlag;
		private List<DeviceFilter> deviceFilters;

		/**
		 * Constructor
         *
		 * @param usbManager the UsbManager
		 * @param deviceAttachedListener the OnMidiDeviceAttachedListener
		 * @param deviceDetachedHandler the OnMidiDeviceDetachedListener
		 */
		MidiDeviceConnectionWatchThread(@NonNull UsbManager usbManager, @NonNull OnMidiDeviceAttachedListener deviceAttachedListener, @NonNull Handler deviceDetachedHandler) {
			this.usbManager = usbManager;
			this.deviceAttachedListener = deviceAttachedListener;
			this.deviceDetachedHandler = deviceDetachedHandler;
			connectedDevices = new HashSet<>();
			stopFlag = false;
			deviceFilters = DeviceFilter.getDeviceFilters(context);
		}

		@Override
		public void run() {
			super.run();
			
			while (stopFlag == false) {
				checkConnectedDevices();
				
				synchronized (deviceGrantQueue) {
					if (!deviceGrantQueue.isEmpty() && !isGranting) {
						isGranting = true;
						grantingDevice = deviceGrantQueue.remove();
						
						PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0, new Intent(UsbMidiGrantedReceiver.USB_PERMISSION_GRANTED_ACTION), 0);
						context.registerReceiver(new UsbMidiGrantedReceiver(grantingDevice, deviceAttachedListener), new IntentFilter(UsbMidiGrantedReceiver.USB_PERMISSION_GRANTED_ACTION));
						usbManager.requestPermission(grantingDevice, permissionIntent);
					}
				}
				
				try {
					sleep(1000);
				} catch (InterruptedException e) {
                    // interrupted
				}
			}

            // the thread is finishing now.
            // notify detaches all devices
            for (UsbDevice device : grantedDevices) {
                // call the method immediately
                onDeviceDetached(device);
            }
            grantedDevices.clear();
		}

		/**
		 * checks Attached/Detached devices
		 */
		synchronized void checkConnectedDevices() {
			HashMap<String, UsbDevice> deviceMap = usbManager.getDeviceList();
			
			// check attached device
			for (UsbDevice device : deviceMap.values()) {
                if (deviceGrantQueue.contains(device) || connectedDevices.contains(device)) {
                    continue;
                }

                Set<UsbInterface> midiInterfaces = UsbMidiDeviceUtils.findAllMidiInterfaces(device, deviceFilters);
                if (midiInterfaces.size() > 0) {
                    Log.d(Constants.TAG, "attached deviceName:" + device.getDeviceName() + ", device:" + device);
                    synchronized (deviceGrantQueue) {
                        deviceGrantQueue.add(device);
                    }
                }
			}
			
			// check detached device
			for (UsbDevice device : connectedDevices) {
				if (!deviceMap.containsValue(device)) {
                    if (device.equals(grantingDevice)) {
                        // currently granting, but detached
                        grantingDevice = null;
                        continue;
                    }

                    grantedDevices.remove(device);

					Log.d(Constants.TAG, "detached deviceName:" + device.getDeviceName() + ", device:" + device);
                    Message message = deviceDetachedHandler.obtainMessage();
                    message.obj = device;
                    deviceDetachedHandler.sendMessage(message);
				}
			}

            // update current connection status
			connectedDevices.clear();
            connectedDevices.addAll(deviceMap.values());
		}
	}
}
