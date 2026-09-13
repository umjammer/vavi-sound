/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi.message;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.vavi.chunk.TrackChunk.FormatType;


/**
 * The Sequence Data encoding of {@link FormatType#HandyPhoneStandard}.
 * <p>
 * An event is a duration followed by 2 or 3 bytes, a control event being
 * <pre>
 *  duration    1 or 2
 *  data0       0x00
 *  data1       cc 11 nnnn  cc: channel, nnnn: {@code control}
 *  data2       value
 * </pre>
 * which is what {@link #control(int, int, int, int)} writes, and a note being a status byte
 * followed by a gate time ({@link #note(int, int, int)}).
 * </p>
 * <p>
 * TODO a message does not know which {@link FormatType} it was read as, so this is all
 *      {@link SmafMessage#getMessage()} is able to write for now.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260912 nsano initial version <br>
 * @see "vavi.sound.smaf.vavi.chunk.SequenceDataChunk#readHandyPhoneStandard, the counterpart"
 */
final class HandyPhoneStandard {

    private HandyPhoneStandard() {
    }

    /** the largest duration or gate time, {@link #writeVariableLength} is 2 bytes at most */
    static final int maxSteps = 16511;

    /**
     * Encodes an event.
     * @param duration in {@code Timebase_D} steps, 0 ~ {@link #maxSteps}
     * @param data the event, the bytes which follow the duration
     */
    static byte[] message(int duration, int... data) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            writeVariableLength(dos, duration);
            for (int datum : data) {
                dos.writeByte(datum);
            }
            dos.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e); // never, it is a ByteArrayOutputStream
        }
        return baos.toByteArray();
    }

    /**
     * Encodes a control event.
     * @param channel smaf channel 0x00 ~ 0x03
     * @param control the lower nibble of data1
     * @param value 0x00 ~ 0xff
     */
    static byte[] control(int duration, int channel, int control, int value) {
        return message(duration, 0x00, ((channel & 0x03) << 6) | 0x30 | (control & 0x0f), value);
    }

    /**
     * Encodes a note event.
     * @param status cc oo nnnn, channel, octave and note
     * @param gateTime in {@code Timebase_G} steps, 1 ~ {@link #maxSteps}
     */
    static byte[] note(int duration, int status, int gateTime) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            writeVariableLength(dos, duration);
            dos.writeByte(status);
            writeVariableLength(dos, gateTime);
            dos.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e); // never, it is a ByteArrayOutputStream
        }
        return baos.toByteArray();
    }

    /**
     * Writes a duration or a gate time. One byte below 0x80, two bytes up to {@link #maxSteps},
     * the high 7 bits are stored decremented so that 2 bytes never encode what 1 byte can.
     *
     * @throws IllegalArgumentException when the value does not fit in 2 bytes
     */
    static void writeVariableLength(DataOutput out, int value) throws IOException {
        if (value < 0) {
            throw new IllegalArgumentException("negative: " + value);
        } else if (value < 0x80) {
            out.writeByte(value);
        } else if (value <= maxSteps) {
            out.writeByte(0x80 | ((value >> 7) - 1));
            out.writeByte(value & 0x7f);
        } else {
            throw new IllegalArgumentException("larger than " + maxSteps + ": " + value);
        }
    }
}
