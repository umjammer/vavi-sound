/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sequencer;

import java.util.HashMap;
import java.util.Map;

import vavi.sound.mfi.MfiMessage;


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
 * @version 0.00 030821 nsano initial version <br>
 * @see vavi.sound.mfi.vavi.VaviSynthesizer.VaviReceiver
 */
public class MfiMessageStore {

    /** store's key */
    private static int currentId = 0;

    /** */
    private static final Map<Integer, MfiMessage> stores = new HashMap<>();

    private MfiMessageStore() {
    }

    /**
     * @return id used by {@link javax.sound.midi.SysexMessage}
     */
    public static synchronized int put(MfiMessage message) {
        try {
            stores.put(currentId, message);
            return currentId;
        } finally {
            currentId++;
        }
    }

    /** */
    public static MfiMessage get(int id) {
        return stores.get(id);
    }
}
