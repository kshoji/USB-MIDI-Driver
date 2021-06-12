package jp.kshoji.unity.midi;

import java.util.HashMap;
import java.util.Locale;
import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.unity3d.player.UnityPlayer;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.driver.midi.util.UsbMidiDriver;

/**
 * USB MIDI Plugin for Unity
 * @author K.Shoji
 */
public class UsbMidiUnityPlugin {
    private static final String GAME_OBJECT_NAME = "MidiManager";

    private UsbMidiDriver usbMidiDriver;
    HashMap<String, MidiOutputDevice> midiOutputDeviceMap = new HashMap<>();

    public void initialize(Context context) {
        usbMidiDriver = new UsbMidiDriver(context) {
            @Override
            public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
                // deprecated method.
                // do nothing
            }

            @Override
            public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiInputDeviceAttached", midiInputDevice.getDeviceAddress());
            }

            @Override
            public void onMidiOutputDeviceAttached(@NonNull final MidiOutputDevice midiOutputDevice) {
                midiOutputDeviceMap.put(midiOutputDevice.getDeviceAddress(), midiOutputDevice);
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiOutputDeviceAttached", midiOutputDevice.getDeviceAddress());
            }

            @Override
            public void onDeviceDetached(@NonNull UsbDevice usbDevice) {
                // deprecated method.
                // do nothing
            }

            @Override
            public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiInputDeviceDetached", midiInputDevice.getDeviceAddress());
            }

            @Override
            public void onMidiOutputDeviceDetached(@NonNull final MidiOutputDevice midiOutputDevice) {
                midiOutputDeviceMap.remove(midiOutputDevice.getDeviceAddress());
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiOutputDeviceDetached", midiOutputDevice.getDeviceAddress());
            }

            private String serializeMidiMessage(String deviceAddress, int[] data) {
                StringBuilder sb = new StringBuilder(deviceAddress);
                for (int i = 0; i < data.length; i++) {
                    sb.append(String.format(Locale.ROOT, "%d", data[i]));
                    if (i == data.length - 1) {
                        return sb.toString();
                    }
                    sb.append(",");
                }
                return sb.toString();
            }

            private String serializeMidiMessage(String deviceAddress, int cable, byte[] data) {
                StringBuilder sb = new StringBuilder(deviceAddress);
                sb.append(",");
                sb.append(String.format(Locale.ROOT, "%d", cable));
                for (int i = 0; i < data.length; i++) {
                    sb.append(",");
                    sb.append(String.format(Locale.ROOT, "%d", data[i] & 0xff));
                    if (i == data.length - 1) {
                        return sb.toString();
                    }
                }
                return sb.toString();
            }

            private String serializeMidiMessage(String deviceAddress, int[] data1, byte[] data2) {
                StringBuilder sb = new StringBuilder(deviceAddress);
                for (int i = 0; i < data1.length; i++) {
                    sb.append(",");
                    sb.append(String.format(Locale.ROOT, "%d", data1[i]));
                }
                for (int i = 0; i < data2.length; i++) {
                    sb.append(",");
                    sb.append(String.format(Locale.ROOT, "%d", data2[i] & 0xff));
                }
                return sb.toString();
            }

            private String serializeMidiMessage(String deviceAddress, int[] data1, long data2) {
                StringBuilder sb = new StringBuilder(deviceAddress);
                for (int i = 0; i < data1.length; i++) {
                    sb.append(",");
                    sb.append(String.format(Locale.ROOT, "%d", data1[i]));
                }
                sb.append(",");
                sb.append(String.format(Locale.ROOT, "%d", data2));
                return sb.toString();
            }

            @Override
            public void onMidiNoteOff(@NonNull final MidiInputDevice sender, int cable, int channel, int note, int velocity) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiNoteOff", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, note, velocity}));
            }

            @Override
            public void onMidiNoteOn(@NonNull final MidiInputDevice sender, int cable, int channel, int note, int velocity) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiNoteOn", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, note, velocity}));
            }

            @Override
            public void onMidiPolyphonicAftertouch(@NonNull final MidiInputDevice sender, int cable, int channel, int note, int pressure) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiPolyphonicAftertouch", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, note, pressure}));
            }

            @Override
            public void onMidiControlChange(@NonNull final MidiInputDevice sender, int cable, int channel, int function, int value) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiControlChange", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, function, value}));
            }

            @Override
            public void onMidiProgramChange(@NonNull final MidiInputDevice sender, int cable, int channel, int program) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiProgramChange", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, program}));
            }

            @Override
            public void onMidiChannelAftertouch(@NonNull final MidiInputDevice sender, int cable, int channel, int pressure) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiChannelAftertouch", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, pressure}));
            }

            @Override
            public void onMidiPitchWheel(@NonNull final MidiInputDevice sender, int cable, int channel, int amount) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiPitchWheel", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, amount}));
            }

            @Override
            public void onMidiSystemExclusive(@NonNull final MidiInputDevice sender, int cable, final byte[] systemExclusive) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiSystemExclusive", serializeMidiMessage(sender.getDeviceAddress(), cable, systemExclusive));
            }

            @Override
            public void onMidiSystemCommonMessage(@NonNull final MidiInputDevice sender, int cable, final byte[] bytes) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiSystemCommonMessage", serializeMidiMessage(sender.getDeviceAddress(), cable, bytes));
            }

            @Override
            public void onMidiSingleByte(@NonNull final MidiInputDevice sender, int cable, int byte1) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiSingleByte", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, byte1}));
            }

            @Override
            public void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice sender, int cable, int timing) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiTimeCodeQuarterFrame", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, timing}));
            }

            @Override
            public void onMidiSongSelect(@NonNull MidiInputDevice sender, int cable, int song) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiSongSelect", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, song}));
            }

            @Override
            public void onMidiSongPositionPointer(@NonNull MidiInputDevice sender, int cable, int position) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiSongPositionPointer", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, position}));
            }

            @Override
            public void onMidiTuneRequest(@NonNull MidiInputDevice sender, int cable) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiTuneRequest", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiTimingClock(@NonNull MidiInputDevice sender, int cable) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiTimingClock", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiStart(@NonNull MidiInputDevice sender, int cable) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiStart", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiContinue(@NonNull MidiInputDevice sender, int cable) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiContinue", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiStop(@NonNull MidiInputDevice sender, int cable) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiStop", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiActiveSensing(@NonNull MidiInputDevice sender, int cable) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiActiveSensing", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiReset(@NonNull MidiInputDevice sender, int cable) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiReset", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiMiscellaneousFunctionCodes(@NonNull final MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiMiscellaneousFunctionCodes", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiCableEvents(@NonNull final MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiCableEvents", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }
        };
        usbMidiDriver.open();
    }

    public void terminate() {
        if (usbMidiDriver != null) {
            usbMidiDriver.close();
        }
    }

    /**
     * Obtains device name for deviceId
     * @param deviceId the device id
     * @return device name, product name, or null
     */
    public String getDeviceName(String deviceId) {
        MidiOutputDevice device = midiOutputDeviceMap.get(deviceId);
        if (device != null) {
            if (!TextUtils.isEmpty(device.getProductName())) {
                return device.getProductName();
            }
            if (!TextUtils.isEmpty(device.getUsbDevice().getDeviceName())) {
                return device.getUsbDevice().getDeviceName();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!TextUtils.isEmpty(device.getUsbDevice().getProductName())) {
                    return device.getUsbDevice().getProductName();
                }
            }
        }
        return null;
    }

    public void sendMidiNoteOn(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 5) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiNoteOn(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]));
        }
    }

    public void sendMidiNoteOff(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 5) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiNoteOff(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]));
        }
    }

    public void sendMidiPolyphonicAftertouch(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 5) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiPolyphonicAftertouch(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]));
        }
    }

    public void sendMidiControlChange(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 5) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiControlChange(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]));
        }
    }

    public void sendMidiProgramChange(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 4) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiProgramChange(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
        }
    }

    public void sendMidiChannelAftertouch(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 4) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiChannelAftertouch(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
        }
    }

    public void sendMidiPitchWheel(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 4) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiPitchWheel(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]));
        }
    }

    public void sendMidiSystemExclusive(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 1) {
            return;
        }
        byte[] sysEx = new byte[split.length - 1];
        for (int i = 0; i < split.length - 1; i++) {
            sysEx[i] = (byte) (Integer.parseInt(split[i + 1]) & 0xff);
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiSystemExclusive(Integer.parseInt(split[1]), sysEx);
        }
   }

    public void sendMidiSystemCommonMessage(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 1) {
            return;
        }
        byte[] message = new byte[split.length - 1];
        for (int i = 0; i < split.length - 1; i++) {
            message[i] = (byte) (Integer.parseInt(split[i + 1]) & 0xff);
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiSystemCommonMessage(Integer.parseInt(split[1]), message);
        }
    }

    public void sendMidiSingleByte(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 3) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiSingleByte(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        }
    }

    public void sendMidiTimeCodeQuarterFrame(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 3) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiTimeCodeQuarterFrame(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        }
    }

    public void sendMidiSongSelect(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 3) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiSongSelect(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        }
    }

    public void sendMidiSongPositionPointer(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 3) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiSongPositionPointer(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
        }
    }

    public void sendMidiTuneRequest(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 2) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiTuneRequest(Integer.parseInt(split[1]));
        }
    }

    public void sendMidiTimingClock(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 2) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiTimingClock(Integer.parseInt(split[1]));
        }
    }

    public void sendMidiStart(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 2) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiStart(Integer.parseInt(split[1]));
        }
    }

    public void sendMidiContinue(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 2) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiContinue(Integer.parseInt(split[1]));
        }
    }

    public void sendMidiStop(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 2) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiStop(Integer.parseInt(split[1]));
        }
    }

    public void sendMidiActiveSensing(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 2) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiActiveSensing(Integer.parseInt(split[1]));
        }
    }

    public void sendMidiReset(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 2) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiReset(Integer.parseInt(split[1]));
        }
    }

    public void sendMidiMiscellaneousFunctionCodes(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 5) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiMiscellaneousFunctionCodes(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]));
        }
    }

    public void sendMidiCableEvents(String serializedMidiMessage) {
        String[] split = serializedMidiMessage.split(",");
        if (split.length < 5) {
            return;
        }
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(split[0]);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiCableEvents(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3]), Integer.parseInt(split[4]));
        }
    }
}
