package jp.kshoji.javax.sound.midi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

	public int getByteLength() {
		return byteLength;
	}

	public float getDivisionType() {
		return divisionType;
	}

	public long getMicrosecondLength() {
		return microsecondLength;
	}

	public Object getProperty(String key) {
		return properties.get(key);
	}

	public int getResolution() {
		return resolution;
	}

	public int getType() {
		return type;
	}

	public Map<String, Object> properties() {
		return Collections.unmodifiableMap(properties);
	}
}
