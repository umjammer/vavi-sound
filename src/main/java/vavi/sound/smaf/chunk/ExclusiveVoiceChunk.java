/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.StringJoiner;

import vavi.sound.mobile.AudioEngine.Data;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.SysexMessage;
import vavi.sound.smaf.message.MachineDependentMessage.Factory;

import static java.lang.System.getLogger;


/**
 * ExclusiveVoiceChunk.
 * <pre>
 * "EXVO"
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-12-14 nsano initial version <br>
 */
public class ExclusiveVoiceChunk extends Chunk {

    private static final Logger logger = getLogger(ExclusiveVoiceChunk.class.getName());

    enum ExclusiveType {
        Unknown,
        /** VMAVoice */
        VMAVoice,
        /** VM3/VM5Voice */
        VM35Voice;
        static ExclusiveType valueOf(int i) {
            return switch (i) {
                case 1 -> VMAVoice;
                case 2 -> VM35Voice;
                default -> Unknown;
            };
        }
    }

    ExclusiveType exclusiveType;

    /**
     * ff f0 len 43 ..
     * len 10: 43 79 07 7F 01 ...
     * len 10: 43 79 06 7F 01 ...
     * len 3 : 43 05 01
     * len 6 : 43 03 ...
     * @see "https://github.com/but80/smaf825/blob/v1/smaf/subtypes/exclusive.go"
     */
    SysexMessage exclusive;

    /**
     * the VM35 PCM (wavetable) voice, only when this is a "43 05 02" exclusive of 0x16 bytes.
     * <pre>
     *  43 05 02 bb pp | VM35 PCM voice (16 bytes) | f7
     *                        bb: bank (0x01 or 0x81)
     *                        pp: program
     *
     *  + 0 | Fs(H)                 |
     *  + 1 | Fs(L)                 | sampling rate [Hz], 1 ~ 48000
     *  + 2 | panpot  |  ?  |P|E    |
     *  + 3 | lfo |         ?       |
     *  + 4 | S R   |xof| |sus|     |
     *  + 5 | R R   | D R           |
     *  + 6 | A R   | S L           |
     *  + 7 | T L       |  ?        |
     *  + 8 |?|dam|eam|?|dvb|evb    |
     *  + 9 | ?                     |
     *  +10 | ?                     |
     *  +11 | LP(H)                 |
     *  +12 | LP(L)                 | loop point [sample]
     *  +13 | EP(H)                 |
     *  +14 | EP(L)                 | end point [sample], (adpcm bytes * 2) - 1
     *  +15 |R M| ... WaveID        | RM: 0 ... "EXWV" wave, 1 ... preset wave (0 ~ 6)
     * </pre>
     * @see EXWVChunk
     * @see "https://github.com/but80/smaf825/blob/v1/smaf/voice/vm35_pcm_voice.go"
     */
    private boolean pcmVoice;

    private int bank;

    private int program;

    /** 1 ~ 48000 [Hz] */
    private int samplingRate;

    /** [sample] */
    private int loopPoint;

    /** [sample], (adpcm bytes * 2) - 1 */
    private int endPoint;

    /** when {@link #romWave} is false, the "EXWV" wave id, otherwise a preset wave number */
    private int waveId;

    /** true: preset (rom) wave, false: the wave sent by "EXWV" */
    private boolean romWave;

    private static final String FOURCC = "EXVO";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key);
    }

    @Override
    public ExclusiveVoiceChunk init(byte[] id, int size) {
        return (ExclusiveVoiceChunk) super.init(id, size);
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        int e1 = dis.readUnsignedByte(); // ff
        int e2 = dis.readUnsignedByte(); // f0
        int len = dis.readUnsignedByte();
        byte[] data = new byte[len];
        dis.readFully(data);
        assert e1 == 0xff && e2 == 0xf0;
        exclusiveType = ExclusiveType.valueOf(data[2]);

        // "43 05 02" of 0x16 bytes is a VM35 PCM (wavetable) voice, it links to an "EXWV" wave
        pcmVoice = len == 0x16 && data[0] == 0x43 && data[1] == 0x05 && data[2] == 0x02;
        if (pcmVoice) {
            bank = data[3] & 0xff;
            program = data[4] & 0xff;
            samplingRate = ((data[5] & 0xff) << 8) | (data[6] & 0xff);
            loopPoint = ((data[16] & 0xff) << 8) | (data[17] & 0xff);
            endPoint = ((data[18] & 0xff) << 8) | (data[19] & 0xff);
            romWave = (data[20] & 0x80) != 0;
            waveId = data[20] & 0x7f;
logger.log(Level.DEBUG, "EXVO: pcm voice: bank: %d, program: %d, %dHz, lp: %d, ep: %d, %s wave: %d"
        .formatted(bank, program, samplingRate, loopPoint, endPoint, romWave ? "rom" : "exwv", waveId));
        }

        exclusive = Factory.getSysexMessage(0, e2, data, len);
    }

    /**
     * <pre>
     *  0xff        : 1 byte, exclusive prefix
     *  0xf0        : 1 byte, exclusive
     *  length      : 1 byte
     *  data        : length byte
     * </pre>
     */
    @Override
    public void writeTo(OutputStream os) throws IOException {
        writeChunk(os, bos -> {
            DataOutputStream dos = new DataOutputStream(bos);

            byte[] data = exclusive.getData();
            dos.writeByte(0xff);
            dos.writeByte(0xf0);
            dos.writeByte(data.length);
            dos.write(data);
            dos.flush();
        });
    }

    /** */
    public SmafMessage getSmafMessage() {
        return exclusive;
    }

    /** is this a VM35 PCM (wavetable) voice which links to an "EXWV" wave? */
    public boolean isPcmVoice() {
        return pcmVoice;
    }

    /** when {@link #isRomWave()} is false, the "EXWV" wave id, otherwise a preset wave number */
    public int getWaveId() {
        return waveId;
    }

    /** true: preset (rom) wave, false: the wave sent by "EXWV" */
    public boolean isRomWave() {
        return romWave;
    }

    /** 1 ~ 48000 [Hz] */
    public int getSamplingRate() {
        return samplingRate;
    }

    /** [sample] */
    public int getLoopPoint() {
        return loopPoint;
    }

    /** [sample], (adpcm bytes * 2) - 1 */
    public int getEndPoint() {
        return endPoint;
    }

    @Override
    public String toString() {
        StringJoiner sj = new StringJoiner(", ", getId() + ": {", "}")
                .add("exclusiveType: " + exclusiveType);
        if (pcmVoice) {
            sj.add("bank: " + bank)
                .add("program: " + program)
                .add("samplingRate: " + samplingRate)
                .add("loopPoint: " + loopPoint)
                .add("endPoint: " + endPoint)
                .add((romWave ? "romWave: " : "waveId: ") + waveId);
        }
        return sj.add("exclusive: " + exclusive).toString();
    }
}
