/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.mfi5;

import static vavi.sound.mfi.vavi.mfi5.Mfi5Sequencer.VENDOR_MFI5;


/**
 * MFi 5 System exclusive message function 0xb0 processor.
 * (sound source parameter 0)
 * <p>
 * The same message {@link vavi.sound.mfi.vavi.fujitsu.Function176} reads: the MFi 5
 * writer puts the 0xb0 of the MFi 4.0 plug ins under the vendor byte {@code 0x01}. The
 * width byte rule of {@code vavi.sound.mfi.vavi.fujitsu.ParameterFunction} holds for the
 * MFi 5 ones of the corpus at {@code ~/Public/np2/mfi} too. See that class for the layout.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
public class Function176 extends vavi.sound.mfi.vavi.fujitsu.Function176 {

    @Override
    protected int getVendor() {
        return VENDOR_MFI5;
    }
}
