package jp.kshoji.driver.midi.fragment;

import android.app.Activity;
import android.app.Fragment;
import android.hardware.usb.UsbDevice;
import android.os.Bundle;
import android.util.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jp.kshoji.driver.midi.activity.MidiFragmentHostActivity;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.util.Constants;

/**
 * Base {@link Fragment} for using USB MIDI interface.
 * 
 * @author K.Shoji
 */
public abstract class AbstractMidiFragment extends Fragment implements OnMidiDeviceDetachedListener, OnMidiDeviceAttachedListener, OnMidiInputEventListener {
	private MidiFragmentHostActivity hostActivity;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Activity activity = getActivity();
		if (!(activity instanceof MidiFragmentHostActivity)) {
			Log.i(Constants.TAG, "activity:" + activity);
			throw new IllegalArgumentException("Parent Activity is not MidiFragmentHostActivity.");
		}
		
		this.hostActivity = (MidiFragmentHostActivity) activity;
	}
	
	/**
	 * Suspends receiving/transmitting MIDI messages.
	 * All events will be discarded until the devices being resumed.
	 */
	public final void suspendMidiDevices() {
		if (hostActivity != null) {
			hostActivity.suspendMidiDevices();
		}
	}
	
	/**
	 * Resumes from {@link #suspendMidiDevices()}
	 */
	public final void resumeMidiDevices() {
		if (hostActivity != null) {
			hostActivity.resumeMidiDevices();
		}
	}

	/**
	 * Get {@link MidiOutputDevice} attached with {@link MidiFragmentHostActivity}
	 * 
	 * @param usbDevice the UsbDevice
	 * @return {@link Set<MidiOutputDevice>} unmodifiable
	 */
	public final Set<MidiOutputDevice> getMidiOutputDevices(UsbDevice usbDevice) {
		if (hostActivity == null) {
			return Collections.unmodifiableSet(new HashSet<MidiOutputDevice>());
		}
		
		return hostActivity.getMidiOutputDevices(usbDevice);
	}
	
	/**
	 * Get {@link UsbDevice} attached with {@link MidiFragmentHostActivity}
	 * 
	 * @return {@link Set<UsbDevice>} unmodifiable
	 */
	public final Set<UsbDevice> getConnectedUsbDevices() {
		if (hostActivity == null) {
			return Collections.unmodifiableSet(new HashSet<UsbDevice>());
		}
		
		return hostActivity.getConnectedUsbDevices();
	}
}
