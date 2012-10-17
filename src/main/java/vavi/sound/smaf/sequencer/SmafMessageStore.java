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
 * TODO 何とかして撲滅したい -> ThreadLocal ?
 * </p>
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public class SmafMessageStore {

    /** */
    private static int currentId = 0;

    /** */
    private static Map<Integer, SmafMessage> stores = new HashMap<Integer, SmafMessage>();

    private SmafMessageStore() {
    }

    /**
     * @return {@link javax.sound.midi.MetaMessage} で使用される id
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

/* */
