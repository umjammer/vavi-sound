/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.panasonic;

import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;


/**
 * Base of the Panasonic machine dependent functions this artifact has.
 * <p>
 * A Panasonic machine dependent message is a {@code 0xff 0xff} sysex whose payload
 * starts with the vendor byte {@code 0x41} ({@code VENDOR_PANASONIC | CARRIER_DOCOMO}),
 * the byte after it is the function:
 * </p>
 * <pre>
 *  ff ff &lt;len:2&gt; 41 &lt;function&gt; &lt;payload...&gt;
 * </pre>
 * <p>
 * {@code PanasonicSequencer} (vavi-sound-nda) builds the lookup key from the function
 * byte alone, so the key is {@code 64.&lt;function&gt;} - which is why these classes are
 * usable from here although the sequencer itself is not in this artifact.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260923 nsano initial version <br>
 */
abstract class PanasonicFunction implements MachineDependentFunction {

    /**
     * Panasonic vendor id, the same value {@code PanasonicSequencer.VENDOR_PANASONIC} of
     * vavi-sound-nda has.
     */
    static final int VENDOR_PANASONIC = 0x40; // P

    /** the function byte, {@code data[6]} */
    abstract int getFunction();

    @Override
    public String getId() {
        return VENDOR_PANASONIC + "." + getFunction();
    }
}
