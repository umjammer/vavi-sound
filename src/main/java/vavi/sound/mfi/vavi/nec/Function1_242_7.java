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
 * NEC System exclusive message function 0x01, 0xf2, 0x07 processor.
 * (Channel Status control information)
 * <p>
 * 16 bytes, one per channel, which is exactly the 4 tracks * 4 channels an
 * MFi 3.0 file addresses. Unlike the MA-7 message ({@link Function2_242_7})
 * the bytes are <b>not</b> rotated, the converter copies the SMAF channel
 * status bytes straight through (`CnvMA5MFi_N.dll` at {@code 0x10017e93}).
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see vavi.sound.smaf.chunk.ChannelStatus
 */
public class Function1_242_7 implements MachineDependentFunction {

    /** number of channels an MFi 3.0 file addresses */
    public static final int CHANNELS = 16;

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "1_242_7";
    }

    /**
     * 0x01, 0xf2, 0x07 Channel Status control information
     *
     * @param message see below
     * <pre>
     * 0        delta
     * 1        ff
     * 2        ff
     * 3-4      length
     * 5        vendor
     *
     * 6        01
     * 7        f2
     * 8        ....0111
     *              ~~~~
     *              +------ 0x7
     *
     * 9~24     channel status, one byte per channel
     *          76543210
     *          ~~ ||  ~~
     *          |  ||  ++-- type (0: no care, 1: melody, 2: no melody, 3: rhythm)
     *          |  |+------ LED
     *          |  +------- vibration
     *          +---------- KCS (key control status), 2 means on
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
        .filter(i -> channelStatuses[i] != 0)
        .mapToObj(i -> "[%d] type=%d, KCS=%d, LED=%d, vibration=%d".formatted(
                i, getType(i), getKeyControlStatus(i), (channelStatuses[i] & 0x10) >> 4, (channelStatuses[i] & 0x20) >> 5))
        .collect(Collectors.joining(", ")));
    }

    /** one per {@link #CHANNELS} */
    private int[] channelStatuses = new int[CHANNELS];

    /** SMAF channel status bytes, one per {@link #CHANNELS} */
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
        return channelStatuses[channel] & 0x03;
    }

    /** key control status, 2 means on */
    public int getKeyControlStatus(int channel) {
        return (channelStatuses[channel] & 0xc0) >> 6;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[4 + CHANNELS];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf2;
        tmp[3] = (byte) 0x07;

        for (int i = 0; i < CHANNELS; i++) {
            tmp[4 + i] = (byte) channelStatuses[i];
        }

        return tmp;
    }
}
