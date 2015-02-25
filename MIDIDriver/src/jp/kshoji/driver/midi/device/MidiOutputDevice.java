package jp.kshoji.driver.midi.device;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbRequest;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;

/**
 * MIDI Output Device stop() method must be called when the application will be destroyed.
 *
 * @author K.Shoji
 */
public final class MidiOutputDevice {

	private final UsbDevice usbDevice;
	final UsbDeviceConnection usbDeviceConnection;
	private final UsbInterface usbInterface;
	final UsbEndpoint outputEndpoint;

	final WaiterThread waiterThread;

	/**
	 * constructor
	 *
	 * @param usbDevice the UsbDevice
	 * @param usbDeviceConnection the UsbDeviceConnection
	 * @param usbInterface the UsbInterface
	 * @param usbEndpoint the UsbEndpoint
	 */
	public MidiOutputDevice(@NonNull UsbDevice usbDevice, @NonNull UsbDeviceConnection usbDeviceConnection, @NonNull UsbInterface usbInterface, @NonNull UsbEndpoint usbEndpoint) {
		this.usbDevice = usbDevice;
		this.usbDeviceConnection = usbDeviceConnection;
		this.usbInterface = usbInterface;

		waiterThread = new WaiterThread();

		outputEndpoint = usbEndpoint;

		this.usbDeviceConnection.claimInterface(this.usbInterface, true);

        waiterThread.setName("MidiOutputDevice[" + usbDevice.getDeviceName() + "].WaiterThread");
		waiterThread.start();
	}

	/**
	 * stop to use this device.
	 */
	public void stop() {
		usbDeviceConnection.releaseInterface(usbInterface);

		resume();
		waiterThread.stopFlag = true;

		// blocks while the thread will stop
		while (waiterThread.isAlive()) {
			try {
                waiterThread.interrupt();
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	/**
	 * Suspends event sending
	 */
	public void suspend() {
		waiterThread.suspendFlag = true;
		waiterThread.interrupt();
	}

	/**
	 * Resumes event sending
	 */
	public void resume() {
		waiterThread.suspendFlag = false;
		waiterThread.interrupt();
	}

    /**
     * Get the product name
     * 
     * @return the product name. null if API Level < {@link android.os.Build.VERSION_CODES#HONEYCOMB_MR2 }, or the product name is truly null
     */
    @Nullable
    public String getProductName() {
        return UsbMidiDeviceUtils.getProductName(usbDevice, usbDeviceConnection);
    }

    /**
     * Get the manufacturer name
     *
     * @return the manufacturer name. null if API Level < {@link android.os.Build.VERSION_CODES#HONEYCOMB_MR2 }, or the manufacturer name is truly null
     */
    @Nullable
    public String getManufacturerName() {
        return UsbMidiDeviceUtils.getManufacturerName(usbDevice, usbDeviceConnection);
    }

    /**
     * Get the device name(linux device path)
     * @return the device name(linux device path)
     */
    @NonNull
    public String getDeviceAddress() {
        return usbDevice.getDeviceName();
    }

    /**
	 * @return the usbDevice
	 */
    @NonNull
    public UsbDevice getUsbDevice() {
		return usbDevice;
	}

	/**
	 * @return the usbInterface
	 */
    @NonNull
    public UsbInterface getUsbInterface() {
		return usbInterface;
	}

	/**
	 * @return the usbEndpoint
	 */
    @NonNull
    public UsbEndpoint getUsbEndpoint() {
		return outputEndpoint;
	}

	/**
	 * Sending thread for output data. Loops infinitely while stopFlag == false.
	 *
	 * @author K.Shoji
	 */
	private final class WaiterThread extends Thread {
		final Queue<byte[]> queue = new LinkedList<byte[]>();

		volatile boolean stopFlag;
		volatile boolean suspendFlag;

		private UsbRequest usbRequest;

		/**
		 * constructor
		 */
		WaiterThread() {
			stopFlag = false;
			suspendFlag = false;
		}

		private final Handler handler = new Handler(new Callback() {
			@Override
			public synchronized boolean handleMessage(Message msg) {
				if (!(msg.obj instanceof byte[])) {
					return false;
				}

				byte[] writeBuffer = (byte[]) msg.obj;

				synchronized (queue) {
					queue.add(writeBuffer);
				}

				// message has been queued, so interrupt the waiter thread
				waiterThread.interrupt();

				// message handled successfully
				return true;
			}
		});

		@NonNull
        Handler getHandler() {
			return handler;
		}

		@Override
		public void run() {
			byte[] dequedDataBuffer;
			int queueSize = 0;
			final int maxPacketSize = outputEndpoint.getMaxPacketSize();
			byte[] endpointBuffer = new byte[maxPacketSize];
			int endpointBufferLength = 0;
			int bufferPosition;
			int dequedDataBufferLength;
            int usbRequestFailCount;

			while (stopFlag == false) {
				dequedDataBuffer = null;
				synchronized (queue) {
					queueSize = queue.size();
					if (queueSize > 0) {
						dequedDataBuffer = queue.poll();
					}
				}

				if (suspendFlag) {
					try {
						// sleep until interrupted
						sleep(500);
					} catch (InterruptedException e) {
						// interrupted: event queued, or stopFlag/suspendFlag changed.
					}
					continue;
				}

				if (dequedDataBuffer != null) {
					dequedDataBufferLength = dequedDataBuffer.length;

					// usbRequest.queue() is not thread-safe
					synchronized (usbDeviceConnection) {
						if (usbRequest == null) {
							usbRequest = new UsbRequest();
							usbRequest.initialize(usbDeviceConnection, outputEndpoint);
						}

						// usbRequest can't send data larger than maxPacketSize. split the data.
						for (bufferPosition = 0; bufferPosition < dequedDataBufferLength; bufferPosition += maxPacketSize) {
							if (bufferPosition + maxPacketSize > dequedDataBufferLength) {
								endpointBufferLength = dequedDataBufferLength % maxPacketSize;
							} else {
								endpointBufferLength = maxPacketSize;
							}
							System.arraycopy(dequedDataBuffer, bufferPosition, endpointBuffer, 0, endpointBufferLength);

                            usbRequestFailCount = 0;
                            // if device disconnected, usbRequest.queue returns false
							while (usbRequest.queue(ByteBuffer.wrap(endpointBuffer), endpointBufferLength) == false) {
								// loop until queue completed

                                usbRequestFailCount++;
                                if (usbRequestFailCount > 10) {
                                    // maybe disconnected
                                    stopFlag = true;
                                    break;
                                }
							}

                            if (stopFlag) {
                                break;
                            }

                            usbRequestFailCount = 0;
							while (usbRequest.equals(usbDeviceConnection.requestWait()) == false) {
								// loop until result received
                                usbRequestFailCount++;
                                if (usbRequestFailCount > 10) {
                                    // maybe disconnected
                                    stopFlag = true;
                                    break;
                                }
							}
						}
					}
				}

				// no more data in queue, sleep.
				if (queueSize == 0 && !interrupted()) {
					try {
						// sleep until interrupted
						sleep(500);
					} catch (InterruptedException e) {
						// interrupted: event queued, or stopFlag changed.
					}
				}
			}

			if (usbRequest != null) {
				usbRequest.close();
			}
		}
	}

	/**
	 * Sends MIDI message to output device.
	 *
	 * @param codeIndexNumber Code Index Number(CIN)
	 * @param cable the cable ID 0-15
	 * @param byte1 the first byte
	 * @param byte2 the second byte
	 * @param byte3 the third byte
	 */
	private void sendMidiMessage(int codeIndexNumber, int cable, int byte1, int byte2, int byte3) {
		byte[] writeBuffer = new byte[4];

		writeBuffer[0] = (byte) (((cable & 0xf) << 4) | (codeIndexNumber & 0xf));
		writeBuffer[1] = (byte) byte1;
		writeBuffer[2] = (byte) byte2;
		writeBuffer[3] = (byte) byte3;

		Handler handler = waiterThread.getHandler();
		handler.sendMessage(Message.obtain(handler, 0, writeBuffer));
	}

	/**
	 * Miscellaneous function codes. Reserved for future extensions. Code Index Number : 0x0
	 *
     * @param cable the cable ID 0-15
     * @param byte1 the first byte
     * @param byte2 the second byte
     * @param byte3 the third byte
	 */
	public void sendMidiMiscellaneousFunctionCodes(int cable, int byte1, int byte2, int byte3) {
		sendMidiMessage(0x0, cable, byte1, byte2, byte3);
	}

	/**
	 * Cable events. Reserved for future expansion. Code Index Number : 0x1
	 *
     * @param cable the cable ID 0-15
     * @param byte1 the first byte
     * @param byte2 the second byte
     * @param byte3 the third byte
	 */
	public void sendMidiCableEvents(int cable, int byte1, int byte2, int byte3) {
		sendMidiMessage(0x1, cable, byte1, byte2, byte3);
	}

	/**
	 * System Common messages, or SysEx ends with following single byte. Code Index Number : 0x2 0x3 0x5
	 *
     * @param cable the cable ID 0-15
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
	 * SysEx Code Index Number : 0x4, 0x5, 0x6, 0x7
	 *
     * @param cable the cable ID 0-15
	 * @param systemExclusive : start with 'F0', and end with 'F7'
	 */
	public void sendMidiSystemExclusive(int cable, @NonNull byte[] systemExclusive) {
		ByteArrayOutputStream transferDataStream = new ByteArrayOutputStream();

		for (int sysexIndex = 0; sysexIndex < systemExclusive.length; sysexIndex += 3) {
			if ((sysexIndex + 3 < systemExclusive.length)) {
				// sysex starts or continues...
				transferDataStream.write((((cable & 0xf) << 4) | 0x4));
				transferDataStream.write(systemExclusive[sysexIndex    ] & 0xff);
				transferDataStream.write(systemExclusive[sysexIndex + 1] & 0xff);
				transferDataStream.write(systemExclusive[sysexIndex + 2] & 0xff);
			} else {
				switch (systemExclusive.length % 3) {
				case 1:
					// sysex end with 1 byte
					transferDataStream.write((((cable & 0xf) << 4) | 0x5));
					transferDataStream.write(systemExclusive[sysexIndex    ] & 0xff);
					transferDataStream.write(0);
					transferDataStream.write(0);
					break;
				case 2:
					// sysex end with 2 bytes
					transferDataStream.write((((cable & 0xf) << 4) | 0x6));
					transferDataStream.write(systemExclusive[sysexIndex    ] & 0xff);
					transferDataStream.write(systemExclusive[sysexIndex + 1] & 0xff);
					transferDataStream.write(0);
					break;
				case 0:
					// sysex end with 3 bytes
					transferDataStream.write((((cable & 0xf) << 4) | 0x7));
					transferDataStream.write(systemExclusive[sysexIndex    ] & 0xff);
					transferDataStream.write(systemExclusive[sysexIndex + 1] & 0xff);
					transferDataStream.write(systemExclusive[sysexIndex + 2] & 0xff);
					break;
                default:
                    break;
				}
			}
		}

		byte[] buffer = transferDataStream.toByteArray();

		Handler handler = waiterThread.getHandler();
		handler.sendMessage(Message.obtain(handler, 0, buffer));
	}

	/**
	 * Note-off Code Index Number : 0x8
	 *
	 * @param cable
	 *            0-15
	 * @param channel
	 *            0-15
	 * @param note
	 *            0-127
	 * @param velocity
	 *            0-127
	 */
	public void sendMidiNoteOff(int cable, int channel, int note, int velocity) {
		sendMidiMessage(0x8, cable, 0x80 | (channel & 0xf), note, velocity);
	}

	/**
	 * Note-on Code Index Number : 0x9
	 *
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param note
	 *            0-127
	 * @param velocity
	 *            0-127
	 */
	public void sendMidiNoteOn(int cable, int channel, int note, int velocity) {
		sendMidiMessage(0x9, cable, 0x90 | (channel & 0xf), note, velocity);
	}

	/**
	 * Poly-KeyPress Code Index Number : 0xa
	 *
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param note
	 *            0-127
	 * @param pressure
	 *            0-127
	 */
	public void sendMidiPolyphonicAftertouch(int cable, int channel, int note, int pressure) {
		sendMidiMessage(0xa, cable, 0xa0 | (channel & 0xf), note, pressure);
	}

	/**
	 * Control Change Code Index Number : 0xb
	 *
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param function
	 *            0-127
	 * @param value
	 *            0-127
	 */
	public void sendMidiControlChange(int cable, int channel, int function, int value) {
		sendMidiMessage(0xb, cable, 0xb0 | (channel & 0xf), function, value);
	}

	/**
	 * Program Change Code Index Number : 0xc
	 *
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param program
	 *            0-127
	 */
	public void sendMidiProgramChange(int cable, int channel, int program) {
		sendMidiMessage(0xc, cable, 0xc0 | (channel & 0xf), program, 0);
	}

	/**
	 * Channel Pressure Code Index Number : 0xd
	 *
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param pressure
	 *            0-127
	 */
	public void sendMidiChannelAftertouch(int cable, int channel, int pressure) {
		sendMidiMessage(0xd, cable, 0xd0 | (channel & 0xf), pressure, 0);
	}

	/**
	 * PitchBend Change Code Index Number : 0xe
	 *
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param amount 0(low)-8192(center)-16383(high)
	 */
	public void sendMidiPitchWheel(int cable, int channel, int amount) {
		sendMidiMessage(0xe, cable, 0xe0 | (channel & 0xf), amount & 0x7f, (amount >> 7) & 0x7f);
	}

	/**
	 * Single Byte Code Index Number : 0xf
	 *
     * @param cable the cable ID 0-15
	 * @param byte1 the first byte
	 */
	public void sendMidiSingleByte(int cable, int byte1) {
		sendMidiMessage(0xf, cable, byte1, 0, 0);
	}

	/**
	 * RPN message
	 *
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param function 14bits
	 * @param value 7bits or 14bits
	 */
	public void sendRPNMessage(int cable, int channel, int function, int value) {
		sendRPNMessage(cable, channel, (function >> 7) & 0x7f, function & 0x7f, value);
	}

	/**
	 * RPN message
	 *
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
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
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
	 * @param function 14bits
	 * @param value 7bits or 14bits
	 */
	public void sendNRPNMessage(int cable, int channel, int function, int value) {
		sendNRPNMessage(cable, channel, (function >> 7) & 0x7f, function & 0x7f, value);
	}

	/**
	 * NRPN message
	 *
     * @param cable the cable ID 0-15
     * @param channel the MIDI channel number 0-15
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
