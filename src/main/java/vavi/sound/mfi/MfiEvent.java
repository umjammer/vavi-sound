/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;


/**
 * MFi のイベントです．
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.10 020629 nsano javax.sound.midi compliant <br>
 *          0.11 030915 nsano add tick related <br>
 */
public class MfiEvent {

    /** */
    private MfiMessage message;

    /** */
    private long tick;

    /**
     * Creates an MFi event.
     * 
     * @param message the {@link MfiMessage}
     */
    public MfiEvent(MfiMessage message, long tick) {
        this.message = message;
        this.tick = tick;
    }

    /** */
    public MfiMessage getMessage() {
        return message;
    }

    /** */
    public void setTick(long tick) {
        this.tick = tick;
    }

    /** */
    public long getTick() {
        return tick;
    }
}

/* */
