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
	private Vector<Patch> patches;

	/**
	 * Create {@link Sequence} with divisionType and resolution.
	 * 
	 * @param divisionType
	 * @param resolution
	 * @throws InvalidMidiDataException
	 */
	public Sequence(float divisionType, int resolution) throws InvalidMidiDataException {
		if (divisionType != Sequence.PPQ && divisionType != Sequence.SMPTE_24 && divisionType != Sequence.SMPTE_25 && divisionType != Sequence.SMPTE_30 && divisionType != Sequence.SMPTE_30DROP) {
			throw new InvalidMidiDataException("Unsupported division type: " + divisionType);
		}
		this.divisionType = divisionType;
		this.resolution = resolution;
		this.tracks = new Vector<Track>();
		this.patches = new Vector<Patch>();

	}

	/**
	 * Create {@link Sequence} with divisionType, resolution and numberOfTracks.
	 * 
	 * @param divisionType
	 * @param resolution
	 * @param numberOfTracks
	 * @throws InvalidMidiDataException
	 */
	public Sequence(float divisionType, int resolution, int numberOfTracks) throws InvalidMidiDataException {
		if (divisionType != Sequence.PPQ && divisionType != Sequence.SMPTE_24 && divisionType != Sequence.SMPTE_25 && divisionType != Sequence.SMPTE_30 && divisionType != Sequence.SMPTE_30DROP) {
			throw new InvalidMidiDataException("Unsupported division type: " + divisionType);
		}
		this.divisionType = divisionType;
		this.resolution = resolution;
		this.patches = new Vector<Patch>();
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
		Track tr = new Track();
		tracks.add(tr);
		return tr;
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

	public long getMicrosecondLength() {
		return (long) (1000000.0f * getTickLength() / ((this.divisionType == 0.0f ? 2 : this.divisionType) * this.resolution * 1.0f));
	}

	/**
	 * Get the array of {@link Patch}es
	 * 
	 * @return always empty array
	 */
	public Patch[] getPatchList() {
		// FIXME
		/*
		 * I don't understand how to works this method, and so I simply return an empty array. 'patches' initializes in the constructor as empty vector
		 */
		Patch[] patch = new Patch[patches.size()];
		patches.toArray(patch);
		return patch;
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
