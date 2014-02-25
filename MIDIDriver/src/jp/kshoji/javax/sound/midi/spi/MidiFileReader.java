package jp.kshoji.javax.sound.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.MidiFileFormat;
import jp.kshoji.javax.sound.midi.Sequence;

public abstract class MidiFileReader {

	public abstract MidiFileFormat getMidiFileFormat(File file) throws InvalidMidiDataException, IOException;

	public abstract MidiFileFormat getMidiFileFormat(InputStream stream) throws InvalidMidiDataException, IOException;

	public abstract MidiFileFormat getMidiFileFormat(URL url) throws InvalidMidiDataException, IOException;

	public abstract Sequence getSequence(File file) throws InvalidMidiDataException, IOException;

	public abstract Sequence getSequence(InputStream stream) throws InvalidMidiDataException, IOException;

	public abstract Sequence getSequence(URL url) throws InvalidMidiDataException, IOException;

}
