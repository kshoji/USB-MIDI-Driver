package jp.kshoji.javax.sound.midi.usb;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
    private MidiInputDevice inputDevice;

	Receiver receiver;

    private final OnMidiInputEventListenerImpl onMidiInputEventListener = new OnMidiInputEventListenerImpl();

    /**
     * Constructor
     * 
     * @param usbMidiDevice the UsbMidiDevice
     */
	public UsbMidiTransmitter(@NonNull UsbMidiDevice usbMidiDevice, @NonNull MidiInputDevice midiInputDevice) {
        this.usbMidiDevice = usbMidiDevice;
        this.inputDevice = midiInputDevice;
        open();
	}

	@Override
	public void setReceiver(@Nullable Receiver receiver) {
		this.receiver = receiver;
	}

    @Nullable
	@Override
	public Receiver getReceiver() {
		return receiver;
	}
	
	public void open() {
        if (inputDevice != null) {
            inputDevice.setMidiEventListener(onMidiInputEventListener);
        }
	}

	@Override
	public void close() {
        if (inputDevice != null) {
            inputDevice.setMidiEventListener(null);
            inputDevice = null;
        }
	}

    @NonNull
    @Override
    public MidiDevice getMidiDevice() {
        return usbMidiDevice;
    }

    class OnMidiInputEventListenerImpl implements OnMidiInputEventListener {
		@Override
		public void onMidiMiscellaneousFunctionCodes(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
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
		public void onMidiCableEvents(@NonNull MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
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
		public void onMidiSystemCommonMessage(@NonNull MidiInputDevice sender, int cable, byte[] bytes) {
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
		public void onMidiSystemExclusive(@NonNull MidiInputDevice sender, int cable, byte[] systemExclusive) {
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
		public void onMidiNoteOff(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {
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
		public void onMidiNoteOn(@NonNull MidiInputDevice sender, int cable, int channel, int note, int velocity) {
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
		public void onMidiPolyphonicAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int note, int pressure) {
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
		public void onMidiControlChange(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
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
		public void onMidiProgramChange(@NonNull MidiInputDevice sender, int cable, int channel, int program) {
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
		public void onMidiChannelAftertouch(@NonNull MidiInputDevice sender, int cable, int channel, int pressure) {
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
		public void onMidiPitchWheel(@NonNull MidiInputDevice sender, int cable, int channel, int amount) {
			if (receiver != null) {
				try {
					final ShortMessage message = new ShortMessage();
					message.setMessage(ShortMessage.PITCH_BEND, channel, amount & 0x7f, (amount >> 7) & 0x7f);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiSingleByte(@NonNull MidiInputDevice sender, int cable, int byte1) {
 			if (receiver != null) {
                try {
                    final ShortMessage message = new ShortMessage();
                    message.setMessage(new byte[] {(byte) (byte1 & 0xff)}, 1);
                    receiver.send(message, -1);
                } catch (final InvalidMidiDataException e) {
                    Log.d(Constants.TAG, "InvalidMidiDataException", e);
                }
            }
		}

		@Override
		public void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice sender, int cable, int timing) {
			// already processed on onMidiSystemCommonMessage, ignore this event
		}

		@Override
		public void onMidiSongSelect(@NonNull MidiInputDevice sender, int cable, int song) {
			// already processed on onMidiSystemCommonMessage, ignore this event
		}

		@Override
		public void onMidiSongPositionPointer(@NonNull MidiInputDevice sender, int cable, int position) {
			// already processed on onMidiSystemCommonMessage, ignore this event
		}

		@Override
		public void onMidiTuneRequest(@NonNull MidiInputDevice sender, int cable) {
			// already processed on onMidiSingleByte, ignore this event
		}

		@Override
		public void onMidiTimingClock(@NonNull MidiInputDevice sender, int cable) {
			// already processed on onMidiSingleByte, ignore this event
		}

		@Override
		public void onMidiStart(@NonNull MidiInputDevice sender, int cable) {
			// already processed on onMidiSingleByte, ignore this event
		}

		@Override
		public void onMidiContinue(@NonNull MidiInputDevice sender, int cable) {
			// already processed on onMidiSingleByte, ignore this event
		}

		@Override
		public void onMidiStop(@NonNull MidiInputDevice sender, int cable) {
			// already processed on onMidiSingleByte, ignore this event
		}

		@Override
		public void onMidiActiveSensing(@NonNull MidiInputDevice sender, int cable) {
			// already processed on onMidiSingleByte, ignore this event
		}

		@Override
		public void onMidiReset(@NonNull MidiInputDevice sender, int cable) {
			// already processed on onMidiSingleByte, ignore this event
		}

		@Override
		public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
			// do nothing in this implementation
		}

		@Override
		public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int valueMSB, int valueLSB) {
			// do nothing in this implementation
		}

		@Override
		public void onMidiRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
			// do nothing in this implementation
		}
		
		@Override
		public void onMidiNRPNReceived(@NonNull MidiInputDevice sender, int cable, int channel, int function, int value) {
			// do nothing in this implementation
		}
	}
}
