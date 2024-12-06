/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;

import static java.lang.System.getLogger;


/**
 * MasterTrackSequenceData Chunk.
 * <pre>
 * "Mssq"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041227 nsano initial version <br>
 */
public class MasterTrackSequenceDataChunk extends SequenceDataChunk {

    private static final Logger logger = getLogger(MasterTrackSequenceDataChunk.class.getName());

    /** */
    public MasterTrackSequenceDataChunk(byte[] id, int size) {
        super(id, size);
logger.log(Level.DEBUG, "MasterTrackSequenceData: " + size + " bytes");
    }

    /** TODO how to get formatType from parent chunk ??? */
    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {
dis.skipBytes((int) (long) size); // TODO
    }
}
