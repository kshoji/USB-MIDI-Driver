package jp.kshoji.javax.sound.midi.usb;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.kshoji.javax.sound.midi.ControllerEventListener;
import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.MetaEventListener;
import jp.kshoji.javax.sound.midi.MidiSystem;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.Sequence;
import jp.kshoji.javax.sound.midi.Sequencer;
import jp.kshoji.javax.sound.midi.Track;
import jp.kshoji.javax.sound.midi.Transmitter;
import jp.kshoji.javax.sound.midi.io.StandardMidiFileReader;
import android.annotation.SuppressLint;

/**
 * {@link jp.kshoji.javax.sound.midi.Sequencer} implementation
 * 
 * XXX Work in progress, @see http://docs.oracle.com/javase/jp/6/api/javax/sound/midi/Sequencer.html
 * 
 * @author K.Shoji
 */
public class UsbMidiSequencer implements Sequencer {
	List<Transmitter> transmitters = new ArrayList<Transmitter>();
	List<Receiver> receivers = new ArrayList<Receiver>();
	
	Set<MetaEventListener> metaEventListeners = new HashSet<MetaEventListener>();
	@SuppressLint("UseSparseArrays")
	Map<Integer, Set<ControllerEventListener>> controllerEventListenerMap = new HashMap<Integer, Set<ControllerEventListener>>();

	private Sequence sequence = null;
	private boolean isOpen = false;
	private boolean isRunning = false;
	private boolean isRecording = false;
	private int loopCount = 0;
	private SyncMode masterSyncMode = SyncMode.INTERNAL_CLOCK;
	private SyncMode slaveSyncMode = SyncMode.NO_SYNC;
	private float tempoFactor = 1.0f;
	private long loopStartPoint = 0;
	private long loopEndPoint = 0;
	@SuppressLint("UseSparseArrays")
	private Map<Integer, Boolean> trackMute = new HashMap<Integer, Boolean>();
	@SuppressLint("UseSparseArrays")
	private Map<Integer, Boolean> trackSolo = new HashMap<Integer, Boolean>();
	private long tickPosition = 0;
	private float tempoInBPM = 60f;
	private Map<Track, Set<Integer>> recordEnable = new HashMap<Track, Set<Integer>>();
	
	public UsbMidiSequencer() throws MidiUnavailableException {
		transmitters.add(MidiSystem.getTransmitter());
		receivers.add(MidiSystem.getReceiver());
	}
	
	@Override
	public Info getDeviceInfo() {
		return new Info("UsbMidiSequencer", "jp.kshoji", "Android USB MIDI Sequencer", "0.1");
	}

	@Override
	public void open() throws MidiUnavailableException {
		// TODO open device
		isOpen = true;
	}

	@Override
	public void close() {
		// TODO close device
		isOpen = false;
	}

	@Override
	public boolean isOpen() {
		return isOpen;
	}

	@Override
	public int getMaxReceivers() {
		return receivers.size();
	}

	@Override
	public int getMaxTransmitters() {
		return transmitters.size();
	}

	@Override
	public Receiver getReceiver() throws MidiUnavailableException {
		return receivers.get(0);
	}

	@Override
	public List<Receiver> getReceivers() {
		return receivers;
	}

	@Override
	public Transmitter getTransmitter() throws MidiUnavailableException {
		return transmitters.get(0);
	}

	@Override
	public List<Transmitter> getTransmitters() {
		return transmitters;
	}

	@Override
	public int[] addControllerEventListener(ControllerEventListener listener, int[] controllers) {
		for (int controllerId : controllers) {
			Set<ControllerEventListener> listeners = controllerEventListenerMap.get(controllerId);
			if (listeners == null) {
				listeners = new HashSet<ControllerEventListener>();
			}
			listeners.add(listener);
			controllerEventListenerMap.put(controllerId, listeners);
		}
		return controllers;
	}

	@Override
	public int[] removeControllerEventListener(ControllerEventListener listener, int[] controllers) {
		List<Integer> resultList = new ArrayList<Integer>();
		for (int controllerId : controllers) {
			Set<ControllerEventListener> listeners = controllerEventListenerMap.get(controllerId);
			if (listeners != null && listeners.contains(listener)) {
				listeners.remove(listener);
			} else {
				resultList.add(controllerId);
			}
			controllerEventListenerMap.put(controllerId, listeners);
		}
		
		// returns currently registered controllers
		Integer[] resultArray = resultList.toArray(new Integer[] {});
		int[] resultPrimitiveArray = new int[resultArray.length];
		for (int i = 0; i < resultArray.length; i++) {
			resultPrimitiveArray[i] = resultArray[i];
		}
		return resultPrimitiveArray;
	}
	
	@Override
	public boolean addMetaEventListener(MetaEventListener listener) {
		// return true if registered successfully
		return metaEventListeners.add(listener);
	}

	@Override
	public void removeMetaEventListener(MetaEventListener listener) {
		metaEventListeners.remove(listener);
	}
	
	@Override
	public int getLoopCount() {
		return loopCount;
	}

	@Override
	public void setLoopCount(int count) {
		loopCount = count;
	}

	@Override
	public long getLoopStartPoint() {
		return this.loopStartPoint;
	}

	@Override
	public void setLoopStartPoint(long tick) {
		loopStartPoint = tick;
	}

	@Override
	public long getLoopEndPoint() {
		return loopEndPoint;
	}

	@Override
	public void setLoopEndPoint(long tick) {
		loopEndPoint = tick;
	}

	@Override
	public SyncMode getMasterSyncMode() {
		return masterSyncMode;
	}

	@Override
	public void setMasterSyncMode(SyncMode sync) {
		for (SyncMode availableMode : getMasterSyncModes()) {
			if (availableMode == sync) {
				masterSyncMode = sync;
			}
		}
	}

	@Override
	public SyncMode[] getMasterSyncModes() {
		// TODO other SyncModes.
		return new SyncMode[] { SyncMode.INTERNAL_CLOCK };
	}

	@Override
	public long getMicrosecondPosition() {
		return (long) (tickPosition * getTicksPerMicrosecond());
	}

	@Override
	public void setMicrosecondPosition(long microseconds) {
		setTickPosition((long) (getTicksPerMicrosecond() * microseconds));
	}

	/**
	 * convert parameter from microseconds to tick
	 * 
	 * @return
	 */
	private float getTicksPerMicrosecond() {
		float ticksPerMicrosecond;
		if (sequence.getDivisionType() == Sequence.PPQ) {
			// PPQ : 2 * resolution / 1000000 ticks per microsecond
			ticksPerMicrosecond = 2f * sequence.getResolution() / 1000000f;
		} else {
			// SMPTE : divisionType * resolution / 1000000 ticks per microsecond
			ticksPerMicrosecond = sequence.getDivisionType() * sequence.getResolution() / 1000000f;
		}
		return ticksPerMicrosecond;
	}
	
	@Override
	public long getMicrosecondLength() {
		return sequence.getMicrosecondLength();
	}

	@Override
	public Sequence getSequence() {
		return sequence;
	}

	@Override
	public void setSequence(InputStream stream) throws IOException, InvalidMidiDataException {
		setSequence(new StandardMidiFileReader().getSequence(stream));
	}

	@Override
	public void setSequence(Sequence sequence) throws InvalidMidiDataException {
		this.sequence = sequence;
	}

	@Override
	public SyncMode getSlaveSyncMode() {
		return slaveSyncMode;
	}
	
	@Override
	public void setSlaveSyncMode(SyncMode sync) {
		for (SyncMode availableMode : getSlaveSyncModes()) {
			if (availableMode == sync) {
				slaveSyncMode = sync;
			}
		}
	}

	@Override
	public SyncMode[] getSlaveSyncModes() {
		// TODO other SyncModes.
		return new SyncMode[] { SyncMode.NO_SYNC };
	}

	@Override
	public float getTempoFactor() {
		return tempoFactor;
	}

	@Override
	public void setTempoFactor(float factor) {
		tempoFactor  = factor;
	}

	@Override
	public float getTempoInBPM() {
		return tempoInBPM;
	}

	@Override
	public void setTempoInBPM(float bpm) {
		tempoInBPM = bpm;
	}

	@Override
	public float getTempoInMPQ() {
		return 60000000f / tempoInBPM;
	}

	@Override
	public void setTempoInMPQ(float mpq) {
		tempoInBPM = 60000000f / mpq;
	}

	@Override
	public long getTickLength() {
		if (sequence == null) {
			return 0;
		}
		return sequence.getTickLength();
	}

	@Override
	public long getTickPosition() {
		return tickPosition;
	}

	@Override
	public void setTickPosition(long tick) {
		tickPosition = tick;
	}

	@Override
	public boolean getTrackMute(int track) {
		return Boolean.TRUE.equals(trackMute.get(track));
	}

	@Override
	public void setTrackMute(int track, boolean mute) {
		trackMute.put(track, mute);
	}

	@Override
	public boolean getTrackSolo(int track) {
		return Boolean.TRUE.equals(trackMute.get(track));
	}

	@Override
	public void setTrackSolo(int track, boolean solo) {
		trackSolo.put(track, solo);
	}

	@Override
	public void recordDisable(Track track) {
		if (track == null) {
			// disable all track
			recordEnable.clear();
		} else {
			// disable specified track
			Set<Integer> trackRecordEnable = recordEnable.get(track);
			if (trackRecordEnable != null) {
				recordEnable.put(track, null);
			}
		}
	}

	@Override
	public void recordEnable(Track track, int channel) {
		Set<Integer> trackRecordEnable = recordEnable.get(track);
		if (trackRecordEnable == null) {
			trackRecordEnable = new HashSet<Integer>();
		}
		
		if (channel == -1) {
			for (int i = 0; i < 16; i++) {
				// record to the all channels
				trackRecordEnable.add(i);
			}
			recordEnable.put(track, trackRecordEnable);
		} else if (channel >= 0 && channel < 16) {
			trackRecordEnable.add(channel);
			recordEnable.put(track, trackRecordEnable);
		}
	}

	@Override
	public void startRecording() {
		isRecording = true;
		// TODO start recording
	}
	
	@Override
	public boolean isRecording() {
		return isRecording;
	}

	@Override
	public void stopRecording() {
		isRecording = false;
		// TODO stop recording
	}
	
	@Override
	public void start() {
		isRunning = true;
		// TODO start playing
	}

	@Override
	public boolean isRunning() {
		return isRunning || isRecording;
	}

	@Override
	public void stop() {
		isRunning = false;
		isRecording = false;
		// TODO stop playing and recording
	}
}
