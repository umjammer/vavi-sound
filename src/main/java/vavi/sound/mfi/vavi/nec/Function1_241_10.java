/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;


/**
 * NEC System exclusive message function 0x01, 0xf1, 0x0a processor.
 * (filter resonance, 0 ~ 127, 64 is neutral)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see ChannelValueFunction
 */
public class Function1_241_10 extends ChannelValueFunction {

    @Override
    int getLevel() {
        return 0x01;
    }

    @Override
    int getFunction() {
        return 0x0a;
    }

    @Override
    String getName() {
        return "FilterResonance";
    }
}
