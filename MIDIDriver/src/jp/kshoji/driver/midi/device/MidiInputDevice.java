package jp.kshoji.driver.midi.device;

import java.util.Arrays;

import jp.kshoji.driver.midi.handler.MidiMessageCallback;
import jp.kshoji.driver.midi.listener.OnMidiEventListener;
import jp.kshoji.driver.midi.util.Constants;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * MIDI Input Device
 * 
 * @author K.Shoji
 */
public class MidiInputDevice {

	final UsbDeviceConnection deviceConnection;
	UsbEndpoint inputEndpoint;
	private final WaiterThread waiterThread;

	/**
	 * @param connection
	 * @param intf
	 * @param midiEventListener
	 * @throws IllegalArgumentException
	 */
	public MidiInputDevice(UsbDeviceConnection connection, UsbInterface intf, OnMidiEventListener midiEventListener) throws IllegalArgumentException {
		deviceConnection = connection;

		waiterThread = new WaiterThread(new Handler(new MidiMessageCallback(midiEventListener)));

		// look for our bulk endpoints
		for (int i = 0; i < intf.getEndpointCount(); i++) {
			UsbEndpoint endpoint = intf.getEndpoint(i);
			Log.d(Constants.TAG, "found endpoint: " + endpoint + ", type: " + endpoint.getType() + ", direction: " + endpoint.getDirection());
			if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK || endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
				if (endpoint.getDirection() == UsbConstants.USB_DIR_IN) {
					inputEndpoint = endpoint;
				}
			}
		}
		
		if (inputEndpoint == null) {
			throw new IllegalArgumentException("Input endpoint was not found.");
		}

		if (deviceConnection != null) {
			deviceConnection.claimInterface(intf, true);
		}
	}

	public void start() {
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
		public WaiterThread(Handler handler) {
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
