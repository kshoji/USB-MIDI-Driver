package jp.kshoji.driver.midi.util;

/**
 * Bit width scale utilities
 */
public class ValueScaleUtils {

    /**
     * Scale up bit width
     *
     * @param srcVal  the source value
     * @param srcBits the source bit width
     * @param dstBits the destination bit width
     * @return scale-uped value
     */
    public static long scaleUp(long srcVal, int srcBits, int dstBits) {
        // simple bit shift
        int scaleBits = dstBits - srcBits;
        long bitShiftedValue = srcVal << scaleBits;
        int srcCenter = 1 << (srcBits - 1);
        if (srcVal <= srcCenter) {
            return bitShiftedValue;
        }

        // expanded bit repeat scheme
        int repeatBits = srcBits - 1;
        int repeatMask = (1 << repeatBits) - 1;
        long repeatValue = srcVal & repeatMask;
        if (scaleBits > repeatBits) {
            repeatValue <<= scaleBits - repeatBits;
        } else {
            repeatValue >>>= repeatBits - scaleBits;
        }
        while (repeatValue != 0) {
            bitShiftedValue |= repeatValue;
            repeatValue >>= repeatBits;
        }
        return bitShiftedValue;
    }

    /**
     * Scale down bit width
     *
     * @param srcVal  the source value
     * @param srcBits the source bit width
     * @param dstBits the destination bit width
     * @return scale-downed value
     */
    public static long scaleDown(long srcVal, int srcBits, int dstBits) {
        // simple bit shift
        int scaleBits = srcBits - dstBits;
        return srcVal >>> scaleBits;
    }
}
