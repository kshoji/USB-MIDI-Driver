package jp.kshoji.driver.midi.sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import jp.kshoji.driver.midi.activity.AbstractMultipleMidiActivity;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.sample.util.SoundMaker;
import jp.kshoji.driver.midi.sample.util.Tone;
import android.graphics.PorterDuff.Mode;
import android.hardware.usb.UsbDevice;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

/**
 * Sample Activity for MIDI Driver library
 * 
 * @author K.Shoji
 */
public class MIDIDriverMultipleSampleActivity extends AbstractMultipleMidiActivity {
	// User interface
	private ArrayAdapter<String> midiInputEventAdapter;
	ArrayAdapter<String> midiOutputEventAdapter;
	private ToggleButton thruToggleButton;
	Spinner cableIdSpinner;
	Spinner deviceSpinner;

	ArrayAdapter<UsbDevice> connectedDevicesAdapter;

	// Play sounds
	AudioTrack audioTrack;
	Timer timer;
	TimerTask timerTask;
	SoundMaker soundMaker;
	final Set<Tone> tones = new HashSet<Tone>();
	int currentProgram = 0;

	/**
	 * Choose device from spinner
	 * 
	 * @return
	 */
	MidiOutputDevice getMidiOutputDeviceFromSpinner() {
		if (deviceSpinner != null && deviceSpinner.getSelectedItemPosition() >= 0 && connectedDevicesAdapter != null && !connectedDevicesAdapter.isEmpty()) {
			UsbDevice device = connectedDevicesAdapter.getItem(deviceSpinner.getSelectedItemPosition());
			if (device != null) {
				Set<MidiOutputDevice> midiOutputDevices = getMidiOutputDevices(device);
				
				if (midiOutputDevices.size() > 0) {
					// returns the first one.
					return (MidiOutputDevice) midiOutputDevices.toArray()[0];
				}
			}
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.activity.AbstractMultipleMidiActivity#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		ListView midiInputEventListView = (ListView) findViewById(R.id.midiInputEventListView);
		midiInputEventAdapter = new ArrayAdapter<String>(this, R.layout.midi_event, R.id.midiEventDescriptionTextView);
		midiInputEventListView.setAdapter(midiInputEventAdapter);

		ListView midiOutputEventListView = (ListView) findViewById(R.id.midiOutputEventListView);
		midiOutputEventAdapter = new ArrayAdapter<String>(this, R.layout.midi_event, R.id.midiEventDescriptionTextView);
		midiOutputEventListView.setAdapter(midiOutputEventAdapter);

		thruToggleButton = (ToggleButton) findViewById(R.id.toggleButtonThru);
		cableIdSpinner = (Spinner) findViewById(R.id.cableIdSpinner);
		deviceSpinner = (Spinner) findViewById(R.id.deviceNameSpinner);
		
		connectedDevicesAdapter = new ArrayAdapter<UsbDevice>(getApplicationContext(), android.R.layout.simple_spinner_dropdown_item, android.R.id.text1, new ArrayList<UsbDevice>());
		deviceSpinner.setAdapter(connectedDevicesAdapter);

		OnTouchListener onToneButtonTouchListener = new OnTouchListener() {

			/*
			 * (non-Javadoc)
			 * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
			 */
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				MidiOutputDevice midiOutputDevice = getMidiOutputDeviceFromSpinner();
				if (midiOutputDevice == null) {
					return false;
				}

				int note = 60 + Integer.parseInt((String) v.getTag());
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
						midiOutputDevice.sendMidiNoteOn(cableIdSpinner.getSelectedItemPosition(), 0, note, 127);
					if (midiOutputEventAdapter != null) {
						midiOutputEventAdapter.add("NoteOn to: " + midiOutputDevice.getUsbDevice().getDeviceName() + ", cableId: " + cableIdSpinner.getSelectedItemPosition() + ", note: " + note + ", velocity: 127");
					}
					break;
				case MotionEvent.ACTION_UP:
						midiOutputDevice.sendMidiNoteOff(cableIdSpinner.getSelectedItemPosition(), 0, note, 127);
					if (midiOutputEventAdapter != null) {
							midiOutputEventAdapter.add("NoteOff to: " + midiOutputDevice.getUsbDevice().getDeviceName() + ", cableId: " + cableIdSpinner.getSelectedItemPosition() + ", note: " + note + ", velocity: 127");
					}
					break;
				default:
					// do nothing.
					break;
				}
				return false;
			}
		};
		((Button) findViewById(R.id.buttonC)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonCis)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonD)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonDis)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonE)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonF)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonFis)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonG)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonGis)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonA)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonAis)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonB)).setOnTouchListener(onToneButtonTouchListener);
		((Button) findViewById(R.id.buttonC2)).setOnTouchListener(onToneButtonTouchListener);

		int whiteKeyColor = 0xFFFFFFFF;
		int blackKeyColor = 0xFF808080;
		((Button) findViewById(R.id.buttonC)).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonCis)).getBackground().setColorFilter(blackKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonD)).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonDis)).getBackground().setColorFilter(blackKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonE)).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonF)).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonFis)).getBackground().setColorFilter(blackKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonG)).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonGis)).getBackground().setColorFilter(blackKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonA)).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonAis)).getBackground().setColorFilter(blackKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonB)).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		((Button) findViewById(R.id.buttonC2)).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);

		soundMaker = SoundMaker.getInstance();
		final int bufferSize = AudioTrack.getMinBufferSize(soundMaker.getSamplingRate(), AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		int timerRate = bufferSize * 1000 / soundMaker.getSamplingRate() / 2;
		final short[] wav = new short[bufferSize / 2];

		audioTrack = prepareAudioTrack(soundMaker.getSamplingRate());
		timer = new Timer();
		timerTask = new TimerTask() {
			/*
			 * (non-Javadoc)
			 * @see java.util.TimerTask#run()
			 */
			@Override
			public void run() {
				if (soundMaker != null) {
					synchronized (tones) {
						for (int i = 0; i < wav.length; i++) {
							wav[i] = (short) (soundMaker.makeWaveStream(tones) * 1024);
						}
					}
					try {
						if (audioTrack != null) {
							audioTrack.write(wav, 0, wav.length);
						}
					} catch (NullPointerException e) {
						// do nothing
					}
				}
			}
		};
		timer.scheduleAtFixedRate(timerTask, 10, timerRate);

	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.activity.AbstractMultipleMidiActivity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (timer != null) {
			try {
				timer.cancel();
				timer.purge();
			} catch (Throwable t) {
				// do nothing
			} finally {
				timer = null;
			}
		}
		if (audioTrack != null) {
			try {
				audioTrack.stop();
				audioTrack.flush();
				audioTrack.release();
			} catch (Throwable t) {
				// do nothing
			} finally {
				audioTrack = null;
			}
		}
	}

	/**
	 * @param samplingRate
	 * @return
	 */
	private AudioTrack prepareAudioTrack(int samplingRate) {
		AudioTrack result = new AudioTrack(AudioManager.STREAM_MUSIC, samplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioTrack.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
		result.setStereoVolume(1f, 1f);
		result.play();
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener#onDeviceAttached(android.hardware.usb.UsbDevice)
	 */
	@Override
	public void onDeviceAttached(final UsbDevice usbDevice) {
		if (connectedDevicesAdapter != null) {
			connectedDevicesAdapter.remove(usbDevice);
			connectedDevicesAdapter.add(usbDevice);
			connectedDevicesAdapter.notifyDataSetChanged();
		}
		Toast.makeText(this, "USB MIDI Device " + usbDevice.getDeviceName() + " has been attached.", Toast.LENGTH_LONG).show();
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
	 */
	@Override
	public void onDeviceDetached(final UsbDevice usbDevice) {
		if (connectedDevicesAdapter != null) {
			connectedDevicesAdapter.remove(usbDevice);
			connectedDevicesAdapter.notifyDataSetChanged();
		}
		Toast.makeText(this, "USB MIDI Device " + usbDevice.getDeviceName() + " has been detached.", Toast.LENGTH_LONG).show();
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiNoteOff(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int)
	 */
	@Override
	public void onMidiNoteOff(final MidiInputDevice sender, int cable, int channel, int note, int velocity) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("NoteOff from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", note: " + note + ", velocity: " + velocity);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiNoteOff(cable, channel, note, velocity);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("NoteOff from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", note: " + note + ", velocity: " + velocity);
			}
		}

		synchronized (tones) {
			Iterator<Tone> it = tones.iterator();
			while (it.hasNext()) {
				Tone tone = it.next();
				if (tone.getNote() == note) {
					it.remove();
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiNoteOn(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int)
	 */
	@Override
	public void onMidiNoteOn(final MidiInputDevice sender, int cable, int channel, int note, int velocity) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("NoteOn from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ",  channel: " + channel + ", note: " + note + ", velocity: " + velocity);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiNoteOn(cable, channel, note, velocity);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("NoteOn from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ",  channel: " + channel + ", note: " + note + ", velocity: " + velocity);
			}
		}

		synchronized (tones) {
			if (velocity == 0) {
				Iterator<Tone> it = tones.iterator();
				while (it.hasNext()) {
					Tone tone = it.next();
					if (tone.getNote() == note) {
						it.remove();
					}
				}
			} else {
				tones.add(new Tone(note, velocity / 127.0, currentProgram));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiPolyphonicAftertouch(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int)
	 */
	@Override
	public void onMidiPolyphonicAftertouch(final MidiInputDevice sender, int cable, int channel, int note, int pressure) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("PolyphonicAftertouch from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", note: " + note + ", pressure: " + pressure);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiPolyphonicAftertouch(cable, channel, note, pressure);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("PolyphonicAftertouch from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", note: " + note + ", pressure: " + pressure);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiControlChange(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int, int)
	 */
	@Override
	public void onMidiControlChange(final MidiInputDevice sender, int cable, int channel, int function, int value) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("ControlChange from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", function: " + function + ", value: " + value);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiControlChange(cable, channel, function, value);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("ControlChange from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", function: " + function + ", value: " + value);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiProgramChange(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int)
	 */
	@Override
	public void onMidiProgramChange(final MidiInputDevice sender, int cable, int channel, int program) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("ProgramChange from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", program: " + program);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiProgramChange(cable, channel, program);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("ProgramChange from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", program: " + program);
			}
		}

		currentProgram = program % Tone.FORM_MAX;
		synchronized (tones) {
			for (Tone tone : tones) {
				tone.setForm(currentProgram);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiChannelAftertouch(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int)
	 */
	@Override
	public void onMidiChannelAftertouch(final MidiInputDevice sender, int cable, int channel, int pressure) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("ChannelAftertouch from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", pressure: " + pressure);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiChannelAftertouch(cable, channel, pressure);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("ChannelAftertouch from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", pressure: " + pressure);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiPitchWheel(jp.kshoji.driver.midi.device.MidiInputDevice, int, int, int)
	 */
	@Override
	public void onMidiPitchWheel(final MidiInputDevice sender, int cable, int channel, int amount) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("PitchWheel from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", amount: " + amount);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiPitchWheel(cable, channel, amount);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("PitchWheel from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", channel: " + channel + ", amount: " + amount);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiSystemExclusive(jp.kshoji.driver.midi.device.MidiInputDevice, int, byte[])
	 */
	@Override
	public void onMidiSystemExclusive(final MidiInputDevice sender, int cable, final byte[] systemExclusive) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("SystemExclusive from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", data:" + Arrays.toString(systemExclusive));
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiSystemExclusive(cable, systemExclusive);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("SystemExclusive from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", data:" + Arrays.toString(systemExclusive));
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiSystemCommonMessage(jp.kshoji.driver.midi.device.MidiInputDevice, int, byte[])
	 */
	@Override
	public void onMidiSystemCommonMessage(final MidiInputDevice sender, int cable, final byte[] bytes) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("SystemCommonMessage from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", bytes: " + Arrays.toString(bytes));
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiSystemCommonMessage(cable, bytes);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("SystemCommonMessage from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", bytes: " + Arrays.toString(bytes));
			}
		}
	}

	/*
	 * @Override(non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiSingleByte(jp.kshoji.driver.midi.device.MidiInputDevice, int, int)
	 */
	@Override
	public void onMidiSingleByte(final MidiInputDevice sender, int cable, int byte1) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("SingleByte from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", data: " + byte1);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiSingleByte(cable, byte1);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("SingleByte from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", data: " + byte1);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiMiscellaneousFunctionCodes(android.hardware.usb.UsbDevice, int, int, int, int)
	 */
	@Override
	public void onMidiMiscellaneousFunctionCodes(final MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("MiscellaneousFunctionCodes from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiMiscellaneousFunctionCodes(cable, byte1, byte2, byte3);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("MiscellaneousFunctionCodes from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.driver.midi.listener.OnMidiInputEventListener#onMidiCableEvents(android.hardware.usb.UsbDevice, int, int, int, int)
	 */
	@Override
	public void onMidiCableEvents(final MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("CableEvents from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDeviceFromSpinner() != null) {
			getMidiOutputDeviceFromSpinner().sendMidiCableEvents(cable, byte1, byte2, byte3);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("CableEvents from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3);
			}
		}
	}
}
