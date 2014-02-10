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
import android.content.Context;

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
	Map<Integer, Set<ControllerEventListener>> controllerEventListenerMap = new HashMap<Integer, Set<ControllerEventListener>>();

	private Sequence sequence = null;
	private boolean isOpen = false;
	private boolean isRunning = false;
	private boolean isRecording = false;
	private int loopCount = 0;
	private SyncMode masterSyncMode = SyncMode.INTERNAL_CLOCK;
	private SyncMode slaveSyncMode = SyncMode.NO_SYNC;
	private float tempoFactor = 1.0f;
	
	public UsbMidiSequencer(Context context) throws MidiUnavailableException{
		MidiSystem.initialize(context);
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLoopStartPoint(long tick) {
		// TODO Auto-generated method stub

	}

	@Override
	public long getLoopEndPoint() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLoopEndPoint(long tick) {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setMicrosecondPosition(long microseconds) {
		// TODO Auto-generated method stub

	}
	
	@Override
	public long getMicrosecondLength() {
		// TODO Auto-generated method stub
		return 0;
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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setTempoInBPM(float bpm) {
		// TODO Auto-generated method stub
	}

	@Override
	public float getTempoInMPQ() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setTempoInMPQ(float mpq) {
		// TODO Auto-generated method stub

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
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setTickPosition(long tick) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean getTrackMute(int track) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setTrackMute(int track, boolean mute) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean getTrackSolo(int track) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setTrackSolo(int track, boolean solo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void recordDisable(Track track) {
		// TODO Auto-generated method stub

	}

	@Override
	public void recordEnable(Track track, int channel) {
		// TODO Auto-generated method stub

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
