package jp.kshoji.javax.sound.midi.usb;


import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.MidiDevice;
import jp.kshoji.javax.sound.midi.MidiDeviceTransmitter;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.ShortMessage;
import jp.kshoji.javax.sound.midi.SysexMessage;

/**
 * {@link jp.kshoji.javax.sound.midi.Transmitter} implementation
 * 
 * @author K.Shoji
 */
public final class UsbMidiTransmitter implements MidiDeviceTransmitter {
    private final UsbMidiDevice usbMidiDevice;
	private final UsbDevice usbDevice;
	private final UsbDeviceConnection usbDeviceConnection;
	private final UsbInterface usbInterface;
	private final UsbEndpoint inputEndpoint;
	
	private MidiInputDevice inputDevice;
	Receiver receiver;

	public UsbMidiTransmitter(UsbMidiDevice usbMidiDevice, UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection, UsbInterface usbInterface, UsbEndpoint inputEndpoint) {
        this.usbMidiDevice = usbMidiDevice;
		this.usbDevice = usbDevice;
		this.usbDeviceConnection = usbDeviceConnection;
		this.usbInterface = usbInterface;
		this.inputEndpoint = inputEndpoint;
        open();
	}

	@Override
	public void setReceiver(Receiver receiver) {
		this.receiver = receiver;
	}

	@Override
	public Receiver getReceiver() {
		return receiver;
	}
	
	public void open() {
        if (inputDevice == null) {
            inputDevice = new MidiInputDevice(usbDevice, usbDeviceConnection, usbInterface, inputEndpoint, new OnMidiInputEventListenerImpl());
        }
	}

	@Override
	public void close() {
		if (inputDevice != null) {
			inputDevice.stop();
		}
        inputDevice = null;
	}

    @Override
    public MidiDevice getMidiDevice() {
        return usbMidiDevice;
    }

    class OnMidiInputEventListenerImpl implements OnMidiInputEventListener{
		@Override
		public void onMidiMiscellaneousFunctionCodes(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
			if (receiver != null) {
				try {
					final SysexMessage message = new SysexMessage();
					message.setMessage(new byte[] {(byte) (byte1 & 0xff), (byte) (byte2 & 0xff), (byte) (byte3 & 0xff)}, 3);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiCableEvents(MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
			if (receiver != null) {
				try {
					final SysexMessage message = new SysexMessage();
					message.setMessage(new byte[] {(byte) (byte1 & 0xff), (byte) (byte2 & 0xff), (byte) (byte3 & 0xff)}, 3);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiSystemCommonMessage(MidiInputDevice sender, int cable, byte[] bytes) {
			if (receiver != null) {
				try {
					final SysexMessage message = new SysexMessage();
					message.setMessage(bytes, bytes.length);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiSystemExclusive(MidiInputDevice sender, int cable, byte[] systemExclusive) {
			if (receiver != null) {
				try {
					final SysexMessage message = new SysexMessage();
					message.setMessage(systemExclusive, systemExclusive.length);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
}
			}
		}

		@Override
		public void onMidiNoteOff(MidiInputDevice sender, int cable, int channel, int note, int velocity) {
			if (receiver != null) {
				try {
					final ShortMessage message = new ShortMessage();
					message.setMessage(ShortMessage.NOTE_OFF, channel, note, velocity);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiNoteOn(MidiInputDevice sender, int cable, int channel, int note, int velocity) {
			if (receiver != null) {
				try {
					final ShortMessage message = new ShortMessage();
					message.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiPolyphonicAftertouch(MidiInputDevice sender, int cable, int channel, int note, int pressure) {
			if (receiver != null) {
				try {
					final ShortMessage message = new ShortMessage();
					message.setMessage(ShortMessage.POLY_PRESSURE, channel, note, pressure);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiControlChange(MidiInputDevice sender, int cable, int channel, int function, int value) {
			if (receiver != null) {
				try {
					final ShortMessage message = new ShortMessage();
					message.setMessage(ShortMessage.CONTROL_CHANGE, channel, function, value);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiProgramChange(MidiInputDevice sender, int cable, int channel, int program) {
			if (receiver != null) {
				try {
					final ShortMessage message = new ShortMessage();
					message.setMessage(ShortMessage.PROGRAM_CHANGE, channel, program, 0);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiChannelAftertouch(MidiInputDevice sender, int cable, int channel, int pressure) {
			if (receiver != null) {
				try {
					final ShortMessage message = new ShortMessage();
					message.setMessage(ShortMessage.CHANNEL_PRESSURE, channel, pressure, 0);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiPitchWheel(MidiInputDevice sender, int cable, int channel, int amount) {
			if (receiver != null) {
				try {
					final ShortMessage message = new ShortMessage();
					message.setMessage(ShortMessage.PITCH_BEND, channel, (amount >> 7) & 0x7f, amount & 0x7f);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiSingleByte(MidiInputDevice sender, int cable, int byte1) {
			if (receiver != null) {
				try {
					final SysexMessage message = new SysexMessage();
					message.setMessage(new byte[] {(byte) (byte1 & 0xff)}, 1);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}
		
		@Override
		public void onMidiRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int value, int valueLSB) {
			// do nothing in this implementation
		}
		
		@Override
		public void onMidiNRPNReceived(MidiInputDevice sender, int cable, int channel, int function, int value, int valueLSB) {
			// do nothing in this implementation
		}
	}
}
