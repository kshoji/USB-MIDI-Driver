Android USB MIDI Driver
====
[![Build Status](https://travis-ci.org/kshoji/USB-MIDI-Driver.svg?branch=master)](https://travis-ci.org/kshoji/USB-MIDI-Driver)
[![Android Arsenal](https://img.shields.io/badge/Android%20Arsenal-USB%20MIDI%20Driver-brightgreen.svg?style=flat)](https://android-arsenal.com/details/1/511)

USB MIDI Driver using Android USB Host API

- No root privilege needed.
- Supports the standard USB MIDI devices; like sequencers, or instruments.
- Supports some non-standard USB MIDI (but protocol is compatible with USB MIDI) devices.
    - YAMAHA, Roland, MOTU, or other makers' devices listed on [device_filter.xml](https://github.com/kshoji/USB-MIDI-Driver/blob/master/MIDIDriver/res/xml/device_filter.xml) can be connected.
- Supports multiple device connections.
- Has `javax.sound.midi` compatible classes.
    - See the [javax.sound.midi Documents](https://github.com/kshoji/USB-MIDI-Driver/wiki/javax.sound.midi-porting-for-Android).

Requirement
====
- Android : OS version 3.1(API Level 12) or higher, and have an USB host port.
    - The android Linux kernel must support USB MIDI devices. Some Android device recognizes only USB-HID and USB-MSD by kernel configurations.
- USB MIDI (compatible) device

the optional thing:

- The self powered USB hub (if want to connect multiple USB MIDI devices).
- USB OTG cable (if the Android device has no standard USB-A port).
- USB MIDI <--> Lagacy MIDI(MIDI 1.0) converter cable (if want to connect with legacy MIDI instruments).

Repository Overview
====
- Library Project : `MIDIDriver`
    - The driver for connecting an USB MIDI device.

- Sample Project : `MIDIDriverSample`
    - The sample implementation of the synthesizer / MIDI event logger.
    - Pre-compiled sample project is available on [Google Play Market](https://play.google.com/store/apps/details?id=jp.kshoji.driver.midi.sample).

Library Project Usages
====

See the [project wiki](https://github.com/kshoji/USB-MIDI-Driver/wiki) for the library usages.

FAQ
----
- What is the 'cable' argument of `"onMidi..."` or `"sendMidi..."` method?
    - A single USB MIDI endpoint has multiple "virtual MIDI cables". 
    It's used for increasing the midi channels. The cable number's range is 0 to 15.
- The application doesn't detect the device even if the USB MIDI device connected.
    - See the [Trouble shooting](https://github.com/kshoji/USB-MIDI-Driver/wiki/TroubleShooting-on-connecting-an-USB-MIDI-device) documents.

License
====
[Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
