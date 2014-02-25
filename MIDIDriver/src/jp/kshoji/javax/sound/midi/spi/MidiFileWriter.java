package jp.kshoji.javax.sound.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import jp.kshoji.javax.sound.midi.Sequence;

public abstract class MidiFileWriter {

	public abstract int[] getMidiFileTypes();

	public abstract int[] getMidiFileTypes(Sequence sequence);

	public boolean isFileTypeSupported(int fileType) {
		int[] supported = getMidiFileTypes();
		for (int element : supported) {
			if (fileType == element) {
				return true;
			}
		}
		return false;
	}

	public boolean isFileTypeSupported(int fileType, Sequence sequence) {
		int[] supported = getMidiFileTypes(sequence);
		for (int element : supported) {
			if (fileType == element) {
				return true;
			}
		}
		return false;
	}

	public abstract int write(Sequence sequence, int fileType, File file) throws IOException;

	public abstract int write(Sequence sequence, int fileType, OutputStream outputStream) throws IOException;
}
