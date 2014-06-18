package jp.kshoji.javax.sound.midi;

public interface MidiChannel {
    void allNotesOff();

    void allSoundOff();

    void controlChange(int controller, int value);

    int getChannelPressure();

    int getController(int controller);

    boolean getMono();

    boolean getMute();

    boolean getOmni();

    int getPitchBend();

    int getPolyPressure(int noteNumber);

    int getProgram();

    boolean getSolo();

    boolean localControl(boolean on);

    void noteOff(int noteNumber);

    void noteOff(int noteNumber, int velocity);

    void noteOn(int noteNumber, int velocity);

    void programChange(int program);

    void programChange(int bank, int program);

    void resetAllControllers();

    void setChannelPressure(int pressure);

    void setMono(boolean on);

    void setMute(boolean mute);

    void setOmni(boolean on);

    void setPitchBend(int bend);

    void setPolyPressure(int noteNumber, int pressure);

    void setSolo(boolean soloState);
}
