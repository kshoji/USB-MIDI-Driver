javax.sound.midi porting for Android
====================================

- This branch is just 'work in progress'.
- Does not work yet.

Branch goal
-----------

- Provides USB MIDI driver as `javax.sound.midi` package.

How to use
==========

Almost same as using the original `javax.sound.midi`, except for calling `MidiSystem.initialize(Context)` / `MidiSystem.terminate()` methods.

- Initialize library 
    - `MidiSystem.initialize(Context)` may called at `Activity#onCreate()` , or somewhere.
    - Then, application starts device polling.

```java
@Override
protected void onCreate() {
    super.onCreate();

    MidiSystem.initialize(MyActivity.this);
}
```

- Get `MidiDevice.Info` instances from `MidiSystem`.
- Get `MidiDevice` instance from `MidiSystem`.
- Open the device.

```java
MidiDeviceInfo[] informations = MidiSystem.getMidiDeviceInfo();

if (informations != null && informations.length > 0) {
    MidiDevice device = MidiSystem.getMidiDevice(informations[0]);

    try {
        device.open();
    } catch (MidiUnavailableException mue) {
        // TODO
    }
}
```
- Using `MidiDevice`
    - Get `Receiver` / `Transmitter` instance from `MidiDevice`
    - Receiver can send `MidiMessage`.
    - Transmitter can attach a `Receiver` instance, and can receive `MidiMessage` with `send(MidiMessage)` method.

```java
try {
    // get the receiver
    Receiver receiver = device.getReceiver();
    // send something..
    receiver.send(new ShortMessage());

    // get the transmitter
    Transmitter transmitter = device.getTransmitter();
    // start to receive
    transmitter.setReceiver(new Receiver() {
        void send(MidiMessage message, long timeStamp) {
            Log.i("MIDI", "received:" + Arrays.toString(message.getMessage()));
        }

        void close() {
            Log.i("MIDI", "transmitter closed");
        }
    });
} catch (MidiUnavailableException mue) {
    // TODO
}
```
- clean up `MidiDevice` instance

```java
device.close();
```
- Finish using `MidiSystem`
    - `MidiSystem.terminate()` may called at `Activity#onDestroy()`;
    - This method closes all unclosed devices, and the application ends device polling.

```java
@Override
protected void onDestroy() {
    super.onDestroy();

    MidiSystem.terminate();
}
```

Additional Function
===================
- `UsbMidiReceiver`
    - `setCableId(int)` / `getCableId()` to set the cable ID. The default cable ID is 0.

Unsupported APIs
================

Methods related these classes are **not supported**.

- javax.sound.midi.MidiFileFormat
- javax.sound.midi.Sequence
- javax.sound.midi.Sequencer
- javax.sound.midi.Soundbank
- javax.sound.midi.Synthesizer

Time-stamping is not supported. 
- `Receiver#send(MidiMessage message, long timeStamp)` method's `timeStamp` argument is **always ignored**. (When called by the library, `timeStamp` will be set as -1).
