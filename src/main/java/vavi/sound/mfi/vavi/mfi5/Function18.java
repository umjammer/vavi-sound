/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mfi5;

import static vavi.sound.mfi.vavi.mfi5.Mfi5Sequencer.VENDOR_MFI5;


/**
 * MFi 5 System exclusive message function 0x12 processor.
 * (Wave Voice Setting)
 * <p>
 * The message Sharp writes as 0x12, {@link vavi.sound.mfi.vavi.sharp.Function18} reads
 * it: the same layout under the vendor byte {@code 0x01}. The MFi 5 writer only uses the
 * parameter 0x00 (767 of 767 in the corpus at {@code ~/Public/np2/mfi}), and writes each
 * one twice.
 * </p>
 * <p>
 * A voice that is the second of a pair (see {@link Function17}) has none of its own, the
 * bank and program of the first one are the pair's.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function18 extends vavi.sound.mfi.vavi.sharp.Function18 {

    @Override
    protected int getVendor() {
        return VENDOR_MFI5;
    }
}
