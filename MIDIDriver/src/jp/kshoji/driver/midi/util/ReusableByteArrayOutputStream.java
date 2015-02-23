package jp.kshoji.driver.midi.util;

import java.io.ByteArrayOutputStream;

/**
 * {@link ByteArrayOutputStream} that can reset without memory leak.
 * 
 * @author K.Shoji
 */
public class ReusableByteArrayOutputStream extends ByteArrayOutputStream {
	private static final int DEFAULT_BUFFER_LIMIT = 1024;
	private final byte[] fixedSizeBuffer;

	/**
	 * Construct instance
	 * 
	 * @param size the initial size of this Stream
	 */
	public ReusableByteArrayOutputStream(int size) {
		super(size);
		fixedSizeBuffer = new byte[size];
		this.buf = fixedSizeBuffer;
	}

	/**
	 * Construct default instance, maximum buffer size is 1024 bytes.
	 */
	public ReusableByteArrayOutputStream() {
		this(DEFAULT_BUFFER_LIMIT);
	}

	@Override
	public synchronized void reset() {
		super.reset();
		
		// reset buffer size when the buffer has been extended
		if (this.buf.length > fixedSizeBuffer.length) {
			this.buf = fixedSizeBuffer;
		}
	}
}
