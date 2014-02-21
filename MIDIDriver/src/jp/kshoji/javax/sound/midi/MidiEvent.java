package jp.kshoji.javax.sound.midi;

/**
 * Represents a MIDI Event.
 * 
 * @author K.Shoji
 */
public class MidiEvent {
    private MidiMessage message;
    
    private long tick;
   
    /**
     * Constructor
     * 
     * @param message
     * @param tick -1 if timeStamp not supported.
     */
    public MidiEvent(MidiMessage message, long tick) {
        this.message = message;
        this.tick = tick;
    }

    /**
     * Get the {@link MidiDevice} of this {@link MidiEvent}
     * 
     * @return
     */
    public MidiMessage getMessage() {
        return message;
    }

    /**
     * Get the timeStamp in tick
     * @return -1 if timeStamp not supported.
     */
    public long getTick() {
        return tick;
    }

    /**
     * Set the timeStamp in tick
     * 
     * @param tick timeStamp
     */
    public void setTick(long tick) {
        this.tick = tick;
    }
}