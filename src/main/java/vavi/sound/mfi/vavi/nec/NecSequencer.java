/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.nec;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.YamahaAudioEngine;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.mfi.vavi.sequencer.MachineDependentFunction.CARRIER_DOCOMO;


/**
 * NEC System exclusive message processor.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020703 nsano initial version <br>
 *          0.01 030711 nsano fix f1, f2 definitions <br>
 *          0.02 030827 nsano refactoring <br>
 *          0.03 030829 nsano ignore channel <br>
 */
public class NecSequencer implements MachineDependentSequencer {

    private static final Logger logger = getLogger(NecSequencer.class.getName());

    static final int VENDOR_NEC = 0x10; // N

    @Override
    public int getId() {
        return VENDOR_NEC | CARRIER_DOCOMO;
    }

    /**
     *
     * @param message see below
     * <pre>
     * 0        delta
     * 1        ff
     * 2        ff
     * 3-4      length
     * 5        vendor
     * ---- MFi <= 2 ----
     * 6        f1
     * 7        f2, channel
     * </pre>
     */
    @Override
    public void sequence(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        int f1 = data[6] & 0xff;
        int f2;

        String key = VENDOR_NEC + ".";

        if (f1 == 0x01 || f1 == 0x02) {
            f2 = data[7] & 0xff;        // 0 ~ 32
            int f3 = data[8] & 0x0f;    // 0 ~ 16
logger.log(Level.DEBUG, "%02x %02x %02x".formatted(f1, f2, f3));
            key += f1 + "_" + f2 + "_" + f3;
        } else {
            f2 = data[7] & 0x0f;        // 0 ~ 16
logger.log(Level.DEBUG, "%02x %02x".formatted(f1, f2));
            key += f1 + "_" + f2;
        }

        MachineDependentFunction mdf = MachineDependentFunction.Factory.getFunction(key);
        if (mdf != null) {
            mdf.process(message);
        } else {
logger.log(Level.WARNING, "unsupported function: %s, %d%n%s".formatted(key, data.length, StringUtil.getDump(data)));
        }
    }

    // ----

    /** */
    private static final AudioEngine player = new YamahaAudioEngine();

    /** */
    static AudioEngine getAudioEngine() {
        return player;
    }
}
