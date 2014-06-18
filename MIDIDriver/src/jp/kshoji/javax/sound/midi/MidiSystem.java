package jp.kshoji.javax.sound.midi;

import android.content.Context;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener;
import jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener;
import jp.kshoji.driver.midi.thread.MidiDeviceConnectionWatcher;
import jp.kshoji.driver.midi.util.Constants;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;
import jp.kshoji.driver.usb.util.DeviceFilter;
import jp.kshoji.javax.sound.midi.MidiDevice.Info;
import jp.kshoji.javax.sound.midi.io.StandardMidiFileReader;
import jp.kshoji.javax.sound.midi.io.StandardMidiFileWriter;
import jp.kshoji.javax.sound.midi.usb.UsbMidiDevice;
import jp.kshoji.javax.sound.midi.usb.UsbMidiSequencer;

/**
 * MidiSystem porting for Android USB MIDI.<br />
 * Implemented Receiver, Transmitter and Sequencer only.
 * 
 * @author K.Shoji
 */
public final class MidiSystem {
	static List<DeviceFilter> deviceFilters = null;
	static Map<UsbDevice, Set<UsbMidiDevice>> midiDevices = null;
	static Map<UsbDevice, UsbDeviceConnection> deviceConnections;
	static OnMidiDeviceAttachedListener deviceAttachedListener = null;
	static OnMidiDeviceDetachedListener deviceDetachedListener = null;
	static MidiDeviceConnectionWatcher deviceConnectionWatcher = null;
	static OnMidiSystemEventListener systemEventListener = null;

	/**
	 * Utilities for {@link MidiSystem}
	 *
	 * @author K.Shoji
	 */
	public static class MidiSystemUtils {
		/**
		 * Get currently connected {@link Receiver}s
		 *
		 * @return
		 * @throws MidiUnavailableException
		 */
		public static List<Receiver> getReceivers() throws MidiUnavailableException {
			List<Receiver> result = new ArrayList<Receiver>();
			Info[] midiDeviceInfos = MidiSystem.getMidiDeviceInfo();
			for (Info midiDeviceInfo : midiDeviceInfos) {
				result.addAll(MidiSystem.getMidiDevice(midiDeviceInfo).getReceivers());
			}

			return result;
		}

		/**
		 * Get currently connected {@link Transmitter}s
		 *
		 * @return
		 * @throws MidiUnavailableException
		 */
		public static List<Transmitter> getTransmitters() throws MidiUnavailableException {
			List<Transmitter> result = new ArrayList<Transmitter>();
			Info[] midiDeviceInfos = MidiSystem.getMidiDeviceInfo();
			for (Info midiDeviceInfo : midiDeviceInfos) {
				result.addAll(MidiSystem.getMidiDevice(midiDeviceInfo).getTransmitters());
			}

			return result;
		}
	}

	/**
	 * Find {@link Set<UsbMidiDevice>} from {@link UsbDevice}<br />
	 * method for jp.kshoji.javax.sound.midi package.
	 *
	 * @param usbDevice
	 * @param usbDeviceConnection
	 * @return {@link Set<UsbMidiDevice>}, always not null
	 */
	static Set<UsbMidiDevice> findAllUsbMidiDevices(UsbDevice usbDevice, UsbDeviceConnection usbDeviceConnection) {
		Set<UsbMidiDevice> result = new HashSet<UsbMidiDevice>();

		Set<UsbInterface> interfaces = UsbMidiDeviceUtils.findAllMidiInterfaces(usbDevice, deviceFilters);
		for (UsbInterface usbInterface : interfaces) {
			UsbEndpoint inputEndpoint = UsbMidiDeviceUtils.findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_IN, deviceFilters);
			UsbEndpoint outputEndpoint = UsbMidiDeviceUtils.findMidiEndpoint(usbDevice, usbInterface, UsbConstants.USB_DIR_OUT, deviceFilters);

			result.add(new UsbMidiDevice(usbDevice, usbDeviceConnection, usbInterface, inputEndpoint, outputEndpoint));
		}

		return Collections.unmodifiableSet(result);
	}


	/**
	 * Implementation for multiple device connections.
	 *
	 * @author K.Shoji
	 */
	static final class OnMidiDeviceAttachedListenerImpl implements OnMidiDeviceAttachedListener {
		private final UsbManager usbManager;

		/**
		 * constructor
		 *
		 * @param usbManager
		 */
		public OnMidiDeviceAttachedListenerImpl(UsbManager usbManager) {
			this.usbManager = usbManager;
		}

		/*
		 * (non-Javadoc)
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceAttachedListener#onDeviceAttached(android.hardware.usb.UsbDevice)
		 */
		@Override
		public synchronized void onDeviceAttached(UsbDevice attachedDevice) {
			deviceConnectionWatcher.notifyDeviceGranted();

			UsbDeviceConnection deviceConnection = usbManager.openDevice(attachedDevice);
			if (deviceConnection == null) {
				return;
			}

			synchronized (deviceConnection) {
				deviceConnections.put(attachedDevice, deviceConnection);
			}

			synchronized (midiDevices) {
				midiDevices.put(attachedDevice, findAllUsbMidiDevices(attachedDevice, deviceConnection));
			}

			Log.d(Constants.TAG, "Device " + attachedDevice.getDeviceName() + " has been attached.");

			if (systemEventListener != null) {
				systemEventListener.onMidiSystemChanged();
			}
		}
	}

	/**
	 * Implementation for multiple device connections.
	 *
	 * @author K.Shoji
	 */
	static final class OnMidiDeviceDetachedListenerImpl implements OnMidiDeviceDetachedListener {
		/*
		 * (non-Javadoc)
		 * @see jp.kshoji.driver.midi.listener.OnMidiDeviceDetachedListener#onDeviceDetached(android.hardware.usb.UsbDevice)
		 */
		@Override
		public void onDeviceDetached(UsbDevice detachedDevice) {
			UsbDeviceConnection usbDeviceConnection;
			synchronized (deviceConnections) {
				usbDeviceConnection = deviceConnections.get(detachedDevice);
			}

			if (usbDeviceConnection == null) {
				return;
			}

			Set<UsbMidiDevice> detachedMidiDevices = midiDevices.get(detachedDevice);
            if (detachedMidiDevices != null) {
                for (UsbMidiDevice usbMidiDevice : detachedMidiDevices) {
                    usbMidiDevice.close();
                }
            }

			synchronized (midiDevices) {
				midiDevices.remove(detachedDevice);
			}

			Log.d(Constants.TAG, "Device " + detachedDevice.getDeviceName() + " has been detached.");

			if (systemEventListener != null) {
				systemEventListener.onMidiSystemChanged();
			}
		}
	}

	/**
	 * Listener for MidiSystem event listener
	 *
	 * @author K.Shoji
	 */
	public interface OnMidiSystemEventListener {
		/**
		 * MidiSystem has been changed.
		 * (new device is connected, or disconnected.)
		 */
		void onMidiSystemChanged();
	}

	/**
	 * Set the listener of Device connection/disconnection
	 * @param listener
	 */
	public static void setOnMidiSystemEventListener(OnMidiSystemEventListener listener) {
		systemEventListener = listener;
	}

	/**
	 * Initializes MidiSystem
	 *
	 * @param context
	 * @throws NullPointerException
	 */
	public static void initialize(Context context) throws NullPointerException {
		if (context == null) {
			throw new NullPointerException("context is null");
		}

		UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		if (usbManager == null) {
			throw new NullPointerException("UsbManager is null");
		}

		deviceFilters = DeviceFilter.getDeviceFilters(context);
		midiDevices = new HashMap<UsbDevice, Set<UsbMidiDevice>>();
		deviceConnections = new HashMap<UsbDevice, UsbDeviceConnection>();
		deviceAttachedListener = new OnMidiDeviceAttachedListenerImpl(usbManager);
		deviceDetachedListener = new OnMidiDeviceDetachedListenerImpl();
		deviceConnectionWatcher = new MidiDeviceConnectionWatcher(context, usbManager, deviceAttachedListener, deviceDetachedListener);
	}

	/**
	 * Terminates MidiSystem
	 */
	public static void terminate() {
		if (midiDevices != null) {
			synchronized (midiDevices) {
				for (UsbDevice device : midiDevices.keySet()) {
                    for (UsbMidiDevice midiDevice : midiDevices.get(device)) {
                        midiDevice.close();
                    }
				}
				midiDevices.clear();
			}
		}
		midiDevices = null;

		if (deviceConnections != null) {
			synchronized (deviceConnections) {
				deviceConnections.clear();
			}
		}
		deviceConnections = null;

		if (deviceConnectionWatcher != null) {
			deviceConnectionWatcher.stop();
		}
		deviceConnectionWatcher = null;
	}

	private MidiSystem() {
	}

	/**
	 * get all connected {@link MidiDevice.Info} as array
	 *
	 * @return device informations
	 */
	public static MidiDevice.Info[] getMidiDeviceInfo() {
		List<MidiDevice.Info> result = new ArrayList<MidiDevice.Info>();
		if (midiDevices != null) {
            for (UsbDevice device : midiDevices.keySet()) {
                for (UsbMidiDevice midiDevice : midiDevices.get(device)) {
                    result.add(midiDevice.getDeviceInfo());
                }
            }
		}
		return result.toArray(new MidiDevice.Info[0]);
	}

	/**
	 * get {@link MidiDevice} by device information
	 *
	 * @param info
	 * @return {@link MidiDevice}
	 * @throws MidiUnavailableException
	 */
	public static MidiDevice getMidiDevice(MidiDevice.Info info) throws MidiUnavailableException {
		if (midiDevices != null) {
            for (UsbDevice device : midiDevices.keySet()) {
                for (UsbMidiDevice midiDevice : midiDevices.get(device)) {
                    if (info.equals(midiDevice.getDeviceInfo())) {
                        return midiDevice;
                    }
                }
            }
		}

		throw new IllegalArgumentException("Requested device not installed: " + info);
	}

	/**
	 * get the first detected Receiver
	 *
	 * @return {@link Receiver}
	 * @throws MidiUnavailableException
	 */
	public static Receiver getReceiver() throws MidiUnavailableException {
		if (midiDevices != null) {
            for (UsbDevice device : midiDevices.keySet()) {
                for (UsbMidiDevice midiDevice : midiDevices.get(device)) {
                    Receiver receiver = midiDevice.getReceiver();
                    if (receiver != null) {
                        return receiver;
                    }
                }
            }
		}
		return null;
	}

	/**
	 * get the first detected Transmitter
	 *
	 * @return {@link Transmitter}
	 * @throws MidiUnavailableException
	 */
	public static Transmitter getTransmitter() throws MidiUnavailableException {
		if (midiDevices != null) {
            for (UsbDevice device : midiDevices.keySet()) {
                for (UsbMidiDevice midiDevice : midiDevices.get(device)) {
                    Transmitter transmitter = midiDevice.getTransmitter();
                    if (transmitter != null) {
                        return transmitter;
                    }
                }
            }
		}
		return null;
	}

	/**
	 * get a {@link Sequence} from the specified File.
	 *
	 * @param file
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static Sequence getSequence(File file) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getSequence(file);
	}

	/**
	 * get a {@link Sequence} from the specified input stream.
	 *
	 * @param stream
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static Sequence getSequence(InputStream stream) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getSequence(stream);
	}

	/**
	 * get a {@link Sequence} from the specified URL.
	 * @param url
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static Sequence	getSequence(URL url) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getSequence(url);
	}

	/**
	 * get the default {@link Sequencer}, connected to a default device.
	 *
	 * @return {@link Sequencer} must call the {@link Sequencer#open()} method.
	 * @throws MidiUnavailableException
	 */
	public static Sequencer	getSequencer() throws MidiUnavailableException {
		return new UsbMidiSequencer();
	}

	/**
	 * get the default {@link Sequencer}, optionally connected to a default device.
	 *
	 * @param connected ignored
	 * @return {@link Sequencer} must call the {@link Sequencer#open()} method.
	 * @throws MidiUnavailableException
	 */
	public static Sequencer	getSequencer(boolean connected) throws MidiUnavailableException {
		return new UsbMidiSequencer();
	}

    /**
     * Obtain {@link jp.kshoji.javax.sound.midi.Soundbank} from File<br />
     * not implemented.
     *
     * @param file
     * @return
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public static Soundbank getSoundbank(File file) throws InvalidMidiDataException, IOException {
        throw new UnsupportedOperationException("not implemented.");
    }

    /**
     * Obtain {@link jp.kshoji.javax.sound.midi.Soundbank} from InputStream<br />
     * not implemented.
     *
     * @param stream
     * @return
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public static Soundbank getSoundbank(InputStream stream) throws InvalidMidiDataException, IOException {
        throw new UnsupportedOperationException("not implemented.");
    }

    /**
     * Obtain {@link jp.kshoji.javax.sound.midi.Soundbank} from URL<br />
     * not implemented.
     *
     * @param url
     * @return
     * @throws InvalidMidiDataException
     * @throws IOException
     */
    public static Soundbank getSoundbank(URL url) throws InvalidMidiDataException, IOException {
        throw new UnsupportedOperationException("not implemented.");
    }

    private static final Set<Synthesizer> synthesizers = new HashSet<Synthesizer>();

    /**
     * Obtains {@link jp.kshoji.javax.sound.midi.Synthesizer} registered by {@link #registerSynthesizer(Synthesizer)}
     * @return a Synthesizer, null if instance has not registered
     * @throws MidiUnavailableException
     */
    public static Synthesizer getSynthesizer() throws MidiUnavailableException {
        if (synthesizers.size() == 0) {
            return null;
        }

        for (Synthesizer synthesizer : synthesizers) {
            // returns the first one
            return synthesizer;
        }

        return null;
    }

    /**
     * Registers a {@link jp.kshoji.javax.sound.midi.Synthesizer} instance.
     * @param synthesizer
     */
    public static void registerSynthesizer(Synthesizer synthesizer) {
        if (synthesizer != null) {
            synthesizers.add(synthesizer);
        }
    }

	/**
	 * get the {@link MidiFileFormat} information of the specified File.
	 * 
	 * @param file
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static MidiFileFormat getMidiFileFormat(File file) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getMidiFileFormat(file);
	}

	/**
	 * get the {@link MidiFileFormat} information in the specified input stream.
	 * 
	 * @param stream
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static MidiFileFormat getMidiFileFormat(InputStream stream) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getMidiFileFormat(stream);
	}

	/**
	 * get the {@link MidiFileFormat} information in the specified URL.
	 * 
	 * @param url
	 * @return
	 * @throws InvalidMidiDataException
	 * @throws IOException
	 */
	public static MidiFileFormat getMidiFileFormat(URL url) throws InvalidMidiDataException, IOException {
		StandardMidiFileReader standardMidiFileReader = new StandardMidiFileReader();
		return standardMidiFileReader.getMidiFileFormat(url);
	}

	/**
	 * get the set of SMF types that the library can write
	 * 
	 * @return
	 */
	public static int[] getMidiFileTypes() {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.getMidiFileTypes();
	}

	/**
	 * get the set of SMF types that the library can write from the {@link Sequence}
	 * 
	 * @param sequence
	 * @return
	 */
	public static int[] getMidiFileTypes(Sequence sequence) {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.getMidiFileTypes(sequence);
	}
	
	/**
	 * check if the specified SMF fileType is available
	 * 
	 * @param fileType
	 * @return
	 */
	public static boolean isFileTypeSupported(int fileType) {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.isFileTypeSupported(fileType);
	}

	/**
	 * check if the specified SMF fileType is available from the {@link Sequence}
	 * 
	 * @param fileType
	 * @param sequence
	 * @return
	 */
	public static boolean isFileTypeSupported(int fileType, Sequence sequence) {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.isFileTypeSupported(fileType, sequence);
	}

	/**
	 * write sequence to the specified {@link File} as SMF
	 * 
	 * @param sequence
	 * @param fileType
	 * @param file
	 * @return
	 * @throws IOException
	 */
    public static int write(Sequence sequence, int fileType, File file) throws IOException {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.write(sequence, fileType, file);
	}

	/**
	 * write sequence to the specified {@link OutputStream} as SMF
	 * 
	 * @param sequence
	 * @param fileType
	 * @param outputStream
	 * @return
	 * @throws IOException
	 */
    public static int write(Sequence sequence, int fileType, OutputStream outputStream) throws IOException {
		StandardMidiFileWriter standardMidiFileWriter = new StandardMidiFileWriter();
		return standardMidiFileWriter.write(sequence, fileType, outputStream);
	}
}
