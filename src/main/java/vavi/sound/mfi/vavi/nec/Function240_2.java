/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mobile.AudioEngine;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * NEC System exclusive message function 0xf0, 0x_2 processor.
 * (ADPCM data)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030827 nsano initial version <br>
 */
public class Function240_2 implements MachineDependentFunction {

    /** このデータのヘッダ分長さ */
    private static final int HEADER_LENGTH = 10;

    /**
     * 0xf0, 0x_2 ADPCM, length 2
     *
     * @param message see below
     * <pre>
     * 0    delta
     * 1    ff
     * 2    ff
     * 3-4  length
     * 5    vendor
     *
     * 6    f1
     * 7    _2
     * 8    streamNumber
     * 9    sample rate    0:4kHz, 1:8kHz
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        this.channel      = (data[7] & 0xc0) >> 6;  // 0 ~ 3
        this.streamNumber =  data[8] & 0xff;        // ??
        int format1       =  data[9] & 0xff;        //

        this.sampleRate = 4000;
        switch (format1) {
        case 0x00:
            sampleRate = 4000;
            break;
        case 0x01:
            sampleRate = 8000;
            break;
        }

        int adpcmLength = data.length - HEADER_LENGTH;
Debug.println(Level.FINE, "ADPCM data: No." + streamNumber + ", " + sampleRate + "Hz, " + adpcmLength);
Debug.println(Level.FINEST, "data:\n" + StringUtil.getDump(data, 64));

        this.adpcm = new byte[adpcmLength];
        System.arraycopy(data, HEADER_LENGTH, adpcm, 0, adpcmLength);

        NecSequencer.getAudioEngine().setData(streamNumber, channel, sampleRate, 4, 1 /* TODO mono */, adpcm, false);
    }

    /** adpcm channel no */
    protected int channel;
    /** */
    protected int streamNumber;
    /** */
    protected int sampleRate = 4000;
    /** */
    protected byte[] adpcm;

    /** unused ??? */
    private byte[] pcm;

    /**
     * unused ???
     * @param pcm wave (PCM) data
     */
    public void setAdpcm(byte[] pcm) {
        this.pcm = pcm;
    }

    /** unused ??? */
    public void setSamplingRate(int samplingRate) {
        this.sampleRate = samplingRate;
    }

    /** unused ??? */
    public byte[] getMessage() throws InvalidMfiDataException {

        AudioEngine audioEngine = NecSequencer.getAudioEngine();
        adpcm = audioEngine.encode(4, 1, pcm); // TODO bits, channel
Debug.println(Level.FINE, "adpcm length: " + adpcm.length);

        // [10~]
        byte[] tmp = new byte[adpcm.length + 5];

        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0xf0;                               // f1
        tmp[2] = (byte) ((channel << 6) | 0x2);             // f2 & channel
        tmp[3] = (byte) 0x01;
        tmp[4] = (byte) (sampleRate == 8000 ? 0x01 : 0x00); // sample rate

        System.arraycopy(adpcm, 0, tmp, 5, adpcm.length);

        return tmp;
    }
}

/* */
