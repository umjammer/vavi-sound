/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.panasonic;

import static vavi.sound.mfi.vavi.panasonic.PanasonicFunction.VENDOR_PANASONIC;


/**
 * Panasonic System exclusive message function 0x11 processor.
 * (Wave Voice Parameter)
 * <p>
 * The same message {@link vavi.sound.mfi.vavi.sharp.Function17} reads, the wave table voice messages Sharp writes,
 * only the vendor byte differs. See that class for the layout and how it was read.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function17 extends vavi.sound.mfi.vavi.sharp.Function17 {

    @Override
    protected int getVendor() {
        return VENDOR_PANASONIC;
    }
}
