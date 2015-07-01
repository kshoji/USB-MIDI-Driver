package jp.kshoji.driver.midi.activity;

import android.app.Activity;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import jp.kshoji.driver.midi.device.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;

/**
 * base Activity for using USB MIDI interface.
 * In this implement, the only one device (connected at first) will be detected.
 * launchMode must be "singleTask" or "singleInstance".
 * 
 * @author K.Shoji
 */
public abstract class AbstractSingleMidiActivity extends Activity implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {
	/**
	 * Implementation for single device connections.
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
            if (AbstractSingleMidiActivity.this.midiInputDevice != null) {
                return;
            }
            midiInputDevice.setMidiEventListener(AbstractSingleMidiActivity.this);
            AbstractSingleMidiActivity.this.midiInputDevice = midiInputDevice;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AbstractSingleMidiActivity.this.onMidiInputDeviceAttached(midiInputDevice);
                }
            });
        }

        @Override
        public void onMidiOutputDeviceAttached(@NonNull final MidiOutputDevice midiOutputDevice) {
            if (AbstractSingleMidiActivity.this.midiOutputDevice != null) {
                return;
            }

            AbstractSingleMidiActivity.this.midiOutputDevice = midiOutputDevice;

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AbstractSingleMidiActivity.this.onMidiOutputDeviceAttached(midiOutputDevice);
                }
            });
        }
    }
	
	/**
	 * Implementation for single device connections.
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
            if (AbstractSingleMidiActivity.this.midiInputDevice != null && AbstractSingleMidiActivity.this.midiInputDevice == midiInputDevice) {
                AbstractSingleMidiActivity.this.midiInputDevice = null;
            }
            midiInputDevice.setMidiEventListener(null);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AbstractSingleMidiActivity.this.onMidiInputDeviceDetached(midiInputDevice);
                }
            });
        }

        @Override
        public void onMidiOutputDeviceDetached(@NonNull final MidiOutputDevice midiOutputDevice) {
            if (AbstractSingleMidiActivity.this.midiOutputDevice != null && AbstractSingleMidiActivity.this.midiOutputDevice == midiOutputDevice) {
                AbstractSingleMidiActivity.this.midiOutputDevice = null;
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AbstractSingleMidiActivity.this.onMidiOutputDeviceDetached(midiOutputDevice);
                }
            });
        }
    }
	
	MidiInputDevice midiInputDevice = null;
	MidiOutputDevice midiOutputDevice = null;
	OnMidiDeviceAttachedListener deviceAttachedListener = null;
	OnMidiDeviceDetachedListener deviceDetachedListener = null;
	private MidiDeviceConnectionWatcher deviceConnectionWatcher = null;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		UsbManager usbManager = (UsbManager) getApplicationContext().getSystemService(Context.USB_SERVICE);
		deviceAttachedListener = new OnMidiDeviceAttachedListenerImpl();
		deviceDetachedListener = new OnMidiDeviceDetachedListenerImpl(); 
		
		deviceConnectionWatcher = new MidiDeviceConnectionWatcher(getApplicationContext(), usbManager, deviceAttachedListener, deviceDetachedListener);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.stop();
		}
		deviceConnectionWatcher = null;
		
        midiInputDevice = null;
		midiOutputDevice = null;
	}


	/**
	 * Suspends receiving/transmitting MIDI messages.
	 * All events will be discarded until the devices being resumed.
	 */
	protected final void suspendMidiDevices() {
		if (midiInputDevice != null) {
			midiInputDevice.suspend();
		}
		
		if (midiOutputDevice != null) {
			midiOutputDevice.suspend();
		}
	}
	
	/**
	 * Resumes from {@link #suspendMidiDevices()}
	 */
	protected final void resumeMidiDevices() {
		if (midiInputDevice != null) {
			midiInputDevice.resume();
		}
		
		if (midiOutputDevice != null) {
			midiOutputDevice.resume();
		}
	}

	/**
	 * Get MIDI output device, if available.
	 * 
	 * @return MidiOutputDevice, null if not available
	 */
    @Nullable
    public final MidiOutputDevice getMidiOutputDevice() {
		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.checkConnectedDevicesImmediately();
		}
		
		return midiOutputDevice;
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
