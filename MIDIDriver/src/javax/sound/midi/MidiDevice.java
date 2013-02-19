package javax.sound.midi;

import java.util.List;

public interface MidiDevice {
	Info getDeviceInfo();

	void open() throws MidiUnavailableException;

	void close();

	boolean isOpen();

	long getMicrosecondPosition();

	int getMaxReceivers();

	int getMaxTransmitters();

	Receiver getReceiver() throws MidiUnavailableException;

	List<Receiver> getReceivers();

	Transmitter getTransmitter() throws MidiUnavailableException;

	List<Transmitter> getTransmitters();

	public static class Info {
		private String name;
		private String vendor;
		private String description;
		private String version;

		public Info(String name, String vendor, String description, String version) {
			this.name = name;
			this.vendor = vendor;
			this.description = description;
			this.version = version;
		}

		public final String getName() {
			return name;
		}

		public final String getVendor() {
			return vendor;
		}

		public final String getDescription() {
			return description;
		}

		public final String getVersion() {
			return version;
		}

		@Override
		public final String toString() {
			return name;
		}
	}
}
