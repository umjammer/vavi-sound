/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger.Level;
import java.util.Arrays;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;

import static vavi.sound.mfi.vavi.nec.NecSequencer.VENDOR_NEC;


/**
 * NEC System exclusive message function 0x01, 0xf0, 0x06 processor.
 * (Extended WT waveform specification)
 * <p>
 * Carries the wave a {@link Function1_240_5} voice refers to by its wave id.
 * The end point of that voice is the sample count, which is twice the data
 * length for {@link #FORMAT_ADPCM} and equal to it for {@link #FORMAT_PCM8} -
 * that relation holds for 1751 of the 2055 wave / voice pairs in the ~4400 file
 * corpus and the rest are voices that stop before the end of the wave.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 */
public class Function1_240_6 implements MachineDependentFunction {

    /** 4 bit ADPCM, 2 samples per byte */
    public static final int FORMAT_ADPCM = 0;

    /** 8 bit PCM, 1 sample per byte */
    public static final int FORMAT_PCM8 = 2;

    @Override
    public String getId() {
        return VENDOR_NEC + "." + "1_240_6";
    }

    /**
     * 0x01, 0xf0, 0x06 Extended WT waveform specification
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
     * 8        ....0110
     *              ~~~~
     *              +------ 0x6
     *
     * 9        wave id, referred to by the RM/WaveID byte of a WT voice
     * 10       format, 0: 4bit ADPCM, 2: 8bit PCM
     * 11~      wave data
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        if (data.length < 11) {
            throw new InvalidMfiDataException("WT-WaveSetting is too short: " + (data.length - 9));
        }

        this.waveId = data[ 9] & 0xff;
        this.format = data[10] & 0xff;
        this.data = Arrays.copyOfRange(data, 11, data.length);

logger.log(Level.DEBUG, "WT-WaveSetting: No." + waveId + ", " +
        (format == FORMAT_ADPCM ? "4bit adpcm" : format == FORMAT_PCM8 ? "8bit pcm" : "format " + format) +
        ", " + this.data.length + " bytes, " + getSampleCount() + " samples");
    }

    /** wave id */
    private int waveId;
    /** {@link #FORMAT_ADPCM} or {@link #FORMAT_PCM8} */
    private int format;
    /** wave data */
    private byte[] data = new byte[0];

    /** */
    public int getWaveId() {
        return waveId;
    }

    /** {@link #FORMAT_ADPCM} or {@link #FORMAT_PCM8} */
    public int getFormat() {
        return format;
    }

    /** */
    public byte[] getData() {
        return data;
    }

    /** how many samples {@link #getData()} holds */
    public int getSampleCount() {
        return format == FORMAT_ADPCM ? data.length * 2 : data.length;
    }

    /** */
    public void setWaveId(int waveId) {
        this.waveId = waveId & 0xff;
    }

    /** */
    public void setFormat(int format) {
        this.format = format & 0xff;
    }

    /** */
    public void setData(byte[] data) {
        this.data = data;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[6 + data.length];
        tmp[0] = (byte) (VENDOR_NEC | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x01;
        tmp[2] = (byte) 0xf0;
        tmp[3] = (byte) 0x06;
        tmp[4] = (byte) waveId;
        tmp[5] = (byte) format;

        System.arraycopy(data, 0, tmp, 6, data.length);

        return tmp;
    }
}
