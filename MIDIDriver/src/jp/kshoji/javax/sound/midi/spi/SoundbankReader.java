package jp.kshoji.javax.sound.midi.spi;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.Soundbank;

public abstract class SoundbankReader {
    public SoundbankReader() {

    }

    public abstract Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException;

    public abstract Soundbank getSoundbank(InputStream stream) throws InvalidMidiDataException, IOException;

    public abstract Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException;
}
