/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import vavi.sound.mfi.vavi.sequencer.SmafExclusive;


/**
 * NEC System exclusive message function 0x01, 0xf0, 0x04 processor.
 * (Extended FM tone specification)
 * <p>
 * Record: {@code type bank program &lt;voice&gt;}, the type tells how many
 * operators the voice has and therefore how long the record is.
 * </p>
 * <pre>
 *  type 1: 2 operator, record 20 bytes, voice 17
 *  type 2: 4 operator, record 34 bytes, voice 31
 *
 *  voice + 0    KeyNumber
 *        + 1    Panpot, BO
 *        + 2    LFO, PE, ALG
 *        then 7 bytes per operator
 *        + 0    SR, XOF, SUS, KSR
 *        + 1    RR, DR
 *        + 2    AR, SL
 *        + 3    TL, KSL
 *        + 4    DAM, EAM, DVB, EVB
 *        + 5    MULTI, DT
 *        + 6    WS, FB
 * </pre>
 * <p>
 * Do not use ALG to tell 2 from 4 operator, the type byte is the only reliable
 * source - 104 messages in the ~4400 file corpus are 4 operator voices whose
 * ALG is below 2.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see ToneFunction
 */
public class Function1_240_4 extends ToneFunction {

    /** 2 operator */
    public static final int TYPE_2OP = 1;

    /** 4 operator */
    public static final int TYPE_4OP = 2;

    @Override
    int getFunction() {
        return 0x04;
    }

    @Override
    String getName() {
        return "FM-ToneSetting";
    }

    @Override
    boolean hasType() {
        return true;
    }

    @Override
    int getRecordLength(byte[] data, int offset, int remaining) {
        return switch (data[offset] & 0xff) {
            case TYPE_2OP -> 20;
            case TYPE_4OP -> 34;
            default -> -1;
        };
    }

    /** the voice is already the VM35 FM voice image */
    @Override
    SmafExclusive.VoiceType getVoiceType(Tone tone) {
        return SmafExclusive.VoiceType.FM;
    }
}
