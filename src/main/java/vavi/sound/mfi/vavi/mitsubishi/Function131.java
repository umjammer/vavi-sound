/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mitsubishi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mobile.AudioEngine;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * Mitsubishi System exclusive message function 0x83 processor.
 * (Wave Packet Data)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class Function131 implements MachineDependentFunction {

    private static final Logger logger = getLogger(Function131.class.getName());

    /** not use data immediately, store temporarily */
    public static final int MODE_STORE = 0;
    /** use data immediately */
    public static final int MODE_SET = 1;
    /** recycle mode wave size = 0 */
    public static final int MODE_RECYCLE = 2;
    /** reserved */
    public static final int MODE_RESERVED = 3;

    /** header length of this data */
    public static final int HEADER_LENGTH = 10;

    /**
     * 0x83 Wave Packet Data    MFi2, MFi3
     *
     * @param message see below
     * <pre>
     * 0        delta
     * 1        ff
     * 2        ff
     * 3-4      length
     * 5        vendor (0x61)
     *
     * 6        0x83
     * 7        76 543210
     *          ~~ ~~~~~~
     *          |  +----------- packet id
     *          +-------------- channel
     * 8        76 5432 10
     *          ~~ ~~~~ ~~
     *          |  |    +------ bits
     *          |  +----------- sampling rate
     *          +-------------- mode
     * 9        .......0
     *                 ~
     *                 +------- continue flag
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel   = (data[7] & 0xc0) >> 6; // 0 ~ 3
        this.packetId  =  data[7] & 0x3f;       // packet id 0 ~ 15
        this.mode      = (data[8] & 0xc0) >> 6; // 0 ~ 3
        int format1    = (data[8] & 0x3c) >> 2; //
        int format2    =  data[8] & 0x03;       //
        this.continued = (data[9] & 0x01) == 1; //

        int adpcmLength = data.length - HEADER_LENGTH;
logger.log(Level.DEBUG, "ADPCM voice: %dch, No.%d, mode=%d, continued=%s, %d".formatted(channel, packetId, mode, continued, adpcmLength));
logger.log(Level.TRACE, "data:\n" + StringUtil.getDump(data, 32));

        this.sampleRate = getSamplingRateInternal(format1);
        this.bits = getSamplingBitsInternal(format2);
logger.log(Level.DEBUG, "sampling: %02x: rate=%d, bits=%d".formatted(data[8] & 0x3f, sampleRate, bits));

        this.adpcm = new byte[adpcmLength];
        System.arraycopy(data, HEADER_LENGTH, adpcm, 0, adpcmLength);

        play();
    }

    /** Plays by mode. */
    protected void play() {

        AudioEngine player = MitsubishiSequencer.getAudioEngine();
        switch (mode) {
        case MODE_STORE:
            player.setData(packetId, channel, sampleRate, bits, 1, adpcm, continued);
            break;
        case MODE_SET:
            player.setData(packetId, channel, sampleRate, bits, 1, adpcm, continued);
            player.start(packetId);
            break;
        case MODE_RECYCLE:
            player.start(packetId);
            break;
        }
    }

    /** adpcm channel no */
    protected int channel;
    /** packet id */
    protected int packetId;
    /** @see #MODE_RECYCLE MODE_* */
    protected int mode = MODE_SET;
    /** sampling rate {4k, 8k, 16k, 32k} */
    protected int sampleRate = 16000;
    /** adpcm minimum unit {2bit, 4bit} */
    protected int bits = 4;
    /** */
    protected boolean continued = false;
    /** ADPCM buffer */
    protected byte[] adpcm;

    /** Gets sampling rate. */
    protected int getSamplingRateInternal(int format1) {
        int sampleRate = 8000;
        switch (format1) {
        case 0x0:    // 0000
            sampleRate = 4000;
            break;
        case 0x1:    // 0001
            sampleRate = 8000;
            break;
        case 0x3:    // 0011
            sampleRate = 16000;
            break;
        case 0x5:    // 0101
            sampleRate = 32000;
            break;
        default:
logger.log(Level.WARNING, "unknown sampling rate: " + format1);
            break;
        }
        return sampleRate;
    }

    /** Get sampling bits. */
    protected int getSamplingBitsInternal(int format2) {
        int bits = 4;
        switch (format2) {
        case 0x0:    // 00
            bits = 2;
            break;
        case 0x1:    // 01
            bits = 4;
            break;
        default:
logger.log(Level.WARNING, "unknown bits: " + format2);
            break;
        }
        return bits;
    }

    // ----

    /** ADPCM channel */
    public void setChannel(int channel) {
        this.channel = channel & 0x03;
    }

    /** packet id */
    public void setPacketId(int packetId) {
        this.packetId = packetId & 0x3f;
    }

    /** */
    public void setMode(int mode) {
        this.mode = mode & 0x03;
    }

    /** @param samplingRate {4k, 8k, 16k, 32k} are available */
    public void setSamplingRate(int samplingRate) {
        this.sampleRate = samplingRate;
    }

    /** @param samplingBits {2bit, 4bit} are available */
    public void setSamplingBits(int samplingBits) {
        this.bits = samplingBits;
    }

    /** */
    public void setContinued(boolean continued) {
        this.continued = continued;
    }

    /** ADPCM */
    public void setAdpcm(byte[] adpcm) {
        this.adpcm = adpcm;
    }

    /** */
    protected int getSamplingRateInternal()  {
        int format1 = switch (this.sampleRate) {
            case 32000 -> 0x14;
            case 16000 -> 0x0c;
            case 8000 -> 0x04;
            case 4000 -> 0x00;
            default -> 0x04;
        };
        return format1;
    }

    /** */
    protected int getSamplingBitsInternal()  {
        int format2 = switch (this.bits) {
            case 4 -> 0x1;
            case 2 -> 0x0;
            default -> 0x1;
        };
        return format2;
    }

    /**
     * Set fields {@link #setAdpcm(byte[])}, {@link #setSamplingRate(int)},
     * using {@link #setSamplingBits(int)} in advance.
     * <li> wav2mld: when continued, mode is MODE_STORE, and at last MODE_RECYCLE
     */
    public byte[] getMessage() throws InvalidMfiDataException {

        // [10~]
        byte[] tmp = new byte[adpcm.length + 5];

        // [8]
        int format1 = getSamplingRateInternal();

        // [8]
        int format2 = getSamplingBitsInternal();

        // [5~]
        tmp[0] = (byte) (VENDOR_MITSUBISHI | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x83;
        tmp[2] = (byte) ((channel << 6) | packetId);        // 7: channel, id
        tmp[3] = (byte) ((mode << 6) | format1 | format2);  // 8: sample rate
        tmp[4] = (byte) (continued ? 0x01 : 0x00);          // 9: continued

        System.arraycopy(adpcm, 0, tmp, 5, adpcm.length);

        return tmp;
    }
}
