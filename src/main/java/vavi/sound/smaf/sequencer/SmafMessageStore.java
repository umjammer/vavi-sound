/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.sequencer;

import java.util.HashMap;
import java.util.Map;

import vavi.sound.smaf.SmafMessage;


/**
 * temporary store. avoiding creating data contained sysex message.
 * referencing id, message behaves as a sequencer using data inside.
 * <pre>
 * handle sysex mi si hh ll
 * mi (manufacturer id) ... must be vavi 0x45
 * si (sub id) ... 1 or 2
 * hh ll (currentId) ... store's key
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 * @see vavi.sound.smaf.SmafSynthesizer.SmafReceiver
 */
public class SmafMessageStore {

    /** */
    private static int currentId = 0;

    /** */
    private static final Map<Integer, SmafMessage> stores = new HashMap<>();

    private SmafMessageStore() {
    }

    /**
     * @return id used in {@link javax.sound.midi.SysexMessage}
     */
    public static synchronized int put(SmafMessage message) {
        try {
            stores.put(currentId, message);
            return currentId;
        } finally {
            currentId++;
        }
    }

    /** */
    public static SmafMessage get(int id) {
        return stores.get(id);
    }
}
