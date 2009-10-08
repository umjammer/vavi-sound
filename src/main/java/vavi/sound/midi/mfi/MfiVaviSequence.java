/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.mfi;

import java.io.File;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.midi.VaviSequence;
import vavi.util.StringUtil;


/**
 * MfiVaviSequence.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class MfiVaviSequence extends Sequence implements VaviSequence {

    /* */
    public MfiVaviSequence(float divisionType, int resolution) throws InvalidMidiDataException {
        super(divisionType, resolution);
    }

    /* */
    public MfiVaviSequence(float divisionType, int resolution, int numTracks) throws InvalidMidiDataException {
        super(divisionType, resolution, numTracks);
    }

    /* */
    public MetaEventListener getMetaEventListener() {
        try {
            return MfiSystem.getMetaEventListener();
        } catch (MfiUnavailableException e) {
            throw new IllegalStateException(e);
        }
    }

    //----

    /** load MFi */
    public static void main(String[] args) throws Exception {
        MidiFileFormat mff = MidiSystem.getMidiFileFormat(new File(args[0]));
System.err.println(StringUtil.paramString(mff));
        System.exit(0);
    }
}

/* */
