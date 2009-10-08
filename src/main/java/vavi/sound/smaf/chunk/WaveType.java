/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;

import vavi.util.Debug;
import vavi.util.StringUtil;


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
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050101 nsano initial version <br>
 */
public class WaveType {

    /** 1, 2 */
    private int waveChannels;

    /**
     * TODO チャンクで数値が違う enum で解消？
     * "Awa*"
     * <pre>
     * 0x0 Signed
     * 0x1 ADPCM
     * 0x2 TwinVQ
     * 0x3 MP3
     * </pre>
     * "Mwa*"
     * <pre>
     * 0x0 2’s complement PCM
     * 0x1 Offset Binary PCM
     * 0x2 ADPCM(YAMAHA)
     * </pre>
     */
    private int waveFormat;

    /** */
    private static final int[] samplingFreqs = { 4000, 8000, 11000, 22050, 44100 };

    /** 4000, 8000, 11000, 22050, 44100 */
    private int waveSamplingFreq;

    /** 4, 8, 12, 16 */
    private int waveBaseBit;

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
Debug.println("waveType: " + StringUtil.toHex4(waveType));
        this.waveChannels = (waveType & 0x8000) != 0 ? 2 : 1;
        this.waveFormat = (waveType & 0x7000) >> 12;
        this.waveSamplingFreq = samplingFreqs[(waveType & 0x0f00) >> 8];
        this.waveBaseBit = 4 * (((waveType & 0x00f0) >> 4) + 1);
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
Debug.println("waveType: " + StringUtil.toHex2(waveType[0]) + " " + StringUtil.toHex2(waveType[1]) + " " + StringUtil.toHex2(waveType[2]));
        this.waveChannels = (waveType[0] & 0x80) != 0 ? 2 : 1;
        this.waveFormat = (waveType[0] & 0x70) >> 12;
        this.waveBaseBit = 4 * ((waveType[0] & 0x0f) + 1);
        this.waveSamplingFreq = (waveType[1] << 8) | waveType[2];
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
        waveType |= waveFormat << 12;
        int v;
        switch (waveSamplingFreq) {
        case 4000:
            v = 0;
            break;
        case 8000:
        default:
            v = 1;
        break;
        case 11000:
            v = 2;
            break;
        case 22050:
            v = 3;
            break;
        case 44100:
            v = 4;
            break;
        }
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
            " (waveType: " + StringUtil.toHex4(intValue()) + ")";
    }
}

/* */
