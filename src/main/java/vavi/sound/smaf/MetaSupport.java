/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.Serializable;

import javax.swing.event.EventListenerList;


/**
 * MetaEvent 機構のユーティリティクラスです．
 * <li>javax.sound.midi パッケージにはない。(SAMF オリジナル)
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
class MetaSupport implements Serializable {

    /** The metaEvent listeners */
    private EventListenerList listenerList = new EventListenerList();

    /** {@link MetaEventListener} を追加します． */
    public void addMetaEventListener(MetaEventListener l) {
        listenerList.add(MetaEventListener.class, l);
    }

    /** {@link MetaEventListener} を削除します． */
    public void removeMetaEventListener(MetaEventListener l) {
        listenerList.remove(MetaEventListener.class, l);
    }

    /** meta message を発行します． */
    public void fireMeta(MetaMessage meta) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == MetaEventListener.class) {
                ((MetaEventListener) listeners[i + 1]).meta(meta);
            }
        }
    }
}

/* */
