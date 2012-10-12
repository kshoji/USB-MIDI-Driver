Android USB MIDI Driver
====

USB MIDI Driver using Android USB Host API

- No root needed
- Supports the standard USB MIDI devices; like sequencers, or instruments.
- Supports some non-standard USB MIDI (but protocol is compatible with USB MIDI) devices.
 - YAMAHA, Roland, MOTU's devices can be connected(not been tested much).

Restriction
----
- Currently, this library can connect only one device.

Requirement
----
- Android : OS version 3.1(API Level 11) or higher, and have an USB host port.
- USB MIDI (compatible) device

Device Connection
----
    Android [USB A port]------[USB B port] USB MIDI Device

Projects
----
- Library Project  
 - MIDIDriver : The driver for connecting an USB MIDI device.

- Sample Project
 - MIDIDriverSample : The sample implementation of the synthesizer / MIDI event logger.

Library Project Usage
----
Project setup

- Clone the library project.
- Import the library project into Eclipse workspace, and build it.
- Create new Android Project. And add the library project to the project.
- Override `jp.kshoji.driver.midi.activity.AbstractMidiActivity`.
- Modify the AndroidManifest.xml file's activity tag.
 - Add **intent-filter** android.hardware.usb.action.USB_DEVICE_ATTACHED and **meta-data** to the overridden Activity.
 - Activity's **launchMode** must be "singleTask".
- 

        <activity
            android:name=".MyMidiMainActivity"
            android:label="@string/app_name"
            android:launchMode="singleTask" >
            <intent-filter>
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED" />
            </intent-filter>
    
            <meta-data
                android:name="android.hardware.usb.action.USB_DEVICE_ATTACHED"
                android:resource="@xml/device_filter" />
        </activity>


MIDI event receiving

- Implement the MIDI event handling method (named `"onMidi..."`) to receive MIDI events.

MIDI event sending

- Call AbstractMidiActivity's `getMidiOutputDevice()` method to get the instance on `MIDIOutputDevice`.
 - And call the instance's method (named `"onMidi..."`) to send MIDI events.

Use library with Maven
----
Maven 3.0.4 or later required for maven-android-plugin.
More infomation to build Android application with Maven, See the 'maven-android-plugin' project's wiki. http://code.google.com/p/maven-android-plugin/wiki/GettingStarted

- Create new Maven project with Eclipse
- Install "Android 3.1" dependency with maven, using this tool: https://github.com/mosabua/maven-android-sdk-deployer
- Edit `pom.xml` file for the created project, like below. (See also Sample Project's `pom.xml` file).
-
    
    <repositories>
        <repository>
            <id>midi-driver-snapshots</id>
            <url>http://github.com/kshoji/USB-MIDI-Driver/raw/master/snapshots</url>
        </repository>
    </repositories>
    
    <dependencies>
        <dependency>
            <groupId>jp.kshoji</groupId>
            <artifactId>midi-driver</artifactId>
            <version>${project.version}</version>
            <type>apklib</type>
        </dependency>
        
        <dependency>
            <groupId>android</groupId>
            <artifactId>android</artifactId>
            <version>3.1_r3</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

License
----
[Apache License, Version 2.0][Apache]
[Apache]: http://www.apache.org/licenses/LICENSE-2.0
