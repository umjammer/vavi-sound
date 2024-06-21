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
 * temporary store.
 * <p>
 * TODO to remove this class somehow -> ThreadLocal?
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public class SmafMessageStore {

    /** */
    private static int currentId = 0;

    /** */
    private static final Map<Integer, SmafMessage> stores = new HashMap<>();

    private SmafMessageStore() {
    }

    /**
     * @return id used in {@link javax.sound.midi.MetaMessage}
     */
    public static /* synchronized */ int put(SmafMessage message) {
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
