/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;


/**
 * SmafEvent.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano initial version <br>
 */
public class SmafEvent {

    /** */
    private final long tick;

    /** */
    private final SmafMessage smafMessage;

    /**
     * @param smafMessage
     * @param tick
     */
    public SmafEvent(SmafMessage smafMessage, long tick) {
        this.smafMessage = smafMessage;
        this.tick = tick;
    }

    /**
     * @return SmafMessage
     */
    public SmafMessage getMessage() {
        return smafMessage;
    }

    /**
     * @return Returns the tick.
     */
    public long getTick() {
        return tick;
    }
}
