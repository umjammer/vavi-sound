/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependFunction;
import vavi.sound.mfi.vavi.track.MachineDependMessage;
import vavi.util.Debug;


/**
 * NEC System exclusive message function 0x01, 0xf0, 0x07 processor.
 * (拡張ストリーム波形制御情報)
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 *          0.01 030829 nsano complete <br>
 */
public class Function1_240_7 implements MachineDependFunction {

    /** このデータのヘッダ分長さ */
    public static final int HEADER_LENGTH = 13;

    /**
     * 0x01, 0xf0, 0x07 拡張ストリーム波形制御情報
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
     * 7        f0
     * 8        0x07
     *
     * 9        stream number
     * 10       7.....10
     *          ~     ~~
     *          |     +-------- format
     *          +-------------- mono
     * 11-12    sampling rate
     * 13-      data ...
     *
     * </pre>
     */
    public void process(MachineDependMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.streamNumber = data[ 9] & 0xff;            // stream number 0 ~ 31
        this.mono =        (data[10] & 0x80) == 0;      // 0: mono, 1: stereo
        this.format =      (data[10] & 0x03) >> 1;      // 0: 4bit ADPCM (0db Center 固定), 1: 4bit ADPCM
        this.sampleRate =  (data[11] << 8) + data[12];  // 4000 ~ 16000 (stereo 時は半分)

        int adpcmLength = data.length - HEADER_LENGTH;
Debug.println("ADPCM: No." + streamNumber + ", " + sampleRate + "Hz, " + adpcmLength + " bytes, " + (mono ? "mono" : "stereo") + ", " + format);
//Debug.dump(data, 64);

        this.adpcm = new byte[adpcmLength];
        System.arraycopy(data, HEADER_LENGTH, adpcm, 0, adpcmLength);

        NecSequencer.getAudioEngine().setData(streamNumber, -1, sampleRate, 4, mono ? 1 : 2, adpcm, false);
    }

    /** stream number 0 ~ 31 */
    private int streamNumber;
    /** 0: mono, 1: stereo */
    private boolean mono;
    /** 0: 4bit ADPCM (0db Center 固定), 1: 4bit ADPCM */
    private int format;
    /** 4000 ~ 16000 (stereo 時は半分) */
    private int sampleRate;
    /** little endian */
    private byte[] adpcm;

    //-------------------------------------------------------------------------

    /** ADPCM */
    public void setAdpcm(byte[] adpcm) {
        this.adpcm = adpcm;
    }

    /**
     * @param format 0: 4bit ADPCM (0db Center 固定), 1: 4bit ADPCM (wav2mld use 1)
     */
    public void setFormat(int format) {
        this.format = format & 0x03;
    }

    /** */
    public void setStreamNumber(int streamNumber) {
        this.streamNumber = streamNumber & 0xff;
    }

    /**
     * @param mono true mono
     */
    public void setMono(boolean mono) {
        this.mono = mono;
    }

    /** @param samplingRate {4000 ~ 16000} are available */
    public void setSamplingRate(int samplingRate) {
        this.sampleRate = samplingRate;
    }

    /**
     * 事前に #setPcm(InputStream), #setSamplingRate(int)
     * を用いてフィールドを設定しておいて下さい。
     */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        // [14~]
        byte[] tmp = new byte[adpcm.length + 8];

        // [5~]
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf0;
        tmp[3] = (byte) 0x07;
        tmp[4] = (byte) streamNumber;
        tmp[5] = (byte) ((mono ? 0x00 : 0x80) | format);
        tmp[6] = (byte) ((sampleRate & 0xff00) >> 8);
        tmp[7] = (byte)  (sampleRate & 0x00ff);

        System.arraycopy(adpcm, 0, tmp, 8, adpcm.length);

        return tmp;
    }
}

/* */
