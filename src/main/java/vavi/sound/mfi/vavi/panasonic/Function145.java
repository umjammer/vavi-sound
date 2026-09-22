/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.panasonic;

import static vavi.sound.mfi.vavi.panasonic.PanasonicFunction.VENDOR_PANASONIC;


/**
 * Panasonic System exclusive message function 0x91 processor.
 * (Wave Parameter)
 * <p>
 * The same message {@link vavi.sound.mfi.vavi.fujitsu.Function145} reads, the Panasonic MFi 4.0 plug in ({@code MFi4PlugIn_P}, {@code P_PlugIn}) writing the
 * 0x9# ~ 0xb# messages of Fujitsu's byte for byte,
 * only the vendor byte differs. See that class for the layout and how it was read.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function145 extends vavi.sound.mfi.vavi.fujitsu.Function145 {

    @Override
    protected int getVendor() {
        return VENDOR_PANASONIC;
    }
}
