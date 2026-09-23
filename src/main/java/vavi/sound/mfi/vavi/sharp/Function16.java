/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sharp;

import java.lang.System.Logger.Level;
import java.util.Arrays;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;

import static vavi.sound.mfi.vavi.sharp.SharpSequencer.VENDOR_SHARP;


/**
 * Sharp System exclusive message function 0x10 processor.
 * (Wave Data)
 * <p>
 * The first of the three messages a Sharp MFi 3.0 file registers a wave table voice
 * with, 0x10 (this), {@link Function17} and {@link Function18}, all three numbered by
 * the voice. A wave comes in two parts, a header then the samples:
 * </p>
 * <pre>
 * 0     delta
 * 1     ff
 * 2     ff
 * 3-4   length
 * 5     vendor
 * 6     0x10
 * 7     wave (voice) number
 * 8     part, 1: header, 2: samples
 *
 * part 1
 * 9-11  length of the wave [byte]
 * 12-14 loop start [byte]
 * 15-17 loop end [byte]
 *
 * part 2
 * 9-10  length of the wave [byte]
 * 11-   samples, signed 8 bit
 * </pre>
 * <p>
 * Read from the corpus at {@code ~/Public/np2/mfi}, 1553 waves of which 1477 are
 * Sharp's, the other 76 Sony's (0x30, {@code vavi.sound.mfi.vavi.sony.Function48}, the
 * same message):
 * </p>
 * <ul>
 *  <li>each header is followed by the samples of the same wave number, and their
 *      length field is both the length of the samples and the length of the header,
 *      1553 of 1553.</li>
 *  <li>{@code loop start <= loop end} in all of them and {@code loop end} is
 *      {@code length - 3} in 1550.</li>
 *  <li>the samples move smoothly read signed ({@code ff fe fd fc fb ...}) and clip at
 *      0x7f / 0x80.</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function16 implements MachineDependentFunction {

    /** part 1 */
    public static final int PART_HEADER = 1;
    /** part 2 */
    public static final int PART_SAMPLES = 2;

    /** the vendor id, the Sony subclass overrides it */
    protected int getVendor() {
        return VENDOR_SHARP;
    }

    /** the function byte, the Sony subclass overrides it */
    protected int getFunction() {
        return 0x10;
    }

    @Override
    public String getId() {
        return getVendor() + "." + getFunction();
    }

    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.waveNumber = data[7] & 0xff;
        this.part       = data[8] & 0xff;
        switch (part) {
        case PART_HEADER -> {
            if (data.length < 18) {
                throw new InvalidMfiDataException("wave header is too short: " + data.length);
            }
            this.length    = get24(data, 9);
            this.loopStart = get24(data, 12);
            this.loopEnd   = get24(data, 15);
logger.log(Level.DEBUG, "wave header: No.%d, length: %d, loop: %d ~ %d".formatted(waveNumber, length, loopStart, loopEnd));
        }
        case PART_SAMPLES -> {
            this.length = ((data[9] & 0xff) << 8) | (data[10] & 0xff);
            this.wave = Arrays.copyOfRange(data, 11, data.length);
logger.log(Level.DEBUG, "wave: No.%d, %d bytes".formatted(waveNumber, wave.length));
        }
        default ->
logger.log(Level.DEBUG, "wave: No.%d, unknown part: %d".formatted(waveNumber, part));
        }
    }

    /** */
    private static int get24(byte[] data, int offset) {
        return ((data[offset] & 0xff) << 16) | ((data[offset + 1] & 0xff) << 8) | (data[offset + 2] & 0xff);
    }

    /** */
    private int waveNumber;
    /** @see #PART_HEADER */
    private int part = PART_HEADER;
    /** [byte] */
    private int length;
    /** [byte] */
    private int loopStart;
    /** [byte] */
    private int loopEnd;
    /** signed 8 bit */
    private byte[] wave = new byte[0];

    /** */
    public int getWaveNumber() {
        return waveNumber;
    }

    /** @see #PART_HEADER */
    public int getPart() {
        return part;
    }

    /** [byte] */
    public int getLength() {
        return length;
    }

    /** [byte], {@link #PART_HEADER} only */
    public int getLoopStart() {
        return loopStart;
    }

    /** [byte], {@link #PART_HEADER} only */
    public int getLoopEnd() {
        return loopEnd;
    }

    /** signed 8 bit, {@link #PART_SAMPLES} only */
    public byte[] getWave() {
        return wave;
    }

    /** */
    public void setWaveNumber(int waveNumber) {
        this.waveNumber = waveNumber & 0xff;
    }

    /** makes this a {@link #PART_HEADER} */
    public void setHeader(int length, int loopStart, int loopEnd) {
        this.part = PART_HEADER;
        this.length = length;
        this.loopStart = loopStart;
        this.loopEnd = loopEnd;
    }

    /** makes this a {@link #PART_SAMPLES} */
    public void setWave(byte[] wave) {
        this.part = PART_SAMPLES;
        this.length = wave.length;
        this.wave = wave;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp;
        if (part == PART_HEADER) {
            tmp = new byte[4 + 9];
            put24(tmp, 4, length);
            put24(tmp, 7, loopStart);
            put24(tmp, 10, loopEnd);
        } else {
            tmp = new byte[4 + 2 + wave.length];
            tmp[4] = (byte) (length >> 8);
            tmp[5] = (byte) length;
            System.arraycopy(wave, 0, tmp, 6, wave.length);
        }
        tmp[0] = (byte) (getVendor() | CARRIER_DOCOMO);
        tmp[1] = (byte) getFunction();
        tmp[2] = (byte) waveNumber;
        tmp[3] = (byte) part;
        return tmp;
    }

    /** */
    private static void put24(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >> 16);
        data[offset + 1] = (byte) (value >> 8);
        data[offset + 2] = (byte) value;
    }
}
