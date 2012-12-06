package jp.kshoji.driver.midi.device;

import java.util.Arrays;

import jp.kshoji.driver.midi.handler.MidiMessageCallback;
import jp.kshoji.driver.midi.listener.OnMidiEventListener;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbDeviceUtils;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * MIDI Input Device
 * stop() method must be called when the application will be destroyed.
 * 
 * @author K.Shoji
 */
public final class MidiInputDevice {

	final UsbDeviceConnection deviceConnection;
	UsbEndpoint inputEndpoint;
	private final WaiterThread waiterThread;

	/**
	 * @param device
	 * @param connection
	 * @param usbInterface
	 * @param midiEventListener
	 * @throws IllegalArgumentException
	 */
	public MidiInputDevice(final UsbDevice device, final UsbDeviceConnection connection, final UsbInterface usbInterface, final OnMidiEventListener midiEventListener) throws IllegalArgumentException {
		deviceConnection = connection;

		waiterThread = new WaiterThread(new Handler(new MidiMessageCallback(device, midiEventListener)));

		inputEndpoint = UsbDeviceUtils.findMidiEndpoint(usbInterface, UsbConstants.USB_DIR_IN);
		if (inputEndpoint == null) {
			throw new IllegalArgumentException("Input endpoint was not found.");
		}

		if (deviceConnection != null) {
			deviceConnection.claimInterface(usbInterface, true);
		}
		
		waiterThread.start();
	}

	public void stop() {
		synchronized (waiterThread) {
			waiterThread.stopFlag = true;
		}
	}

	/**
	 * Polling thread for input data.
	 * Loops infinitely while stopFlag == false.
	 * 
	 * @author K.Shoji
	 */
	private class WaiterThread extends Thread {
		private byte[] readBuffer = new byte[64];

		public boolean stopFlag;
		
		private Handler receiveHandler;

		/**
		 * Constructor
		 * 
		 * @param handler
		 */
		public WaiterThread(final Handler handler) {
			stopFlag = false;
			this.receiveHandler = handler;
		}

		/*
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			while (true) {
				synchronized (this) {
					if (stopFlag) {
						return;
					}
				}
				if (inputEndpoint == null) {
					continue;
				}
				
				int length = deviceConnection.bulkTransfer(inputEndpoint, readBuffer, readBuffer.length, 0);
				if (length > 0) {
					byte[] read = new byte[length];
					System.arraycopy(readBuffer, 0, read, 0, length);
					Log.d(Constants.TAG, "Input:" + Arrays.toString(read));
					
					Message message = new Message();
					message.obj = read;
					receiveHandler.sendMessage(message);
				}
			}
		}
	}
}
