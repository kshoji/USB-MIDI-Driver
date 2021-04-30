package jp.kshoji.javax.sound.midi.usb;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import jp.kshoji.driver.midi.device.Midi2InputDevice;
import jp.kshoji.driver.midi.listener.OnMidi2InputEventListener;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.ValueScaleUtils;
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
public final class UsbMidi2Transmitter implements MidiDeviceTransmitter {
	private final UsbMidi2Device usbMidiDevice;
	private final OnMidi2InputEventListenerImpl onMidiInputEventListener = new OnMidi2InputEventListenerImpl();
	Receiver receiver;
	private Midi2InputDevice inputDevice;

	/**
	 * Constructor
	 *
	 * @param usbMidiDevice the UsbMidiDevice
	 */
	public UsbMidi2Transmitter(@NonNull UsbMidi2Device usbMidiDevice, @NonNull Midi2InputDevice midiInputDevice) {
		this.usbMidiDevice = usbMidiDevice;
		this.inputDevice = midiInputDevice;
		open();
	}

	@Nullable
	@Override
	public Receiver getReceiver() {
		return receiver;
	}

	@Override
	public void setReceiver(@Nullable Receiver receiver) {
		this.receiver = receiver;
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

	class OnMidi2InputEventListenerImpl implements OnMidi2InputEventListener {
		@Override
		public void onMidi1SystemExclusive(@NonNull Midi2InputDevice sender, int group, @NonNull byte[] systemExclusive) {
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
		public void onMidi2NoteOff(Midi2InputDevice sender, int group, int channel, int note, int velocity, int attributeType, int attributeData) {
			onMidi1NoteOff(sender, group, channel, note, (int) ValueScaleUtils.scaleDown(velocity, 16, 7));
		}

		@Override
		public void onMidi2NoteOn(Midi2InputDevice sender, int group, int channel, int note, int velocity, int attributeType, int attributeData) {
			onMidi1NoteOn(sender, group, channel, note, (int) ValueScaleUtils.scaleDown(velocity, 16, 7));
		}

		@Override
		public void onMidi2PolyphonicAftertouch(Midi2InputDevice sender, int group, int channel, int note, long pressure) {
			onMidi1PolyphonicAftertouch(sender, group, channel, note, (int) ValueScaleUtils.scaleDown(pressure, 32, 7));
		}

		@Override
		public void onMidi2ControlChange(Midi2InputDevice sender, int group, int channel, int index, long value) {
			onMidi1ControlChange(sender, group, index, channel, (int) ValueScaleUtils.scaleDown(value, 32, 7));
		}

		@Override
		public void onMidi2ProgramChange(Midi2InputDevice sender, int group, int channel, int optionFlags, int program, int bank) {
			onMidi1ProgramChange(sender, group, channel, program);
		}

		@Override
		public void onMidi2ChannelAftertouch(Midi2InputDevice sender, int group, int channel, long pressure) {
			onMidi1ChannelAftertouch(sender, group, channel, (int) ValueScaleUtils.scaleDown(pressure, 32, 7));
		}

		@Override
		public void onMidi2PitchWheel(Midi2InputDevice sender, int group, int channel, long amount) {
			onMidi1PitchWheel(sender, group, channel, (int) ValueScaleUtils.scaleDown(amount, 32, 14));
		}

		@Override
		public void onMidiPerNotePitchWheel(Midi2InputDevice sender, int group, int channel, int note, long amount) {
			// Does not translate to the MIDI 1.0 Protocol
		}

		@Override
		public void onMidiPerNoteManagement(Midi2InputDevice sender, int group, int channel, int note, int optionFlags) {
			// Does not translate to the MIDI 1.0 Protocol
		}

		@Override
		public void onMidiRegisteredPerNoteController(Midi2InputDevice sender, int group, int channel, int note, int index, long data) {
			// Does not translate to the MIDI 1.0 Protocol
		}

		@Override
		public void onMidiAssignablePerNoteController(Midi2InputDevice sender, int group, int channel, int note, int index, long data) {
			// Does not translate to the MIDI 1.0 Protocol
		}

		@Override
		public void onMidiRegisteredController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {
			// send the RPN function
			onMidi1ControlChange(sender, group, channel, 101, bank & 0x7f);
			onMidi1ControlChange(sender, group, channel, 100, index & 0x7f);

			// send the value
			long value = ValueScaleUtils.scaleDown(data, 32, 14);
			onMidi1ControlChange(sender, group, channel, 6, (int) ((value >> 7) & 0x7f));
			onMidi1ControlChange(sender, group, channel, 38, (int) (value & 0x7f));
		}

		@Override
		public void onMidiAssignableController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {
			// send the NRPN function
			onMidi1ControlChange(sender, group, channel, 99, bank & 0x7f);
			onMidi1ControlChange(sender, group, channel, 98, index & 0x7f);

			// send the value
			long value = ValueScaleUtils.scaleDown(data, 32, 14);
			onMidi1ControlChange(sender, group, channel, 6, (int) ((value >> 7) & 0x7f));
			onMidi1ControlChange(sender, group, channel, 38, (int) (value & 0x7f));
		}

		@Override
		public void onMidiRelativeRegisteredController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {
			// Does not translate to the MIDI 1.0 Protocol
		}

		@Override
		public void onMidiRelativeAssignableController(Midi2InputDevice sender, int group, int channel, int bank, int index, long data) {
			// Does not translate to the MIDI 1.0 Protocol
		}

		@Override
		public void onMidi2SystemExclusive(Midi2InputDevice sender, int group, int streamId, @NonNull byte[] systemExclusive) {
			// Does not translate to MIDI 1.0 Protocol, but may be used by a UMP MIDI 1.0 Device
		}

		@Override
		public void onMidiMixedDataSetHeader(Midi2InputDevice sender, int group, int mdsId, @NonNull byte[] headers) {
			// Does not translate to MIDI 1.0 Protocol, but may be used by a UMP MIDI 1.0 Device
		}

		@Override
		public void onMidiMixedDataSetPayload(Midi2InputDevice sender, int group, int mdsId, @NonNull byte[] payloads) {
			// Does not translate to MIDI 1.0 Protocol, but may be used by a UMP MIDI 1.0 Device
		}

		@Override
		public void onMidi1NoteOff(@NonNull Midi2InputDevice sender, int group, int channel, int note, int velocity) {
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
		public void onMidi1NoteOn(@NonNull Midi2InputDevice sender, int group, int channel, int note, int velocity) {
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
		public void onMidi1PolyphonicAftertouch(@NonNull Midi2InputDevice sender, int group, int channel, int note, int pressure) {
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
		public void onMidi1ControlChange(@NonNull Midi2InputDevice sender, int group, int channel, int function, int value) {
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
		public void onMidi1ProgramChange(@NonNull Midi2InputDevice sender, int group, int channel, int program) {
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
		public void onMidi1ChannelAftertouch(@NonNull Midi2InputDevice sender, int group, int channel, int pressure) {
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
		public void onMidi1PitchWheel(@NonNull Midi2InputDevice sender, int group, int channel, int amount) {
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
		public void onMidiNoop(Midi2InputDevice sender, int group) {
			// Does not translate to MIDI 1.0 Protocol, but may be used by a UMP MIDI 1.0 Device
		}

		@Override
		public void onMidiJitterReductionClock(Midi2InputDevice sender, int group, int senderClockTime) {
			// Does not translate to MIDI 1.0 Protocol, but may be used by a UMP MIDI 1.0 Device
		}

		@Override
		public void onMidiJitterReductionTimestamp(Midi2InputDevice sender, int group, int senderClockTimestamp) {
			// Does not translate to MIDI 1.0 Protocol, but may be used by a UMP MIDI 1.0 Device
		}

		private void onMidiSystemCommonMessage(int byte1, int byte2) {
			if (receiver != null) {
				try {
					final ShortMessage message = new ShortMessage();
					message.setMessage(new byte[]{(byte) byte1, (byte) byte2}, 2);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiTimeCodeQuarterFrame(@NonNull Midi2InputDevice sender, int group, int timing) {
			onMidiSystemCommonMessage(0xf1, timing & 0x7f);
		}

		@Override
		public void onMidiSongSelect(@NonNull Midi2InputDevice sender, int group, int song) {
			onMidiSystemCommonMessage(0xf3, song & 0x7f);
		}

		@Override
		public void onMidiSongPositionPointer(@NonNull Midi2InputDevice sender, int group, int position) {
			onMidiSystemCommonMessage(0xf2, position & 0x7f);
		}

		private void onMidiSingleByte(int byte1) {
			if (receiver != null) {
				try {
					final ShortMessage message = new ShortMessage();
					message.setMessage(new byte[]{(byte) byte1}, 1);
					receiver.send(message, -1);
				} catch (final InvalidMidiDataException e) {
					Log.d(Constants.TAG, "InvalidMidiDataException", e);
				}
			}
		}

		@Override
		public void onMidiTuneRequest(@NonNull Midi2InputDevice sender, int group) {
			onMidiSingleByte(0xf6);
		}

		@Override
		public void onMidiTimingClock(@NonNull Midi2InputDevice sender, int group) {
			onMidiSingleByte(0xf8);
		}

		@Override
		public void onMidiStart(@NonNull Midi2InputDevice sender, int group) {
			onMidiSingleByte(0xfa);
		}

		@Override
		public void onMidiContinue(@NonNull Midi2InputDevice sender, int group) {
			onMidiSingleByte(0xfb);
		}

		@Override
		public void onMidiStop(@NonNull Midi2InputDevice sender, int group) {
			onMidiSingleByte(0xfc);
		}

		@Override
		public void onMidiActiveSensing(@NonNull Midi2InputDevice sender, int group) {
			onMidiSingleByte(0xfe);
		}

		@Override
		public void onMidiReset(@NonNull Midi2InputDevice sender, int group) {
			onMidiSingleByte(0xff);
		}
	}
}
