package jp.kshoji.javax.sound.midi;

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

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((description == null) ? 0 : description.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((vendor == null) ? 0 : vendor.hashCode());
			result = prime * result + ((version == null) ? 0 : version.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			final Info other = (Info) obj;
			if (description == null) {
				if (other.description != null) {
					return false;
				}
			} else if (!description.equals(other.description)) {
				return false;
			}
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (vendor == null) {
				if (other.vendor != null) {
					return false;
				}
			} else if (!vendor.equals(other.vendor)) {
				return false;
			}
			if (version == null) {
				if (other.version != null) {
					return false;
				}
			} else if (!version.equals(other.version)) {
				return false;
			}
			return true;
		}
	}
}
