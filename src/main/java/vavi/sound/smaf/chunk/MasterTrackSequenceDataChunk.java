/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.InputStream;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;


/**
 * MasterTrackSequenceData Chunk.
 * <pre>
 * "Mssq"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano initial version <br>
 */
public class MasterTrackSequenceDataChunk extends SequenceDataChunk {

    /** */
    public MasterTrackSequenceDataChunk(byte[] id, int size) {
        super(id, size);
Debug.println("MasterTrackSequenceData: " + size + " bytes");
    }

    /** TODO how to get formatType from parent chunk ??? */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {
skip(is, size); // TODO
    }
}

/* */
