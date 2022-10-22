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
 * MetaEvent 機構のユーティリティクラスです．
 * <li>javax.sound.midi パッケージにはない。(SMAF オリジナル)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
class MetaSupport implements Serializable {

    /** The metaEvent listeners */
    private List<MetaEventListener> listenerList = new ArrayList<>();

    /** {@link MetaEventListener} を追加します． */
    public void addMetaEventListener(MetaEventListener l) {
        listenerList.add(l);
    }

    /** {@link MetaEventListener} を削除します． */
    public void removeMetaEventListener(MetaEventListener l) {
        listenerList.remove(l);
    }

    /** meta message を発行します． */
    public void fireMeta(MetaMessage meta) {
        for (MetaEventListener listener : listenerList) {
            listener.meta(meta);
        }
    }
}

/* */
