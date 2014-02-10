package jp.kshoji.javax.sound.midi.io;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import jp.kshoji.javax.sound.midi.MetaMessage;
import jp.kshoji.javax.sound.midi.MidiEvent;
import jp.kshoji.javax.sound.midi.MidiFileFormat;
import jp.kshoji.javax.sound.midi.Sequence;
import jp.kshoji.javax.sound.midi.Track;
import jp.kshoji.javax.sound.midi.spi.MidiFileWriter;

public class StandardMidiFileWriter extends MidiFileWriter {
	static class MidiDataOutputStream extends DataOutputStream {
		public MidiDataOutputStream(OutputStream outputStream) {
			super(outputStream);
		}
		
		private static int getValueToWrite(int value) {
			int result = value & 0x7f;
			int currentValue = value;

			while ((currentValue >>= 7) != 0) {
				result <<= 8;
				result |= ((currentValue & 0x7f) | 0x80);
			}
			return result;
		}

		public static int variableLengthIntLength(int value) {
			int valueToWrite = getValueToWrite(value);

			int length = 0;
			while (true) {
				length++;
				
				if ((valueToWrite & 0x80) != 0) {
					valueToWrite >>>= 8;
				} else {
					break;
				}
			}

			return length;
		}

		public synchronized void writeVariableLengthInt(int value) throws IOException {
			int valueToWrite = getValueToWrite(value);

			while (true) {
				writeByte(valueToWrite & 0xff);

				if ((valueToWrite & 0x80) != 0) {
					valueToWrite >>>= 8;
				} else {
					break;
				}
			}
		}
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see jp.kshoji.javax.sound.midi.spi.MidiFileWriter#getMidiFileTypes()
	 */
	@Override
	public int[] getMidiFileTypes() {
		return new int[] { 0, 1 };
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jp.kshoji.javax.sound.midi.spi.MidiFileWriter#getMidiFileTypes(jp.kshoji.javax.sound.midi.Sequence)
	 */
	@Override
	public int[] getMidiFileTypes(Sequence sequence) {
		if (sequence.getTracks().length > 1) {
			return new int[] { 1 };
		} else {
			return new int[] { 0, 1 };
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jp.kshoji.javax.sound.midi.spi.MidiFileWriter#write(jp.kshoji.javax.sound.midi.Sequence, int, java.io.File)
	 */
	@Override
	public int write(Sequence sequence, int fileType, File file) throws IOException {
		FileOutputStream fileOutputStream = new FileOutputStream(file);
		int written = write(sequence, fileType, fileOutputStream);
		fileOutputStream.close();
		return written;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jp.kshoji.javax.sound.midi.spi.MidiFileWriter#write(jp.kshoji.javax.sound.midi.Sequence, int, java.io.OutputStream)
	 */
	@Override
	public int write(Sequence sequence, int fileType, OutputStream outputStream) throws IOException {
		MidiDataOutputStream midiDataOutputStream = new MidiDataOutputStream(outputStream);
		
		Track[] tracks = sequence.getTracks();
		midiDataOutputStream.writeInt(MidiFileFormat.HEADER_MThd);
		midiDataOutputStream.writeInt(6);
		midiDataOutputStream.writeShort(fileType);
		midiDataOutputStream.writeShort(tracks.length);
		
		float divisionType = sequence.getDivisionType();
		int resolution = sequence.getResolution();
		int division = 0;
		if (divisionType == Sequence.PPQ) {
			division = resolution & 0x7fff;
		} else if (divisionType == Sequence.SMPTE_24) {
			division = (24 << 8) * -1;
			division += (resolution & 0xff);
		} else if (divisionType == Sequence.SMPTE_25) {
			division = (25 << 8) * -1;
			division += (resolution & 0xff);
		} else if (divisionType == Sequence.SMPTE_30DROP) {
			division = (29 << 8) * -1;
			division += (resolution & 0xff);
		} else if (divisionType == Sequence.SMPTE_30) {
			division = (30 << 8) * -1;
			division += (resolution & 0xff);
		}
		midiDataOutputStream.writeShort(division);
		
		int length = 0;
		for (int i = 0; i < tracks.length; i++) {
			length += writeTrack(tracks[i], midiDataOutputStream);
		}
		
		midiDataOutputStream.close();
		return length + 14;
	}

	/**
	 * Write {@link Track} data into {@link MidiDataOutputStream}
	 * 
	 * @param track
	 * @param midiDataOutputStream
	 * @return written byte length
	 * @throws IOException
	 */
	private static int writeTrack(Track track, MidiDataOutputStream midiDataOutputStream) throws IOException {
		int eventCount = track.size();

		MidiEvent lastMidiEvent = null;
		midiDataOutputStream.writeInt(MidiFileFormat.HEADER_MTrk);
		
		// calculate the track length
		int trackLength = 0;
		long lastTick = 0;
		for (int i = 0; i < eventCount; i++) {
			MidiEvent midiEvent = track.get(i);
			long tick = midiEvent.getTick();
			trackLength += MidiDataOutputStream.variableLengthIntLength((int) (tick - lastTick));
			lastTick = tick;
			trackLength += midiEvent.getMessage().getLength();
		}
		midiDataOutputStream.writeInt(trackLength);

		// write the track data
		for (int i = 0; i < eventCount; i++) {
			MidiEvent currentMidiEvent = track.get(i);
			
			int deltaTime = 0;
			if (lastMidiEvent != null) {
				deltaTime = (int) (currentMidiEvent.getTick() - lastMidiEvent.getTick());
			}
			midiDataOutputStream.writeVariableLengthInt(deltaTime);
			
			byte msg[] = currentMidiEvent.getMessage().getMessage();
			midiDataOutputStream.write(msg);
			lastMidiEvent = currentMidiEvent;
		}

		// process End of Track message
		if (lastMidiEvent != null && (lastMidiEvent.getMessage() instanceof MetaMessage)) {
			MetaMessage metaMessage = (MetaMessage) lastMidiEvent.getMessage();
			if (metaMessage.getType() == MetaMessage.TYPE_END_OF_TRACK) {
				// End of Track
				return trackLength + 8;
			}
		}

		// write End of Track message if not found.
		midiDataOutputStream.writeVariableLengthInt(0);
		midiDataOutputStream.writeByte(MetaMessage.META);
		midiDataOutputStream.writeByte(MetaMessage.TYPE_END_OF_TRACK);
		midiDataOutputStream.writeVariableLengthInt(0);

		return trackLength + 12;
	}
}
