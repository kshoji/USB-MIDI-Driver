package jp.kshoji.driver.midi.device;

import java.util.Arrays;

import jp.kshoji.driver.midi.listener.OnMidiEventListener;
import jp.kshoji.driver.midi.util.Constants;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

/**
 * MIDI Output Device
 * 
 * @author K.Shoji
 */
public class MidiOutputDevice implements OnMidiEventListener {

	private final UsbDeviceConnection deviceConnection;
	private UsbEndpoint outputEndpoint;

	/**
	 * @param connection
	 * @param intf
	 */
	public MidiOutputDevice(UsbDeviceConnection connection, UsbInterface intf) {
		deviceConnection = connection;

		for (int i = 0; i < intf.getEndpointCount(); i++) {
			UsbEndpoint endpoint = intf.getEndpoint(i);
			if (endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_BULK || endpoint.getType() == UsbConstants.USB_ENDPOINT_XFER_INT) {
				if (endpoint.getDirection() == UsbConstants.USB_DIR_OUT) {
					outputEndpoint = endpoint;
				}
			}
		}

		if (outputEndpoint == null) {
			throw new IllegalArgumentException("Output endpoint was not found.");
		}

		deviceConnection.claimInterface(intf, true);
	}

	/**
	 * Sends MIDI message to output device.<br />
	 * TODO do this method with another thread
	 * 
	 * @param codeIndexNumber
	 * @param cable
	 * @param byte1
	 * @param byte2
	 * @param byte3
	 */
	private void sendMidiMessage(int codeIndexNumber, int cable, int byte1, int byte2, int byte3) {
		byte[] writeBuffer = new byte[outputEndpoint.getMaxPacketSize()];

		writeBuffer[0] = (byte) (((cable & 0xf) << 4) | (codeIndexNumber & 0xf));
		writeBuffer[1] = (byte) byte1;
		writeBuffer[2] = (byte) byte2;
		writeBuffer[3] = (byte) byte3;

		deviceConnection.bulkTransfer(outputEndpoint, writeBuffer, writeBuffer.length, 0);

		Log.d(Constants.TAG, "Output:" + Arrays.toString(writeBuffer));
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiMiscellaneousFunctionCodes(int, int, int, int)
	 */
	@Override
	public void onMidiMiscellaneousFunctionCodes(int cable, int byte1, int byte2, int byte3) {
		sendMidiMessage(0x0, cable, byte1, byte2, byte3);
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiCableEvents(int, int, int, int)
	 */
	@Override
	public void onMidiCableEvents(int cable, int byte1, int byte2, int byte3) {
		sendMidiMessage(0x1, cable, byte1, byte2, byte3);
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiSystemCommonMessage(int, byte[])
	 */
	@Override
	public void onMidiSystemCommonMessage(int cable, byte bytes[]) {
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

	private static final int PARAM_BUFFER_SIZE = 64;
	private static final int PARAM_BUFFER_SIZE_FOR_RAW_SYSEX = PARAM_BUFFER_SIZE * 3 / 4;

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiSystemExclusive(int, byte[])
	 */
	@Override
	public void onMidiSystemExclusive(int cable, byte[] systemExclusive) {

		for (int sysexStartPosition = 0; sysexStartPosition < systemExclusive.length; sysexStartPosition += PARAM_BUFFER_SIZE_FOR_RAW_SYSEX) {
			int sysexTransferLength = 0;
			if (sysexStartPosition + PARAM_BUFFER_SIZE_FOR_RAW_SYSEX > systemExclusive.length) {
				sysexTransferLength = systemExclusive.length - sysexStartPosition;
			} else {
				sysexTransferLength = PARAM_BUFFER_SIZE_FOR_RAW_SYSEX;
			}

			byte[] buffer = new byte[PARAM_BUFFER_SIZE];
			int bufferIndex = 0;

			for (int sysexIndex = sysexStartPosition; sysexIndex < sysexStartPosition + sysexTransferLength; sysexIndex += 3, bufferIndex += 4) {
				if (sysexIndex + 3 < sysexStartPosition + sysexTransferLength) {
					// sysex continues...
					buffer[bufferIndex + 0] = (byte) (((cable & 0xf) << 4) | (0x4 & 0xf));
					buffer[bufferIndex + 1] = systemExclusive[sysexIndex + 0];
					buffer[bufferIndex + 2] = systemExclusive[sysexIndex + 1];
					buffer[bufferIndex + 3] = systemExclusive[sysexIndex + 2];
				} else {
					switch (sysexStartPosition + sysexTransferLength - sysexIndex) {
					case 1:
						// sysex end with 1 byte
						buffer[bufferIndex + 0] = (byte) (((cable & 0xf) << 4) | (0x5 & 0xf));
						buffer[bufferIndex + 1] = systemExclusive[sysexIndex + 0];
						buffer[bufferIndex + 2] = 0;
						buffer[bufferIndex + 3] = 0;
						break;
					case 2:
						// sysex end with 2 bytes
						buffer[bufferIndex + 0] = (byte) (((cable & 0xf) << 4) | (0x6 & 0xf));
						buffer[bufferIndex + 1] = systemExclusive[sysexIndex + 0];
						buffer[bufferIndex + 2] = systemExclusive[sysexIndex + 1];
						buffer[bufferIndex + 3] = 0;
						break;
					case 3:
						// sysex end with 3 bytes
						buffer[bufferIndex + 0] = (byte) (((cable & 0xf) << 4) | (0x7 & 0xf));
						buffer[bufferIndex + 1] = systemExclusive[sysexIndex + 0];
						buffer[bufferIndex + 2] = systemExclusive[sysexIndex + 1];
						buffer[bufferIndex + 3] = systemExclusive[sysexIndex + 2];
						break;
					default:
						// do nothing.
						break;
					}
				}
			}

			deviceConnection.bulkTransfer(outputEndpoint, buffer, buffer.length, 0);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiNoteOff(int, int, int, int)
	 */
	@Override
	public void onMidiNoteOff(int cable, int channel, int note, int velocity) {
		sendMidiMessage(0x8, cable, 0x80 | (channel & 0xf), note, velocity);
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiNoteOn(int, int, int, int)
	 */
	@Override
	public void onMidiNoteOn(int cable, int channel, int note, int velocity) {
		sendMidiMessage(0x9, cable, 0x90 | (channel & 0xf), note, velocity);
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiPolyphonicAftertouch(int, int, int, int)
	 */
	@Override
	public void onMidiPolyphonicAftertouch(int cable, int channel, int note, int pressure) {
		sendMidiMessage(0xa, cable, 0xa0 | (channel & 0xf), note, pressure);
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiControlChange(int, int, int, int)
	 */
	@Override
	public void onMidiControlChange(int cable, int channel, int function, int value) {
		sendMidiMessage(0xb, cable, 0xb0 | (channel & 0xf), function, value);
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiProgramChange(int, int, int)
	 */
	@Override
	public void onMidiProgramChange(int cable, int channel, int program) {
		sendMidiMessage(0xc, cable, 0xc0 | (channel & 0xf), program, 0);
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiChannelAftertouch(int, int, int)
	 */
	@Override
	public void onMidiChannelAftertouch(int cable, int channel, int pressure) {
		sendMidiMessage(0xd, cable, 0xd0 | (channel & 0xf), pressure, 0);
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiPitchWheel(int, int, int)
	 */
	@Override
	public void onMidiPitchWheel(int cable, int channel, int amount) {
		sendMidiMessage(0xe, cable, 0xe0 | (channel & 0xf), amount & 0xff, (amount >> 8) & 0xff);
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiEventListener#onMidiSingleByte(int, int)
	 */
	@Override
	public void onMidiSingleByte(int cable, int byte1) {
		sendMidiMessage(0xf, cable, byte1, 0, 0);
	}
}
