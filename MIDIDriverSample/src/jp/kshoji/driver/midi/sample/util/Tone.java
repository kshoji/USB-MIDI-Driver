package jp.kshoji.driver.midi.sample.util;

/**
 * Holder for tone information
 * 
 * @author K.Shoji
 */
public class Tone {
	public static final int FORM_SIN = 0;
	public static final int FORM_SAW = 1;
	public static final int FORM_SQUARE = 2;
	public static final int FORM_TRIANGLE = 3;
	public static final int FORM_MAX = 4;

	private double frequency;
	private double volume;
	private int form;
	private int note;
	
	private double generateTone(int src) {
		return 440.0 * Math.pow(2.0, (src - 69) / 12.0);
	}
	
	public Tone(int note, double volume, int form) {
		this.frequency = generateTone(note);
		this.volume = volume;
		this.form = form;
		this.note = note;
	}
	
	/**
	 * @param frequency the frequency to set
	 */
	public void setFrequency(double frequency) {
		this.frequency = frequency;
	}
	
	/**
	 * @return the frequency
	 */
	public double getFrequency() {
		return frequency;
	}
	
	/**
	 * @param volume the volume to set
	 */
	public void setVolume(double volume) {
		this.volume = volume;
	}
	
	/**
	 * @return the volume
	 */
	public double getVolume() {
		return volume;
	}
	
	/**
	 * @param form the form to set
	 */
	public void setForm(int form) {
		this.form = form;
	}
	
	/**
	 * @return the form
	 */
	public int getForm() {
		return form;
	}
	
	/**
	 * @param note the note to set
	 */
	public void setNote(int note) {
		this.note = note;
	}
	
	/**
	 * @return the note
	 */
	public int getNote() {
		return note;
	}
}
