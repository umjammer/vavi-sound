/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.util.ArrayList;
import java.util.List;


/**
 * Sequence.
 * <p>
 * {@link javax.sound.midi} subset compatible.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.10 020630 nsano javax.sound.midi compliant <br>
 */
public class Sequence {

    /** */
    protected final List<Track> tracks;

    /** Creates a sequence. */
    public Sequence() {
        tracks = new ArrayList<>();
    }

    /** */
    public Track createTrack() {
        Track track = new Track();
        tracks.add(track);
        return track;
    }

    /** */
    public boolean deleteTrack(Track track) {
        if (tracks.contains(track)) {
            tracks.remove(track);
            return true;
        } else {
            return false;
        }
    }

    /** */
    public Track[] getTracks() {
        Track[] ts = new Track[tracks.size()];
        tracks.toArray(ts);
        return ts;
    }
}
