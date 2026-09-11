/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;


/**
 * NEC System exclusive message function 0x01, 0xf1, 0x09 processor.
 * (PlayOn, no data)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see ChannelFlagFunction
 */
public class Function1_241_9 extends ChannelFlagFunction {

    @Override
    int getFunction() {
        return 0x09;
    }

    @Override
    String getName() {
        return "PlayOn";
    }
}
