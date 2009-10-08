/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.util.ArrayList;
import java.util.List;


/**
 * SMAF Sequence.
 * <p>
 * {@link javax.sound.midi} subset.
 * </p>
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041223 nsano initial version <br>
 */
public class Sequence {

    /** */
    protected List<Track> tracks;

    
    /** Creates a sequence. */
    public Sequence() {
        tracks = new ArrayList<Track>();
    }

    /** Not implemented for SMAF. */
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
        return tracks.toArray(new Track[tracks.size()]);
    }
}

/* */
