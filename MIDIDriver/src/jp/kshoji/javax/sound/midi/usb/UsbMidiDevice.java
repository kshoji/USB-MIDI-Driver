package jp.kshoji.javax.sound.midi.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.javax.sound.midi.MidiDevice;
import jp.kshoji.javax.sound.midi.MidiUnavailableException;
import jp.kshoji.javax.sound.midi.Receiver;
import jp.kshoji.javax.sound.midi.Transmitter;

/**
 * {@link jp.kshoji.javax.sound.midi.MidiDevice} implementation
 * 
 * @author K.Shoji
 */
public final class UsbMidiDevice implements MidiDevice {
	private final UsbDevice usbDevice;
	private final UsbDeviceConnection usbDeviceConnection;
	private final UsbInterface usbInterface;
	
	private final List<Receiver> receivers = new ArrayList<Receiver>();
	private final List<Transmitter> transmitters = new ArrayList<Transmitter>();
	
	private boolean isOpened;

	public UsbMidiDevice(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection, UsbInterface usbInterface, UsbEndpoint inputEndpoint, UsbEndpoint outputEndpoint) {
		this.usbDevice = usbDevice;
		this.usbDeviceConnection = usbDeviceConnection;
		this.usbInterface = usbInterface;

		receivers.add(new UsbMidiReceiver(this, usbDevice, usbDeviceConnection, usbInterface, outputEndpoint));
		transmitters.add(new UsbMidiTransmitter(this, usbDevice, usbDeviceConnection, usbInterface, inputEndpoint));

		isOpened = false;

        try {
            open();
        } catch (MidiUnavailableException e) {
            Log.e(Constants.TAG, e.getMessage(), e);
        }
    }

	@SuppressWarnings("boxing")
	@Override
	public Info getDeviceInfo() {
		return new Info(usbDevice.getDeviceName(), //
				String.format("vendorId: %x, productId: %x", usbDevice.getVendorId(), usbDevice.getProductId()), //
				"deviceId:" + usbDevice.getDeviceId(), //
				"interfaceId:" + usbInterface.getId());
	}

	@Override
	public void open() throws MidiUnavailableException {
		if (isOpened) {
			return;
		}
		
		for (final Receiver receiver : receivers) {
			if (receiver instanceof UsbMidiReceiver) {
				final UsbMidiReceiver usbMidiReceiver = (UsbMidiReceiver) receiver;
				// claimInterface will be called
				usbMidiReceiver.open();
			}
		}
		for (final Transmitter transmitter : transmitters) {
			if (transmitter instanceof UsbMidiTransmitter) {
				final UsbMidiTransmitter usbMidiTransmitter = (UsbMidiTransmitter) transmitter;
				// claimInterface will be called
				usbMidiTransmitter.open();
			}
		}
		isOpened = true;
	}

	@Override
	public void close() {
		if (!isOpened) {
			return;
		}

		for (final Transmitter transmitter : transmitters) {
			transmitter.close();
		}
		transmitters.clear();
		for (final Receiver receiver : receivers) {
			receiver.close();
		}
		receivers.clear();

		if (usbDeviceConnection != null && usbInterface != null) {
			usbDeviceConnection.releaseInterface(usbInterface);
		}
		
		isOpened = false;
	}

	@Override
	public boolean isOpen() {
		return isOpened;
	}

	@Override
	public long getMicrosecondPosition() {
		// time-stamping is not supported
		return -1;
	}

	@Override
	public int getMaxReceivers() {
		if (receivers != null) {
			return receivers.size();
		}
		return 0;
	}

	@Override
	public int getMaxTransmitters() {
		if (transmitters != null) {
			return transmitters.size();
		}
		return 0;
	}

	@Override
	public Receiver getReceiver() throws MidiUnavailableException {
		if (receivers == null || receivers.size() < 1) {
			return null;
		}
		
		return receivers.get(0);
	}

	@Override
	public List<Receiver> getReceivers() {
		return Collections.unmodifiableList(receivers);
	}

	@Override
	public Transmitter getTransmitter() throws MidiUnavailableException {
		if (transmitters == null || transmitters.size() < 1) {
			return null;
		}
		
		return transmitters.get(0);
	}

	@Override
	public List<Transmitter> getTransmitters() {
		return Collections.unmodifiableList(transmitters);
	}
}
