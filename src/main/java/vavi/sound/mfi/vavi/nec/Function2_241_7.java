/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;


/**
 * NEC System exclusive message function 0x02, 0xf1, 0x07 processor.
 * (Hold1)
 * <p>
 * The level 0x01 counterpart is {@link Function1_241_7}. Note the level 0x02
 * form carries one data byte where MonoOn and PlayOn carry none at level 0x01
 * (internal events 209 ~ 211 all push a length of 1).
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see ChannelValueFunction
 */
public class Function2_241_7 extends ChannelValueFunction {

    @Override
    int getLevel() {
        return 0x02;
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
