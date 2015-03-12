package jp.kshoji.driver.midi.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;

import java.lang.reflect.Method;

/**
 * This class is used for disabling USB Audio output.<br />
 * <br />
 * Some Android devices treat USB MIDI devices as USB Audio devices.<br />
 * When such Android device attaches USB MIDI device, USB Audio will be selected as the audio output source.<br />
 * As the result, sound playing will be stopped.<br />
 * This class cancels all of USB Audio source connections.
 * <p/>
 * <ul>
 * <li>This class requires a permission 'android.permission.MODIFY_AUDIO_SETTINGS'.</li>
 * <li>This class uses the hidden API, so the function will be broken in the future Android updates.</li>
 * </ul>
 *
 * @author K.Shoji
 */
@SuppressWarnings("JavadocReference")
public final class UsbAudioConnectionCanceller extends BroadcastReceiver {
    private final Context context;

    /**
     * Constructor, initialize and attach this BroadcastReceiver to the specified context
     *
     * @param context the context
     */
    public UsbAudioConnectionCanceller(@NonNull Context context) {
        this.context = context;
        IntentFilter filter = new IntentFilter();
        filter.addAction(INTENT_ACTION_ANALOG_AUDIO_DOCK_PLUG);
        filter.addAction(MEDIA_ACTION_ANALOG_AUDIO_DOCK_PLUG);
        context.registerReceiver(this, filter);
    }

    /**
     * Must be called on Activity.onDestroy()
     */
    public void terminate() {
        context.unregisterReceiver(this);
    }

    /**
     * Written in {@link android.media.AudioSystem} (hidden API)
     */
    private static final int DEVICE_STATE_UNAVAILABLE = 0;
    private static final int DEVICE_OUT_ANLG_DOCK_HEADSET = 0x800;

    /**
     * Intent actions
     */
    public static final String INTENT_ACTION_ANALOG_AUDIO_DOCK_PLUG = "android.intent.action.ANALOG_AUDIO_DOCK_PLUG";
    public static final String MEDIA_ACTION_ANALOG_AUDIO_DOCK_PLUG = "android.media.action.ANALOG_AUDIO_DOCK_PLUG";

    private Class<?> audioSystem;

    /**
     * Obtains AudioSystem class
     * @return class object
     */
    private Class<?> getAudioSystem() {
        try {
            if (audioSystem == null) {
                audioSystem = Class.forName("android.media.AudioSystem");
            }
        } catch (Exception ignored) {
        }

        return audioSystem;
    }

    /**
     * Set the device connection state
     *
     * @param device device kind id
     * @param state DEVICE_STATE_AVAILABLE or DEVICE_STATE_UNAVAILABLE
     * @param deviceAddress device address
     */
    private void setDeviceConnectionState(int device, int state, @NonNull String deviceAddress) {
        Class<?> audioSystem = getAudioSystem();

        try {
            Method method = audioSystem.getMethod("setDeviceConnectionState", Integer.TYPE, Integer.TYPE, String.class);
            method.invoke(audioSystem, device, state, deviceAddress);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (INTENT_ACTION_ANALOG_AUDIO_DOCK_PLUG.equals(action) || MEDIA_ACTION_ANALOG_AUDIO_DOCK_PLUG.equals(action)) {
            Bundle extras = intent.getExtras();
            if (extras != null && "usb_audio".equals(extras.getString("name")) && extras.getInt("state") == 1) {
                // USB Audio(USB MIDI) has been connected, disable it
                setDeviceConnectionState(DEVICE_OUT_ANLG_DOCK_HEADSET, DEVICE_STATE_UNAVAILABLE, "");
            }
        }
    }
}
