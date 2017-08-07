package jp.kshoji.driver.midi.device;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseIntArray;

import jp.kshoji.driver.midi.listener.OnMidiInputEventListener;
import jp.kshoji.driver.midi.util.ReusableByteArrayOutputStream;
import jp.kshoji.driver.midi.util.UsbMidiDeviceUtils;

/**
 * MIDI Input Device
 *
 * @author K.Shoji
 */
public final class MidiInputDevice {
    // for RPN/NRPN
    private static final int RPN_STATUS_NONE = 0;
    private static final int RPN_STATUS_RPN = 1;
    private static final int RPN_STATUS_NRPN = 2;

    private static final int CABLE_COUNT = 16;

    private final UsbDevice usbDevice;
    final UsbDeviceConnection usbDeviceConnection;
    private final UsbInterface usbInterface;
    final UsbEndpoint inputEndpoint;

    private OnMidiInputEventListener midiEventListener;

    private final WaiterThread waiterThread;

    /**
     * Constructor
     *
     * @param usbDevice           the UsbDevice
     * @param usbDeviceConnection the UsbDeviceConnection
     * @param usbInterface        the UsbInterface
     * @throws IllegalArgumentException endpoint not found.
     */
    public MidiInputDevice(@NonNull UsbDevice usbDevice, @NonNull UsbDeviceConnection usbDeviceConnection, @NonNull UsbInterface usbInterface, @NonNull UsbEndpoint usbEndpoint) throws IllegalArgumentException {
        this.usbDevice = usbDevice;
        this.usbDeviceConnection = usbDeviceConnection;
        this.usbInterface = usbInterface;

        waiterThread = new WaiterThread();

        inputEndpoint = usbEndpoint;

        usbDeviceConnection.claimInterface(usbInterface, true);
        waiterThread.setPriority(8);
        waiterThread.setName("MidiInputDevice[" + usbDevice.getDeviceName() + "].WaiterThread");
        waiterThread.start();
    }

    /**
     * Sets the OnMidiInputEventListener
     *
     * @param midiEventListener the OnMidiInputEventListener
     */
    public void setMidiEventListener(OnMidiInputEventListener midiEventListener) {
        this.midiEventListener = midiEventListener;
        this.waiterThread.setOnMidiInputEventListener(midiEventListener);
    }

    /**
     * stops the watching thread
     */
    void stop() {
        midiEventListener = null;
        usbDeviceConnection.releaseInterface(usbInterface);

        waiterThread.stopFlag = true;
        resume();

        // blocks while the thread will stop
        while (waiterThread.isAlive()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * Suspends event listening
     */
    public void suspend() {
        synchronized (waiterThread.suspendSignal) {
            waiterThread.suspendFlag = true;
        }
    }

    /**
     * Resumes event listening
     */
    public void resume() {
        synchronized (waiterThread.suspendSignal) {
            waiterThread.suspendFlag = false;
            waiterThread.suspendSignal.notifyAll();
        }
    }

    /**
     * Get the product name
     *
     * @return the product name. null if API Level < {@link android.os.Build.VERSION_CODES#HONEYCOMB_MR2 }, or the product name is truly null
     */
    @Nullable
    public String getProductName() {
        return UsbMidiDeviceUtils.getProductName(usbDevice, usbDeviceConnection);
    }

    /**
     * Get the manufacturer name
     *
     * @return the manufacturer name. null if API Level < {@link android.os.Build.VERSION_CODES#HONEYCOMB_MR2 }, or the manufacturer name is truly null
     */
    @Nullable
    public String getManufacturerName() {
        return UsbMidiDeviceUtils.getManufacturerName(usbDevice, usbDeviceConnection);
    }

    /**
     * Get the device name(linux device path)
     *
     * @return the device name(linux device path)
     */
    @NonNull
    public String getDeviceAddress() {
        return usbDevice.getDeviceName();
    }

    /**
     * @return the usbDevice
     */
    @NonNull
    public UsbDevice getUsbDevice() {
        return usbDevice;
    }

    /**
     * @return the usbInterface
     */
    @Deprecated
    @NonNull
    public UsbInterface getUsbInterface() {
        return usbInterface;
    }

    /**
     * @return the usbEndpoint
     */
    @Deprecated
    @NonNull
    public UsbEndpoint getUsbEndpoint() {
        return inputEndpoint;
    }

    /**
     * Polling thread for input data.
     * Loops infinitely while stopFlag == false.
     *
     * @author K.Shoji
     */
    final class WaiterThread extends Thread {
        volatile boolean stopFlag;
        final Object suspendSignal = new Object();
        volatile boolean suspendFlag;
        private OnMidiInputEventListener midiEventListener = MidiInputDevice.this.midiEventListener;

        void setOnMidiInputEventListener(OnMidiInputEventListener listener) {
            midiEventListener = listener;
        }

        /**
         * Constructor
         */
        WaiterThread() {
            stopFlag = false;
            suspendFlag = false;
        }

        @Override
        public void run() {
            final UsbDeviceConnection deviceConnection = usbDeviceConnection;
            final UsbEndpoint usbEndpoint = inputEndpoint;
            final int maxPacketSize = inputEndpoint.getMaxPacketSize();
            final MidiInputDevice sender = MidiInputDevice.this;

            // prepare buffer variables
            final byte[] bulkReadBuffer = new byte[maxPacketSize];
            final byte[] readBuffer = new byte[maxPacketSize * 2]; // *2 for safety (BUFFER_LENGTH+4 would be enough)
            int readBufferSize = 0;
            final byte[] read = new byte[maxPacketSize * 2];
            int length;
            int cable;
            int codeIndexNumber;
            int byte1;
            int byte2;
            int byte3;
            int i;
            int readSize;
            int unreadSize;

            // for RPN/NRPN
            final int[] rpnNrpnFunction = new int[CABLE_COUNT];
            final int[] rpnNrpnValueMsb = new int[CABLE_COUNT];
            final int[] rpnNrpnValueLsb = new int[CABLE_COUNT];
            final int[] rpnStatus = new int[CABLE_COUNT];
            final int[] rpnFunctionMsb = new int[CABLE_COUNT];
            final int[] rpnFunctionLsb = new int[CABLE_COUNT];
            final int[] nrpnFunctionMsb = new int[CABLE_COUNT];
            final int[] nrpnFunctionLsb = new int[CABLE_COUNT];

            final SparseIntArray[] rpnCacheMsb = new SparseIntArray[CABLE_COUNT];
            final SparseIntArray[] rpnCacheLsb = new SparseIntArray[CABLE_COUNT];
            final SparseIntArray[] nrpnCacheMsb = new SparseIntArray[CABLE_COUNT];
            final SparseIntArray[] nrpnCacheLsb = new SparseIntArray[CABLE_COUNT];

            // for SysEx
            final ReusableByteArrayOutputStream[] systemExclusive = new ReusableByteArrayOutputStream[CABLE_COUNT];

            for (i = 0; i < CABLE_COUNT; i++) {
                rpnStatus[i] = RPN_STATUS_NONE;
                rpnFunctionMsb[i] = 0x7f;
                rpnFunctionLsb[i] = 0x7f;
                nrpnFunctionMsb[i] = 0x7f;
                nrpnFunctionLsb[i] = 0x7f;
                rpnCacheMsb[i] = new SparseIntArray();
                rpnCacheLsb[i] = new SparseIntArray();
                nrpnCacheMsb[i] = new SparseIntArray();
                nrpnCacheLsb[i] = new SparseIntArray();
                systemExclusive[i] = new ReusableByteArrayOutputStream();
            }

            // Don't allocate instances in the loop, as much as possible.
            while (!stopFlag) {
                length = deviceConnection.bulkTransfer(usbEndpoint, bulkReadBuffer, maxPacketSize, 10);

                synchronized (suspendSignal) {
                    if (suspendFlag) {
                        try {
                            // check the deviceConnection to ignore events while suspending.
                            // Note: Events received within last sleeping(100msec) will be sent to the midiEventListener.
                            suspendSignal.wait(100);
                        } catch (InterruptedException e) {
                            // ignore exception
                        }
                        continue;
                    }
                }

                if (length <= 0) {
                    continue;
                }

                System.arraycopy(bulkReadBuffer, 0, readBuffer, readBufferSize, length);
                readBufferSize += length;

                if (readBufferSize < 4) {
                    // more data needed
                    continue;
                }

                // USB MIDI data stream: 4 bytes boundary
                readSize = readBufferSize / 4 * 4;
                System.arraycopy(readBuffer, 0, read, 0, readSize); // fill the read array

                // keep unread bytes
                unreadSize = readBufferSize - readSize;
                if (unreadSize > 0) {
                    System.arraycopy(readBuffer, readSize, readBuffer, 0, unreadSize);
                    readBufferSize = unreadSize;
                } else {
                    readBufferSize = 0;
                }

                for (i = 0; i < readSize; i += 4) {
                    cable = (read[i] >> 4) & 0xf;
                    codeIndexNumber = read[i] & 0xf;
                    byte1 = read[i + 1] & 0xff;
                    byte2 = read[i + 2] & 0xff;
                    byte3 = read[i + 3] & 0xff;

                    switch (codeIndexNumber) {
                        case 0:
                            if (midiEventListener != null) {
                                midiEventListener.onMidiMiscellaneousFunctionCodes(sender, cable, byte1, byte2, byte3);
                            }
                            break;
                        case 1:
                            if (midiEventListener != null) {
                                midiEventListener.onMidiCableEvents(sender, cable, byte1, byte2, byte3);
                            }
                            break;
                        case 2:
                            // system common message with 2 bytes
                            if (midiEventListener != null) {
                                switch (byte1) {
                                    case 0xf1:
                                        midiEventListener.onMidiTimeCodeQuarterFrame(sender, cable, byte2 & 0x7f);
                                        break;
                                    case 0xf3:
                                        midiEventListener.onMidiSongSelect(sender, cable, byte2 & 0x7f);
                                        break;
                                }

                                byte[] bytes = new byte[]{(byte) byte1, (byte) byte2};
                                midiEventListener.onMidiSystemCommonMessage(sender, cable, bytes);
                            }
                            break;
                        case 3:
                            // system common message with 3 bytes
                            if (midiEventListener != null) {
                                switch (byte1) {
                                    case 0xf2:
                                        midiEventListener.onMidiSongPositionPointer(sender, cable, (byte2 & 0x7f) | ((byte3 & 0x7f) << 7));
                                        break;
                                }

                                byte[] bytes = new byte[]{(byte) byte1, (byte) byte2, (byte) byte3};
                                midiEventListener.onMidiSystemCommonMessage(sender, cable, bytes);
                            }
                            break;
                        case 4:
                            // sysex starts, and has next
                            synchronized (systemExclusive[cable]) {
                                systemExclusive[cable].write(byte1);
                                systemExclusive[cable].write(byte2);
                                systemExclusive[cable].write(byte3);
                            }
                            break;
                        case 5:
                            // system common message with 1byte
                            // sysex end with 1 byte
                            synchronized (systemExclusive[cable]) {
                                systemExclusive[cable].write(byte1);
                                if (midiEventListener != null) {
                                    byte[] sysexBytes = systemExclusive[cable].toByteArray();
                                    if (sysexBytes.length == 1) {
                                        switch (sysexBytes[0] & 0xff) {
                                            case 0xf6:
                                                midiEventListener.onMidiTuneRequest(sender, cable);
                                                break;
                                            case 0xf8:
                                                midiEventListener.onMidiTimingClock(sender, cable);
                                                break;
                                            case 0xfa:
                                                midiEventListener.onMidiStart(sender, cable);
                                                break;
                                            case 0xfb:
                                                midiEventListener.onMidiContinue(sender, cable);
                                                break;
                                            case 0xfc:
                                                midiEventListener.onMidiStop(sender, cable);
                                                break;
                                            case 0xfe:
                                                midiEventListener.onMidiActiveSensing(sender, cable);
                                                break;
                                            case 0xff:
                                                midiEventListener.onMidiReset(sender, cable);
                                                break;
                                        }
                                    }
                                    midiEventListener.onMidiSystemExclusive(sender, cable, sysexBytes);
                                }
                                systemExclusive[cable].reset();
                            }
                            break;
                        case 6:
                            // sysex end with 2 bytes
                            synchronized (systemExclusive[cable]) {
                                systemExclusive[cable].write(byte1);
                                systemExclusive[cable].write(byte2);
                                if (midiEventListener != null) {
                                    midiEventListener.onMidiSystemExclusive(sender, cable, systemExclusive[cable].toByteArray());
                                }
                                systemExclusive[cable].reset();
                            }
                            break;
                        case 7:
                            // sysex end with 3 bytes
                            synchronized (systemExclusive[cable]) {
                                systemExclusive[cable].write(byte1);
                                systemExclusive[cable].write(byte2);
                                systemExclusive[cable].write(byte3);
                                if (midiEventListener != null) {
                                    midiEventListener.onMidiSystemExclusive(sender, cable, systemExclusive[cable].toByteArray());
                                }
                                systemExclusive[cable].reset();
                            }
                            break;
                        case 8:
                            if (midiEventListener != null) {
                                midiEventListener.onMidiNoteOff(sender, cable, byte1 & 0xf, byte2, byte3);
                            }
                            break;
                        case 9:
                            if (midiEventListener != null) {
                                if (byte3 == 0x00) {
                                    midiEventListener.onMidiNoteOff(sender, cable, byte1 & 0xf, byte2, byte3);
                                } else {
                                    midiEventListener.onMidiNoteOn(sender, cable, byte1 & 0xf, byte2, byte3);
                                }
                            }
                            break;
                        case 10:
                            // poly key press
                            if (midiEventListener != null) {
                                midiEventListener.onMidiPolyphonicAftertouch(sender, cable, byte1 & 0xf, byte2, byte3);
                            }
                            break;
                        case 11:
                            // control change
                            if (midiEventListener != null) {
                                midiEventListener.onMidiControlChange(sender, cable, byte1 & 0xf, byte2, byte3);
                            }

                            // process RPN/NRPN messages
                            switch (byte2) {
                                case 6: {
                                    // RPN/NRPN value MSB
                                    rpnNrpnValueMsb[cable] = byte3 & 0x7f;
                                    if (rpnStatus[cable] == RPN_STATUS_RPN) {
                                        rpnNrpnFunction[cable] = ((rpnFunctionMsb[cable] & 0x7f) << 7) | (rpnFunctionLsb[cable] & 0x7f);
                                        rpnCacheMsb[cable].put(rpnNrpnFunction[cable], rpnNrpnValueMsb[cable]);
                                        rpnNrpnValueLsb[cable] = rpnCacheLsb[cable].get(rpnNrpnFunction[cable], 0/*if not found*/);
                                        if (midiEventListener != null) {
                                            midiEventListener.onMidiRPNReceived(sender, cable, byte1 & 0xf, rpnNrpnFunction[cable], (rpnNrpnValueMsb[cable] << 7 | rpnNrpnValueLsb[cable]));
                                            midiEventListener.onMidiRPNReceived(sender, cable, byte1 & 0xf, rpnNrpnFunction[cable], rpnNrpnValueMsb[cable], rpnNrpnValueLsb[cable]);
                                        }
                                    } else if (rpnStatus[cable] == RPN_STATUS_NRPN) {
                                        rpnNrpnFunction[cable] = ((nrpnFunctionMsb[cable] & 0x7f) << 7) | (nrpnFunctionLsb[cable] & 0x7f);
                                        nrpnCacheMsb[cable].put(rpnNrpnFunction[cable], rpnNrpnValueMsb[cable]);
                                        rpnNrpnValueLsb[cable] = nrpnCacheLsb[cable].get(rpnNrpnFunction[cable], 0/*if not found*/);
                                        if (midiEventListener != null) {
                                            midiEventListener.onMidiNRPNReceived(sender, cable, byte1 & 0xf, rpnNrpnFunction[cable], (rpnNrpnValueMsb[cable] << 7 | rpnNrpnValueLsb[cable]));
                                            midiEventListener.onMidiNRPNReceived(sender, cable, byte1 & 0xf, rpnNrpnFunction[cable], rpnNrpnValueMsb[cable], rpnNrpnValueLsb[cable]);
                                        }
                                    }
                                    break;
                                }
                                case 38: {
                                    // RPN/NRPN value LSB
                                    rpnNrpnValueLsb[cable] = byte3 & 0x7f;
                                    if (rpnStatus[cable] == RPN_STATUS_RPN) {
                                        rpnNrpnFunction[cable] = ((rpnFunctionMsb[cable] & 0x7f) << 7) | (rpnFunctionLsb[cable] & 0x7f);
                                        rpnNrpnValueMsb[cable] = rpnCacheMsb[cable].get(rpnNrpnFunction[cable], 0/*if not found*/);
                                        rpnCacheLsb[cable].put(rpnNrpnFunction[cable], rpnNrpnValueLsb[cable]);
                                        if (midiEventListener != null) {
                                            midiEventListener.onMidiRPNReceived(sender, cable, byte1 & 0xf, rpnNrpnFunction[cable], (rpnNrpnValueMsb[cable] << 7 | rpnNrpnValueLsb[cable]));
                                            midiEventListener.onMidiRPNReceived(sender, cable, byte1 & 0xf, rpnNrpnFunction[cable], rpnNrpnValueMsb[cable], rpnNrpnValueLsb[cable]);
                                        }
                                    } else if (rpnStatus[cable] == RPN_STATUS_NRPN) {
                                        rpnNrpnFunction[cable] = ((nrpnFunctionMsb[cable] & 0x7f) << 7) | (nrpnFunctionLsb[cable] & 0x7f);
                                        rpnNrpnValueMsb[cable] = nrpnCacheMsb[cable].get(rpnNrpnFunction[cable], 0/*if not found*/);
                                        nrpnCacheLsb[cable].put(rpnNrpnFunction[cable], rpnNrpnValueLsb[cable]);
                                        if (midiEventListener != null) {
                                            midiEventListener.onMidiNRPNReceived(sender, cable, byte1 & 0xf, rpnNrpnFunction[cable], (rpnNrpnValueMsb[cable] << 7 | rpnNrpnValueLsb[cable]));
                                            midiEventListener.onMidiNRPNReceived(sender, cable, byte1 & 0xf, rpnNrpnFunction[cable], rpnNrpnValueMsb[cable], rpnNrpnValueLsb[cable]);
                                        }
                                    }
                                    break;
                                }
                                case 98: {
                                    // NRPN parameter number LSB
                                    nrpnFunctionLsb[cable] = byte3 & 0x7f;
                                    rpnStatus[cable] = RPN_STATUS_NRPN;
                                    break;
                                }
                                case 99: {
                                    // NRPN parameter number MSB
                                    nrpnFunctionMsb[cable] = byte3 & 0x7f;
                                    rpnStatus[cable] = RPN_STATUS_NRPN;
                                    break;
                                }
                                case 100: {
                                    // RPN parameter number LSB
                                    rpnFunctionLsb[cable] = byte3 & 0x7f;
                                    if (rpnFunctionMsb[cable] == 0x7f && rpnFunctionLsb[cable] == 0x7f) {
                                        rpnStatus[cable] = RPN_STATUS_NONE;
                                    } else {
                                        rpnStatus[cable] = RPN_STATUS_RPN;
                                    }
                                    break;
                                }
                                case 101: {
                                    // RPN parameter number MSB
                                    rpnFunctionMsb[cable] = byte3 & 0x7f;
                                    if (rpnFunctionMsb[cable] == 0x7f && rpnFunctionLsb[cable] == 0x7f) {
                                        rpnStatus[cable] = RPN_STATUS_NONE;
                                    } else {
                                        rpnStatus[cable] = RPN_STATUS_RPN;
                                    }
                                    break;
                                }
                                default:
                                    break;
                            }
                            break;
                        case 12:
                            // program change
                            if (midiEventListener != null) {
                                midiEventListener.onMidiProgramChange(sender, cable, byte1 & 0xf, byte2);
                            }
                            break;
                        case 13:
                            // channel pressure
                            if (midiEventListener != null) {
                                midiEventListener.onMidiChannelAftertouch(sender, cable, byte1 & 0xf, byte2);
                            }
                            break;
                        case 14:
                            // pitch bend
                            if (midiEventListener != null) {
                                midiEventListener.onMidiPitchWheel(sender, cable, byte1 & 0xf, byte2 | (byte3 << 7));
                            }
                            break;
                        case 15:
                            // single byte
                            if (midiEventListener != null) {
                                switch (byte1) {
                                    case 0xf6:
                                        midiEventListener.onMidiTuneRequest(sender, cable);
                                        break;
                                    case 0xf8:
                                        midiEventListener.onMidiTimingClock(sender, cable);
                                        break;
                                    case 0xfa:
                                        midiEventListener.onMidiStart(sender, cable);
                                        break;
                                    case 0xfb:
                                        midiEventListener.onMidiContinue(sender, cable);
                                        break;
                                    case 0xfc:
                                        midiEventListener.onMidiStop(sender, cable);
                                        break;
                                    case 0xfe:
                                        midiEventListener.onMidiActiveSensing(sender, cable);
                                        break;
                                    case 0xff:
                                        midiEventListener.onMidiReset(sender, cable);
                                        break;
                                }

                                midiEventListener.onMidiSingleByte(sender, cable, byte1);
                            }
                            break;
                        default:
                            // do nothing.
                            break;
                    }
                }
            }
        }
    }
}
