/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.fujitsu;


/**
 * Fujitsu System exclusive message function 0xb0 processor.
 * (a sound source wide parameter)
 * <p>
 * Written near the head of track 0 together with {@link Function177}, with or
 * without a wave table voice block, so it does not belong to a voice - its target
 * byte is 1 in all 78 messages of the ~4400 file corpus at
 * {@code ~/Public/np2/mfi}. Parameter 3 in 68 of them, parameter 2 in 10.
 * </p>
 * <p>
 * The value is 0x3f, the top of a 6 bit range, in 46 of the 78 and the message
 * sits where the MFi
 * {@link vavi.sound.mfi.vavi.track.MasterVolumeMessage} (0xb0, the same number in
 * the MFi message space) does, so it reads like a level - but that is where the
 * evidence stops. See {@link ParameterFunction} for the payload.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
public class Function176 extends ParameterFunction {

    @Override
    int getFunction() {
        return 0xb0;
    }

    @Override
    String getName() {
        return "sound source param0";
    }
}
