package jp.kshoji.javax.sound.midi;

import java.io.IOException;
import java.io.InputStream;
import java.util.EventListener;

/**
 * Interface for MIDI Sequencer
 * 
 * @author K.Shoji
 */
public interface Sequencer extends MidiDevice {
	/**
	 * Loops eternally.
	 * 
	 * @see Sequencer#setLoopCount(int)
	 */
    int LOOP_CONTINUOUSLY = -1;

    /**
     * {@link Sequencer}'s Synchronization mode
     * 
     * @author K.Shoji
     */
    public static class SyncMode {
		public static final SyncMode INTERNAL_CLOCK = new SyncMode("Internal Clock");
        public static final SyncMode NO_SYNC = new SyncMode("No Sync");

        private String name;

        protected SyncMode(String name) {
            this.name = name;
        }
        
        @Override
        public final boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
            	return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final SyncMode other = (SyncMode) obj;
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }

        @Override
        public final int hashCode() {
            final int PRIME = 31;
            int result = super.hashCode();
            result = PRIME * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }

        @Override
        public final String toString() {
            return name;
        }
    }
    
    /**
     * Get the available {@link SyncMode} for master.
     * 
     * @return
     */
    Sequencer.SyncMode[] getMasterSyncModes();

    /**
     * Get the {@link SyncMode} for master.
     * 
     * @return
     */
    Sequencer.SyncMode getMasterSyncMode();

    /**
     * Set the {@link SyncMode} for master.
     * 
     * @param sync
     */
    void setMasterSyncMode(Sequencer.SyncMode sync);
    
    /**
     * Get the available {@link SyncMode} for master.
     * 
     * @return
     */
    Sequencer.SyncMode[] getSlaveSyncModes();

    /**
     * Get the {@link SyncMode} for slave.
     * 
     * @return
     */
    Sequencer.SyncMode getSlaveSyncMode();
    
    /**
     * Set the {@link SyncMode} for slave.
     * @param sync
     */
    void setSlaveSyncMode(Sequencer.SyncMode sync);

    /**
     * Get {@link Sequence}
     * 
     * @return
     */
    Sequence getSequence();
   
    /**
     * Load {@link Sequence} from stream.
     * 
     * @param stream
     * @throws IOException
     * @throws InvalidMidiDataException
     */
    void setSequence(InputStream stream) throws IOException, InvalidMidiDataException;

    /**
     * Set {@link Sequence} for the {@link Sequencer}
     * 
     * @param sequence
     * @throws InvalidMidiDataException
     */
    void setSequence(Sequence sequence) throws InvalidMidiDataException;

    /**
     * Add {@link EventListener} for {@link ShortMessage.CONTROL_CHANGE}
     * 
     * @param listener event listener
     * @param controllers controller codes
     * @return int[] registered controllers for the specified listener
     */
    int[] addControllerEventListener(ControllerEventListener listener, int[] controllers);

    /**
     * Remove {@link EventListener} for {@link ShortMessage.CONTROL_CHANGE}
     * 
     * @param listener event listener
     * @param controllers controller codes
     * @return int[] registered controllers for the specified listener
     */
    int[] removeControllerEventListener(ControllerEventListener listener, int[] controllers);
    
    /**
     * Add {@link EventListener} for {@link MetaMessage}
     * 
     * @param listener event listener
     * @return true if registered successfully
     */
    boolean addMetaEventListener(MetaEventListener listener);

    /**
     * Remove {@link EventListener} for {@link MetaMessage}
     * 
     * @param listener event listener
     */
    void removeMetaEventListener(MetaEventListener listener);
    
    /**
     * Get if the {@link Sequencer} is recording.
     * 
     * @return
     */
    boolean isRecording();

    /**
     * Get if the {@link Sequencer} is playing OR recording.
     * 
     * @return
     */
    boolean isRunning();

    /**
     * Set {@link Track} to disable recording
     * 
     * @param track
     */
    void recordDisable(Track track);

    /***
     * Set {@link Track} to enable recording on the specified channel.
     * 
     * @param track
     * @param channel
     */
    void recordEnable(Track track, int channel);
    
    /**
     * Get count of loop.
     * 
     * @return
     */
    int getLoopCount();

    /**
     * Set count of loop.
     * 
     * @param count
     * <ul>
     * <li>{@link #LOOP_CONTINUOUSLY}: play loops eternally</li>
     * <li>0: play once(no loop)</li>
     * <li>1: play twice(loop once)</li>
     * </ul>
     */
    void setLoopCount(int count);
    
    /**
     * Get start point(ticks) of loop.
     * 
     * @return ticks
     */
    long getLoopStartPoint();
    
    /**
     * Set start point(ticks) of loop.
     * 
     * @param tick 0: start of {@link Sequence}
     */
    void setLoopStartPoint(long tick);

    /**
     * Get end point(ticks) of loop.
     * 
     * @return
     */
    long getLoopEndPoint();
    
    /**
     * Set end point(ticks) of loop.
     * 
     * @param tick -1: end of {@link Sequence}
     */
    void setLoopEndPoint(long tick);

    /**
     * Get the tempo factor.
     * 
     * @return
     */
    float getTempoFactor();
    
    /**
     * Set the tempo factor. This method don't change {@link Sequence}'s tempo.
     * 
     * @param factor 
     * <ul>
     * <li>1.0f : the normal tempo</li>
     * <li>0.5f : half slow tempo</li>
     * <li>2.0f : 2x fast tempo</li>
     * </ul>
     */
    void setTempoFactor(float factor);

    float getTempoInBPM();

    /**
     * Set the tempo in the Beats per minute.
     * 
     * @param bpm
     */
    void setTempoInBPM(float bpm);

    float getTempoInMPQ();

    /**
     * Set the tempos in the microseconds per quarter note.
     * 
     * @param mpq
     */
    void setTempoInMPQ(float mpq);

    /**
     * Get the {@link Sequence} length in ticks.
     * 
     * @return
     */
    long getTickLength();

    /**
     * Get the {@link Sequence} length in microseconds.
     * 
     * @return
     */
    long getMicrosecondLength();
    
    /**
     * Get the current tick position.
     * 
     * @return
     */
    long getTickPosition();
    
    /**
     * Set the current tick position.
     * 
     * @param tick
     */
    void setTickPosition(long tick);

    /**
     * Get current microsecond position.
     */
    @Override
    long getMicrosecondPosition();

    /**
     * Set current microsecond position.
     * 
     * @param microseconds
     */
    void setMicrosecondPosition(long microseconds);

    boolean getTrackMute(int track);
    
    /**
     * Set track to mute on the playback.
     * 
     * @param track
     * @param mute
     */
    void setTrackMute(int track, boolean mute);

    boolean getTrackSolo(int track);
    
    /**
     * Set track to solo on the playback.
     * 
     * @param track
     * @param solo
     */
    void setTrackSolo(int track, boolean solo);

    /**
     * Start playing (starting at current sequencer position)
     */
    void start();

    /**
     * Start recording (starting at current sequencer position)
     * 
     * Current {@link Sequence}'s events are sent to the all {@link Transmitter}.
     * Received events art also sent to the all {@link Transmitter}.
     */
    void startRecording();

    /**
     * Stop playing AND recording.
     */
    void stop();

    /**
     * Stop recording. Playing continues.
     */
    void stopRecording();
}
