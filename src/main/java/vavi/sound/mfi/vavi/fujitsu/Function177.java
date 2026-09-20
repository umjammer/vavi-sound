/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.fujitsu;


/**
 * Fujitsu System exclusive message function 0xb1 processor.
 * (a sound source wide parameter)
 * <p>
 * The companion of {@link Function176}: the two are always written one after the
 * other, 78 messages each in the ~4400 file corpus at {@code ~/Public/np2/mfi},
 * target 1 in all of them. Parameter 2 in 77, parameter 4 in 1.
 * </p>
 * <p>
 * Its value is 1 in 68 of the 78 and 0 in 7, that is a switch rather than a
 * level. See {@link ParameterFunction} for the payload.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function177 extends ParameterFunction {

    @Override
    int getFunction() {
        return 0xb1;
    }

    @Override
    String getName() {
        return "sound source param1";
    }
}
