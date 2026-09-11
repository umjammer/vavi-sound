/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;


/**
 * NEC System exclusive message function 0x81, 0xf2 processor.
 * <p>
 * TODO more investigation, see {@link Level1AliasFunction}. Treated as the
 * level 0x01 message of the same 0xf2 group.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260911 nsano initial version <br>
 * @see Level1AliasFunction
 */
public class Function129_2 extends Level1AliasFunction {

    @Override
    int getFunction() {
        return 2;
    }
}
