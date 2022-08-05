package jp.kshoji.driver.midi.sample;

import android.graphics.PorterDuff.Mode;
import android.hardware.usb.UsbDevice;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import jp.kshoji.driver.midi.activity.AbstractSingleMidiActivity;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.sample.util.SoundMaker;
import jp.kshoji.driver.midi.sample.util.Tone;

/**
 * Sample Activity for MIDI Driver library
 * 
 * @author K.Shoji
 */
public class MIDIDriverSingleSampleActivity extends AbstractSingleMidiActivity {
	// User interface
	final Handler midiInputEventHandler = new Handler(new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			if (midiInputEventAdapter != null) {
				midiInputEventAdapter.add((String)msg.obj);
			}
			// message handled successfully
			return true;
		}
	});
	
	final Handler midiOutputEventHandler = new Handler(new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			if (midiOutputEventAdapter != null) {
				midiOutputEventAdapter.add((String)msg.obj);
			}
			// message handled successfully
			return true;
		}
	});
	
	ArrayAdapter<String> midiInputEventAdapter;
	ArrayAdapter<String> midiOutputEventAdapter;
	private ToggleButton thruToggleButton;
	Spinner cableIdSpinner;

	// Play sounds
	AudioTrack audioTrack;
	Timer timer;
	TimerTask timerTask;
	SoundMaker soundMaker;
	final Set<Tone> tones = new HashSet<Tone>();
	int currentProgram = 0;

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
		
		Spinner deviceSpinner;
		deviceSpinner = (Spinner) findViewById(R.id.deviceNameSpinner);
		deviceSpinner.setVisibility(View.GONE);
		
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
					midiOutputDevice.sendMidiNoteOn(cableIdSpinner.getSelectedItemPosition(), 0, note, 127);
					midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "NoteOn to: " + midiOutputDevice.getUsbDevice().getDeviceName() + ", cableId: " + cableIdSpinner.getSelectedItemPosition() + ", note: " + note + ", velocity: 127"));
					break;
				case MotionEvent.ACTION_UP:
					midiOutputDevice.sendMidiNoteOff(cableIdSpinner.getSelectedItemPosition(), 0, note, 127);
					midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "NoteOff to: " + midiOutputDevice.getUsbDevice().getDeviceName() + ", cableId: " + cableIdSpinner.getSelectedItemPosition() + ", note: " + note + ", velocity: 127"));
					break;
				default:
					// do nothing.
					break;
				}
				return false;
			}
		};
		findViewById(R.id.buttonC).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonCis).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonD).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonDis).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonE).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonF).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonFis).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonG).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonGis).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonA).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonAis).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonB).setOnTouchListener(onToneButtonTouchListener);
		findViewById(R.id.buttonC2).setOnTouchListener(onToneButtonTouchListener);

		int whiteKeyColor = 0xFFFFFFFF;
		int blackKeyColor = 0xFF808080;
		findViewById(R.id.buttonC).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonCis).getBackground().setColorFilter(blackKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonD).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonDis).getBackground().setColorFilter(blackKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonE).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonF).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonFis).getBackground().setColorFilter(blackKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonG).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonGis).getBackground().setColorFilter(blackKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonA).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonAis).getBackground().setColorFilter(blackKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonB).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);
		findViewById(R.id.buttonC2).getBackground().setColorFilter(whiteKeyColor, Mode.MULTIPLY);

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

    /**
     * Prepare the AudioTrack instance
     *
     * @param samplingRate the sampling rate of AudioTrack
     * @return AudioTrack
     */
	private static @NonNull AudioTrack prepareAudioTrack(int samplingRate) {
		AudioTrack result = new AudioTrack(AudioManager.STREAM_MUSIC, samplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, AudioTrack.getMinBufferSize(samplingRate, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT), AudioTrack.MODE_STREAM);
		result.setStereoVolume(1f, 1f);
		result.play();
		return result;
	}

    @Override
    public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
        // deprecated method.
        // do nothing
    }

    @Override
    public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {

    }

    @Override
    public void onMidiOutputDeviceAttached(@NonNull final MidiOutputDevice midiOutputDevice) {
        Toast.makeText(MIDIDriverSingleSampleActivity.this, "USB MIDI Device " + midiOutputDevice.getUsbDevice().getDeviceName() + " has been attached.", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDeviceDetached(@NonNull UsbDevice usbDevice) {
        // deprecated method.
        // do nothing
    }

    @Override
    public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {

    }

    @Override
    public void onMidiOutputDeviceDetached(@NonNull final MidiOutputDevice midiOutputDevice) {
        Toast.makeText(MIDIDriverSingleSampleActivity.this, "USB MIDI Device " + midiOutputDevice.getUsbDevice().getDeviceName() + " has been detached.", Toast.LENGTH_LONG).show();
    }

	@Override
	public void onMidiNoteOff(@NonNull final MidiInputDevice sender, int cable, int channel, int note, int velocity) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "NoteOff cable: " + cable + ", channel: " + channel + ", note: " + note + ", velocity: " + velocity));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiNoteOff(cable, channel, note, velocity);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "NoteOff cable: " + cable + ", channel: " + channel + ", note: " + note + ", velocity: " + velocity));
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
	public void onMidiNoteOn(@NonNull final MidiInputDevice sender, int cable, int channel, int note, int velocity) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "NoteOn cable: " + cable + ",  channel: " + channel + ", note: " + note + ", velocity: " + velocity));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiNoteOn(cable, channel, note, velocity);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "NoteOn cable: " + cable + ",  channel: " + channel + ", note: " + note + ", velocity: " + velocity));
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
	public void onMidiPolyphonicAftertouch(@NonNull final MidiInputDevice sender, int cable, int channel, int note, int pressure) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "PolyphonicAftertouch cable: " + cable + ", channel: " + channel + ", note: " + note + ", pressure: " + pressure));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiPolyphonicAftertouch(cable, channel, note, pressure);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "PolyphonicAftertouch cable: " + cable + ", channel: " + channel + ", note: " + note + ", pressure: " + pressure));
		}
	}

	@Override
	public void onMidiControlChange(@NonNull final MidiInputDevice sender, int cable, int channel, int function, int value) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "ControlChange cable: " + cable + ", channel: " + channel + ", function: " + function + ", value: " + value));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiControlChange(cable, channel, function, value);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "ControlChange cable: " + cable + ", channel: " + channel + ", function: " + function + ", value: " + value));
		}
	}

	@Override
	public void onMidiProgramChange(@NonNull final MidiInputDevice sender, int cable, int channel, int program) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "ProgramChange cable: " + cable + ", channel: " + channel + ", program: " + program));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiProgramChange(cable, channel, program);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "ProgramChange cable: " + cable + ", channel: " + channel + ", program: " + program));
		}

		currentProgram = program % Tone.FORM_MAX;
		synchronized (tones) {
			for (Tone tone : tones) {
				tone.setForm(currentProgram);
			}
		}
	}

	@Override
	public void onMidiChannelAftertouch(@NonNull final MidiInputDevice sender, int cable, int channel, int pressure) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "ChannelAftertouch cable: " + cable + ", channel: " + channel + ", pressure: " + pressure));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiChannelAftertouch(cable, channel, pressure);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "ChannelAftertouch cable: " + cable + ", channel: " + channel + ", pressure: " + pressure));
		}
	}

	@Override
	public void onMidiPitchWheel(@NonNull final MidiInputDevice sender, int cable, int channel, int amount) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "PitchWheel cable: " + cable + ", channel: " + channel + ", amount: " + amount));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiPitchWheel(cable, channel, amount);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "PitchWheel cable: " + cable + ", channel: " + channel + ", amount: " + amount));
		}
	}

	@Override
	public void onMidiSystemExclusive(@NonNull final MidiInputDevice sender, int cable, final byte[] systemExclusive) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "SystemExclusive cable: " + cable + ", data:" + Arrays.toString(systemExclusive)));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiSystemExclusive(cable, systemExclusive);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "SystemExclusive cable: " + cable + ", data:" + Arrays.toString(systemExclusive)));
		}
	}

	@Override
	public void onMidiSystemCommonMessage(@NonNull final MidiInputDevice sender, int cable, final byte[] bytes) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "SystemCommonMessage cable: " + cable + ", bytes: " + Arrays.toString(bytes)));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiSystemCommonMessage(cable, bytes);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "SystemCommonMessage cable: " + cable + ", bytes: " + Arrays.toString(bytes)));
		}
	}

	@Override
	public void onMidiSingleByte(@NonNull final MidiInputDevice sender, int cable, int byte1) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "SingleByte cable: " + cable + ", data: " + byte1));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiSingleByte(cable, byte1);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "SingleByte cable: " + cable + ", data: " + byte1));
		}
	}

	@Override
	public void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice sender, int cable, int timing) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "TimeCodeQuarterFrame from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", timing: " + timing));
	}

	@Override
	public void onMidiSongSelect(@NonNull MidiInputDevice sender, int cable, int song) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "SongSelect from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", song: " + song));
	}

	@Override
	public void onMidiSongPositionPointer(@NonNull MidiInputDevice sender, int cable, int position) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "SongPositionPointer from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable + ", position: " + position));
	}

	@Override
	public void onMidiTuneRequest(@NonNull MidiInputDevice sender, int cable) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "TuneRequest from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable));
	}

	@Override
	public void onMidiTimingClock(@NonNull MidiInputDevice sender, int cable) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "TimingClock from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable));
	}

	@Override
	public void onMidiStart(@NonNull MidiInputDevice sender, int cable) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "Start from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable));
	}

	@Override
	public void onMidiContinue(@NonNull MidiInputDevice sender, int cable) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "Continue from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable));
	}

	@Override
	public void onMidiStop(@NonNull MidiInputDevice sender, int cable) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "Stop from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable));
	}

	@Override
	public void onMidiActiveSensing(@NonNull MidiInputDevice sender, int cable) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "ActiveSensing from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable));
	}

	@Override
	public void onMidiReset(@NonNull MidiInputDevice sender, int cable) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "Reset from: " + sender.getUsbDevice().getDeviceName() + ", cable: " + cable));
	}

	@Override
	public void onMidiMiscellaneousFunctionCodes(@NonNull final MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "MiscellaneousFunctionCodes cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiMiscellaneousFunctionCodes(cable, byte1, byte2, byte3);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "MiscellaneousFunctionCodes cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3));
		}
	}

	@Override
	public void onMidiCableEvents(@NonNull final MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
		midiInputEventHandler.sendMessage(Message.obtain(midiInputEventHandler, 0, "CableEvents cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3));

		if (thruToggleButton != null && thruToggleButton.isChecked() && getMidiOutputDevice() != null) {
			getMidiOutputDevice().sendMidiCableEvents(cable, byte1, byte2, byte3);
			midiOutputEventHandler.sendMessage(Message.obtain(midiOutputEventHandler, 0, "CableEvents cable: " + cable + ", byte1: " + byte1 + ", byte2: " + byte2 + ", byte3: " + byte3));
		}
	}
}
