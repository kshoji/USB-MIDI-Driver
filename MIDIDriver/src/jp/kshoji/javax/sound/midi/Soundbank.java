package jp.kshoji.javax.sound.midi;

public interface Soundbank {
    String getDescription();

    Instrument getInstrument(Patch patch);

    Instrument[] getInstruments();

    String getName();

    SoundbankResource[] getResources();

    String getVendor();

    String getVersion();
}
