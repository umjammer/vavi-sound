/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.util.ArrayList;
import java.util.List;


/**
 * MFi Track.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.10 020627 nsano refine <br>
 *          0.11 030915 nsano add insert <br>
 */
public class Track {

    /** */
    protected List<MfiEvent> events;

    /** Creates new Track */
    Track() {
        events = new ArrayList<>();
    }

    /** */
    public boolean add(MfiEvent event) {
        if (events.contains(event)) {
            return false;
        } else {
            events.add(event);
            return true;
        }
    }

    /** */
    public MfiEvent get(int index) {
        return events.get(index);
    }

    /** */
    public void remove(MfiEvent event) {
        events.remove(event);
    }

    /** */
    public int size() {
        return events.size();
    }

// TODO public long ticks()
}

/* */
