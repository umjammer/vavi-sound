/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;


/**
 * NEC System exclusive message function 0x01, 0xf0, 0x08 processor.
 * (Extended AL tone specification)
 * <p>
 * Same record shape as {@link Function1_240_4}, the voice is one of the three
 * AL (filter) forms. The type byte only separates FM from WT, so an FM record
 * is 2 or 4 operator depending on its length - every message in the ~4400 file
 * corpus carries exactly one voice, which makes that unambiguous.
 * </p>
 * <pre>
 *  type 1: WT + AL,          record 46 bytes, voice 43
 *  type 0: FM 2 operator+AL, record 47 bytes, voice 44
 *  type 0: FM 4 operator+AL, record 61 bytes, voice 58
 * </pre>
 * <p>
 * The filter part is Q, FC0 ~ FC4, FAR / FDR / FSR / FRR, FKSL, FVSL, FXOF,
 * FSUS and the filter LFO; the trailing part is the plain FM or WT voice. The
 * package readme has the exact field map (the `src` column of the MA-7 tables).
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see ToneFunction
 */
public class Function1_240_8 extends ToneFunction {

    /** FM + AL */
    public static final int TYPE_FM = 0;

    /** WT + AL */
    public static final int TYPE_WT = 1;

    @Override
    int getFunction() {
        return 0x08;
    }

    @Override
    String getName() {
        return "AL-ToneSetting";
    }

    @Override
    boolean hasType() {
        return true;
    }

    @Override
    int getRecordLength(byte[] data, int offset, int remaining) {
        if ((data[offset] & 0xff) == TYPE_WT) {
            return 46;
        }
        // FM: 47 (2 operator) or 61 (4 operator), only the length tells them apart
        return remaining == 47 ? 47 : 61;
    }
}
