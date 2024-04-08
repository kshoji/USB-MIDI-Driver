-keep public class jp.kshoji.driver.midi.** {
    public protected *;
}
-keep public class jp.kshoji.driver.usb.util.** {
    public protected *;
}
-keep public class jp.kshoji.javax.sound.midi.** {
    public protected *;
}
-keep public class jp.kshoji.unity.midi.** {
    public protected *;
}

-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Signature,Exceptions,*Annotation*,
                InnerClasses,PermittedSubclasses,EnclosingMethod,
                Deprecated,SourceFile,LineNumberTable

-keepclassmembers,allowoptimization enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
