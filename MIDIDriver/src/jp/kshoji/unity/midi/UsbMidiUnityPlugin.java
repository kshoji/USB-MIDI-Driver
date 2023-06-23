package jp.kshoji.unity.midi;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.unity3d.player.UnityPlayer;

import java.util.HashMap;
import java.util.Locale;

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
    HashMap<String, MidiInputDevice> midiInputDeviceMap = new HashMap<>();

    OnUsbMidiDeviceConnectionListener onMidiDeviceConnectionListener;
    OnUsbMidiInputEventListener onMidiInputEventListener;

    public void initialize(Context context, OnUsbMidiDeviceConnectionListener onMidiDeviceDetachedListener, OnUsbMidiInputEventListener onMidiInputEventListener)
    {
        this.onMidiDeviceConnectionListener = onMidiDeviceDetachedListener;
        this.onMidiInputEventListener = onMidiInputEventListener;
        initialize(context);
    }

    public void initialize(Context context) {
        usbMidiDriver = new UsbMidiDriver(context) {
            @Override
            public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
                // deprecated method.
                // do nothing
            }

            @Override
            public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {
                midiInputDeviceMap.put(midiInputDevice.getDeviceAddress(), midiInputDevice);
                if (onMidiDeviceConnectionListener != null) {
                    onMidiDeviceConnectionListener.onMidiInputDeviceAttached(midiInputDevice.getDeviceAddress());
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiInputDeviceAttached", midiInputDevice.getDeviceAddress());
            }

            @Override
            public void onMidiOutputDeviceAttached(@NonNull final MidiOutputDevice midiOutputDevice) {
                midiOutputDeviceMap.put(midiOutputDevice.getDeviceAddress(), midiOutputDevice);
                if (onMidiDeviceConnectionListener != null) {
                    onMidiDeviceConnectionListener.onMidiOutputDeviceAttached(midiOutputDevice.getDeviceAddress());
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiOutputDeviceAttached", midiOutputDevice.getDeviceAddress());
            }

            @Override
            public void onDeviceDetached(@NonNull UsbDevice usbDevice) {
                // deprecated method.
                // do nothing
            }

            @Override
            public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {
                midiInputDeviceMap.remove(midiInputDevice.getDeviceAddress());
                if (onMidiDeviceConnectionListener != null) {
                    onMidiDeviceConnectionListener.onMidiInputDeviceDetached(midiInputDevice.getDeviceAddress());
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiInputDeviceDetached", midiInputDevice.getDeviceAddress());
            }

            @Override
            public void onMidiOutputDeviceDetached(@NonNull final MidiOutputDevice midiOutputDevice) {
                midiOutputDeviceMap.remove(midiOutputDevice.getDeviceAddress());
                if (onMidiDeviceConnectionListener != null) {
                    onMidiDeviceConnectionListener.onMidiOutputDeviceDetached(midiOutputDevice.getDeviceAddress());
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiOutputDeviceDetached", midiOutputDevice.getDeviceAddress());
            }

            private String serializeMidiMessage(String deviceAddress, int[] data) {
                StringBuilder sb = new StringBuilder(deviceAddress.replaceAll(",", "_"));
                for (int i = 0; i < data.length; i++) {
                    sb.append(",");
                    sb.append(String.format(Locale.ROOT, "%d", data[i]));
                }
                return sb.toString();
            }

            private String serializeMidiMessage(String deviceAddress, int cable, byte[] data) {
                StringBuilder sb = new StringBuilder(deviceAddress.replaceAll(",", "_"));
                sb.append(",");
                sb.append(String.format(Locale.ROOT, "%d", cable));
                for (int i = 0; i < data.length; i++) {
                    sb.append(",");
                    sb.append(String.format(Locale.ROOT, "%d", data[i] & 0xff));
                }
                return sb.toString();
            }

            @Override
            public void onMidiNoteOff(@NonNull final MidiInputDevice sender, int cable, int channel, int note, int velocity) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiNoteOff(sender.getDeviceAddress(), cable, channel, note, velocity);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiNoteOff", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, note, velocity}));
            }

            @Override
            public void onMidiNoteOn(@NonNull final MidiInputDevice sender, int cable, int channel, int note, int velocity) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiNoteOn(sender.getDeviceAddress(), cable, channel, note, velocity);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiNoteOn", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, note, velocity}));
            }

            @Override
            public void onMidiPolyphonicAftertouch(@NonNull final MidiInputDevice sender, int cable, int channel, int note, int pressure) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiPolyphonicAftertouch(sender.getDeviceAddress(), cable, channel, note, pressure);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiPolyphonicAftertouch", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, note, pressure}));
            }

            @Override
            public void onMidiControlChange(@NonNull final MidiInputDevice sender, int cable, int channel, int function, int value) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiControlChange(sender.getDeviceAddress(), cable, channel, function, value);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiControlChange", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, function, value}));
            }

            @Override
            public void onMidiProgramChange(@NonNull final MidiInputDevice sender, int cable, int channel, int program) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiProgramChange(sender.getDeviceAddress(), cable, channel, program);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiProgramChange", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, program}));
            }

            @Override
            public void onMidiChannelAftertouch(@NonNull final MidiInputDevice sender, int cable, int channel, int pressure) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiChannelAftertouch(sender.getDeviceAddress(), cable, channel, pressure);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiChannelAftertouch", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, pressure}));
            }

            @Override
            public void onMidiPitchWheel(@NonNull final MidiInputDevice sender, int cable, int channel, int amount) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiPitchWheel(sender.getDeviceAddress(), cable, channel, amount);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiPitchWheel", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, channel, amount}));
            }

            @Override
            public void onMidiSystemExclusive(@NonNull final MidiInputDevice sender, int cable, final byte[] systemExclusive) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiSystemExclusive(sender.getDeviceAddress(), cable, systemExclusive);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiSystemExclusive", serializeMidiMessage(sender.getDeviceAddress(), cable, systemExclusive));
            }

            @Override
            public void onMidiSystemCommonMessage(@NonNull final MidiInputDevice sender, int cable, final byte[] bytes) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiSystemCommonMessage(sender.getDeviceAddress(), cable, bytes);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiSystemCommonMessage", serializeMidiMessage(sender.getDeviceAddress(), cable, bytes));
            }

            @Override
            public void onMidiSingleByte(@NonNull final MidiInputDevice sender, int cable, int byte1) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiSingleByte(sender.getDeviceAddress(), cable, byte1);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiSingleByte", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, byte1}));
            }

            @Override
            public void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice sender, int cable, int timing) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiTimeCodeQuarterFrame(sender.getDeviceAddress(), cable, timing);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiTimeCodeQuarterFrame", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, timing}));
            }

            @Override
            public void onMidiSongSelect(@NonNull MidiInputDevice sender, int cable, int song) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiSongSelect(sender.getDeviceAddress(), cable, song);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiSongSelect", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, song}));
            }

            @Override
            public void onMidiSongPositionPointer(@NonNull MidiInputDevice sender, int cable, int position) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiSongPositionPointer(sender.getDeviceAddress(), cable, position);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiSongPositionPointer", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, position}));
            }

            @Override
            public void onMidiTuneRequest(@NonNull MidiInputDevice sender, int cable) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiTuneRequest(sender.getDeviceAddress(), cable);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiTuneRequest", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiTimingClock(@NonNull MidiInputDevice sender, int cable) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiTimingClock(sender.getDeviceAddress(), cable);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiTimingClock", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiStart(@NonNull MidiInputDevice sender, int cable) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiStart(sender.getDeviceAddress(), cable);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiStart", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiContinue(@NonNull MidiInputDevice sender, int cable) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiContinue(sender.getDeviceAddress(), cable);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiContinue", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiStop(@NonNull MidiInputDevice sender, int cable) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiStop(sender.getDeviceAddress(), cable);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiStop", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiActiveSensing(@NonNull MidiInputDevice sender, int cable) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiActiveSensing(sender.getDeviceAddress(), cable);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiActiveSensing", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiReset(@NonNull MidiInputDevice sender, int cable) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiReset(sender.getDeviceAddress(), cable);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiReset", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable}));
            }

            @Override
            public void onMidiMiscellaneousFunctionCodes(@NonNull final MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiMiscellaneousFunctionCodes(sender.getDeviceAddress(), cable, byte1, byte2, byte3);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiMiscellaneousFunctionCodes", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, byte1, byte2, byte3}));
            }

            @Override
            public void onMidiCableEvents(@NonNull final MidiInputDevice sender, int cable, int byte1, int byte2, int byte3) {
                if (onMidiInputEventListener != null) {
                    onMidiInputEventListener.onMidiCableEvents(sender.getDeviceAddress(), cable, byte1, byte2, byte3);
                    return;
                }
                UnityPlayer.UnitySendMessage(GAME_OBJECT_NAME, "OnMidiCableEvents", serializeMidiMessage(sender.getDeviceAddress(), new int[] {cable, byte1, byte2, byte3}));
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
        MidiOutputDevice outputDevice = midiOutputDeviceMap.get(deviceId);
        if (outputDevice != null) {
            if (!TextUtils.isEmpty(outputDevice.getProductName())) {
                return outputDevice.getProductName();
            }
            if (!TextUtils.isEmpty(outputDevice.getUsbDevice().getDeviceName())) {
                return outputDevice.getUsbDevice().getDeviceName();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!TextUtils.isEmpty(outputDevice.getUsbDevice().getProductName())) {
                    return outputDevice.getUsbDevice().getProductName();
                }
            }
        }

        MidiInputDevice inputDevice = midiInputDeviceMap.get(deviceId);
        if (inputDevice != null) {
            if (!TextUtils.isEmpty(inputDevice.getProductName())) {
                return inputDevice.getProductName();
            }
            if (!TextUtils.isEmpty(inputDevice.getUsbDevice().getDeviceName())) {
                return inputDevice.getUsbDevice().getDeviceName();
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (!TextUtils.isEmpty(inputDevice.getUsbDevice().getProductName())) {
                    return inputDevice.getUsbDevice().getProductName();
                }
            }
        }

        return null;
    }

    /**
     * Obtains device vendor id for deviceId
     * @param deviceId the device id
     * @return device vendor id, or null
     */
    public String getVendorId(String deviceId) {
        MidiOutputDevice outputDevice = midiOutputDeviceMap.get(deviceId);
        if (outputDevice != null) {
            return String.valueOf(outputDevice.getUsbDevice().getVendorId());
        }

        MidiInputDevice inputDevice = midiInputDeviceMap.get(deviceId);
        if (inputDevice != null) {
            return String.valueOf(inputDevice.getUsbDevice().getVendorId());
        }

        return null;
    }

    /**
     * Obtains device product id for deviceId
     * @param deviceId the device id
     * @return device product id, or null
     */
    public String getProductId(String deviceId) {
        MidiOutputDevice outputDevice = midiOutputDeviceMap.get(deviceId);
        if (outputDevice != null) {
            return String.valueOf(outputDevice.getUsbDevice().getProductId());
        }

        MidiInputDevice inputDevice = midiInputDeviceMap.get(deviceId);
        if (inputDevice != null) {
            return String.valueOf(inputDevice.getUsbDevice().getProductId());
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
    public void sendMidiNoteOn(String deviceId, int cable, int channel, int note, int velocity) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiNoteOn(cable, channel, note, velocity);
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
    public void sendMidiNoteOff(String deviceId, int cable, int channel, int note, int velocity) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiNoteOff(cable, channel, note, velocity);
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
    public void sendMidiPolyphonicAftertouch(String deviceId, int cable, int channel, int note, int pressure) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiPolyphonicAftertouch(cable, channel, note, pressure);
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
    public void sendMidiControlChange(String deviceId, int cable, int channel, int function, int value) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiControlChange(cable, channel, function, value);
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
    public void sendMidiProgramChange(String deviceId, int cable, int channel, int program) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiProgramChange(cable, channel, program);
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
    public void sendMidiChannelAftertouch(String deviceId, int cable, int channel, int pressure) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiChannelAftertouch(cable, channel, pressure);
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
    public void sendMidiPitchWheel(String deviceId, int cable, int channel, int amount) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiPitchWheel(cable, channel, amount);
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
    public void sendMidiSystemExclusive(String deviceId, int cable, byte[] systemExclusive) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiSystemExclusive(cable, systemExclusive);
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
    public void sendMidiSystemCommonMessage(String deviceId, int cable, byte[] bytes) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiSystemCommonMessage(cable, bytes);
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
    public void sendMidiSingleByte(String deviceId, int cable, int byte1) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiSingleByte(cable, byte1);
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
    public void sendMidiTimeCodeQuarterFrame(String deviceId, int cable, int timing) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiTimeCodeQuarterFrame(cable, timing);
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
    public void sendMidiSongSelect(String deviceId, int cable, int song) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiSongSelect(cable, song);
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
    public void sendMidiSongPositionPointer(String deviceId, int cable, int position) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiSongPositionPointer(cable, position);
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
    public void sendMidiTuneRequest(String deviceId, int cable) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiTuneRequest(cable);
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
    public void sendMidiTimingClock(String deviceId, int cable) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiTimingClock(cable);
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
    public void sendMidiStart(String deviceId, int cable) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiStart(cable);
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
    public void sendMidiContinue(String deviceId, int cable) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiContinue(cable);
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
    public void sendMidiStop(String deviceId, int cable) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiStop(cable);
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
    public void sendMidiActiveSensing(String deviceId, int cable) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiActiveSensing(cable);
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
    public void sendMidiReset(String deviceId, int cable) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiReset(cable);
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
    public void sendMidiMiscellaneousFunctionCodes(String deviceId, int cable, int byte1, int byte2, int byte3) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiMiscellaneousFunctionCodes(cable, byte1, byte2, byte3);
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
    public void sendMidiCableEvents(String deviceId, int cable, int byte1, int byte2, int byte3) {
        MidiOutputDevice midiOutputDevice = midiOutputDeviceMap.get(deviceId);
        if (midiOutputDevice != null) {
            midiOutputDevice.sendMidiCableEvents(cable, byte1, byte2, byte3);
        }
    }
}
