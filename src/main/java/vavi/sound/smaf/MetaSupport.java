/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


/**
 * This is a utility class for the MetaEvent mechanism.
 * <li>not in javax.sound.midi package. (SMAF original)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
class MetaSupport implements Serializable {

    /** The metaEvent listeners */
    private List<MetaEventListener> listenerList = new ArrayList<>();

    /** Adds {@link MetaEventListener}. */
    public void addMetaEventListener(MetaEventListener l) {
        listenerList.add(l);
    }

    /** Removes {@link MetaEventListener}. */
    public void removeMetaEventListener(MetaEventListener l) {
        listenerList.remove(l);
    }

    /** Fires a meta message. */
    public void fireMeta(MetaMessage meta) {
        for (MetaEventListener listener : listenerList) {
            listener.meta(meta);
        }
    }
}
