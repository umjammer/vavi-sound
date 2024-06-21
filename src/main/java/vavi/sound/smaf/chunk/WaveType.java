/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import static java.lang.System.getLogger;


/**
 * WaveType.
 * <pre>
 * 7    mono, stereo
 *
 * 6
 * 5    format
 * 4
 *
 * 3
 *
 * 2
 * 1    freq
 * 0
 * 16 bit
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050101 nsano initial version <br>
 */
public class WaveType {

    private static final Logger logger = getLogger(WaveType.class.getName());

    /** 1, 2 */
    private final int waveChannels;

    /**
     * TODO different numbers in chunks, can it be solved with enum?
     * "Awa*"
     * <pre>
     * 0x0 Signed
     * 0x1 ADPCM
     * 0x2 TwinVQ
     * 0x3 MP3
     * </pre>
     * "Mwa*"
     * <pre>
     * 0x0 2's complement PCM
     * 0x1 Offset Binary PCM
     * 0x2 ADPCM(YAMAHA)
     * </pre>
     * using common number for now
     * <pre>
     * 0 Signed
     * 1 ADPCM
     * 2 TwinVQ
     * 3 MP3
     * 4 2's complement PCM (signed PCM?)
     * 5 Offset Binary PCM (unsigned PCM?)
     * </pre>
     */
    private final int waveFormat;

    /** */
    private static final int[] samplingFreqs = { 4000, 8000, 11000, 22050, 44100 };

    /** 4000, 8000, 11000, 22050, 44100 */
    private final int waveSamplingFreq;

    /** 4, 8, 12, 16 */
    private final int waveBaseBit;

    /** */
    private static final int[] tableForMwq = { 4, 5, 1 };

    /**
     * "Awa*"
     * <pre>
     * 7 654 3210 7654 3210
     * ~ ~~~ ~~~~ ~~~~ ~~~~
     * | |   |    |    +--- reserved
     * | |   |    +-------- base bit
     * | |   +------------- sampling freq
     * | +----------------- format
     * +------------------- channels
     * </pre>
     */
    WaveType(int waveType) throws IOException {
//logger.log(Level.DEBUG, "waveType: " + StringUtil.toHex4(waveType));
        this.waveChannels = (waveType & 0x8000) != 0 ? 2 : 1;
        this.waveFormat = (waveType & 0x7000) >> 12;
        this.waveSamplingFreq = samplingFreqs[(waveType & 0x0f00) >> 8];
        this.waveBaseBit = 4 * (((waveType & 0x00f0) >> 4) + 1);
logger.log(Level.DEBUG, "waveType: " + this);
    }

    /**
     * "Mwa*"
     * <pre>
     * 0: 7 654 3210
     *    ~ ~~~ ~~~~
     *    | |   +--- base bit
     *    | +------- format
     *    +--------- channels
     * 1, 2: sampling freq
     * </pre>
     */
    WaveType(byte[] waveType) throws IOException {
logger.log(Level.DEBUG, String.format("waveType: %02x %02x %02x", waveType[0], waveType[1], waveType[2]));
        this.waveChannels = (waveType[0] & 0x80) != 0 ? 2 : 1;
        this.waveFormat = tableForMwq[(waveType[0] & 0x70) >> 4];
        this.waveBaseBit = 4 * ((waveType[0] & 0x0f) + 1);
        this.waveSamplingFreq = ((waveType[1] & 0xff) << 8) | waveType[2] & 0xff;
logger.log(Level.DEBUG, "waveType: " + this);
    }

    /**
     *
     * @param waveChannels 1, 2
     * @param waveFormat 0: Signed, 1: ADPCM, 2: TwinVQ, 3: MP3
     * @param waveSamplingFreq 4000, 8000, 11000, 22050, 44100
     * @param waveBaseBit 4, 8, 12, 16
     */
    public WaveType(int waveChannels, int waveFormat, int waveSamplingFreq, int waveBaseBit) {
        this.waveChannels = waveChannels;
        this.waveFormat = waveFormat;
        this.waveSamplingFreq = waveSamplingFreq;
        this.waveBaseBit = waveBaseBit;
    }

    /** for "Awa*" */
    int intValue() {
        int waveType = 0;
        waveType |= waveChannels == 2 ? 0x8000: 0;
        waveType |= waveFormat << 12; // TODO 5, 6
        int v = switch (waveSamplingFreq) {
            case 4000 -> 0;
            default -> 1;
            case 11000 -> 2;
            case 22050 -> 3;
            case 44100 -> 4;
        };
        waveType |= v << 8;
        waveType |= (waveBaseBit / 4 - 1) << 4;
        return waveType;
    }

    /** @return 1, 2 */
    int getWaveChannels() {
        return waveChannels;
    }

    /** @return "Awa*": 0, 1, 2, 3 */
    int getWaveFormat() {
        return waveFormat;
    }

    /** @return 4000, 8000, 11000, 22050, 44100 */
    int getWaveSamplingFreq() {
        return waveSamplingFreq;
    }

    /** @return 4, 8, 12, 16 */
    int getWaveBaseBit() {
        return waveBaseBit;
    }

    /** */
    public String toString() {
        return "waveChannels: " + waveChannels +
            ", waveFormat: " + waveFormat +
            ", waveSamplingFreq: " + waveSamplingFreq +
            ", waveBaseBit: " + waveBaseBit +
            " (waveType: " + String.format("%04x", intValue()) + ")";
    }
}
