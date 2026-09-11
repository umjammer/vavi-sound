/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import vavi.sound.mfi.vavi.sequencer.SmafExclusive;


/**
 * NEC System exclusive message function 0x01, 0xf0, 0x05 processor.
 * (Extended WT tone specification)
 * <p>
 * Record: {@code bank program &lt;16 byte voice&gt;}, no type byte. The voice is
 * the VM35 PCM voice, the same 16 bytes a SMAF "EXVO" {@code 43 05 02} exclusive
 * carries, and it points at a wave sent by {@link Function1_240_6}.
 * </p>
 * <pre>
 *  voice + 0  Fs(H)
 *        + 1  Fs(L)      sampling rate [Hz], 1 ~ 48000
 *        + 2  panpot, P, E
 *        + 3  LFO
 *        + 4  SR, XOF, SUS
 *        + 5  RR, DR
 *        + 6  AR, SL
 *        + 7  TL
 *        + 8  DAM, EAM, DVB, EVB
 *        + 9  StartAddressOffset(H)
 *        +10  StartAddressOffset(L)
 *        +11  LP(H)
 *        +12  LP(L)      loop point [sample]
 *        +13  EP(H)
 *        +14  EP(L)      end point [sample]
 *        +15  RM, WaveID RM: 0 the wave of {@link Function1_240_6}, 1 a preset wave
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see ToneFunction
 * @see vavi.sound.smaf.chunk.ExclusiveVoiceChunk
 */
public class Function1_240_5 extends ToneFunction {

    /** voice length */
    public static final int VOICE = 16;

    @Override
    int getFunction() {
        return 0x05;
    }

    @Override
    String getName() {
        return "WT-ToneSetting";
    }

    @Override
    boolean hasType() {
        return false;
    }

    @Override
    int getRecordLength(byte[] data, int offset, int remaining) {
        return 2 + VOICE;
    }

    /** the voice is already the VM35 PCM voice image */
    @Override
    SmafExclusive.VoiceType getVoiceType(Tone tone) {
        return SmafExclusive.VoiceType.PCM;
    }

    /** 1 ~ 48000 [Hz] */
    public static int getSamplingRate(Tone tone) {
        return ((tone.voice[0] & 0xff) << 8) | (tone.voice[1] & 0xff);
    }

    /** [sample] */
    public static int getLoopPoint(Tone tone) {
        return ((tone.voice[11] & 0xff) << 8) | (tone.voice[12] & 0xff);
    }

    /** [sample] */
    public static int getEndPoint(Tone tone) {
        return ((tone.voice[13] & 0xff) << 8) | (tone.voice[14] & 0xff);
    }

    /** true: a preset (rom) wave, false: the wave sent by {@link Function1_240_6} */
    public static boolean isRomWave(Tone tone) {
        return (tone.voice[15] & 0x80) != 0;
    }

    /** when {@link #isRomWave(Tone)} is false, the {@link Function1_240_6} wave id */
    public static int getWaveId(Tone tone) {
        return tone.voice[15] & 0x7f;
    }
}
