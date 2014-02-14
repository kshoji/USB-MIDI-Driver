package jp.kshoji.javax.sound.midi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.util.Log;

public class Track {
    // MetaMessage with MetaMessage.data contains -1, 47 and 0 is meta-event End of Track.
    private static final byte[] END_OF_TRACK = new byte[] {-1, 47, 0};

	private List<MidiEvent> events; //vector of events contain in the Track
    
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
		public static Sequence mergeSequenceTrack(Sequence source) throws InvalidMidiDataException {
			Sequence merged = new Sequence(source.getDivisionType(), source.getResolution());
			Track mergedTrack = merged.createTrack();

			Track[] tracks = source.getTracks();
			for (Track track : tracks) {
				mergedTrack.events.addAll(track.events);
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
    
    public boolean nadd(MidiEvent event) {
        //FIXME
        /*
         * Some words about badEvent.
         * When I write tests, I find following situation in the RI:
         * if I want to add to empty Track new event that is not meta-event End of Track,
         * I catch exception ArrayIndexOutOfBoundsException with meaning -1, and my
         * event doesn't add to Track, but meta-event adds. If I try to add the same 
         * event after it, method Track.add(MidiEvent) return 'false', and my event
         * doesn't add again. So, I want to delete this event and use method 
         * Track.remove(MidiEvent) for it, but it return 'false' too! And only after
         * this "shamanism" I can add this event to Track normally. 
         * And only for this situation I use variable badEvent. 
         * 
         * See test org.apache.harmony.sound.tests.javax.sound.midi.TrackTest 
         * for more details
         */
        
        /*
         * if event equals null or badEvent, this method return 'false'
         */
        if (event == null || event == badEvent) {
            return false;
        }
        
        synchronized (events) {
	        /*
	         * If event equals meta-event End of Track and Track in this moment
	         * doesn't contain some events, i.e. Track.size() return 0, this 
	         * event accrue to Track; 
	         * if Track is not empty, but it doesn't contain meta-event, 
	         * this event accrue to the end of Track;
	         * in any case addition of this meta-event is successful, this method 
	         * return 'true' even if meta-event End of Track already contains in the Track
	         */
	        byte[] message = event.getMessage().getMessage();
			if (Arrays.equals(END_OF_TRACK, message)) {
	        	if (events.size() == 0) {
		        	Log.i("USB MIDI Logger", "event added 66");
	                return events.add(event);
	            }
	        	
	            byte[] lastEvent = events.get(events.size() - 1).getMessage().getMessage();
	            if (!Arrays.equals(END_OF_TRACK, lastEvent)) {
		        	Log.i("USB MIDI Logger", "event added 72");
	                return events.add(event);
	            }         
	        	Log.i("USB MIDI Logger", "event not added 75");
	            return true;
	        }
	        /*
	         * after use method Track.add(MidiEvent) Track must contain meta-event
	         * End of Track; so, at first I add this event to Track if it doesn't 
	         * contain meta-event and parameter 'event' is not meta-event
	         */
	        if (events.size() == 0) {
	            events.add(new MidiEvent(new MetaMessage(END_OF_TRACK), 0));
	            badEvent = event;
	            throw new ArrayIndexOutOfBoundsException("-1");
	        }
	        
	        byte[] lastEvent = events.get(events.size() - 1).getMessage().getMessage();
	        if (!Arrays.equals(END_OF_TRACK, lastEvent)) {
	            events.add(new MidiEvent(new MetaMessage(END_OF_TRACK), 0));
	        }
	        
	        if (events.contains(event)) {
	        	Log.i("USB MIDI Logger", "event not added 95");
	            return false;
	        } 
	        
	        /*
	         * events in the Track must take up position in ascending ticks
	         */
	        if (events.size() == 1) {
	            events.add(0, event);
	        	Log.i("USB MIDI Logger", "event added 104");
	        }

	        boolean added = false;
	        for (int i = 0; i < events.size() - 1; i++) {
	            if (events.get(i).getTick() <= event.getTick()) {
	                continue;
	            }
	            events.add(i, event);
	            added = true;
	        	Log.i("USB MIDI Logger", "event added 112");
	            break;
	        }
	        if (!added) {
	            events.add(events.size() - 1, event);
	        }
		}
        
        /*
         * method Track.ticks() return the biggest value of tick of all events
         * and save it even I remove event with the biggest values of tick
         */
        if (tick < event.getTick()) {
            tick = event.getTick();
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