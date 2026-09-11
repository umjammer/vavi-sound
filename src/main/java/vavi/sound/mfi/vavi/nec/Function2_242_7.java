/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static vavi.sound.mfi.vavi.nec.NecSequencer.VENDOR_NEC;


/**
 * NEC System exclusive message function 0x02, 0xf2, 0x07 processor.
 * (Channel Status control information)
 * <p>
 * One byte per MA-7 channel. Each byte is the SMAF channel status byte
 * rotated left by 2, see {@link #toSmaf(int)}.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see vavi.sound.smaf.chunk.ChannelStatus
 */
public class Function2_242_7 implements MachineDependentFunction {

    /** number of MA-7 channels */
    public static final int CHANNELS = 32;

    /** unused channels carry this */
    private static final int UNUSED = 0x10;

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "2_242_7";
    }

    /**
     * 0x02, 0xf2, 0x07 Channel Status control information
     *
     * @param message see below
     * <pre>
     * 0        delta
     * 1        ff
     * 2        ff
     * 3-4      length
     * 5        vendor
     *
     * 6        02
     * 7        f2
     * 8        ....0111
     *              ~~~~
     *              +------ 0x7
     *
     * 9~40     channel status, one byte per channel
     *          76543210
     *          ||    ~~
     *          ||    ++-- KCS (key control status)
     *          ||  ~~---- type (0: no care, 1: melody, 2: no melody, 3: rhythm)
     *          |+-------- LED
     *          +--------- vibration
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        if (data.length - 9 != CHANNELS) {
            throw new InvalidMfiDataException("channel status must be " + CHANNELS + " bytes: " + (data.length - 9));
        }

        this.channelStatuses = new int[CHANNELS];
        for (int i = 0; i < CHANNELS; i++) {
            channelStatuses[i] = data[9 + i] & 0xff;
        }

logger.log(Level.DEBUG, "ChannelStatus: " + IntStream.range(0, CHANNELS)
        .filter(i -> channelStatuses[i] != UNUSED)
        .mapToObj(i -> "[%d] type=%d, KCS=%d, LED=%d, vibration=%d".formatted(
                i, getType(i), getKeyControlStatus(i), (channelStatuses[i] & 0x40) >> 6, (channelStatuses[i] & 0x80) >> 7))
        .collect(Collectors.joining(", ")));
    }

    /** one per {@link #CHANNELS} */
    private int[] channelStatuses = new int[CHANNELS];

    /** raw MFi channel status bytes, one per {@link #CHANNELS} */
    public int[] getChannelStatuses() {
        return channelStatuses;
    }

    /** */
    public void setChannelStatuses(int[] channelStatuses) {
        if (channelStatuses.length != CHANNELS) {
            throw new IllegalArgumentException("channel status must be " + CHANNELS + " bytes: " + channelStatuses.length);
        }
        this.channelStatuses = channelStatuses;
    }

    /** 0: no care, 1: melody, 2: no melody, 3: rhythm */
    public int getType(int channel) {
        return (channelStatuses[channel] & 0x0c) >> 2;
    }

    /** key control status, 2 means on */
    public int getKeyControlStatus(int channel) {
        return channelStatuses[channel] & 0x03;
    }

    /** */
    public boolean isUsed(int channel) {
        return channelStatuses[channel] != UNUSED;
    }

    /** the SMAF channel status byte this MFi byte was made of (rotate right by 2) */
    public static int toSmaf(int mfi) {
        return ((mfi >>> 2) | (mfi << 6)) & 0xff;
    }

    /** the MFi channel status byte for a SMAF channel status byte (rotate left by 2) */
    public static int toMfi(int smaf) {
        return ((smaf << 2) | (smaf >>> 6)) & 0xff;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[4 + CHANNELS];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x02;
        tmp[2] = (byte) 0xf2;
        tmp[3] = (byte) 0x07;

        for (int i = 0; i < CHANNELS; i++) {
            tmp[4 + i] = (byte) channelStatuses[i];
        }

        return tmp;
    }
}
