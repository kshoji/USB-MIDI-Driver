package jp.kshoji.driver.midi.activity;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.device.MidiDeviceConnectionWatcher;

/**
 * base Activity for using USB MIDI interface.
 * In this implement, each devices will be detected on connect.
 * launchMode must be "singleTask" or "singleInstance".
 * 
 * @author K.Shoji
 */
public abstract class AbstractMultipleMidiActivity extends Activity implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {

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
            midiInputDevice.setMidiEventListener(AbstractMultipleMidiActivity.this);
            if (midiInputDevices != null) {
                midiInputDevices.add(midiInputDevice);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AbstractMultipleMidiActivity.this.onMidiInputDeviceAttached(midiInputDevice);
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
                    AbstractMultipleMidiActivity.this.onMidiOutputDeviceAttached(midiOutputDevice);
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
            midiInputDevice.setMidiEventListener(null);
            if (midiInputDevices != null) {
                midiInputDevices.remove(midiInputDevice);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AbstractMultipleMidiActivity.this.onMidiInputDeviceDetached(midiInputDevice);
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
                    AbstractMultipleMidiActivity.this.onMidiOutputDeviceDetached(midiOutputDevice);
                }
            });
        }
    }

	Set<MidiInputDevice> midiInputDevices = null;
	Set<MidiOutputDevice> midiOutputDevices = null;
	OnMidiDeviceAttachedListener deviceAttachedListener = null;
	OnMidiDeviceDetachedListener deviceDetachedListener = null;
	MidiDeviceConnectionWatcher deviceConnectionWatcher = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		midiInputDevices = new HashSet<MidiInputDevice>();
		midiOutputDevices = new HashSet<MidiOutputDevice>();

		UsbManager usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
		deviceAttachedListener = new OnMidiDeviceAttachedListenerImpl();
		deviceDetachedListener = new OnMidiDeviceDetachedListenerImpl();

		deviceConnectionWatcher = new MidiDeviceConnectionWatcher(getApplicationContext(), usbManager, deviceAttachedListener, deviceDetachedListener);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		deviceConnectionWatcher.stop();
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


	/**
	 * Suspends receiving/transmitting MIDI messages.
	 * All events will be discarded until the devices being resumed.
	 */
	protected final void suspendMidiDevices() {
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
	protected final void resumeMidiDevices() {
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
	 * Get connected USB MIDI devices.
	 * 
	 * @return {@link Set<MidiInputDevice>}
	 */
    @NonNull
    public final Set<MidiInputDevice> getMidiInputDevices() {
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.checkConnectedDevicesImmediately();
		}

		return Collections.unmodifiableSet(midiInputDevices);
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
	public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
		// do nothing in this implementation
	}

	@Override
	public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
		// do nothing in this implementation
	}

	@Override
	public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
		// do nothing in this implementation
	}

	@Override
	public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
		// do nothing in this implementation
	}
}
