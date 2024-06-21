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

    /**
     *
     * @param duration
     */
    public UndefinedMessage(int duration) {
        this.duration = duration;
//logger.log(Level.DEBUG, "UndefinedMessage: ★★★★★★★★★★★★★★★★");
    }

    /** */
    public String toString() {
        return "Undefined:" +
            " duration=" + duration;
    }

    //----

    /* */
    @Override
    public byte[] getMessage() {
        return null; // TODO
    }

    /* */
    @Override
    public int getLength() {
        return 0;   // TODO
    }
}
