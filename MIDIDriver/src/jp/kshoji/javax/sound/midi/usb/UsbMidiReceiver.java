package jp.kshoji.javax.sound.midi.usb;


import android.support.annotation.NonNull;

import jp.kshoji.driver.midi.device.MidiOutputDevice;
import jp.kshoji.javax.sound.midi.MetaMessage;
import jp.kshoji.javax.sound.midi.MidiDevice;
import jp.kshoji.javax.sound.midi.MidiDeviceReceiver;
import jp.kshoji.javax.sound.midi.MidiMessage;
import jp.kshoji.javax.sound.midi.ShortMessage;
import jp.kshoji.javax.sound.midi.SysexMessage;

/**
 * {@link jp.kshoji.javax.sound.midi.Receiver} implementation
 *
 * @author K.Shoji
 *
 */
public final class UsbMidiReceiver implements MidiDeviceReceiver {
    private final UsbMidiDevice usbMidiDevice;
    private MidiOutputDevice outputDevice;
	private int cableId;
	

    /**
     * Constructor
     *
     * @param usbMidiDevice the UsbMidiDevice
     */
	public UsbMidiReceiver(@NonNull UsbMidiDevice usbMidiDevice, @NonNull MidiOutputDevice midiOutputDevice) {
        this.usbMidiDevice = usbMidiDevice;
        this.outputDevice = midiOutputDevice;
		cableId = 0;

        open();
    }

	@Override
	public void send(@NonNull MidiMessage message, long timeStamp) {
        if (outputDevice == null) {
            // already closed
            return;
        }

		if (message instanceof MetaMessage) {
			final MetaMessage metaMessage = (MetaMessage) message;
			outputDevice.sendMidiSystemCommonMessage(cableId, metaMessage.getData());
		} else if (message instanceof SysexMessage) {
			final SysexMessage sysexMessage = (SysexMessage) message;
			outputDevice.sendMidiSystemExclusive(cableId, sysexMessage.getData());
		} else if (message instanceof ShortMessage) {
			final ShortMessage shortMessage = (ShortMessage) message;
			switch (shortMessage.getCommand()) {
				case ShortMessage.CHANNEL_PRESSURE:
					outputDevice.sendMidiChannelAftertouch(cableId, shortMessage.getChannel(), shortMessage.getData1());
					break;
				case ShortMessage.CONTROL_CHANGE:
					outputDevice.sendMidiControlChange(cableId, shortMessage.getChannel(), shortMessage.getData1(), shortMessage.getData2());
					break;
				case ShortMessage.NOTE_OFF:
					outputDevice.sendMidiNoteOff(cableId, shortMessage.getChannel(), shortMessage.getData1(), shortMessage.getData2());
					break;
				case ShortMessage.NOTE_ON:
					outputDevice.sendMidiNoteOn(cableId, shortMessage.getChannel(), shortMessage.getData1(), shortMessage.getData2());
					break;
				case ShortMessage.PITCH_BEND:
					outputDevice.sendMidiPitchWheel(cableId, shortMessage.getChannel(), shortMessage.getData1() | (shortMessage.getData2() << 7));
					break;
				case ShortMessage.POLY_PRESSURE:
					outputDevice.sendMidiPolyphonicAftertouch(cableId, shortMessage.getChannel(), shortMessage.getData1(), shortMessage.getData2());
					break;
				case ShortMessage.PROGRAM_CHANGE:
					outputDevice.sendMidiProgramChange(cableId, shortMessage.getChannel(), shortMessage.getData1());
					break;
				default:
			}
		}
	}

    /**
     * must be called from UI thread.
     */
	public void open() {
	}
	
	@Override
	public void close() {
        outputDevice = null;
	}

    /**
     * Get the cableId of this UsbMidiReceiver
     *
     * @return the cable ID
     */
	public int getCableId() {
		return cableId;
	}

    /**
     * Set the cableId of this UsbMidiReceiver
     *
     * @param cableId the cable ID
     */
	public void setCableId(int cableId) {
		this.cableId = cableId;
	}

    @NonNull
    @Override
    public MidiDevice getMidiDevice() {
        return usbMidiDevice;
    }
}
