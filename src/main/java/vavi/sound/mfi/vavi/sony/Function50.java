/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sony;

import static vavi.sound.mfi.vavi.sony.SonyFunction.VENDOR_SONY;


/**
 * Sony System exclusive message function 0x32 processor.
 * (Wave Voice Setting)
 * <p>
 * The message Sharp writes as 0x12, {@link vavi.sound.mfi.vavi.sharp.Function18} reads
 * it: the same layout, the function byte moved up by 0x20 (0x10 is Sony's MA
 * extension, {@link Function16}). See that class for the layout and how it was read.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function50 extends vavi.sound.mfi.vavi.sharp.Function18 {

    @Override
    protected int getVendor() {
        return VENDOR_SONY;
    }

    @Override
    protected int getFunction() {
        return 0x32;
    }
}
