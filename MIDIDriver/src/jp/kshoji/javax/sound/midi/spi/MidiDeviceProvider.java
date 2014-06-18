package jp.kshoji.javax.sound.midi.spi;

import jp.kshoji.javax.sound.midi.MidiDevice;

public abstract class MidiDeviceProvider {
    public MidiDeviceProvider() {

    }

    public abstract MidiDevice getDevice(MidiDevice.Info info) throws IllegalArgumentException;

    public abstract MidiDevice.Info[] getDeviceInfo();

    public boolean isDeviceSupported(MidiDevice.Info info) {
        MidiDevice.Info[] informationArray = getDeviceInfo();

        for (MidiDevice.Info information : informationArray) {
            if (info.equals(information)) {
                return true;
            }
        }
        
        return false;
    }
}
