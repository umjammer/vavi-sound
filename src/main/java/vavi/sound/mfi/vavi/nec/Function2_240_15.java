/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;


/**
 * NEC System exclusive message function 0x02, 0xf0, 0x0f processor.
 * (MA-7 effect (SFX) parameter block B)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see EffectDataFunction
 */
public class Function2_240_15 extends EffectDataFunction {

    @Override
    int getFunction() {
        return 0x0f;
    }

    @Override
    String getName() {
        return "EffectB";
    }
}
