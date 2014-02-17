package jp.kshoji.javax.sound.midi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Track {
    // MetaMessage with MetaMessage.data contains -1, 47 and 0 is meta-event End of Track.
    static final byte[] END_OF_TRACK = new byte[] {-1, 47, 0};

	List<MidiEvent> events; //vector of events contain in the Track
    
    private MidiEvent badEvent; //variable to save event which I try to add
                                //to empty Track; see description below
    
    private long tick; 
    
    Track() {
        /*
         * create an empty Track; new Track must contain only meta-event End of Track.
         */
        events = new ArrayList<MidiEvent>();
    }
    
    public interface IPredicate<T> { boolean apply(T type); }
    
	public static <T> List<T> filter(List<T> target, IPredicate<T> predicate) {
	    List<T> result = new ArrayList<T>();
	    for (T element: target) {
	        if (predicate.apply(element)) {
	            result.add(element);
	        }
	    }
	    return result;
	}
	
	public static class TrackUtils {
		public static Sequence mergeSequenceTrack(Sequencer sequencer, Map<Track, Set<Integer>> recordEnable) throws InvalidMidiDataException {
			Sequence source = sequencer.getSequence();
			Sequence merged = new Sequence(source.getDivisionType(), source.getResolution());
			Track mergedTrack = merged.createTrack();

			// apply track mute and solo
			Track[] tracks = source.getTracks();
			boolean hasSoloTrack = false;
			for (int trackIndex = 0; trackIndex < tracks.length; trackIndex++) {
				if (sequencer.getTrackSolo(trackIndex)) {
					hasSoloTrack = true;
				}
			}
			
			for (int trackIndex = 0; trackIndex < tracks.length; trackIndex++) {
				if (sequencer.getTrackMute(trackIndex)) {
					// muted track, ignore
					continue;
				}
				if (hasSoloTrack && sequencer.getTrackSolo(trackIndex) == false) {
					// not solo track, ignore
					continue;
				}
				if (sequencer.isRecording() && //
						(recordEnable.get(tracks[trackIndex]) != null && recordEnable.get(tracks[trackIndex]).size() > 0)) {
					// recording track, ignore
					continue;
				}
				
				mergedTrack.events.addAll(tracks[trackIndex].events);
			}
			
			sortEvents(mergedTrack);
			
			return merged;
		}
		
		public static void sortEvents(Track track) {
			synchronized (track.events) {
	        	Collections.sort(track.events, new Comparator<MidiEvent>() {
					@Override
					public int compare(MidiEvent lhs, MidiEvent rhs) {
						// sort by tick
						int tickDifference = (int) (lhs.getTick() - rhs.getTick());
						if (tickDifference != 0) {
							return tickDifference;
						}
						
						// same tick
						// sort by MIDI priority order
						// system message > control messages > note on > note off
						
						int lhsInt = (lhs.getMessage().getMessage()[0] & 0xf0);
						int rhsInt = (rhs.getMessage().getMessage()[0] & 0xf0);
						
						if ((lhsInt & 0x90) == 0x80) {
							lhsInt |= 0x10;
						} else {
							lhsInt &= ~0x10;
						}
						if ((rhsInt & 0x90) == 0x80) {
							rhsInt |= 0x10;
						} else {
							rhsInt &= ~0x10;
						}
						
						return - (lhsInt - rhsInt);
					}
				});
	        	
	        	List<MidiEvent> filtered = filter(track.events, new IPredicate<MidiEvent>() {
					@Override
					public boolean apply(MidiEvent event) {
						return !Arrays.equals(END_OF_TRACK, event.getMessage().getMessage());
					}
				});
	        	
	        	track.events.clear();
	        	track.events.addAll(filtered);
	        	
	        	// add to last
	        	track.events.add(new MidiEvent(new MetaMessage(END_OF_TRACK), track.events.get(track.events.size() - 1).getTick() + 1));
			}
		}
	}

	public boolean add(MidiEvent event) {
        synchronized (events) {
        	events.add(event);
        }
        
        return true;
    }

    public MidiEvent get(int index) throws ArrayIndexOutOfBoundsException {
    	synchronized (events) {
	        if (index < 0 || index >= events.size()) {
	            throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Size: " + events.size());
	        }
	        return events.get(index);
    	}
    }

    /**
     * 
     * @param event to remove
     * @return true if the event was removed
     */
    public boolean remove(MidiEvent event) {
        /*
         * if I remove event that equals badEvent, I "delete" badEvent
         */
        if (event == badEvent) {
            badEvent = null;
            return false;
        }
        
        synchronized (events) {
	        /*
	         * method Track.ticks() always return the biggest value that ever has been
	         * in the Track; so only when Track is empty Track.ticks() return 0 
	         */
	        if (events.remove(event)) {
	            if (events.size() == 0) {
	                tick = 0;
	            }
	            return true;
	        }
        }
        
        return false;
    }

    /**
     * Get the number of events in the {@link Track}
     * @return
     */
    public int size() {
        return events.size();
    }

    public long ticks() {
        return tick;
    }
}