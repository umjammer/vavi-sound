/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.Sequence;

import vavi.sound.midi.VaviSequence;
import vavi.sound.smaf.SmafSystem;
import vavi.sound.smaf.SmafUnavailableException;


/**
 * SmafVaviSequence.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class SmafVaviSequence extends Sequence implements VaviSequence {

    /* */
    public SmafVaviSequence(float divisionType, int resolution) throws InvalidMidiDataException {
        super(divisionType, resolution);
    }

    /* */
    public SmafVaviSequence(float divisionType, int resolution, int numTracks) throws InvalidMidiDataException {
        super(divisionType, resolution, numTracks);
    }

    /* */
    public MetaEventListener getMetaEventListener() {
        try {
            return SmafSystem.getMetaEventListener();
        } catch (SmafUnavailableException e) {
            throw new IllegalStateException(e);
        }
    }
}

/* */
