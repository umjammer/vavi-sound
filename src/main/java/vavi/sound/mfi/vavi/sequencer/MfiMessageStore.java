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
 * temporary store.
 * <p>
 * TODO ‰½‚Æ‚©‚µ‚Ä–o–Å‚µ‚½‚¢ -> ThreadLocal ?
 * </p>
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030821 nsano initial version <br>
 */
public class MfiMessageStore {

    /** */
    private static int currentId = 0;

    /** */
    private static Map<Integer, MfiMessage> stores = new HashMap<Integer, MfiMessage>();

    private MfiMessageStore() {
    }

    /**
     * @return {@link javax.sound.midi.MetaMessage} ‚ÅŽg—p‚³‚ê‚é id
     */
    public static /* synchronized */ int put(MfiMessage message) {
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

/* */
