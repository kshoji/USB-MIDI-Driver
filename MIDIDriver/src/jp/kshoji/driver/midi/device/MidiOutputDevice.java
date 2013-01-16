package jp.kshoji.driver.midi.device;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import jp.kshoji.driver.midi.util.Constants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

/**
 * MIDI Output Device
 * 
 * @author K.Shoji
 */
public final class MidiOutputDevice {

	private final UsbDevice usbDevice;
	private final UsbInterface usbInterface;
	private final UsbDeviceConnection deviceConnection;
	private final UsbEndpoint outputEndpoint;

	/**
	 * constructor
	 * 
	 * @param usbDevice
	 * @param usbDeviceConnection
	 * @param usbInterface
	 */
	public MidiOutputDevice(final UsbDevice usbDevice, final UsbDeviceConnection usbDeviceConnection, final UsbInterface usbInterface, final UsbEndpoint usbEndpoint) {
		this.usbDevice = usbDevice;
		this.deviceConnection = usbDeviceConnection;
		this.usbInterface = usbInterface;

		outputEndpoint = usbEndpoint;
		if (outputEndpoint == null) {
			throw new IllegalArgumentException("Output endpoint was not found.");
		}

		Log.i(Constants.TAG, "deviceConnection:" + deviceConnection + ", usbInterface:" + usbInterface);
		deviceConnection.claimInterface(this.usbInterface, true);
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

		deviceConnection.bulkTransfer(outputEndpoint, writeBuffer, writeBuffer.length, 0);

		Log.d(Constants.TAG, "Output:" + Arrays.toString(writeBuffer));
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
	public void sendMidiSystemCommonMessage(int cable, final byte bytes[]) {
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
	public void sendMidiSystemExclusive(int cable, final byte[] systemExclusive) {
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
		int transferedBytes = deviceConnection.bulkTransfer(outputEndpoint, buffer, buffer.length, 0);
		Log.d(Constants.TAG, "" + transferedBytes + " bytes of " + buffer.length + " bytes has been transfered.");
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
	public void sendMidiChannelAftertouch(final int cable, final int channel, final int pressure) {
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
	public void sendMidiPitchWheel(final int cable, final int channel, final int amount) {
		sendMidiMessage(0xe, cable, 0xe0 | (channel & 0xf), amount & 0xff, (amount >> 8) & 0xff);
	}

	/**
	 * ￼Single Byte
	 * Code Index Number : 0xf
	 * 
	 * @param cable 0-15
	 * @param byte1
	 */
	public void sendMidiSingleByte(int cable, int byte1) {
		sendMidiMessage(0xf, cable, byte1, 0, 0);
	}
}
