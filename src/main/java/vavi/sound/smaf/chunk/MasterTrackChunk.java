/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.util.Debug;


/**
 * MasterTrack Chunk.
 * <pre>
 * "MSTR"
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class MasterTrackChunk extends TrackChunk {

    /** */
    public MasterTrackChunk(byte[] id, int size) {
        super(id, size);
Debug.println("MasterTrack: " + size);
    }

    /** */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {

        this.formatType = FormatType.values()[read(is)];
        this.sequenceType = SequenceType.values()[read(is)];
Debug.println("sequenceType: " + sequenceType);
        this.durationTimeBase = read(is);
//Debug.println("durationTimeBase: " + StringUtil.toHex2(durationTimeBase));
        int optionSize = read(is);
        this.optionData = new byte[optionSize];
        read(is, optionData);

        while (available() > 0) {
//Debug.println("available: " + is.available() + ", " + available());
              Chunk chunk = readFrom(is);
              if (chunk instanceof MasterTrackSequenceDataChunk) { // "Mssq"
                  sequenceDataChunk = chunk;
              } else {
Debug.println("unknown chunk: " + chunk.getClass());
              }
        }
    }

    /** */
    public void writeTo(OutputStream os) throws IOException {
        // TODO
    }

    // header

    /** */
    private byte[] optionData;

    // chordName = readOneToFour(is);
    // keySignature = readShort(is);
    // timeSignature = readShort(is);
    // tempo = readShort(is);
    // measureMark = read(is);
    // rehearsalMark = read(is);

    /* */
    @Override
    public List<SmafEvent> getSmafEvents() throws InvalidSmafDataException {
        return null; // TODO
    }
}

/* */
