package jp.kshoji.driver.midi.sample;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import jp.kshoji.driver.midi.activity.AbstractMidiActivity;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.sample.util.SoundMaker;
import jp.kshoji.driver.midi.sample.util.Tone;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.PorterDuff.Mode;
import android.hardware.usb.UsbManager;
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
public class MIDIDriverSampleActivity extends AbstractMidiActivity {
	// User interface
	private ArrayAdapter<String> midiInputEventAdapter;
	ArrayAdapter<String> midiOutputEventAdapter;
	private ToggleButton thruToggleButton;
	private Spinner cableIdSpinner;

	// Play sounds
	AudioTrack audioTrack;
	Timer timer;
	TimerTask timerTask;
	SoundMaker soundMaker;
	final Set<Tone> tones = new HashSet<Tone>();
	int currentProgram = 0;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		Intent intent = getIntent();
		String action = intent.getAction();
		if (!UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("USB MIDI device required.");
			builder.setMessage("Please start with connecting USB MIDI device. This screen will be closed.");
			builder.setPositiveButton("OK", new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			builder.create().show();
		}

		ListView midiInputEventListView = (ListView) findViewById(R.id.midiInputEventListView);
		midiInputEventAdapter = new ArrayAdapter<String>(this, R.layout.midi_event, R.id.midiEventDescriptionTextView);
		midiInputEventListView.setAdapter(midiInputEventAdapter);

		ListView midiOutputEventListView = (ListView) findViewById(R.id.midiOutputEventListView);
		midiOutputEventAdapter = new ArrayAdapter<String>(this, R.layout.midi_event, R.id.midiEventDescriptionTextView);
		midiOutputEventListView.setAdapter(midiOutputEventAdapter);

		thruToggleButton = (ToggleButton) findViewById(R.id.toggleButtonThru);
		cableIdSpinner = (Spinner) findViewById(R.id.cableIdSpinner);

		OnTouchListener onToneButtonTouchListener = new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				MidiOutputDevice midiOutputDevice = getMidiOutputDevice();
				if (midiOutputDevice == null) {
					return false;
				}

				int note = 60 + Integer.parseInt((String) v.getTag());
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					midiOutputDevice.onMidiNoteOn(cableIdSpinner.getSelectedItemPosition(), 0, note, 127);
					if (midiOutputEventAdapter != null) {
						midiOutputEventAdapter.add("NoteOn cableId: " + cableIdSpinner.getSelectedItemPosition() + ", note: " + note + ", velocity: 127");
					}
					break;
				case MotionEvent.ACTION_UP:
					midiOutputDevice.onMidiNoteOff(cableIdSpinner.getSelectedItemPosition(), 0, note, 127);
					if (midiOutputEventAdapter != null) {
						midiOutputEventAdapter.add("NoteOff cableId: " + cableIdSpinner.getSelectedItemPosition() + ", note: " + note + ", velocity: 127");
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

	private AudioTrack prepareAudioTrack(int samplingRate) {
		AudioTrack result = new AudioTrack(AudioManager.STREAM_MUSIC, samplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioTrack.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
		result.setStereoVolume(1f, 1f);
		result.play();
		return result;
	}

	@Override
	protected void onDeviceAttached() {
		Toast.makeText(this, "USB MIDI Device has been attached.", Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onDeviceDetached() {
		Toast.makeText(this, "USB MIDI Device has been detached.", Toast.LENGTH_LONG).show();
		finish();
	}

	@Override
	public void onMidiNoteOff(int cable, int channel, int note, int velocity) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("NoteOff cable: " + cable + ", channel: " + channel + ", note: " + note + ", velocity: " + velocity);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiNoteOff(cable, channel, note, velocity);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("NoteOff cable: " + cable + ", channel: " + channel + ", note: " + note + ", velocity: " + velocity);
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

	@Override
	public void onMidiNoteOn(int cable, int channel, int note, int velocity) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("NoteOn cable: " + cable + ",  channel: " + channel + ", note: " + note + ", velocity: " + velocity);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiNoteOn(cable, channel, note, velocity);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("NoteOn cable: " + cable + ",  channel: " + channel + ", note: " + note + ", velocity: " + velocity);
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

	@Override
	public void onMidiPolyphonicAftertouch(int cable, int channel, int note, int pressure) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("PolyphonicAftertouch cable: " + cable + ", channel: " + channel + ", note: " + note + ", pressure: " + pressure);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiPolyphonicAftertouch(cable, channel, note, pressure);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("PolyphonicAftertouch cable: " + cable + ", channel: " + channel + ", note: " + note + ", pressure: " + pressure);
			}
		}
	}

	@Override
	public void onMidiControlChange(int cable, int channel, int function, int value) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("ControlChange cable: " + cable + ", channel: " + channel + ", function: " + function + ", value: " + value);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiControlChange(cable, channel, function, value);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("ControlChange cable: " + cable + ", channel: " + channel + ", function: " + function + ", value: " + value);
			}
		}
	}

	@Override
	public void onMidiProgramChange(int cable, int channel, int program) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("ProgramChange cable: " + cable + ", channel: " + channel + ", program: " + program);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiProgramChange(cable, channel, program);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("ProgramChange cable: " + cable + ", channel: " + channel + ", program: " + program);
			}
		}

		currentProgram = program % Tone.FORM_MAX;
		synchronized (tones) {
			for (Tone tone : tones) {
				tone.setForm(currentProgram);
			}
		}
	}

	@Override
	public void onMidiChannelAftertouch(int cable, int channel, int pressure) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("ChannelAftertouch cable: " + cable + ", channel: " + channel + ", pressure: " + pressure);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiChannelAftertouch(cable, channel, pressure);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("ChannelAftertouch cable: " + cable + ", channel: " + channel + ", pressure: " + pressure);
			}
		}
	}

	@Override
	public void onMidiPitchWheel(int cable, int channel, int amount) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("PitchWheel cable: " + cable + ", channel: " + channel + ", amount: " + amount);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiPitchWheel(cable, channel, amount);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("PitchWheel cable: " + cable + ", channel: " + channel + ", amount: " + amount);
			}
		}
	}

	@Override
	public void onMidiSystemExclusive(int cable, byte[] systemExclusive) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("SystemExclusive cable: " + cable + ", data:" + Arrays.toString(systemExclusive));
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiSystemExclusive(cable, systemExclusive);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("SystemExclusive cable: " + cable + ", data:" + Arrays.toString(systemExclusive));
			}
		}
	}

	@Override
	public void onMidiSystemCommonMessage(int cable, byte[] bytes) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("SystemCommonMessage cable: " + cable + ", bytes: " + Arrays.toString(bytes));
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiSystemCommonMessage(cable, bytes);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("SystemCommonMessage cable: " + cable + ", bytes: " + Arrays.toString(bytes));
			}
		}
	}

	@Override
	public void onMidiSingleByte(int cable, int byte1) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("SingleByte cable: " + cable + ", data: " + byte1);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiSingleByte(cable, byte1);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("SingleByte cable: " + cable + ", data: " + byte1);
			}
		}
	}

	@Override
	public void onMidiMiscellaneousFunctionCodes(int cable, int byte1, int byte2, int byte3) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("MiscellaneousFunctionCodes cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiMiscellaneousFunctionCodes(cable, byte1, byte2, byte3);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("MiscellaneousFunctionCodes cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3);
			}
		}
	}

	@Override
	public void onMidiCableEvents(int cable, int byte1, int byte2, int byte3) {
		if (midiInputEventAdapter != null) {
			midiInputEventAdapter.add("CableEvents cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3);
		}

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().onMidiCableEvents(cable, byte1, byte2, byte3);
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add("CableEvents cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3);
			}
		}
	}
}
