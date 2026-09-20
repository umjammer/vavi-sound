/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.fujitsu;

import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;


/**
 * Base of the Fujitsu machine dependent functions.
 * <p>
 * A Fujitsu machine dependent message is a {@code 0xff 0xff} sysex whose payload
 * starts with the vendor byte {@code 0x21} ({@code VENDOR_FUJITSU | CARRIER_DOCOMO}),
 * the byte after it is the function:
 * </p>
 * <pre>
 *  ff ff &lt;len:2&gt; 21 &lt;function&gt; &lt;payload...&gt;
 * </pre>
 * <p>
 * {@code FujitsuSequencer} (vavi-sound-nda) builds the lookup key from the function
 * byte alone, so the key is {@code 32.&lt;function&gt;} - which is why these classes are
 * usable from here although the sequencer itself is not in this artifact.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260920 nsano initial version <br>
 */
abstract class FujitsuFunction implements MachineDependentFunction {

    /**
     * Fujitsu vendor id, the same value {@code FujitsuSequencer.VENDOR_FUJITSU} of
     * vavi-sound-nda has (that class cannot be imported from here).
     */
    static final int VENDOR_FUJITSU = 0x20; // F

    /** the function byte, {@code data[6]} */
    abstract int getFunction();

    @Override
    public String getId() {
        return VENDOR_FUJITSU + "." + getFunction();
    }
}
