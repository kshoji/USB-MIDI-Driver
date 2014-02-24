package jp.kshoji.javax.sound.midi;

import java.util.Vector;

public class Sequence {
	public static final float PPQ = 0.0f;
	public static final float SMPTE_24 = 24.0f;
	public static final float SMPTE_25 = 25.0f;
	public static final float SMPTE_30 = 30.0f;
	public static final float SMPTE_30DROP = 29.969999313354492f;

	protected float divisionType;
	protected int resolution;
	protected Vector<Track> tracks;

	/**
	 * Check if the divisionType supported
	 * @param divisionType
	 * @return
	 */
	private static boolean isSupportingDivisionType(float divisionType) {
		return divisionType == Sequence.PPQ || divisionType == Sequence.SMPTE_24 || divisionType == Sequence.SMPTE_25 || divisionType == Sequence.SMPTE_30 || divisionType == Sequence.SMPTE_30DROP;
	}


	/**
	 * Create {@link Sequence} with divisionType and resolution.
	 * 
	 * @param divisionType {@link #PPQ}, {@link #SMPTE_24}, {@link #SMPTE_25}, {@link #SMPTE_30DROP}, or {@link #SMPTE_30}.
	 * @param resolution
	 * <ul>
	 * 	<li>divisionType == {@link #PPQ} : 0 - 0x7fff. typically 24, 480</li>
	 * 	<li>divisionType == {@link #SMPTE_24}, {@link #SMPTE_25}, {@link #SMPTE_30DROP}, {@link #SMPTE_30} : 0 - 0xff</li>
	 * </ul>
	 * @throws InvalidMidiDataException
	 */
	public Sequence(float divisionType, int resolution) throws InvalidMidiDataException {
		if (isSupportingDivisionType(divisionType) == false) {
			throw new InvalidMidiDataException("Unsupported division type: " + divisionType);
		}
		this.divisionType = divisionType;
		this.resolution = resolution;
		this.tracks = new Vector<Track>();

	}

	/**
	 * Create {@link Sequence} with divisionType, resolution and numberOfTracks.
	 * 
	 * @param divisionType {@link #PPQ}, {@link #SMPTE_24}, {@link #SMPTE_25}, {@link #SMPTE_30DROP}, or {@link #SMPTE_30}.
	 * @param resolution
	 * <ul>
	 * 	<li>divisionType == {@link #PPQ} : 0 - 0x7fff. typically 24, 480</li>
	 * 	<li>divisionType == {@link #SMPTE_24}, {@link #SMPTE_25}, {@link #SMPTE_30DROP}, {@link #SMPTE_30} : 0 - 0xff</li>
	 * </ul>
	 * @param numberOfTracks > 0
	 * @throws InvalidMidiDataException
	 */
	public Sequence(float divisionType, int resolution, int numberOfTracks) throws InvalidMidiDataException {
		if (isSupportingDivisionType(divisionType) == false) {
			throw new InvalidMidiDataException("Unsupported division type: " + divisionType);
		}
		this.divisionType = divisionType;
		this.resolution = resolution;
		this.tracks = new Vector<Track>();
		if (numberOfTracks > 0) {
			for (int i = 0; i < numberOfTracks; i++) {
				tracks.add(new Track());
			}
		}
	}

	/**
	 * Create empty {@link Track}
	 * 
	 * @return
	 */
	public Track createTrack() {
		/*
		 * new Tracks accrue to the end of vector
		 */
		Track track = new Track();
		tracks.add(track);
		return track;
	}

	/**
	 * Delete specified {@link Track}
	 * 
	 * @param track to delete
	 * @return
	 */
	public boolean deleteTrack(Track track) {
		return tracks.remove(track);
	}

	/**
	 * Get the divisionType of the {@link Sequence}
	 * 
	 * @return
	 */
	public float getDivisionType() {
		return divisionType;
	}

	/**
	 * Get the {@link Sequence} length in microseconds
	 * 
	 * @return {@link Sequence} length in microseconds
	 */
	public long getMicrosecondLength() {
		return (long) (1000000.0f * getTickLength() / ((this.divisionType == 0.0f ? 2 : this.divisionType) * this.resolution * 1.0f));
	}

	/**
	 * Get the resolution
	 * 
	 * @return resolution
	 */
	public int getResolution() {
		return resolution;
	}

	/**
	 * Get the biggest tick length
	 * 
	 * @return tick
	 */
	public long getTickLength() {
		/*
		 * this method return the biggest value of tick of all tracks contain in the Sequence
		 */
		long maxTick = 0;
		for (int i = 0; i < tracks.size(); i++) {
			maxTick = Math.max(maxTick, tracks.get(i).ticks());
		}
		return maxTick;
	}

	/**
	 * Get the array of {@link Track}s
	 * @return array of tracks
	 */
	public Track[] getTracks() {
		Track[] track = new Track[tracks.size()];
		tracks.toArray(track);
		return track;
	}
}
