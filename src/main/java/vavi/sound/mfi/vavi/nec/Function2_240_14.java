/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;


/**
 * NEC System exclusive message function 0x02, 0xf0, 0x0e processor.
 * (MA-7 effect (SFX) parameter block A)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see EffectDataFunction
 */
public class Function2_240_14 extends EffectDataFunction {

    @Override
    int getFunction() {
        return 0x0e;
    }

    @Override
    String getName() {
        return "EffectA";
    }
}
