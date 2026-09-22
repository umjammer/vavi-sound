/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sony;


/**
 * Sony System exclusive message function 0xe8 processor.
 * (Pitch Bend MSB, voice 0)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 * @see PitchBendFunction
 */
public class Function232 extends PitchBendFunction {

    @Override
    int getFunction() {
        return 0xe8;
    }
}
