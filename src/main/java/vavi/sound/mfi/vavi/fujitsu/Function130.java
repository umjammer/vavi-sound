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

import static java.lang.System.getLogger;


/**
 * Fujitsu System exclusive message function 0x82 processor.
 * (Wave Channel Panpot)
 * <p>
 * The companion of the 0x81 volume ({@code Function129} of vavi-sound-nda): same
 * one byte payload, same {@code channel, 6 bit value} split, exactly the way
 * {@link vavi.sound.mfi.vavi.sharp.Function130} pairs with
 * {@link vavi.sound.mfi.vavi.sharp.Function129}. All 19 messages in the ~4400 file
 * corpus at {@code ~/Public/np2/mfi} carry {@code 0x20}, that is channel 0 centered
 * (0x20 of 0x00 ~ 0x3f), which is what a mono ADPCM stream wants.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function130 extends FujitsuFunction {

    private static final Logger logger = getLogger(Function130.class.getName());

    @Override
    int getFunction() {
        return 0x82;
    }

    /**
     * 0x82 ADPCM panpot
     *
     * @param data see below
     * <pre>
     * 0    delta
     * 1    ff
     * 2    ff
     * 3-4  length
     * 5    vendor
     * 6    0x82
     * 7    CCPPPPPP
     *        CC: channel
     *        PPPPPP: panpot, 0x00 left ~ 0x20 center ~ 0x3f right
     * </pre>
     */
    @Override
    public void process(byte[] data, Receiver receiver)
        throws InvalidMfiDataException {

        this.channel = (data[7] & 0xc0) >> 6;
        this.panpot  =  data[7] & 0x3f;
logger.log(Level.DEBUG, "ADPCM panpot: %dch %02x".formatted(channel, panpot));
    }

    /** 0 ~ 3 */
    private int channel;
    /** 0x00 left ~ 0x20 center ~ 0x3f right */
    private int panpot = 0x20;

    /** 0 ~ 3 */
    public int getChannel() {
        return channel;
    }

    /** 0x00 left ~ 0x20 center ~ 0x3f right */
    public int getPanpot() {
        return panpot;
    }

    /** */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** */
    public void setPanpot(int panpot) {
        this.panpot = panpot & 0x3f;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[3];
        tmp[0] = (byte) (VENDOR_FUJITSU | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x82;
        tmp[2] = (byte) ((channel << 6) | panpot);
        return tmp;
    }
}
