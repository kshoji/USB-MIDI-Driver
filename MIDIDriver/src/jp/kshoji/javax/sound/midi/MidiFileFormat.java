package jp.kshoji.javax.sound.midi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the MIDI File Format.
 * 
 * @author K.Shoji
 */
public class MidiFileFormat {
	public static final int HEADER_MThd = 0x4d546864;
	public static final int HEADER_MTrk = 0x4d54726b;

	public static final int UNKNOWN_LENGTH = -1;

	protected int byteLength;
	protected float divisionType;
	protected long microsecondLength;
	protected int resolution;
	protected int type;
	private HashMap<String, Object> properties;

	public MidiFileFormat(int type, float divisionType, int resolution, int bytes, long microseconds) {
		this.type = type;
		this.divisionType = divisionType;
		this.resolution = resolution;
		this.byteLength = bytes;
		this.microsecondLength = microseconds;
		this.properties = new HashMap<String, Object>();
	}

	public MidiFileFormat(int type, float divisionType, int resolution, int bytes, long microseconds, Map<String, Object> properties) {
		this.type = type;
		this.divisionType = divisionType;
		this.resolution = resolution;
		this.byteLength = bytes;
		this.microsecondLength = microseconds;

		this.properties = new HashMap<String, Object>();
		this.properties.putAll(properties);
	}

	/**
	 * Get the length of {@link MidiFileFormat}
	 * 
	 * @return
	 */
	public int getByteLength() {
		return byteLength;
	}

	/**
	 * Get the division type of {@link MidiFileFormat}
	 * 
	 * @return
	 */
	public float getDivisionType() {
		return divisionType;
	}

	/**
	 * Get the length in microseconds of {@link MidiFileFormat}
	 * 
	 * @return
	 */
	public long getMicrosecondLength() {
		return microsecondLength;
	}

	/**
	 * Get property of {@link MidiFileFormat}
	 * 
	 * @param key
	 * @return
	 */
	public Object getProperty(String key) {
		return properties.get(key);
	}

	/**
	 * Get the resolution of {@link MidiFileFormat}
	 * 
	 * @return
	 */
	public int getResolution() {
		return resolution;
	}

	/**
	 * Get the type of {@link MidiFileFormat}
	 * 
	 * @return
	 */
	public int getType() {
		return type;
	}

	/**
	 * Get properties {@link Map} of {@link MidiFileFormat}
	 * 
	 * @return
	 */
	public Map<String, Object> properties() {
		return Collections.unmodifiableMap(properties);
	}
}
