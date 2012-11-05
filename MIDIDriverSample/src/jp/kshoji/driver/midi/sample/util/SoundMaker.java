package jp.kshoji.driver.midi.sample.util;

import java.util.Set;

/**
 * Simple sound synthesizer
 * 
 * @author K.Shoji
 */
public class SoundMaker {
	private static final SoundMaker instance;

	private static double sinTable[] = new double[1024];
	private static double sawTable[] = new double[1024];
	private static double triangleTable[] = new double[1024];
	private static double squareTable[] = new double[1024];

	private static int iter = 0;

	private static final int samplingRate = 44100 / 2;

	/**
	 * Initializes wave table
	 */
	static {
		for (int i = 0; i < 1024; i++) {
			sawTable[i] = (i / 512.0) - 1.0;
			if (i < 256) {
				triangleTable[i] = (double) i / 256;
			} else if (i < 768) {
				triangleTable[i] = (512.0 - i) / 256.0;
			} else {
				triangleTable[i] = (i - 768.0) / 256.0;
			}
			squareTable[i] = Math.sin(Math.PI * i / 512.0) > 0.0 ? 1.0 : -1.0;
			sinTable[i] = Math.sin(Math.PI * i / 512.0);
		}
		instance = new SoundMaker();
	}

	public static SoundMaker getInstance() {
		return instance;
	}

	public int getSamplingRate() {
		return samplingRate;
	}

	/**
	 * @return -1.0 to 1.0
	 */
	public double makeWaveStream(Set<Tone> tones) {
		iter++;

		double output = 0.0;
		for (Tone tone : tones) {
			switch (tone.getForm()) {
			case Tone.FORM_SIN:
				output += sinTable[(int) (1024 * tone.getFrequency() / samplingRate * iter) % 1024] * tone.getVolume();
				break;
			case Tone.FORM_SAW:
				output += sawTable[(int) (1024 * tone.getFrequency() / samplingRate * iter) % 1024] * tone.getVolume();
				break;
			case Tone.FORM_SQUARE:
				output += squareTable[(int) (1024 * tone.getFrequency() / samplingRate * iter) % 1024] * tone.getVolume();
				break;
			case Tone.FORM_TRIANGLE:
				output += triangleTable[(int) (1024 * tone.getFrequency() / samplingRate * iter) % 1024] * tone.getVolume();
				break;
			default:
				// do nothing.
				break;
			}
		}

		return output;
	}

}
