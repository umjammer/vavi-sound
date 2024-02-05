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
 * MetaEvent 機構のユーティリティクラスです．
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 010820 nsano initial version <br>
 */
class MetaSupport implements Serializable {

    /** The metaEvent listeners */
    private List<MetaEventListener> listeners = new ArrayList<>();

    /** {@link MetaEventListener} を追加します． */
    public void addMetaEventListener(MetaEventListener l) {
        listeners.add(l);
    }

    /** {@link MetaEventListener} を削除します． */
    public void removeMetaEventListener(MetaEventListener l) {
        listeners.remove(l);
    }

    /** meta message を発行します． */
    public void fireMeta(MetaMessage meta) {
        for (MetaEventListener listener : listeners) {
            listener.meta(meta);
        }
    }
}
