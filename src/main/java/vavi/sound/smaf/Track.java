/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.util.ArrayList;
import java.util.List;


/**
 * SMAF Track.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041223 nsano initial version <br>
 */
public class Track {

    /** */
    protected List<SmafEvent> events;

    /**
     * 
     */
    Track() {
        events = new ArrayList<SmafEvent>();
    }

    /** */
    public boolean add(SmafEvent event) {
        if (events.contains(event)) {
            return false;
        } else {
            events.add(event);
            return true;
        }
    }

    /** */
    public SmafEvent get(int index) {
        return events.get(index);
    }

    /** */
    public void remove(SmafEvent event) {
        events.remove(event);
    }

    /** */
    public int size() {
        return events.size();
    }

// TODO public long ticks()
}

/* */
