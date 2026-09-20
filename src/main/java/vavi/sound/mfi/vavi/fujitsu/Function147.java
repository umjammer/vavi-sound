/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.fujitsu;


/**
 * Fujitsu System exclusive message function 0x93 processor.
 * (Voice Parameter 2, one numbered parameter of a wave table voice)
 * <p>
 * The fourth and last message of a wave table voice block, after
 * {@link Function144}, {@link Function145} and {@link Function146}. Its target is
 * the voice number, which ties it to the {@link Function146} record of the same
 * number: the files that register 3 voices write 0x93 with target 0, 1 and 2, the
 * one that registers 4 writes 2 and 3 - one per voice, in voice order.
 * </p>
 * <p>
 * 75 messages in the ~4400 file corpus at {@code ~/Public/np2/mfi}, parameter 2
 * (66 of them), 3 (8) and 4 (1). See {@link ParameterFunction} for the payload.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function147 extends ParameterFunction {

    @Override
    int getFunction() {
        return 0x93;
    }

    @Override
    String getName() {
        return "voice param2";
    }
}
