/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mfi5;

import static vavi.sound.mfi.vavi.mfi5.Mfi5Sequencer.VENDOR_MFI5;


/**
 * MFi 5 System exclusive message function 0x10 processor.
 * (Wave Data)
 * <p>
 * The message Sharp writes as 0x10, {@link vavi.sound.mfi.vavi.sharp.Function16} reads
 * it: the same layout under the vendor byte {@code 0x01}. See that class for the layout.
 * The checks made there hold for the 446 MFi 5 waves of the corpus at
 * {@code ~/Public/np2/mfi} as well.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function16 extends vavi.sound.mfi.vavi.sharp.Function16 {

    @Override
    protected int getVendor() {
        return VENDOR_MFI5;
    }
}
