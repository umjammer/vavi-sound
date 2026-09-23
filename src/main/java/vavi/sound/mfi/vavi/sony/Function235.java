/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sony;


/**
 * Sony System exclusive message function 0xeb processor.
 * (Pitch Bend MSB, voice 3)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 * @see PitchBendFunction
 */
public class Function235 extends PitchBendFunction {

    @Override
    int getFunction() {
        return 0xeb;
    }
}
