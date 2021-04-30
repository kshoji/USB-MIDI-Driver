package jp.kshoji.driver.midi.util;

import android.support.annotation.NonNull;
import android.util.Log;
import android.util.Pair;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import jp.kshoji.driver.midi.device.Midi2InputDevice;
import jp.kshoji.driver.midi.device.Midi2OutputDevice;
import jp.kshoji.driver.midi.listener.NullOnMidi2InputEventListener;

/**
 * Implements MIDI Capability Inquiry sequence
 *
 * @author K.Shoji
 */
public class MidiCapabilityNegotiator extends NullOnMidi2InputEventListener {
    static final byte[] broadcastMuid = {0x7f, 0x7f, 0x7f, 0x7f};
    private static final Random random = new Random();
    final byte[] sourceMuid = generateRandomMuid();
    private final List<Pair<MidiProtocol, MidiExtension>> acceptableMidiProtocols = new ArrayList<>();
    private Midi2InputDevice midiInputDevice;
    private Midi2OutputDevice midiOutputDevice;
    private NegotiationPhase currentNegotiationPhase;
    private byte[] destinationMuid;
    private NegotiationThread negotiationThread;

    /**
     * Constructor
     *
     * @param midiInputDevice  the {@link Midi2InputDevice}
     * @param midiOutputDevice the {@link Midi2OutputDevice}
     */
    public MidiCapabilityNegotiator(Midi2InputDevice midiInputDevice, Midi2OutputDevice midiOutputDevice) {
        if (midiInputDevice == null || midiOutputDevice == null) {
            // need both devices
            return;
        }
        this.midiInputDevice = midiInputDevice;
        this.midiOutputDevice = midiOutputDevice;

        midiInputDevice.setMidiEventListener(this);
    }

    /**
     * Use in source MUID
     *
     * @return generated random MUID
     */
    static byte[] generateRandomMuid() {
        byte[] randomBytes = new byte[4];

        // The value of the MUID shall be in the range 0x00000000 to 0x0FFFFFFF.
        // The values 0x0FFFFF00 to 0x0FFFFFFE are reserved.
        int muid = random.nextInt(0x0FFFFF00);
        randomBytes[0] = (byte) ((muid >> 21) & 0x7f);
        randomBytes[1] = (byte) ((muid >> 14) & 0x7f);
        randomBytes[2] = (byte) ((muid >> 7) & 0x7f);
        randomBytes[3] = (byte) (muid & 0x7f);

        return randomBytes;
    }

    /**
     * Starts MIDI protocol negotiation
     *
     * @param midiNegotiationCallback called on protocol negotiation finishing
     * @param isInitiator true: Initiator, false: Responder
     */
    public void startProtocolNegotiation(MidiNegotiationCallback midiNegotiationCallback, boolean isInitiator) {
        if (midiInputDevice == null || midiOutputDevice == null) {
            // need both devices, treat as MIDI 1.0
            if (midiInputDevice != null) {
                midiInputDevice.setProtocolInformation(MidiProtocol.VERSION_1, MidiExtension.NONE);
            }
            if (midiOutputDevice != null) {
                midiOutputDevice.setProtocolInformation(MidiProtocol.VERSION_1, MidiExtension.NONE);
            }
            midiNegotiationCallback.onMidiNegotiated(MidiProtocol.VERSION_1, MidiExtension.NONE);
            return;
        }
        if (negotiationThread != null && currentNegotiationPhase != NegotiationPhase.CONFIRMATION_NEW_PROTOCOL_ESTABLISHED) {
            // already running
            Log.d(Constants.TAG, "Negotiation Thread has been already running!");
            return;
        }

        negotiationThread = new NegotiationThread(midiNegotiationCallback);
        currentNegotiationPhase = isInitiator ? NegotiationPhase.INITIATE_PROTOCOL_NEGOTIATION : NegotiationPhase.START_PROTOCOL_NEGOTIATION;
        negotiationThread.start();
    }

    /**
     * stops the watching thread
     */
    public void stop(Runnable onStopped) {
        negotiationThread.stopThread(onStopped);
    }

    /**
     * Creates Initiate Protocol Negotiation message
     *
     * @return sysex data
     */
    private byte[] createInitiateProtocolNegotiationMessage() {
        return new byte[]{
                (byte) 0xf0, 0x7e, 0x7f,
                0x0d, // MIDI CI Sub-ID #1
                0x10, // MIDI CI Sub-ID #2 : Initiate Protocol Negotiation
                0x01, // MIDI CI Message Version/Format
                sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                broadcastMuid[0], broadcastMuid[1], broadcastMuid[2], broadcastMuid[3],
                0x60, // Authority Level: Highest
                0x03, // Number of Supported Protocols
                0x02, 0x00, 0x00, 0x00, 0x00, // Protocol #0 : MIDI 2.0
                0x01, 0x00, 0x00, 0x02, 0x00, // Protocol #1 : MIDI 1.0 size extended
                0x01, 0x00, 0x00, 0x00, 0x00, // Protocol #2 : MIDI 1.0
                (byte) 0xf7,
        };
    }

    /**
     * Creates Reply to Initiate Protocol Negotiation message
     *
     * @return sysex data
     */
    private byte[] createReplyToInitiateProtocolNegotiationMessage() {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            stream.write(new byte[]{
                    (byte) 0xf0, 0x7e, 0x7f,
                    0x0d, // MIDI CI Sub-ID #1
                    0x11, // MIDI CI Sub-ID #2 : Reply to Initiate Protocol Negotiation
                    0x01, // MIDI CI Message Version/Format
                    sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                    broadcastMuid[0], broadcastMuid[1], broadcastMuid[2], broadcastMuid[3],
                    0x60, // Authority Level: Highest
                    (byte) acceptableMidiProtocols.size(), // Number of Supported Protocols
            });

            // apply acceptableMidiProtocols
            for (int i = 0; i < acceptableMidiProtocols.size(); i++) {
                Pair<MidiProtocol, MidiExtension> acceptable = acceptableMidiProtocols.get(i);
                int extensionValue = 0;
                switch (acceptable.second) {
                    case NONE:
                        extensionValue = 0;
                        break;
                    case JITTER_REDUCTION:
                        extensionValue = 1;
                        break;
                    case PACKET_SIZE_EXTENSION:
                        extensionValue = 2;
                        break;
                    case ALL:
                        extensionValue = 3;
                        break;
                }

                stream.write(new byte[] {
                        (byte) (acceptable.first == MidiProtocol.VERSION_1 ? 1 : 0),
                        0x00,
                        0x00,
                        (byte) extensionValue,
                });
            }
            stream.write((byte)0xf7);
        } catch (IOException ignored) {
        }

        return stream.toByteArray();
    }

    /**
     * Creates Set New Protocol message
     *
     * @param midiProtocol  protocol version
     * @param midiExtension extension information
     * @return sysex data
     */
    private byte[] createSetNewProtocolMessage(MidiProtocol midiProtocol, MidiExtension midiExtension) {
        byte midiExtensionValue = 0;
        switch (midiExtension) {
            case NONE:
                midiExtensionValue = 0;
                break;
            case JITTER_REDUCTION:
                midiExtensionValue = 1;
                break;
            case PACKET_SIZE_EXTENSION:
                if (midiProtocol == MidiProtocol.VERSION_1) {
                    midiExtensionValue = 2;
                }
                break;
            case ALL:
                if (midiProtocol == MidiProtocol.VERSION_1) {
                    midiExtensionValue = 3;
                } else {
                    midiExtensionValue = 1;
                }
                break;
        }

        return new byte[]{
                (byte) 0xf0, 0x7e, 0x7f,
                0x0d, // MIDI CI Sub-ID #1
                0x12, // MIDI CI Sub-ID #2 : Initiate Protocol Negotiation
                0x01, // MIDI CI Message Version/Format
                sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                destinationMuid[0], destinationMuid[1], destinationMuid[2], destinationMuid[3],
                0x60, // Authority Level: Highest
                // specify acceptable MIDI version
                (byte) (midiProtocol == MidiProtocol.VERSION_1 ? 0x01 : 0x02),  // New Protocol Type: MIDI 2.0
                midiExtensionValue,  // New Protocol Extension
                0x00, 0x00, 0x00,
                (byte) 0xf7,
        };
    }

    /**
     * Creates Test New Protocol Initiator to Responder message
     *
     * @return sysex data
     */
    private byte[] createTestNewProtocolInitToRespMessage() {
        return new byte[]{
                (byte) 0xf0, 0x7e, 0x7f,
                0x0d, // MIDI CI Sub-ID #1
                0x13, // Universal System Exclusive Sub-ID#2: Test New Protocol Initiator to Responder
                0x01, // MIDI CI Message Version/Format
                sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                destinationMuid[0], destinationMuid[1], destinationMuid[2], destinationMuid[3],
                0x60, // Authority Level: Highest
                // TestData: string of 48 numbers in ascending order: 0x00, 0x01, 0x02 ... 0x2E, 0x2F
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,
                0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f,
                (byte) 0xf7,
        };
    }

    /**
     * Creates Test New Protocol Responder to Initiator message
     *
     * @return sysex data
     */
    private byte[] createTestNewProtocolRespToInitMessage() {
        return new byte[]{
                (byte) 0xf0, 0x7e, 0x7f,
                0x0d, // MIDI CI Sub-ID #1
                0x14, // Universal System Exclusive Sub-ID#2: Test New Protocol Responder to Initiator
                0x01, // MIDI CI Message Version/Format
                sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                destinationMuid[0], destinationMuid[1], destinationMuid[2], destinationMuid[3],
                0x60, // Authority Level: Highest
                // TestData: string of 48 numbers in ascending order: 0x00, 0x01, 0x02 ... 0x2E, 0x2F
                0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f,
                0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18, 0x19, 0x1a, 0x1b, 0x1c, 0x1d, 0x1e, 0x1f,
                0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f,
                (byte) 0xf7,
        };
    }

    /**
     * Creates Confirmation New Protocol Established message
     *
     * @return sysex data
     */
    private byte[] createConfirmationNewProtocolEstablishedMessage() {
        return new byte[]{
                (byte) 0xf0, 0x7e, 0x7f,
                0x0d, // MIDI CI Sub-ID #1
                0x15, // Universal System Exclusive Sub-ID#2: Confirmation New Protocol Established
                0x01, // MIDI CI Message Version/Format
                sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                destinationMuid[0], destinationMuid[1], destinationMuid[2], destinationMuid[3],
                0x60, // Authority Level: Highest
                (byte) 0xf7,
        };
    }

    @Override
    public void onMidi2SystemExclusive(@NonNull Midi2InputDevice sender, int group, int streamId, @NonNull byte[] systemExclusive) {
        switch (currentNegotiationPhase) {
            case START_PROTOCOL_NEGOTIATION:
                // [Responder] parse Initiate Protocol Negotiation request
            {
                byte[] header = {
                        (byte) 0xf0, 0x7e, 0x7f,
                        0x0d, 0x10, 0x01,
                        sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                };
                if (systemExclusive.length < header.length + 12) {
                    // invalid length
                    return;
                }
                boolean headerAccepted = true;
                for (int i = 0; i < header.length; i++) {
                    if (header[i] != systemExclusive[i]) {
                        headerAccepted = false;
                        break;
                    }
                }
                if (!headerAccepted) {
                    return;
                }
                int numberOfSupportedProtocols = systemExclusive[15];
                if (numberOfSupportedProtocols < 1) {
                    return;
                }
                if (systemExclusive.length < 17 + numberOfSupportedProtocols * 5) {
                    // invalid length
                    return;
                }
                // store destination MUID
                destinationMuid = new byte[]{
                        systemExclusive[10],
                        systemExclusive[11],
                        systemExclusive[12],
                        systemExclusive[13],
                };

                for (int i = 0; i < numberOfSupportedProtocols; i++) {
                    MidiProtocol protocol = systemExclusive[16 + i * 5] == 2 ? MidiProtocol.VERSION_2 : MidiProtocol.VERSION_1;
                    MidiExtension extension = MidiExtension.NONE;
                    switch (systemExclusive[16 + i * 5 + 2]) {
                        case 0:
                            extension = MidiExtension.NONE;
                            break;
                        case 1:
                            extension = MidiExtension.JITTER_REDUCTION;
                            break;
                        case 2:
                            extension = MidiExtension.PACKET_SIZE_EXTENSION;
                            break;
                        case 3:
                            extension = MidiExtension.ALL;
                            break;
                    }
                    acceptableMidiProtocols.add(new Pair<>(protocol, extension));
                }
                currentNegotiationPhase = NegotiationPhase.REPLY_TO_INITIATE_PROTOCOL_NEGOTIATION;
            }
            break;

            case INITIATE_PROTOCOL_NEGOTIATION:
                // [Initiator] send Initiate Protocol Negotiation packet
                // do nothing here.
                break;

            case REPLY_TO_INITIATE_PROTOCOL_NEGOTIATION:
                // [Initiator] parse Reply to Initiate Protocol Negotiation Message
            {
                byte[] header = {
                        (byte) 0xf0, 0x7e, 0x7f,
                        0x0d, 0x11, 0x01,
                        sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                };
                if (systemExclusive.length < header.length + 12) {
                    // invalid length
                    return;
                }
                boolean headerAccepted = true;
                for (int i = 0; i < header.length; i++) {
                    if (header[i] != systemExclusive[i]) {
                        headerAccepted = false;
                        break;
                    }
                }
                if (!headerAccepted) {
                    return;
                }
                int numberOfSupportedProtocols = systemExclusive[15];
                if (numberOfSupportedProtocols < 1) {
                    return;
                }
                if (systemExclusive.length < 17 + numberOfSupportedProtocols * 5) {
                    // invalid length
                    return;
                }
                // store destination MUID
                destinationMuid = new byte[]{
                        systemExclusive[10],
                        systemExclusive[11],
                        systemExclusive[12],
                        systemExclusive[13],
                };

                for (int i = 0; i < numberOfSupportedProtocols; i++) {
                    MidiProtocol protocol = systemExclusive[16 + i * 5] == 2 ? MidiProtocol.VERSION_2 : MidiProtocol.VERSION_1;
                    MidiExtension extension = MidiExtension.NONE;
                    switch (systemExclusive[16 + i * 5 + 2]) {
                        case 0:
                            extension = MidiExtension.NONE;
                            break;
                        case 1:
                            extension = MidiExtension.JITTER_REDUCTION;
                            break;
                        case 2:
                            extension = MidiExtension.PACKET_SIZE_EXTENSION;
                            break;
                        case 3:
                            extension = MidiExtension.ALL;
                            break;
                    }
                    acceptableMidiProtocols.add(new Pair<>(protocol, extension));
                }
                currentNegotiationPhase = NegotiationPhase.SET_NEW_PROTOCOL;
            }
            break;

            case SET_NEW_PROTOCOL:
                // [Responder] parse Set New Protocol request
            {
                byte[] header = {
                        (byte) 0xf0, 0x7e, 0x7f,
                        0x0d, 0x12, 0x01,
                        sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                };
                if (systemExclusive.length < header.length + 11) {
                    // invalid length
                    return;
                }
                boolean headerAccepted = true;
                for (int i = 0; i < header.length; i++) {
                    if (header[i] != systemExclusive[i]) {
                        headerAccepted = false;
                        break;
                    }
                }
                if (!headerAccepted) {
                    return;
                }

                {
                    MidiProtocol protocol = systemExclusive[16] == 2 ? MidiProtocol.VERSION_2 : MidiProtocol.VERSION_1;
                    MidiExtension extension = MidiExtension.NONE;
                    switch (systemExclusive[18]) {
                        case 0:
                            extension = MidiExtension.NONE;
                            break;
                        case 1:
                            extension = MidiExtension.JITTER_REDUCTION;
                            break;
                        case 2:
                            extension = MidiExtension.PACKET_SIZE_EXTENSION;
                            break;
                        case 3:
                            extension = MidiExtension.ALL;
                            break;
                    }

                    // switch protocol
                    midiInputDevice.setProtocolInformation(protocol, extension);
                    midiOutputDevice.setProtocolInformation(protocol, extension);
                }
                currentNegotiationPhase = NegotiationPhase.TEST_NEW_PROTOCOL_INIT_TO_RESP;
            }
            break;

            case TEST_NEW_PROTOCOL_INIT_TO_RESP:
                // [Responder] parse Test New Protocol Initiator to Responder Message
            {
                byte[] header = {
                        (byte) 0xf0, 0x7e, 0x7f,
                        0x0d, 0x13, 0x01,
                        sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                };
                if (systemExclusive.length < header.length + 54) {
                    // invalid length
                    return;
                }
                boolean headerAccepted = true;
                for (int i = 0; i < header.length; i++) {
                    if (header[i] != systemExclusive[i]) {
                        headerAccepted = false;
                        break;
                    }
                }
                if (!headerAccepted) {
                    return;
                }

                {
                    // send Test New Protocol Responder to Initiator Message
                    byte[] reply = createTestNewProtocolRespToInitMessage();
                    midiOutputDevice.sendMidi2SystemExclusive(0, 0, reply);
                }
                currentNegotiationPhase = NegotiationPhase.TEST_NEW_PROTOCOL_RESP_TO_INIT;
            }
            break;

            case TEST_NEW_PROTOCOL_RESP_TO_INIT:
                // [Initiator] parse Test New Protocol Responder to Initiator Message
            {
                byte[] header = {
                        (byte) 0xf0, 0x7e, 0x7f,
                        0x0d, 0x14, 0x01,
                        sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                        destinationMuid[0], destinationMuid[1], destinationMuid[2], destinationMuid[3],
                };
                if (systemExclusive.length < header.length + 8) {
                    // invalid length
                    return;
                }
                boolean headerAccepted = true;
                for (int i = 0; i < header.length; i++) {
                    if (header[i] != systemExclusive[i]) {
                        headerAccepted = false;
                        break;
                    }
                }
                if (!headerAccepted) {
                    return;
                }
                if (systemExclusive.length < 17 + 48) {
                    // invalid length
                    return;
                }

                for (int i = 0; i < 48; i++) {
                    if (systemExclusive[16 + i] != i) {
                        return;
                    }
                }
                currentNegotiationPhase = NegotiationPhase.CONFIRMATION_NEW_PROTOCOL_ESTABLISHED;
            }
            break;

            case CONFIRMATION_NEW_PROTOCOL_ESTABLISHED:
                // [Responder] parse Confirmation New Protocol Established Message
            {
                byte[] header = {
                        (byte) 0xf0, 0x7e, 0x7f,
                        0x0d, 0x15, 0x01,
                        sourceMuid[0], sourceMuid[1], sourceMuid[2], sourceMuid[3],
                        destinationMuid[0], destinationMuid[1], destinationMuid[2], destinationMuid[3],
                };
                if (systemExclusive.length < header.length + 6) {
                    // invalid length
                    return;
                }
                boolean headerAccepted = true;
                for (int i = 0; i < header.length; i++) {
                    if (header[i] != systemExclusive[i]) {
                        headerAccepted = false;
                        break;
                    }
                }
                if (!headerAccepted) {
                    return;
                }
                currentNegotiationPhase = NegotiationPhase.START_PROTOCOL_NEGOTIATION;
            }
            break;
        }
    }

    private enum NegotiationPhase {
        START_PROTOCOL_NEGOTIATION,
        INITIATE_PROTOCOL_NEGOTIATION,
        REPLY_TO_INITIATE_PROTOCOL_NEGOTIATION,
        SET_NEW_PROTOCOL,
        TEST_NEW_PROTOCOL_INIT_TO_RESP,
        TEST_NEW_PROTOCOL_RESP_TO_INIT,
        CONFIRMATION_NEW_PROTOCOL_ESTABLISHED,
        END_PROTOCOL_NEGOTIATION,
    }

    /**
     * MIDI Protocol major version
     */
    public enum MidiProtocol {
        VERSION_1,
        VERSION_2,
    }

    /**
     * MIDI Extension mode
     */
    public enum MidiExtension {
        NONE,
        JITTER_REDUCTION,
        PACKET_SIZE_EXTENSION, // MIDI 1.0 only
        ALL, // MIDI 1.0 only
    }

    /**
     * Callback interface for MIDI Protocol Negotiation
     */
    public interface MidiNegotiationCallback {
        /**
         * Called on MIDI protocol/extension being negotiated
         *
         * @param midiProtocol  protocol version
         * @param midiExtension extension information
         */
        void onMidiNegotiated(MidiProtocol midiProtocol, MidiExtension midiExtension);
    }

    /**
     * The thread for MIDI Protocol Negotiation
     */
    final class NegotiationThread extends Thread {
        private final MidiNegotiationCallback midiNegotiationCallback;
        private volatile boolean stopFlag;
        private Runnable onStopped;

        NegotiationThread(MidiNegotiationCallback midiNegotiationCallback) {
            stopFlag = false;
            this.midiNegotiationCallback = midiNegotiationCallback;
        }

        void stopThread(Runnable onStopped) {
            this.onStopped = onStopped;
            stopFlag = true;
        }

        @Override
        public void run() {
            while (!stopFlag) {
                switch (currentNegotiationPhase) {
                    case START_PROTOCOL_NEGOTIATION:
                        // [Responder] wait for Initiate Protocol Negotiation packet
                        // do nothing here.
                        break;
                    case INITIATE_PROTOCOL_NEGOTIATION:
                    {
                        // [Initiator] send Initiate Protocol Negotiation packet
                        byte[] systemExclusive = createInitiateProtocolNegotiationMessage();
                        midiOutputDevice.sendMidi2SystemExclusive(0, 0, systemExclusive);
                    }
                    currentNegotiationPhase = NegotiationPhase.REPLY_TO_INITIATE_PROTOCOL_NEGOTIATION;
                    break;
                    case REPLY_TO_INITIATE_PROTOCOL_NEGOTIATION:
                        // [Responder] Initiate Protocol Negotiation packet received
                    {
                        // send Reply to Initiate Protocol Negotiation Message
                        byte[] systemExclusive = createReplyToInitiateProtocolNegotiationMessage();
                        midiOutputDevice.sendMidi2SystemExclusive(0, 0, systemExclusive);
                    }
                    currentNegotiationPhase = NegotiationPhase.SET_NEW_PROTOCOL;
                    break;
                    case SET_NEW_PROTOCOL:
                        // [Initiator] Reply to Initiate Protocol Negotiation Message received
                        if (acceptableMidiProtocols.size() < 1) {
                            // no acceptable protocols: MIDI 1.0
                            midiInputDevice.setProtocolInformation(MidiProtocol.VERSION_1, MidiExtension.NONE);
                            midiOutputDevice.setProtocolInformation(MidiProtocol.VERSION_1, MidiExtension.NONE);
                            midiNegotiationCallback.onMidiNegotiated(MidiProtocol.VERSION_1, MidiExtension.NONE);
                            currentNegotiationPhase = NegotiationPhase.END_PROTOCOL_NEGOTIATION;
                        } else {
                            Pair<MidiProtocol, MidiExtension> acceptable = acceptableMidiProtocols.get(0);
                            {
                                // send Set New Protocol packet
                                byte[] systemExclusive = createSetNewProtocolMessage(acceptable.first, acceptable.second);
                                midiOutputDevice.sendMidi2SystemExclusive(0, 0, systemExclusive);
                            }

                            // 100ms Pause Time for Switching
                            try {
                                sleep(100);
                            } catch (InterruptedException ignored) {
                            }

                            {
                                // send Test New Protocol Init->Resp packet
                                byte[] systemExclusive = createTestNewProtocolInitToRespMessage();
                                midiOutputDevice.sendMidi2SystemExclusive(0, 0, systemExclusive);
                            }
                            currentNegotiationPhase = NegotiationPhase.TEST_NEW_PROTOCOL_INIT_TO_RESP;
                        }
                        break;
                    case TEST_NEW_PROTOCOL_INIT_TO_RESP:
                        // [Responder] Test New Protocol Init->Resp packet received
                    {
                        // send Test New Protocol Resp->Init packet
                        byte[] systemExclusive = createTestNewProtocolRespToInitMessage();
                        midiOutputDevice.sendMidi2SystemExclusive(0, 0, systemExclusive);
                    }
                    currentNegotiationPhase = NegotiationPhase.TEST_NEW_PROTOCOL_RESP_TO_INIT;
                    break;
                    case TEST_NEW_PROTOCOL_RESP_TO_INIT:
                        // [Initiator] Test New Protocol Responder to Initiator Message received
                    {
                        // send Confirmation New Protocol Established packet
                        byte[] systemExclusive = createConfirmationNewProtocolEstablishedMessage();
                        midiOutputDevice.sendMidi2SystemExclusive(0, 0, systemExclusive);
                    }

                    if (acceptableMidiProtocols.size() < 1) {
                        // no acceptable protocols: MIDI 1.0
                        midiInputDevice.setProtocolInformation(MidiProtocol.VERSION_1, MidiExtension.NONE);
                        midiOutputDevice.setProtocolInformation(MidiProtocol.VERSION_1, MidiExtension.NONE);
                        midiNegotiationCallback.onMidiNegotiated(MidiProtocol.VERSION_1, MidiExtension.NONE);
                    } else {
                        // established MIDI version
                        Pair<MidiProtocol, MidiExtension> acceptable = acceptableMidiProtocols.get(0);
                        midiInputDevice.setProtocolInformation(acceptable.first, acceptable.second);
                        midiOutputDevice.setProtocolInformation(acceptable.first, acceptable.second);
                        midiNegotiationCallback.onMidiNegotiated(acceptable.first, acceptable.second);
                    }
                    currentNegotiationPhase = NegotiationPhase.END_PROTOCOL_NEGOTIATION;
                    break;
                    case CONFIRMATION_NEW_PROTOCOL_ESTABLISHED:
                        // [Responder] Confirmation New Protocol Established packet received
                        if (acceptableMidiProtocols.size() < 1) {
                            // no acceptable protocols: MIDI 1.0
                            midiInputDevice.setProtocolInformation(MidiProtocol.VERSION_1, MidiExtension.NONE);
                            midiOutputDevice.setProtocolInformation(MidiProtocol.VERSION_1, MidiExtension.NONE);
                            midiNegotiationCallback.onMidiNegotiated(MidiProtocol.VERSION_1, MidiExtension.NONE);
                        } else {
                            // established MIDI version
                            Pair<MidiProtocol, MidiExtension> acceptable = acceptableMidiProtocols.get(0);
                            midiInputDevice.setProtocolInformation(acceptable.first, acceptable.second);
                            midiOutputDevice.setProtocolInformation(acceptable.first, acceptable.second);
                            midiNegotiationCallback.onMidiNegotiated(acceptable.first, acceptable.second);
                        }
                        currentNegotiationPhase = NegotiationPhase.START_PROTOCOL_NEGOTIATION;
                        break;
                }

                // Initiator finished
                if (currentNegotiationPhase == NegotiationPhase.END_PROTOCOL_NEGOTIATION) {
                    break;
                }

                waitForPhaseChange(currentNegotiationPhase, 300);

                // Initiator finished(timeout)
                if (currentNegotiationPhase == NegotiationPhase.END_PROTOCOL_NEGOTIATION) {
                    break;
                }
            }

            if (onStopped != null) {
                onStopped.run();
            }
        }

        void waitForPhaseChange(NegotiationPhase waitingPhase, long timeout) {
            long initialTime = System.currentTimeMillis();
            while (currentNegotiationPhase == waitingPhase) {
                if (stopFlag) {
                    break;
                }

                try {
                    sleep(10);
                } catch (InterruptedException ignored) {
                }

                if (System.currentTimeMillis() - initialTime > timeout) {
                    switch (currentNegotiationPhase) {
                        case INITIATE_PROTOCOL_NEGOTIATION:
                        case SET_NEW_PROTOCOL:
                        case TEST_NEW_PROTOCOL_RESP_TO_INIT:
                        case REPLY_TO_INITIATE_PROTOCOL_NEGOTIATION:
                            // Negotiation has timeout: Treat as MIDI 1.0
                            midiInputDevice.setProtocolInformation(MidiProtocol.VERSION_1, MidiExtension.NONE);
                            midiOutputDevice.setProtocolInformation(MidiProtocol.VERSION_1, MidiExtension.NONE);
                            midiNegotiationCallback.onMidiNegotiated(MidiProtocol.VERSION_1, MidiExtension.NONE);
                            currentNegotiationPhase = NegotiationPhase.END_PROTOCOL_NEGOTIATION;
                            break;
                        default:
                            currentNegotiationPhase = NegotiationPhase.START_PROTOCOL_NEGOTIATION;
                            break;
                    }
                    break;
                }
            }
        }
    }
}
