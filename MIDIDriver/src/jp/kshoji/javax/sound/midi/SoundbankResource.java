package jp.kshoji.javax.sound.midi;

public abstract class SoundbankResource {
    private final Soundbank soundbank;
    private final String name;
    private final Class<?> dataClass;

    protected SoundbankResource(Soundbank soundbank, String name, Class<?> dataClass) {
        this.soundbank = soundbank;
        this.name = name;
        this.dataClass = dataClass;
    }

    public abstract Object getData();

    public Class<?> getDataClass() {
        return dataClass;
    }

    public String getName() {
        return name;
    }

    public Soundbank getSoundbank() {
        return soundbank;
    }
}
