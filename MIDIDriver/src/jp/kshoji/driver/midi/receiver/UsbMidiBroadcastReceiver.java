package jp.kshoji.driver.midi.receiver;

import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

/**
 * Broadcast receiver for MIDI device
 * 
 * @author K.Shoji
 */
public class UsbMidiBroadcastReceiver extends BroadcastReceiver {
	private OnMidiDeviceDetachedListener onDeviceDetachedListener;

	public UsbMidiBroadcastReceiver(OnMidiDeviceDetachedListener onMidiDeviceDetachedListener) {
		onDeviceDetachedListener = onMidiDeviceDetachedListener;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
			UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
			if (device != null) {
				if (onDeviceDetachedListener != null) {
					onDeviceDetachedListener.onDeviceDetached(device);
				}
			}
		}
	}
}
