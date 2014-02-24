package jp.kshoji.javax.sound.midi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents MIDI Track
 * 
 * @author K.Shoji
 */
public class Track {
	static final byte[] END_OF_TRACK = new byte[] { -1, 47, 0 };

	final List<MidiEvent> events = new ArrayList<MidiEvent>();

	/**
	 * {@link Comparator} for MIDI data sorting
	 */
	static final Comparator<MidiEvent> midiEventComparator = new Comparator<MidiEvent>() {
		@Override
		public int compare(MidiEvent lhs, MidiEvent rhs) {
			// sort by tick
			int tickDifference = (int) (lhs.getTick() - rhs.getTick());
			if (tickDifference != 0) {
				return tickDifference;
			}

			// same timing
			// sort by the MIDI data priority order, as:
			// system message > control messages > note on > note off
			// swap the priority of note on, and note off
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

			return -(lhsInt - rhsInt);
		}
	};

	/**
	 * Utilities for {@link Track}
	 * 
	 * @author K.Shoji
	 */
	public static class TrackUtils {
		/**
		 * Merge the specified {@link Sequencer}'s {@link Track}s into one {@link Track}
		 * 
		 * @param sequencer
		 * @param recordEnable
		 * @return merged {@link Sequence}
		 * @throws InvalidMidiDataException
		 */
		public static Track mergeSequenceToTrack(Sequencer sequencer, Map<Track, Set<Integer>> recordEnable) throws InvalidMidiDataException {
			Sequence sourceSequence = sequencer.getSequence();
			Track mergedTrack = new Track();

			// apply track mute and solo
			Track[] tracks = sourceSequence.getTracks();
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
				if (sequencer.isRecording() && (recordEnable.get(tracks[trackIndex]) != null && recordEnable.get(tracks[trackIndex]).size() > 0)) {
					// currently recording track, ignore
					continue;
				}

				mergedTrack.events.addAll(tracks[trackIndex].events);
			}

			sortEvents(mergedTrack);

			return mergedTrack;
		}

		/**
		 * Sort the {@link Track}'s {@link MidiEvent}, order by tick and events
		 * 
		 * @param track
		 */
		public static void sortEvents(Track track) {
			synchronized (track.events) {
				// sort the events
				Collections.sort(track.events, midiEventComparator);

				// remove all of END_OF_TRACK
				List<MidiEvent> filtered = new ArrayList<MidiEvent>();
				for (MidiEvent event : track.events) {
					if (!Arrays.equals(END_OF_TRACK, event.getMessage().getMessage())) {
						filtered.add(event);
					}
				}
				track.events.clear();
				track.events.addAll(filtered);

				// add END_OF_TRACK to last
				if (track.events.size() == 0) {
					track.events.add(new MidiEvent(new MetaMessage(END_OF_TRACK), 0));
				} else {
					track.events.add(new MidiEvent(new MetaMessage(END_OF_TRACK), track.events.get(track.events.size() - 1).getTick() + 1));
				}
			}
		}
	}

	/**
	 * Add {@link MidiEvent} to this {@link Track}
	 * 
	 * @param event
	 *            to add
	 * @return
	 */
	public boolean add(MidiEvent event) {
		synchronized (events) {
			events.add(event);
		}

		return true;
	}

	/**
	 * Get specified index of {@link MidiEvent}
	 * 
	 * @param index
	 * @return
	 * @throws ArrayIndexOutOfBoundsException
	 */
	public MidiEvent get(int index) throws ArrayIndexOutOfBoundsException {
		synchronized (events) {
			if (index < 0 || index >= events.size()) {
				throw new ArrayIndexOutOfBoundsException("Index: " + index + ", Size: " + events.size());
			}
			return events.get(index);
		}
	}

	/**
	 * Remove {@link MidiEvent} from this {@link Track}
	 * 
	 * @param event
	 *            to remove
	 * @return true if the event was removed
	 */
	public boolean remove(MidiEvent event) {
		synchronized (events) {
			/*
			 * method Track.ticks() always return the biggest value that ever has been in the Track; so only when Track is empty Track.ticks() return 0
			 */
			if (events.remove(event)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Get the number of events in the {@link Track}
	 * 
	 * @return
	 */
	public int size() {
		return events.size();
	}

	/**
	 * Get length of ticks for this {@link Track}
	 * 
	 * @return
	 */
	public long ticks() {
		TrackUtils.sortEvents(this);

		synchronized (events) {
			if (events.size() == 0) {
				return 0L;
			}

			return events.get(events.size() - 1).getTick();
		}
	}
}