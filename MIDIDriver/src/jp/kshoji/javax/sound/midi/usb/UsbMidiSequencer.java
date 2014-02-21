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
import jp.kshoji.javax.sound.midi.MetaMessage;
import jp.kshoji.javax.sound.midi.MidiEvent;
import jp.kshoji.javax.sound.midi.MidiMessage;
import jp.kshoji.javax.sound.midi.MidiSystem;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.Sequence;
import jp.kshoji.javax.sound.midi.Sequencer;
import jp.kshoji.javax.sound.midi.ShortMessage;
import jp.kshoji.javax.sound.midi.Track;
import jp.kshoji.javax.sound.midi.Track.TrackUtils;
import jp.kshoji.javax.sound.midi.Transmitter;
import jp.kshoji.javax.sound.midi.io.StandardMidiFileReader;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

/**
 * {@link jp.kshoji.javax.sound.midi.Sequencer} implementation
 * 
 * @author K.Shoji
 */
public class UsbMidiSequencer implements Sequencer {
	List<Transmitter> transmitters = new ArrayList<Transmitter>();
	List<Receiver> receivers = new ArrayList<Receiver>();
	
	Set<MetaEventListener> metaEventListeners = new HashSet<MetaEventListener>();
	SparseArray<Set<ControllerEventListener>> controllerEventListenerMap = new SparseArray<Set<ControllerEventListener>>();

	Sequence sequence = null;
	private boolean isOpen = false;
	boolean isRunning = false;
	boolean isRecording = false;
	private int loopCount = 0;
	private SyncMode masterSyncMode = SyncMode.INTERNAL_CLOCK;
	private SyncMode slaveSyncMode = SyncMode.NO_SYNC;
	float tempoFactor = 1.0f;
	private long loopStartPoint = 0;
	private long loopEndPoint = -1;
	private SparseBooleanArray trackMute = new SparseBooleanArray();
	private SparseBooleanArray trackSolo = new SparseBooleanArray();
	private float tempoInBPM = 60f;
	Map<Track, Set<Integer>> recordEnable = new HashMap<Track, Set<Integer>>();

	SequencerThread sequencerThread = new SequencerThread();
	
	/**
	 * Thread for this Sequencer
	 * 
	 * @author K.Shoji
	 */
	private class SequencerThread extends Thread {
		private long tickPosition = 0;
		
		// recording
		long recordingStartedTime;
		long recordStartedTick;
		Track recordingTrack;

		// playing
		Track playingTrack = null;
		long tickPositionSetTime;
		long runningStoppedTime;
		boolean needRefreshPlayingTrack = false;
		
		public SequencerThread() {
		}

		/**
		 * Get current tick position
		 * @param tick
		 */
		long getTickPosition() {
			if (isRunning) {
				// running
				return tickPosition + (long) ((System.currentTimeMillis() - tickPositionSetTime) * 1000L * getTicksPerMicrosecond());
			} else {
				// stopping
				return tickPosition + (long) ((runningStoppedTime - tickPositionSetTime) * 1000L * getTicksPerMicrosecond());
			}
		}

		/**
		 * Set current tick position
		 * @param tick
		 */
		public void setTickPosision(long tick) {
			tickPosition = tick;
			if (isRunning) {
				tickPositionSetTime = System.currentTimeMillis();
			}
		}

		/**
		 * Start recording
		 */
		void startRecording() {
			if (isRecording) {
				// already recording
				return;
			}
			
			recordingTrack = sequence.createTrack();
			recordingStartedTime = System.currentTimeMillis();
			recordStartedTick = getTickPosition();
			isRecording = true;
			startPlaying();
		}

		/**
		 * Stop recording
		 */
		void stopRecording() {
			if (isRecording == false) {
				// already recording
				return;
			}
			
			long recordEndedTime = System.currentTimeMillis();
			isRecording = false;
			
			for (Track track : sequence.getTracks()) {
				Set<Integer> recordEnableChannels = recordEnable.get(track);
				
				// remove events while recorded time
				Set<MidiEvent> eventToRemoval = new HashSet<MidiEvent>();
				for (int trackIndex = 0; trackIndex < track.size(); trackIndex++) {
					MidiEvent midiEvent = track.get(trackIndex);
					if (isRecordable(recordEndedTime, recordEnableChannels, midiEvent) && // 
							midiEvent.getTick() >= recordingStartedTime && midiEvent.getTick() <= recordEndedTime) { // recorded time
						eventToRemoval.add(midiEvent);
					}
				}
				
				for (MidiEvent event : eventToRemoval) {
					track.remove(event);
				}
				
				// add recorded events
				for (int eventIndex = 0; eventIndex < recordingTrack.size(); eventIndex++) {
					if (isRecordable(recordEndedTime, recordEnableChannels, recordingTrack.get(eventIndex))) {
						track.add(recordingTrack.get(eventIndex));
					}
				}
				
				TrackUtils.sortEvents(track);
			}
			
			// refresh playingTrack
			needRefreshPlayingTrack = true;
		}

		/**
		 * Start playing
		 */
		void startPlaying() {
			if (isRunning) {
				// already playing
				return;
			}
			
			tickPosition = getLoopStartPoint();
			tickPositionSetTime = System.currentTimeMillis();
			isRunning = true;
		}
		
		/**
		 * Stop playing
		 */
		void stopPlaying() {
			if (isRunning == false) {
				// already stopping
				return;
			}
			
			isRunning = false;
			runningStoppedTime = System.currentTimeMillis();
			
			// force stop sleeping
			interrupt();
		}

		/**
		 * Process MidiMessage and fire events to registered event listeners.
		 * 
		 * @param message
		 */
		void fireEventListeners(MidiMessage message) {
			if (message instanceof MetaMessage) {
				for (MetaEventListener metaEventListener : metaEventListeners) {
					metaEventListener.meta((MetaMessage) message);
				}
			} else if (message instanceof ShortMessage) {
				ShortMessage shortMessage = (ShortMessage) message;
				if (shortMessage.getCommand() == ShortMessage.CONTROL_CHANGE) {
					Set<ControllerEventListener> eventListeners = controllerEventListenerMap.get(shortMessage.getData1());
					if (eventListeners != null) {
						for (ControllerEventListener eventListener : eventListeners) {
							eventListener.controlChange(shortMessage);
						}
					}
				}
			}
		}
		
		/*
		 * (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			super.run();

			refreshPlayingTrack();
			
			final float ticksPerMicrosecond = getTicksPerMicrosecond();
			
			// recording
			Receiver midiEventRecordingReceiver = new Receiver() {
				@Override
				public void send(MidiMessage message, long timeStamp) {
					if (isRecording) {
						recordingTrack.add(new MidiEvent(message, recordStartedTick + (long) ((System.currentTimeMillis() - recordingStartedTime) * 1000L * ticksPerMicrosecond)));
					}
					
					fireEventListeners(message);
				}
				
				@Override
				public void close() {
					// do nothing
				}
			};
			
			for (Transmitter transmitter : transmitters) {
				// receive from all transmitters
				transmitter.setReceiver(midiEventRecordingReceiver);
			}
			
			// playing
			while (true) {
				if (isRunning) {
					if (playingTrack == null) {
						continue;
					}
					
					float microsecondsPerTick = 1.f / ticksPerMicrosecond;

					// process looping
					for (int loop = 0; loop < getLoopCount() + 1; loop = (getLoopCount() == LOOP_CONTINUOUSLY ? loop : loop + 1)) {
						if (needRefreshPlayingTrack) {
							refreshPlayingTrack();
						}
						
						for (int i = 0; i < playingTrack.size(); i++) {
							MidiEvent midiEvent = playingTrack.get(i);

							if (needRefreshPlayingTrack) {
								// skip to lastTick
								if (midiEvent.getTick() < tickPosition) {
									continue;
								} else {
									// refresh playingTrack completed
									needRefreshPlayingTrack = false;
								}
							}
							
							if (midiEvent.getTick() < getLoopStartPoint() || (getLoopEndPoint() != -1 && midiEvent.getTick() > getLoopEndPoint())) {
								// outer loop
								tickPosition = midiEvent.getTick();
								tickPositionSetTime = System.currentTimeMillis();
								continue;
							}
							
							try {
								long sleepLength = (long) (microsecondsPerTick * (midiEvent.getTick() - tickPosition) / 1000f / tempoFactor);
								if (sleepLength > 0) {
									sleep(sleepLength);
								}
								tickPosition = midiEvent.getTick();
								tickPositionSetTime = System.currentTimeMillis();
							} catch (InterruptedException e) {
								// ignore exception
							}

							if (isRunning == false) {
								break;
							}
							
							if (needRefreshPlayingTrack) {
								break;
							}

							// send MIDI events
							for (Receiver receiver : receivers) {
								receiver.send(midiEvent.getMessage(), 0);
							}
							
							fireEventListeners(midiEvent.getMessage());
						}
					}
					isRunning = false;
					runningStoppedTime = System.currentTimeMillis();
				}
			}
		}

		/**
		 * Merge current sequence's track to play
		 */
		private void refreshPlayingTrack() {
			Track[] tracks = sequence.getTracks();
			if (tracks != null && tracks.length > 0) {
				try {
					// at first, merge all track into one track
					playingTrack = TrackUtils.mergeSequenceTrack(UsbMidiSequencer.this, recordEnable).getTracks()[0];
				} catch (InvalidMidiDataException e) {
					// ignore exception
				}
			}
		}

		/**
		 * Check if the event can be recorded
		 * 
		 * @param recordEndedTime
		 * @param recordEnableChannels
		 * @param midiEvent
		 * @return true if the event can be recorded
		 */
		private boolean isRecordable(long recordEndedTime, Set<Integer> recordEnableChannels, MidiEvent midiEvent) {
			if (recordEnableChannels == null) {
				return false;
			}
			
			if (recordEnableChannels.contains(Integer.valueOf(-1))) {
				return true;
			}
			
			int status = midiEvent.getMessage().getStatus();
			switch (status & ShortMessage.MASK_EVENT) {
			// channel messages
			case ShortMessage.NOTE_OFF:
			case ShortMessage.NOTE_ON:
			case ShortMessage.POLY_PRESSURE:
			case ShortMessage.CONTROL_CHANGE:
			case ShortMessage.PROGRAM_CHANGE:
			case ShortMessage.CHANNEL_PRESSURE:
			case ShortMessage.PITCH_BEND:
				// recorded Track and channel
				return recordEnableChannels.contains(Integer.valueOf(status & ShortMessage.MASK_CHANNEL));
			// exclusive messages
			default:
				return true;
			}
		}
	}
	
	/**
	 * Create {@link UsbMidiSequencer} instance.
	 * 
	 * @throws MidiUnavailableException
	 */
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
		// open devices
		for (Receiver receiver : receivers) {
			if (receiver instanceof UsbMidiReceiver) {
				UsbMidiReceiver usbMidiReceiver = (UsbMidiReceiver) receiver;
				usbMidiReceiver.open();
			}
		}
		
		for (Transmitter transmitter : transmitters) {
			if (transmitter instanceof UsbMidiTransmitter) {
				UsbMidiTransmitter usbMidiTransmitter = (UsbMidiTransmitter) transmitter;
				usbMidiTransmitter.open();
			}
		}
		
		sequencerThread.start();
		
		isOpen = true;
	}

	@Override
	public void close() {
		// close devices
		for (Receiver receiver : receivers) {
			if (receiver instanceof UsbMidiReceiver) {
				UsbMidiReceiver usbMidiReceiver = (UsbMidiReceiver) receiver;
				usbMidiReceiver.close();
			}
		}
		
		for (Transmitter transmitter : transmitters) {
			if (transmitter instanceof UsbMidiTransmitter) {
				UsbMidiTransmitter usbMidiTransmitter = (UsbMidiTransmitter) transmitter;
				usbMidiTransmitter.close();
			}
		}
		
		sequencerThread.stop();
		
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
				// remaining controller id
				resultList.add(Integer.valueOf(controllerId));
			}
			controllerEventListenerMap.put(controllerId, listeners);
		}
		
		// returns currently registered controller ids for the argument specified listener
		int[] resultPrimitiveArray = new int[resultList.size()];
		for (int i = 0; i < resultPrimitiveArray.length; i++) {
			Integer resultValue = resultList.get(i);
			if (resultValue == null) {
				continue;
			}
			
			resultPrimitiveArray[i] = resultValue.intValue();
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
		return new SyncMode[] { SyncMode.INTERNAL_CLOCK };
	}

	@Override
	public long getMicrosecondPosition() {
		return (long) (getTickPosition() * getTicksPerMicrosecond());
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
	float getTicksPerMicrosecond() {
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
		return new SyncMode[] { SyncMode.NO_SYNC };
	}

	@Override
	public float getTempoFactor() {
		return tempoFactor;
	}

	@Override
	public void setTempoFactor(float factor) {
		if (factor <= 0f) {
			throw new IllegalArgumentException("The tempo factor must be larger than 0f.");
		}
		
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
		if (sequencerThread == null) {
			return 0L;
		}
		return sequencerThread.getTickPosition();
	}

	@Override
	public void setTickPosition(long tick) {
		if (sequencerThread != null) {
			sequencerThread.setTickPosision(tick);
		}
	}

	@Override
	public boolean getTrackMute(int track) {
		return trackMute.get(track);
	}

	@Override
	public void setTrackMute(int track, boolean mute) {
		trackMute.put(track, mute);
	}

	@Override
	public boolean getTrackSolo(int track) {
		return trackMute.get(track);
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
				trackRecordEnable.add(Integer.valueOf(i));
			}
			recordEnable.put(track, trackRecordEnable);
		} else if (channel >= 0 && channel < 16) {
			trackRecordEnable.add(Integer.valueOf(channel));
			recordEnable.put(track, trackRecordEnable);
		}
	}

	@Override
	public void startRecording() {
		// start playing AND recording
		if (isRunning == false) {
			isRunning = true;
		}
		if (isRecording == false) {
			isRecording = true;
			sequencerThread.startRecording();
		}
	}
	
	@Override
	public boolean isRecording() {
		return isRecording;
	}

	@Override
	public void stopRecording() {
		// stop recording
		if (isRecording) {
			isRecording = false;
			sequencerThread.stopRecording();
		}
	}
	
	@Override
	public void start() {
		// start playing
		if (isRunning == false) {
			isRunning = true;
		}
		sequencerThread.startPlaying();
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public void stop() {
		// stop playing AND recording
		if (isRecording) {
			sequencerThread.stopRecording();
			isRecording = false;
		}
		if (isRunning) {
			sequencerThread.stopPlaying();
			isRunning = false;
		}
	}
}
