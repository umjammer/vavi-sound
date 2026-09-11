/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;


/**
 * NEC System exclusive message function 0x02, 0xf1, 0x0f processor.
 * (MA-7 SendLevel #3)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see ChannelValueFunction
 */
public class Function2_241_15 extends ChannelValueFunction {

    @Override
    int getLevel() {
        return 0x02;
    }

    @Override
    int getFunction() {
        return 0x0f;
    }

    @Override
    String getName() {
        return "SendLevel#3";
    }
}
