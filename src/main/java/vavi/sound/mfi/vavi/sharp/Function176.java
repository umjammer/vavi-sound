/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sharp;

import static vavi.sound.mfi.vavi.sharp.SharpSequencer.VENDOR_SHARP;


/**
 * Sharp System exclusive message function 0xb0 processor.
 * (sound source parameter 0)
 * <p>
 * The same message {@link vavi.sound.mfi.vavi.fujitsu.Function176} reads: the MFi 4.0
 * plug in writers of the vendors share their 0x9# ~ 0xb# messages byte for byte, only
 * the vendor byte differs. See that class for the layout and how it was read.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function176 extends vavi.sound.mfi.vavi.fujitsu.Function176 {

    @Override
    protected int getVendor() {
        return VENDOR_SHARP;
    }
}
