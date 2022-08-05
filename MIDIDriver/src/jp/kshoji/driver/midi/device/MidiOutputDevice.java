package jp.kshoji.driver.midi.device;

import android.annotation.SuppressLint;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.LinkedList;
import java.util.Queue;

import jp.kshoji.driver.midi.util.ReusableByteArrayOutputStream;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;

/**
 * MIDI Output Device
 *
 * @author K.Shoji
 */
public final class MidiOutputDevice {

    private final UsbDevice usbDevice;
    final UsbDeviceConnection usbDeviceConnection;
    private final UsbInterface usbInterface;
    final UsbEndpoint outputEndpoint;

    final WaiterThread waiterThread;

    private static final int BUFFER_POOL_SIZE = 1024;
	final LinkedList<byte[]> bufferPool = new LinkedList<>();

    private ReusableByteArrayOutputStream sysexTransferDataStream = new ReusableByteArrayOutputStream();

    /**
	 * Constructor
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

		for (int i = 0; i < BUFFER_POOL_SIZE; i++) {
			bufferPool.addLast(new byte[4]);
		}
	}

    /**
     * stop to use this device.
     */
    void stop() {
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
    @Deprecated
    @NonNull
    public UsbInterface getUsbInterface() {
        return usbInterface;
    }

    /**
     * @return the usbEndpoint
     */
    @Deprecated
    @NonNull
    public UsbEndpoint getUsbEndpoint() {
        return outputEndpoint;
    }

	/**
	 * Sending thread for output data. Loops infinitely while stopFlag == false.
	 *
	 * @author K.Shoji
	 */
    @SuppressLint("NewApi")
	private final class WaiterThread extends Thread {
        final Queue<byte[]> queue = new LinkedList<>();

		volatile boolean stopFlag;
		volatile boolean suspendFlag;

        /**
		 * Constructor
		 */
		WaiterThread() {
			stopFlag = false;
			suspendFlag = false;
		}

		@Override
		public void run() {
			byte[] dequedDataBuffer;
            int dequedDataBufferLength;
			int queueSize;
			final int maxPacketSize = outputEndpoint.getMaxPacketSize();
			byte[] endpointBuffer = new byte[maxPacketSize];
			int endpointBufferLength;
			int bufferPosition;
            int usbRequestFailCount;
            int bytesWritten;

            while (!stopFlag) {
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

                    synchronized (usbDeviceConnection) {
                        // usb can't send data larger than maxPacketSize. split the data.
                        for (bufferPosition = 0; bufferPosition < dequedDataBufferLength; bufferPosition += maxPacketSize) {
                            endpointBufferLength = dequedDataBufferLength - bufferPosition;
                            if (endpointBufferLength > maxPacketSize) {
                                endpointBufferLength = maxPacketSize;
                            }

                            usbRequestFailCount = 0;
                            // if device disconnected, usbDeviceConnection.bulkTransfer returns negative value
                            while (true) {
                                // loop until transfer completed
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                                    // JELLY_BEAN_MR2 supports bulkTransfer with offset
                                    bytesWritten = usbDeviceConnection.bulkTransfer(outputEndpoint, dequedDataBuffer, bufferPosition, endpointBufferLength, 10);
                                } else {
                                    if (bufferPosition > 0) {
                                        // copy the fragment to the endpointBuffer before transfer
                                        System.arraycopy(dequedDataBuffer, bufferPosition, endpointBuffer, 0, endpointBufferLength);
                                        bytesWritten = usbDeviceConnection.bulkTransfer(outputEndpoint, endpointBuffer, endpointBufferLength, 10);
                                    } else {
                                        // it's the first fragment.. copy is not required
                                        bytesWritten = usbDeviceConnection.bulkTransfer(outputEndpoint, dequedDataBuffer, endpointBufferLength, 10);
                                    }
                                }

                                if (bytesWritten < 0) {
                                    usbRequestFailCount++;
                                } else {
                                    break;
                                }

                                if (usbRequestFailCount > 10) {
                                    // maybe disconnected
                                    stopFlag = true;
                                    break;
                                }
                            }

                            if (stopFlag) {
                                break;
                            }
                        }
                    }

                    if (dequedDataBuffer.length == 4) {
                        synchronized (queue) {
                            bufferPool.addLast(dequedDataBuffer);
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
        while (bufferPool.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {

            }
        }

        synchronized (waiterThread.queue) {
			byte[] writeBuffer = bufferPool.removeFirst();

			writeBuffer[0] = (byte) (((cable & 0xf) << 4) | (codeIndexNumber & 0xf));
			writeBuffer[1] = (byte) byte1;
			writeBuffer[2] = (byte) byte2;
			writeBuffer[3] = (byte) byte3;

			waiterThread.queue.add(writeBuffer);
		}

		// message has been queued, so interrupt the waiter thread
		waiterThread.interrupt();
	}

	/**
     * Send a MIDI message with 3 bytes raw MIDI data
     *
     * @param cable the cable ID 0-15
     * @param byte1 the first byte
     * @param byte2 the second byte: ignored when 1 byte message
     * @param byte3 the third byte: ignored when 1-2 byte message
     */
    public void sendMidiMessage(int cable, int byte1, int byte2, int byte3) {
        int codeIndexNumber = 0;

        switch (byte1 & 0xf0) {
            case 0x80: // Note Off
                codeIndexNumber = 0x8;
                break;
            case 0x90: // Note On
                codeIndexNumber = 0x9;
                break;
            case 0xa0: // Poly Pressure
                codeIndexNumber = 0xa;
                break;
            case 0xb0: // Control Change
                codeIndexNumber = 0xb;
                break;
            case 0xc0: // Program Change
                codeIndexNumber = 0xc;
                break;
            case 0xd0: // Channel Pressure
                codeIndexNumber = 0xd;
                break;
            case 0xe0: // Pitch Bend
                codeIndexNumber = 0xe;
                break;
            case 0xf0: // SysEx with 3 bytes
                switch (byte1) {
                    case 0xf0: // Start Of Exclusive
                        if (byte2 == 0xf7) {
                            // 2 byte SysEx(F0 F7)
                            codeIndexNumber = 0x6;
                        } else if (byte3 == 0xf7) {
                            // 3 byte SysEx(F0 xx F7)
                            codeIndexNumber = 0x7;
                        } else {
                            // ignored
                            return;
                        }
                        break;
                    case 0xf7: // End of Exclusive
                        // ignored
                        return;

                    case 0xf4: // (Undefined MIDI System Common)
                    case 0xf5: // (Undefined MIDI System Common / Bus Select?)
                    case 0xf9: // (Undefined MIDI System Real-time)
                    case 0xfd: // (Undefined MIDI System Real-time)
                        // ignored
                        return;

                    case 0xf6: // Tune Request
                    case 0xf8: // Timing Clock
                    case 0xfa: // Start
                    case 0xfb: // Continue
                    case 0xfc: // Stop
                    case 0xfe: // Active Sensing
                    case 0xff: // System Reset
                        // Single byte message
                        codeIndexNumber = 0x5;
                        break;

                    case 0xf1: // MIDI Time Code
                    case 0xf3: // Song Select
                        // Two byte message
                        codeIndexNumber = 0x2;
                        break;

                    case 0xf2: // Song Point Pointer
                        // Three byte message
                        codeIndexNumber = 0x3;
                        break;
                    default:
                        break;
                }
                break;
            default:
                // ignored
                return;
        }

        sendMidiMessage(codeIndexNumber, cable, byte1, byte2, byte3);
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
     * @param cable           the cable ID 0-15
	 * @param systemExclusive : start with 'F0', and end with 'F7'
	 */
    public void sendMidiSystemExclusive(int cable, @NonNull byte[] systemExclusive) {
        if (systemExclusive.length > 3) {
            sysexTransferDataStream.reset();

            for (int sysexIndex = 0; sysexIndex < systemExclusive.length; sysexIndex += 3) {
                if ((sysexIndex + 3 < systemExclusive.length)) {
                    // sysex starts or continues...
                    sysexTransferDataStream.write((((cable & 0xf) << 4) | 0x4));
                    sysexTransferDataStream.write(systemExclusive[sysexIndex] & 0xff);
                    sysexTransferDataStream.write(systemExclusive[sysexIndex + 1] & 0xff);
                    sysexTransferDataStream.write(systemExclusive[sysexIndex + 2] & 0xff);
                } else {
                    switch (systemExclusive.length % 3) {
                        case 1:
                            // sysex end with 1 byte
                            sysexTransferDataStream.write((((cable & 0xf) << 4) | 0x5));
                            sysexTransferDataStream.write(systemExclusive[sysexIndex] & 0xff);
                            sysexTransferDataStream.write(0);
                            sysexTransferDataStream.write(0);
                            break;
                        case 2:
                            // sysex end with 2 bytes
                            sysexTransferDataStream.write((((cable & 0xf) << 4) | 0x6));
                            sysexTransferDataStream.write(systemExclusive[sysexIndex] & 0xff);
                            sysexTransferDataStream.write(systemExclusive[sysexIndex + 1] & 0xff);
                            sysexTransferDataStream.write(0);
                            break;
                        case 0:
                            // sysex end with 3 bytes
                            sysexTransferDataStream.write((((cable & 0xf) << 4) | 0x7));
                            sysexTransferDataStream.write(systemExclusive[sysexIndex] & 0xff);
                            sysexTransferDataStream.write(systemExclusive[sysexIndex + 1] & 0xff);
                            sysexTransferDataStream.write(systemExclusive[sysexIndex + 2] & 0xff);
                            break;
                        default:
                            break;
                    }
                }
            }

            synchronized (waiterThread.queue) {
                // allocating new byte[] here...
                waiterThread.queue.add(sysexTransferDataStream.toByteArray());
            }

            // message has been queued, so interrupt the waiter thread
            waiterThread.interrupt();
        } else {
            switch (systemExclusive.length) {
                case 1:
                    // sysex end with 1 byte
                    sendMidiMessage(0x5, cable & 0xf, systemExclusive[0], 0, 0);
                    break;
                case 2:
                    // sysex end with 2 bytes
                    sendMidiMessage(0x6, cable & 0xf, systemExclusive[0], systemExclusive[1], 0);
                    break;
                case 3:
                    // sysex end with 3 bytes
                    sendMidiMessage(0x7, cable & 0xf, systemExclusive[0], systemExclusive[1], systemExclusive[2]);
                    break;
            }
        }
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
     * @param cable    the cable ID 0-15
     * @param channel  the MIDI channel number 0-15
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
     * @param cable    the cable ID 0-15
     * @param channel  the MIDI channel number 0-15
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
     * @param cable    the cable ID 0-15
     * @param channel  the MIDI channel number 0-15
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
     * @param cable   the cable ID 0-15
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
     * @param cable    the cable ID 0-15
     * @param channel  the MIDI channel number 0-15
	 * @param pressure
	 *            0-127
     */
    public void sendMidiChannelAftertouch(int cable, int channel, int pressure) {
        sendMidiMessage(0xd, cable, 0xd0 | (channel & 0xf), pressure, 0);
    }

    /**
     * PitchBend Change Code Index Number : 0xe
     *
     * @param cable   the cable ID 0-15
     * @param channel the MIDI channel number 0-15
     * @param amount  0(low)-8192(center)-16383(high)
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
     * MIDI Time Code(MTC) Quarter Frame
     *
     * @param timing 0-127
     */
    public final void sendMidiTimeCodeQuarterFrame(int cable, int timing) {
        sendMidiSystemCommonMessage(cable, new byte[] {(byte) 0xf1, (byte) (timing & 0x7f)});
    }

    /**
     * Song Select
     *
     * @param song 0-127
     */
    public final void sendMidiSongSelect(int cable, int song) {
        sendMidiSystemCommonMessage(cable, new byte[] {(byte) 0xf3, (byte) (song & 0x7f)});
    }

    /**
     * Song Position Pointer
     *
     * @param position 0-16383
     */
    public final void sendMidiSongPositionPointer(int cable, int position) {
        sendMidiSystemCommonMessage(cable, new byte[] {(byte) 0xf2, (byte) (position & 0x7f), (byte) ((position >> 7) & 0x7f)});
    }

    /**
     * Tune Request
     */
    public final void sendMidiTuneRequest(int cable) {
        sendMidiSingleByte(cable, 0xf6);
    }

    /**
     * Timing Clock
     */
    public final void sendMidiTimingClock(int cable) {
        sendMidiSingleByte(cable, 0xf8);
    }

    /**
     * Start Playing
     */
    public final void sendMidiStart(int cable) {
        sendMidiSingleByte(cable, 0xfa);
    }

    /**
     * Continue Playing
     */
    public final void sendMidiContinue(int cable) {
        sendMidiSingleByte(cable, 0xfb);
    }

    /**
     * Stop Playing
     */
    public final void sendMidiStop(int cable) {
        sendMidiSingleByte(cable, 0xfc);
    }

    /**
     * Active Sensing
     */
    public final void sendMidiActiveSensing(int cable) {
        sendMidiSingleByte(cable, 0xfe);
    }

    /**
     * Reset Device
     */
    public final void sendMidiReset(int cable) {
        sendMidiSingleByte(cable, 0xff);
    }

    /**
     * RPN message
     *
     * @param cable    the cable ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param function 14bits
     * @param value    7bits or 14bits
     */
    public void sendRPNMessage(int cable, int channel, int function, int value) {
        sendRPNMessage(cable, channel, (function >> 7) & 0x7f, function & 0x7f, value);
    }

    /**
     * RPN message
     *
     * @param cable       the cable ID 0-15
     * @param channel     the MIDI channel number 0-15
     * @param functionMSB higher 7bits
     * @param functionLSB lower 7bits
     * @param value       7bits or 14bits
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
     * @param cable    the cable ID 0-15
     * @param channel  the MIDI channel number 0-15
     * @param function 14bits
     * @param value    7bits or 14bits
     */
    public void sendNRPNMessage(int cable, int channel, int function, int value) {
        sendNRPNMessage(cable, channel, (function >> 7) & 0x7f, function & 0x7f, value);
    }

    /**
     * NRPN message
     *
     * @param cable       the cable ID 0-15
     * @param channel     the MIDI channel number 0-15
     * @param functionMSB higher 7bits
     * @param functionLSB lower 7bits
     * @param value       7bits or 14bits
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
