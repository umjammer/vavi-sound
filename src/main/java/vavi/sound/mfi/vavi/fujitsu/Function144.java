/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.fujitsu;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * Fujitsu System exclusive message function 0x90 processor.
 * (Wave Data, the MFi 4.0 wave table)
 * <p>
 * One of the four messages the {@code MFi4PlugIn_F} writes per wave table voice,
 * always in this order: 0x90 the wave, {@link Function145} its playback
 * parameters, {@link Function146} the voice, {@link Function147} a voice
 * parameter. This one carries the samples, the destination offset says where in
 * the wave memory they land.
 * </p>
 * <p>
 * How it was identified, since no MFi document was available (the numbers are
 * over the 48 messages of the ~4400 file corpus at {@code ~/Public/np2/mfi}):
 * </p>
 * <ul>
 *  <li>{@code data.length - 11} is the 16 bit value at {@code data[9]} for every
 *      message, so {@code data[9..10]} is the length of the wave and
 *      {@code data[7..8]} something else of the same width.</li>
 *  <li>Within a file the messages come with {@code data[7..8]} ascending and each
 *      one is exactly the previous offset plus the previous length rounded up to
 *      a multiple of 4, first one 0 - it is a destination offset into wave memory
 *      and the waves are packed there 4 byte aligned.</li>
 *  <li>{@link Function145} confirms it: its start address field is
 *      {@code 0x8000 | (offset &gt;&gt; 2)} for all 48, and its loop points fall
 *      inside {@code 0 ~ length}.</li>
 *  <li>The samples are signed 8 bit: read that way the mean of every wave is
 *      within +-4 of zero and the mean absolute step between neighbours is 2.8 ~
 *      35, read unsigned the step is up to 135 - that is, unsigned wraps and
 *      signed does not.</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function144 extends FujitsuFunction {

    private static final Logger logger = getLogger(Function144.class.getName());

    /** the length of this message's header, {@code data[0]} ~ {@code data[10]} */
    public static final int HEADER_LENGTH = 11;

    /** wave memory is filled 4 byte aligned */
    public static final int ALIGNMENT = 4;

    @Override
    int getFunction() {
        return 0x90;
    }

    /**
     * 0x90 Wave Data    MFi4
     *
     * @param data see below
     * <pre>
     * 0     delta
     * 1     ff
     * 2     ff
     * 3-4   length
     * 5     vendor
     * 6     0x90
     * 7-8   destination offset in wave memory [byte]
     * 9-10  wave length [byte]
     * 11-   wave, signed 8 bit pcm
     * </pre>
     */
    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.offset = ((data[7] & 0xff) << 8) | (data[8] & 0xff);
        int length  = ((data[9] & 0xff) << 8) | (data[10] & 0xff);

        if (length != data.length - HEADER_LENGTH) {
logger.log(Level.WARNING, "wave length: %d, but %d bytes follow".formatted(length, data.length - HEADER_LENGTH));
            length = Math.min(length, data.length - HEADER_LENGTH);
        }

        this.wave = new byte[length];
        System.arraycopy(data, HEADER_LENGTH, wave, 0, length);

logger.log(Level.DEBUG, "wave data: @%04x, %d bytes".formatted(offset, length));
logger.log(Level.TRACE, "wave:\n" + StringUtil.getDump(wave, 64));
    }

    /** destination offset in wave memory [byte] */
    private int offset;

    /** signed 8 bit pcm */
    private byte[] wave = new byte[0];

    /** destination offset in wave memory [byte] */
    public int getOffset() {
        return offset;
    }

    /** the offset the next wave of the same file goes to */
    public int getNextOffset() {
        return (offset + wave.length + (ALIGNMENT - 1)) & ~(ALIGNMENT - 1);
    }

    /** signed 8 bit pcm */
    public byte[] getWave() {
        return wave;
    }

    /** destination offset in wave memory [byte] */
    public void setOffset(int offset) {
        this.offset = offset & 0xffff;
    }

    /** @param wave signed 8 bit pcm */
    public void setWave(byte[] wave) {
        this.wave = wave;
    }

    /**
     * @before {@link #setOffset(int)}
     * @before {@link #setWave(byte[])}
     */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[6 + wave.length];
        tmp[0] = (byte) (getVendor() | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x90;
        tmp[2] = (byte) ((offset >> 8) & 0xff);
        tmp[3] = (byte) (offset & 0xff);
        tmp[4] = (byte) ((wave.length >> 8) & 0xff);
        tmp[5] = (byte) (wave.length & 0xff);
        System.arraycopy(wave, 0, tmp, 6, wave.length);
        return tmp;
    }
}
