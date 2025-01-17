/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.message;

import vavi.sound.smaf.SmafMessage;


/**
 * UndefinedMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080501 nsano initial version <br>
 */
public class UndefinedMessage extends SmafMessage {

    int e1;
    int e2;

    /**
     *
     * @param duration
     */
    public UndefinedMessage(int e1, int e2, int duration) {
        this.e1 = e1;
        this.e2 = e2;
        this.duration = duration;
//logger.log(Level.TRACE, "UndefinedMessage: ★★★★★★★★★★★★★★★★");
    }

    @Override
    public String toString() {
        return "🟡 Undefined:" +
            " e1=%02x".formatted(e1) +
            (e2 == -1 ? "" : " e2=%02x".formatted(e2)) +
            ", duration=" + duration;
    }

    // ----

    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    @Override
    public int getLength() {
        return 0;   // TODO
    }
}
