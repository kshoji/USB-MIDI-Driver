Android USB MIDI Driver
====

USB MIDI Driver using Android USB Host API

- No root needed.
- Supports the standard USB MIDI devices; like sequencers, or instruments.
- Supports some non-standard USB MIDI (but protocol is compatible with USB MIDI) devices.
    - YAMAHA, Roland, MOTU's devices can be connected(not been tested much).
- Supports multiple device connections.
- Has `javax.sound.midi` compatible classes.
    - See the [javax.sound.midi Documents](javax.sound.midi.md).

Requirement
----
- Android : OS version 3.1(API Level 12) or higher, and have an USB host port.
    - The android Linux kernel must support USB MIDI devices. Some Android device recognizes only USB-HID and USB-MSD by kernel configurations.
- USB MIDI (compatible) device

the optional thing:

- The self powered USB hub (if want to connect multiple USB MIDI devices).
- USB OTG cable (if the Android device has no standard USB-A port).
- USB MIDI <--> Lagacy MIDI(MIDI 1.0) converter cable (if want to connect with legacy MIDI instruments).

Device Connection
----
```
Android [USB A port]---(USB Hub)---[USB B port] USB MIDI Device
                                ---[USB B port] USB MIDI Device 
                                   ...
```

Repository Overview
====
- Library Project : `MIDIDriver`
    - The driver for connecting an USB MIDI device.

- Sample Project : `MIDIDriverSample`
    - The sample implementation of the synthesizer / MIDI event logger.
    - Pre-compiled sample project is available on [Google Play Market](https://play.google.com/store/apps/details?id=jp.kshoji.driver.midi.sample).

Library Project Usage
====

Project setup
----

- Clone the library project.
- Import the library project into Eclipse workspace, and build it.
- Create new Android Project. And add the library project to the project.
- Override `AbstractSingleMidiActivity` or `AbstractMultipleMidiActivity`.
    - `AbstractSingleMidiActivity` can connect only **one** MIDI device.
    - `AbstractMultipleMidiActivity` can connect **multiple** MIDI devices.
        - NOTE: The performance problem (slow latency or high CPU/memory usage) may occur if many devices have been connected.
- Modify the AndroidManifest.xml
    - Add "uses-feature" tag to use USB Host feature.
    - Activity's **launchMode** must be "singleTask".

```xml
<uses-feature android:name="android.hardware.usb.host" /> 

<application>
    <activity
        android:name=".MyMidiMainActivity"
        android:label="@string/app_name"
        android:launchMode="singleTask" >
        <intent-filter>
            <category android:name="android.intent.category.LAUNCHER" />
            <action android:name="android.intent.action.MAIN" />
        </intent-filter>
    </activity>
    :
```

MIDI event handling with AbstractSingleMidiActivity
----

MIDI event receiving:

- Implement the MIDI event handling method (named `"onMidi..."`) to receive MIDI events.

MIDI event sending:

- Call AbstractSingleMidiActivity's `getMidiOutputDevices()` method to get the instance of `MIDIOutputDevice`.
    - And call the instance's method (named `"sendMidi..."`) to send MIDI events.
        - NOTE: The first found USB MIDI device will be detected if multiple devices has been attached.
        - NOTE: If output endpoint doesn't connected, `getMidiOutputDevices()` method returns null.


MIDI event handling with AbstractMultipleMidiActivity
----

MIDI event receiving:

- Implement the MIDI event handling method (named `"onMidi..."`) to receive MIDI events.
    - The event sender object(MIDIInputDevice instance) will be set on the first argument.

MIDI event sending:

- Call the `getMidiOutputDevices()` method to get the instance of `Set<MIDIOutputDevice>`.
- Choose an instance from the `Set<MIDIOutputDevice>`.
    - And call the instance's method (named `"sendMidi..."`) to send MIDI events.


Supported MIDI events list
----

Some kind of messages don't be transferred frequently. So, the rare messages are not tested well.
Please add an [issue](https://github.com/kshoji/USB-MIDI-Driver/issues/new) If had trouble with using this library.

| Method name ends with..         | Meaning                                                           | Well tested?    |
|:----                            |:----                                                              |:----            |
| MidiMiscellaneousFunctionCodes  | Miscellaneous function codes. Reserved for future extensions.     | NO              |
| MidiCableEvents                 | Cable events. Reserved for future expansion.                      | NO              |
| MidiSystemCommonMessage         | System Common messages, or SysEx ends with following single byte. | NO              |
| MidiSystemExclusive             | SysEx                                                             | YES             |
| MidiNoteOff                     | Note-off                                                          | YES             |
| MidiNoteOn                      | Note-on                                                           | YES             |
| MidiPolyphonicAftertouch        | Poly-KeyPress                                                     | NO              |
| MidiControlChange               | Control Change                                                    | YES             |
| MidiProgramChange               | Program Change                                                    | YES             |
| MidiChannelAftertouch           | Channel Pressure                                                  | NO              |
| MidiPitchWheel                  | PitchBend Change                                                  | YES             |
| MidiSingleByte                  | Single Byte                                                       | NO              |


FAQ
----
- What is the 'cable' argument of `"onMidi..."` or `"sendMidi..."` method?
    - A single USB MIDI endpoint has multiple "virtual MIDI cables". 
    It's used for increasing the midi channels. The cable number's range is 0 to 15.


License
----
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
