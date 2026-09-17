/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

import javax.sound.midi.Receiver;

import vavi.sound.mfi.InvalidMfiDataException;


/**
 * Sub sequencer for machine dependent system exclusive message.
 * <pre>
 * nec 16 | docomo 1
 * fujitsu 32 | docomo 1
 * sony 48 | docomo 1
 * panasonic 64 | docomo 1
 * mitsubishi 96 | docomo 1
 * sharp 112 | docomo 1
 * sanyo 128 | docomo 1
 * motrola 144 | docomo 1
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020704 nsano initial version <br>
 */
public interface MachineDependentSequencer {

    Logger logger = System.getLogger(MachineDependentSequencer.class.getName());

    /** for {@link MachineDependentSequencer} */
    int MFi_SYSEX_FUNCTION_ID_MACHINE_DEPENDENT = 0x01;

    /** */
    int getId();

    /** processes a message. this method is assumed stateless, don't use instance fields */
    void sequence(byte[] data, Receiver receiver) throws InvalidMfiDataException;

    class Factory {

        /** factory */
        static Map<Integer, MachineDependentSequencer> sequencers = new HashMap<>();

        /**
         * @param data 45 01 mfi sysex
         * @throws IllegalArgumentException no sequencer matches the key
         */
        public static MachineDependentSequencer getSequencer(byte[] data) {
            int key = data[2 + 5]; // 5: vendor|carrier
            MachineDependentSequencer sequencer = sequencers.get(key);
            if (sequencer == null) {
                logger.log(Level.ERROR, "error vendor: 0x%02x".formatted(key));
                return new UnknownVendorSequencer();
            }
            return sequencer;
        }

        static {
            for (MachineDependentSequencer sequencer : ServiceLoader.load(MachineDependentSequencer.class)) {
                sequencers.put(sequencer.getId(), sequencer);
            }
        }
    }
}
