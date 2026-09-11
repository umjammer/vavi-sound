/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;


/**
 * NEC System exclusive message function 0x01, 0xf1, 0x07 processor.
 * (Hold1)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see ChannelValueFunction
 */
public class Function1_241_7 extends ChannelValueFunction {

    @Override
    int getLevel() {
        return 0x01;
    }

    @Override
    int getFunction() {
        return 0x07;
    }

    @Override
    String getName() {
        return "Hold1";
    }
}
