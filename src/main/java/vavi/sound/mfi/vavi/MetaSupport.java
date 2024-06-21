/*
 * Copyright (c) 2001 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import vavi.sound.mfi.MetaEventListener;
import vavi.sound.mfi.MetaMessage;


/**
 * Utility class for MetaEvent mechanism.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 010820 nsano initial version <br>
 */
class MetaSupport implements Serializable {

    /** The metaEvent listeners */
    private final List<MetaEventListener> listeners = new ArrayList<>();

    /** Adds {@link MetaEventListener}. */
    public void addMetaEventListener(MetaEventListener l) {
        listeners.add(l);
    }

    /** Removes {@link MetaEventListener}. */
    public void removeMetaEventListener(MetaEventListener l) {
        listeners.remove(l);
    }

    /** Fires a meta message. */
    public void fireMeta(MetaMessage meta) {
        for (MetaEventListener listener : listeners) {
            listener.meta(meta);
        }
    }
}
