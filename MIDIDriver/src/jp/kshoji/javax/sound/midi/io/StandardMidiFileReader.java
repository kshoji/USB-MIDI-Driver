package jp.kshoji.javax.sound.midi.io;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import jp.kshoji.javax.sound.midi.InvalidMidiDataException;
import jp.kshoji.javax.sound.midi.MetaMessage;
import jp.kshoji.javax.sound.midi.MidiEvent;
import jp.kshoji.javax.sound.midi.MidiFileFormat;
import jp.kshoji.javax.sound.midi.MidiMessage;
import jp.kshoji.javax.sound.midi.Sequence;
import jp.kshoji.javax.sound.midi.ShortMessage;
import jp.kshoji.javax.sound.midi.SysexMessage;
import jp.kshoji.javax.sound.midi.Track;
import jp.kshoji.javax.sound.midi.spi.MidiFileReader;

public class StandardMidiFileReader extends MidiFileReader {
	class ExtendedMidiFileFormat extends MidiFileFormat {
		private int numberOfTracks;

		/**
		 * Get the number of tracks for this MIDI file.
		 * 
		 * @return the number of tracks for this MIDI file
		 */
		public int getNumberTracks() {
			return numberOfTracks;
		}

		/**
		 * Create an ExtendedMidiFileFormat object from the given parameters.
		 * 
		 * @param type
		 *            the MIDI file type (0, 1, or 2)
		 * @param divisionType
		 *            the MIDI file division type
		 * @param resolution
		 *            the MIDI file timing resolution
		 * @param bytes
		 *            the MIDI file size in bytes
		 * @param microseconds
		 *            the MIDI file length in microseconds
		 * @param numberOfTracks
		 *            the number of tracks
		 */
		public ExtendedMidiFileFormat(int type, float divisionType, int resolution, int bytes, long microseconds, int numberOfTracks) {
			super(type, divisionType, resolution, bytes, microseconds);
			this.numberOfTracks = numberOfTracks;
		}
	}

	class MidiDataInputStream extends DataInputStream {
		public MidiDataInputStream(InputStream inputStream) {
			super(inputStream);
		}

		public int readVariableLengthInt() throws IOException {
			int c;
			int value = readByte();

			if ((value & 0x80) != 0) {
				value &= 0x7f;
				do {
					value = (value << 7) + ((c = readByte()) & 0x7f);
				} while ((c & 0x80) != 0);
			}

			return value;
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.javax.sound.midi.spi.MidiFileReader#getMidiFileFormat(java.io.InputStream)
	 */
	public MidiFileFormat getMidiFileFormat(InputStream inputStream) throws InvalidMidiDataException, IOException {
		DataInputStream dataInputStream;
		if (inputStream instanceof DataInputStream) {
			dataInputStream = (DataInputStream) inputStream;
		} else {
			dataInputStream = new DataInputStream(inputStream);
		}
		
		try {
			int type, numberOfTracks, division, resolution, bytes;
			float divisionType;
	
			if (dataInputStream.readInt() != MidiFileFormat.HEADER_MThd) {
				throw new InvalidMidiDataException("Invalid header");
			}
	
			bytes = dataInputStream.readInt();
			if (bytes < 6) {
				throw new InvalidMidiDataException("Invalid header");
			}
	
			type = dataInputStream.readShort();
			if (type < 0 || type > 2) {
				throw new InvalidMidiDataException("Invalid header");
			}
	
			numberOfTracks = dataInputStream.readShort();
			if (numberOfTracks <= 0) {
				throw new InvalidMidiDataException("Invalid tracks");
			}
	
			division = dataInputStream.readShort();
			if ((division & 0x8000) != 0) {
				division = -((division >>> 8) & 0xff);
				switch (division) {
				case 24:
					divisionType = Sequence.SMPTE_24;
					break;
				case 25:
					divisionType = Sequence.SMPTE_25;
					break;
				case 29:
					divisionType = Sequence.SMPTE_30DROP;
					break;
				case 30:
					divisionType = Sequence.SMPTE_30;
					break;
	
				default:
					throw new InvalidMidiDataException("Invalid sequence information");
				}
				resolution = division & 0xff;
			} else {
				divisionType = Sequence.PPQ;
				resolution = division & 0x7fff;
			}
	
			dataInputStream.skip(bytes - 6);
	
			return new ExtendedMidiFileFormat(type, divisionType, resolution, MidiFileFormat.UNKNOWN_LENGTH, MidiFileFormat.UNKNOWN_LENGTH, numberOfTracks);
		} finally {
			dataInputStream.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.javax.sound.midi.spi.MidiFileReader#getMidiFileFormat(java.net.URL)
	 */
	public MidiFileFormat getMidiFileFormat(URL url) throws InvalidMidiDataException, IOException {
		InputStream inputStream = url.openStream();
		try {
			return getMidiFileFormat(inputStream);
		} finally {
			inputStream.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.javax.sound.midi.spi.MidiFileReader#getMidiFileFormat(java.io.File)
	 */
	public MidiFileFormat getMidiFileFormat(File file) throws InvalidMidiDataException, IOException {
		InputStream inputStram = new FileInputStream(file);
		try {
			return getMidiFileFormat(inputStram);
		} finally {
			inputStram.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.javax.sound.midi.spi.MidiFileReader#getSequence(java.io.InputStream)
	 */
	public Sequence getSequence(InputStream inputStream) throws InvalidMidiDataException, IOException {
		MidiDataInputStream midiDataInputStream = new MidiDataInputStream(inputStream);
		
		try {
			ExtendedMidiFileFormat midiFileFormat = (ExtendedMidiFileFormat) getMidiFileFormat(midiDataInputStream);
			Sequence sequence = new Sequence(midiFileFormat.getDivisionType(), midiFileFormat.getResolution());
	
			int numberOfTracks = midiFileFormat.getNumberTracks();
			while (numberOfTracks-- > 0) {
				Track track = sequence.createTrack();
				if (midiDataInputStream.readInt() != MidiFileFormat.HEADER_MTrk) {
					throw new InvalidMidiDataException("Invalid track header");
				}
				midiDataInputStream.readInt();
	
				int runningStatus = -1;
				int click = 0;
				boolean isEndOfTrack = false;
	
				// Read all of the events.
				while (!isEndOfTrack) {
					MidiMessage message;

					click += midiDataInputStream.readVariableLengthInt(); // add deltaTime
	
					int data = midiDataInputStream.readUnsignedByte();
					if (data < 0xf0) {
						ShortMessage shortMessage;
						switch (data & 0xf0) {
						case ShortMessage.NOTE_OFF://80
						case ShortMessage.NOTE_ON://90
						case ShortMessage.POLY_PRESSURE://a0
						case ShortMessage.CONTROL_CHANGE://b0
						case ShortMessage.PITCH_BEND://e0
							shortMessage = new ShortMessage();
							shortMessage.setMessage(data, midiDataInputStream.readByte(), midiDataInputStream.readByte());
							runningStatus = data;
							break;
	
						case ShortMessage.PROGRAM_CHANGE://c0
						case ShortMessage.CHANNEL_PRESSURE://d0
							shortMessage = new ShortMessage();
							shortMessage.setMessage(data, midiDataInputStream.readByte(), 0);
							runningStatus = data;
							break;
	
						default:
							// data: 00-7f use runningStatus
							if (runningStatus != -1) {
								switch (runningStatus & ShortMessage.MASK_EVENT) {
								case ShortMessage.NOTE_OFF:
								case ShortMessage.NOTE_ON:
								case ShortMessage.POLY_PRESSURE:
								case ShortMessage.CONTROL_CHANGE:
								case ShortMessage.PITCH_BEND:
									shortMessage = new ShortMessage();
									shortMessage.setMessage(runningStatus, data, midiDataInputStream.readByte());
									break;
	
								case ShortMessage.PROGRAM_CHANGE:
								case ShortMessage.CHANNEL_PRESSURE:
									shortMessage = new ShortMessage();
									shortMessage.setMessage(runningStatus, data, 0);
									break;
	
								default:
									throw new InvalidMidiDataException(String.format("Invalid data: %02x %02x", runningStatus, data));
								}
							} else {
								throw new InvalidMidiDataException(String.format("Invalid data: %02x", data));
							}
						}
						message = shortMessage;
					} else if (data == ShortMessage.START_OF_EXCLUSIVE || data == ShortMessage.END_OF_EXCLUSIVE) {
						// System Exclusive event
						int sysexLength = midiDataInputStream.readVariableLengthInt();
						byte sysexData[] = new byte[sysexLength];
						midiDataInputStream.readFully(sysexData);
						SysexMessage sysexMessage = new SysexMessage();
						sysexMessage.setMessage(data, sysexData, sysexLength);
						message = sysexMessage;
						
						runningStatus = -1;
					} else if (data == MetaMessage.META) {
						// Meta Message
						byte type = midiDataInputStream.readByte();
						
						int metaLength = midiDataInputStream.readVariableLengthInt();
						byte metaData[] = new byte[metaLength];
						midiDataInputStream.readFully(metaData);
						MetaMessage metaMessage = new MetaMessage();
						metaMessage.setMessage(type, metaData, metaLength);
						message = metaMessage;
						
						runningStatus = -1;
						
						if (type == MetaMessage.TYPE_END_OF_TRACK) {
							isEndOfTrack = true;
						}
					} else {
						// f1-f6, f8-fe
						ShortMessage shortMessage;
						if (data > 0x7f) {
							switch (data) {
							case ShortMessage.SONG_POSITION_POINTER://f2
								shortMessage = new ShortMessage();
								shortMessage.setMessage(data, midiDataInputStream.readByte(), midiDataInputStream.readByte());
								runningStatus = data;
								break;
								
							case ShortMessage.SONG_SELECT://f3
							case ShortMessage.BUS_SELECT://f5
								shortMessage = new ShortMessage();
								shortMessage.setMessage(data, midiDataInputStream.readByte(), 0);
								runningStatus = data;
								break;
							
							case ShortMessage.TUNE_REQUEST://f6
							case ShortMessage.TIMING_CLOCK://f8
							case ShortMessage.START://fa
							case ShortMessage.CONTINUE://fb
							case ShortMessage.STOP://fc
							case ShortMessage.ACTIVE_SENSING://fe
								shortMessage = new ShortMessage();
								shortMessage.setMessage(data, 0, 0);
								runningStatus = data;
								break;
							
							default://f1, f9, fd
								throw new InvalidMidiDataException(String.format("Invalid data: %02x", data));
							}
						} else {
							// data: 00-7f use runningStatus
							if (runningStatus != -1) {
								switch (runningStatus) {
								case ShortMessage.SONG_POSITION_POINTER://f2
									shortMessage = new ShortMessage();
									shortMessage.setMessage(runningStatus, data, midiDataInputStream.readByte());
									break;
		
								case ShortMessage.SONG_SELECT://f3
								case ShortMessage.BUS_SELECT://f5
									shortMessage = new ShortMessage();
									shortMessage.setMessage(runningStatus, data, 0);
									break;
		
								case ShortMessage.TUNE_REQUEST://f6
								case ShortMessage.TIMING_CLOCK://f8
								case ShortMessage.START://fa
								case ShortMessage.CONTINUE://fb
								case ShortMessage.STOP://fc
								case ShortMessage.ACTIVE_SENSING://fe
									shortMessage = new ShortMessage();
									shortMessage.setMessage(runningStatus, 0, 0);
									break;
		
								default://f1, f9, fd
									throw new InvalidMidiDataException(String.format("Invalid data: %02x %02x", runningStatus, data));
								}
							} else {
								throw new InvalidMidiDataException(String.format("Invalid data: %02x", data));
							}
						}
						
						message = shortMessage;
					}
	
					track.add(new MidiEvent(message, click));
				}
			}
	
			return sequence;
		} finally {
			midiDataInputStream.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.javax.sound.midi.spi.MidiFileReader#getSequence(java.net.URL)
	 */
	public Sequence getSequence(URL url) throws InvalidMidiDataException, IOException {
		InputStream inputStream = url.openStream();
		try {
			return getSequence(inputStream);
		} finally {
			inputStream.close();
		}
	}

	/*
	 * (non-Javadoc)
	 * @see jp.kshoji.javax.sound.midi.spi.MidiFileReader#getSequence(java.io.File)
	 */
	public Sequence getSequence(File file) throws InvalidMidiDataException, IOException {
		InputStream inputStream = new FileInputStream(file);
		try {
			return getSequence(inputStream);
		} finally {
			inputStream.close();
		}
	}
}
