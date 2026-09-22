/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.panasonic;

import static vavi.sound.mfi.vavi.panasonic.PanasonicFunction.VENDOR_PANASONIC;


/**
 * Panasonic System exclusive message function 0x10 processor.
 * (Wave Data)
 * <p>
 * The same message {@link vavi.sound.mfi.vavi.sharp.Function16} reads, the wave table voice messages Sharp writes,
 * only the vendor byte differs. See that class for the layout and how it was read.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function16 extends vavi.sound.mfi.vavi.sharp.Function16 {

    @Override
    protected int getVendor() {
        return VENDOR_PANASONIC;
    }
}
