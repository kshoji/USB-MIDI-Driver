package jp.kshoji.driver.midi.device;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

import jp.kshoji.driver.midi.util.Constants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;

/**
 * MIDI Output Device
 * stop() method must be called when the application will be destroyed.
 * 
 * @author K.Shoji
 */
public final class MidiOutputDevice {

	private final UsbDevice usbDevice;
	final UsbDeviceConnection usbDeviceConnection;
	private final UsbInterface usbInterface;
	final UsbEndpoint outputEndpoint;

	private final WaiterThread waiterThread;

	/**
	 * constructor
	 * 
	 * @param usbDevice
	 * @param usbDeviceConnection
	 * @param usbInterface
	 */
	public MidiOutputDevice(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection, UsbInterface usbInterface, UsbEndpoint usbEndpoint) {
		this.usbDevice = usbDevice;
		this.usbDeviceConnection = usbDeviceConnection;
		this.usbInterface = usbInterface;

		waiterThread = new WaiterThread();
		
		outputEndpoint = usbEndpoint;
		if (outputEndpoint == null) {
			throw new IllegalArgumentException("Output endpoint was not found.");
		}

		usbDeviceConnection.claimInterface(usbInterface, true);
		
		waiterThread.start();
	}

	/**
	 * stop to use this device.
	 */
	public void stop() {
		waiterThread.stopFlag = true;
		
		// blocks while the thread will stop
		while (waiterThread.isAlive()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		}
		
		usbDeviceConnection.releaseInterface(usbInterface);
	}

	/**
	 * @return the usbDevice
	 */
	public UsbDevice getUsbDevice() {
		return usbDevice;
	}
	
	/**
	 * @return the usbInterface
	 */
	public UsbInterface getUsbInterface() {
		return usbInterface;
	}
	
	/**
	 * @return the usbEndpoint
	 */
	public UsbEndpoint getUsbEndpoint() {
		return outputEndpoint;
	}
	
	/**
	 * Thread for output data.
	 * Loops infinitely while stopFlag == false.
	 * 
	 * @author K.Shoji
	 */
	private final class WaiterThread extends Thread {
		boolean stopFlag;

		UsbRequest usbRequest;

		/**
		 * constructor
		 * 
		 * @param handler
		 */
		WaiterThread() {
			stopFlag = false;
		}
		
		private final Handler handler = new Handler(new Callback() {
			@Override
			public synchronized boolean handleMessage(Message msg) {
				if (!(msg.obj instanceof byte[])) {
					return false;
				}
				
				byte[] writeBuffer = (byte[])msg.obj;
				
				// usbRequest.queue() is not thread-safe
				synchronized (usbDeviceConnection) {
					if (usbRequest == null) {
						usbRequest =  new UsbRequest();
						usbRequest.initialize(usbDeviceConnection, outputEndpoint);
					}
					
					while (usbRequest.queue(ByteBuffer.wrap(writeBuffer), 4) == false) {
						// loop until queue completed
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// ignore exception
						}
					}
					
					while (usbRequest.equals(usbDeviceConnection.requestWait()) == false) {
						// loop until result received
						try {
							Thread.sleep(1);
						} catch (InterruptedException e) {
							// ignore exception
						}
					}
				}

				// message handled successfully
				return true;
			}
		});
		
		Handler getHandler() {
			return handler;
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			while (true) {
				if (stopFlag) {
					if (usbRequest != null) {
						usbRequest.close();
					}
					
					return;
				}
			}
		}
	}

	/**
	 * Sends MIDI message to output device.
	 * 
	 * @param codeIndexNumber
	 * @param cable
	 * @param byte1
	 * @param byte2
	 * @param byte3
	 */
	private void sendMidiMessage(int codeIndexNumber, int cable, int byte1, int byte2, int byte3) {
		byte[] writeBuffer = new byte[4];

		writeBuffer[0] = (byte) (((cable & 0xf) << 4) | (codeIndexNumber & 0xf));
		writeBuffer[1] = (byte) byte1;
		writeBuffer[2] = (byte) byte2;
		writeBuffer[3] = (byte) byte3;

		// usbRequest.queue() is not thread-safe
		Message message = new Message();
		message.obj = writeBuffer;
		waiterThread.getHandler().sendMessage(message);
	}

	/**
	 * Miscellaneous function codes. Reserved for future extensions.
	 * Code Index Number : 0x0
	 * 
	 * @param cable 0-15
	 * @param byte1
	 * @param byte2
	 * @param byte3
	 */
	public void sendMidiMiscellaneousFunctionCodes(int cable, int byte1, int byte2, int byte3) {
		sendMidiMessage(0x0, cable, byte1, byte2, byte3);
	}

	/**
	 * Cable events. Reserved for future expansion.
	 * Code Index Number : 0x1
	 * 
	 * @param cable 0-15
	 * @param byte1
	 * @param byte2
	 * @param byte3
	 */
	public void sendMidiCableEvents(int cable, int byte1, int byte2, int byte3) {
		sendMidiMessage(0x1, cable, byte1, byte2, byte3);
	}

	/**
	 * System Common messages, or SysEx ends with following single byte.
	 * Code Index Number : 0x2 0x3 0x5
	 * 
	 * @param cable 0-15
	 * @param bytes bytes.length:1, 2, or 3
	 */
	public void sendMidiSystemCommonMessage(int cable, byte bytes[]) {
		if (bytes == null) {
			return;
		}
		switch (bytes.length) {
		case 1:
			sendMidiMessage(0x5, cable, bytes[0] & 0xff, 0, 0);
			break;
		case 2:
			sendMidiMessage(0x2, cable, bytes[0] & 0xff, bytes[1] & 0xff, 0);
			break;
		case 3:
			sendMidiMessage(0x3, cable, bytes[0] & 0xff, bytes[1] & 0xff, bytes[2] & 0xff);
			break;
		default:
			// do nothing.
			break;
		}
	}

	/**
	 * SysEx
	 * Code Index Number : 0x4, 0x5, 0x6, 0x7
	 * 
	 * @param cable 0-15
	 * @param systemExclusive : start with 'F0', and end with 'F7'
	 */
	@SuppressWarnings("incomplete-switch")
	public void sendMidiSystemExclusive(int cable, byte[] systemExclusive) {
		ByteArrayOutputStream transferDataStream = new ByteArrayOutputStream();

		for (int sysexIndex = 0; sysexIndex < systemExclusive.length; sysexIndex += 3) {
			if ((sysexIndex + 3 < systemExclusive.length)) {
				// sysex starts or continues...
				transferDataStream.write((((cable & 0xf) << 4) | 0x4));
				transferDataStream.write(systemExclusive[sysexIndex + 0] & 0xff);
				transferDataStream.write(systemExclusive[sysexIndex + 1] & 0xff);
				transferDataStream.write(systemExclusive[sysexIndex + 2] & 0xff);
			} else {
				switch (systemExclusive.length % 3) {
				case 1:
					// sysex end with 1 byte
					transferDataStream.write((((cable & 0xf) << 4) | 0x5));
					transferDataStream.write(systemExclusive[sysexIndex + 0] & 0xff);
					transferDataStream.write(0);
					transferDataStream.write(0);
					break;
				case 2:
					// sysex end with 2 bytes
					transferDataStream.write((((cable & 0xf) << 4) | 0x6));
					transferDataStream.write(systemExclusive[sysexIndex + 0] & 0xff);
					transferDataStream.write(systemExclusive[sysexIndex + 1] & 0xff);
					transferDataStream.write(0);
					break;
				case 0:
					// sysex end with 3 bytes
					transferDataStream.write((((cable & 0xf) << 4) | 0x7));
					transferDataStream.write(systemExclusive[sysexIndex + 0] & 0xff);
					transferDataStream.write(systemExclusive[sysexIndex + 1] & 0xff);
					transferDataStream.write(systemExclusive[sysexIndex + 2] & 0xff);
					break;
				}
			}
		}
		
		byte[] buffer = transferDataStream.toByteArray();
		

		// usbRequest.queue() is not thread-safe
		Message message = new Message();
		message.obj = buffer;
		waiterThread.getHandler().sendMessage(message);
		
		Log.d(Constants.TAG, "" + buffer.length + " bytes of " + buffer.length + " bytes has been queued for transfering.");
	}

	/**
	 * Note-off
	 * Code Index Number : 0x8
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param note 0-127
	 * @param velocity 0-127
	 */
	public void sendMidiNoteOff(int cable, int channel, int note, int velocity) {
		sendMidiMessage(0x8, cable, 0x80 | (channel & 0xf), note, velocity);
	}

	/**
	 * Note-on
	 * Code Index Number : 0x9
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param note 0-127
	 * @param velocity 0-127
	 */
	public void sendMidiNoteOn(int cable, int channel, int note, int velocity) {
		sendMidiMessage(0x9, cable, 0x90 | (channel & 0xf), note, velocity);
	}

	/**
	 * Poly-KeyPress
	 * Code Index Number : 0xa
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param note 0-127
	 * @param pressure 0-127
	 */
	public void sendMidiPolyphonicAftertouch(int cable, int channel, int note, int pressure) {
		sendMidiMessage(0xa, cable, 0xa0 | (channel & 0xf), note, pressure);
	}

	/**
	 * Control Change
	 * Code Index Number : 0xb
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param function 0-127
	 * @param value 0-127
	 */
	public void sendMidiControlChange(int cable, int channel, int function, int value) {
		sendMidiMessage(0xb, cable, 0xb0 | (channel & 0xf), function, value);
	}

	/**
	 * Program Change
	 * Code Index Number : 0xc
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param program 0-127
	 */
	public void sendMidiProgramChange(int cable, int channel, int program) {
		sendMidiMessage(0xc, cable, 0xc0 | (channel & 0xf), program, 0);
	}

	/**
	 * Channel Pressure
	 * Code Index Number : 0xd
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param pressure 0-127
	 */
	public void sendMidiChannelAftertouch(int cable, int channel, int pressure) {
		sendMidiMessage(0xd, cable, 0xd0 | (channel & 0xf), pressure, 0);
	}

	/**
	 * PitchBend Change
	 * Code Index Number : 0xe
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param amount 0(low)-8192(center)-16383(high)
	 */
	public void sendMidiPitchWheel(int cable, int channel, int amount) {
		sendMidiMessage(0xe, cable, 0xe0 | (channel & 0xf), amount & 0x7f, (amount >> 7) & 0x7f);
	}

	/**
	 * Single Byte
	 * Code Index Number : 0xf
	 * 
	 * @param cable 0-15
	 * @param byte1
	 */
	public void sendMidiSingleByte(int cable, int byte1) {
		sendMidiMessage(0xf, cable, byte1, 0, 0);
	}
	
	/**
	 * RPN message
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param function 14bits
	 * @param value 7bits or 14bits
	 */
	public void sendRPNMessage(int cable, int channel, int function, int value) {
		sendRPNMessage(cable, channel, (function >> 7) & 0x7f, function & 0x7f, value);
	}
	
	/**
	 * RPN message
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param functionMSB higher 7bits
	 * @param functionLSB lower 7bits
	 * @param value 7bits or 14bits
	 */
	public void sendRPNMessage(int cable, int channel, int functionMSB, int functionLSB, int value) {
		// send the function
		sendMidiControlChange(cable, channel, 101, functionMSB & 0x7f);
		sendMidiControlChange(cable, channel, 100, functionLSB & 0x7f);
		
		// send the value
		if ((value >> 7) > 0) {
			sendMidiControlChange(cable, channel, 6, (value >> 7) & 0x7f);
			sendMidiControlChange(cable, channel, 38, value & 0x7f);
		} else {
			sendMidiControlChange(cable, channel, 6, value & 0x7f);
		}
		
		// send the NULL function
		sendMidiControlChange(cable, channel, 101, 0x7f);
		sendMidiControlChange(cable, channel, 100, 0x7f);
	}
	
	
	/**
	 * NRPN message
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param function 14bits
	 * @param value 7bits or 14bits
	 */
	public void sendNRPNMessage(int cable, int channel, int function, int value) {
		sendNRPNMessage(cable, channel, (function >> 7) & 0x7f, function & 0x7f, value);
	}

	/**
	 * NRPN message
	 * 
	 * @param cable 0-15
	 * @param channel 0-15
	 * @param functionMSB higher 7bits
	 * @param functionLSB lower 7bits
	 * @param value 7bits or 14bits
	 */
	public void sendNRPNMessage(int cable, int channel, int functionMSB, int functionLSB, int value) {
		// send the function
		sendMidiControlChange(cable, channel, 99, functionMSB & 0x7f);
		sendMidiControlChange(cable, channel, 98, functionLSB & 0x7f);
		
		// send the value
		if ((value >> 7) > 0) {
			sendMidiControlChange(cable, channel, 6, (value >> 7) & 0x7f);
			sendMidiControlChange(cable, channel, 38, value & 0x7f);
		} else {
			sendMidiControlChange(cable, channel, 6, value & 0x7f);
		}
		
		// send the NULL function
		sendMidiControlChange(cable, channel, 101, 0x7f);
		sendMidiControlChange(cable, channel, 100, 0x7f);
	}
}
