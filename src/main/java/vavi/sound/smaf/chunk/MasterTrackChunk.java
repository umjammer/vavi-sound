/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.util.Debug;


/**
 * MasterTrack Chunk.
 * <pre>
 * "MSTR"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class MasterTrackChunk extends TrackChunk {

    /** */
    public MasterTrackChunk(byte[] id, int size) {
        super(id, size);
Debug.println(Level.FINE, "MasterTrack: " + size);
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        this.formatType = FormatType.values()[dis.readUnsignedByte()];
        this.sequenceType = SequenceType.values()[dis.readUnsignedByte()];
Debug.println(Level.FINE, "sequenceType: " + sequenceType);
        this.durationTimeBase = dis.readUnsignedByte();
//Debug.println("durationTimeBase: " + StringUtil.toHex2(durationTimeBase));
        int optionSize = dis.readUnsignedByte();
        this.optionData = new byte[optionSize];
        dis.readFully(optionData);

        while (dis.available() > 0) {
//Debug.println("available: " + is.available() + ", " + available());
              Chunk chunk = readFrom(dis);
              if (chunk instanceof MasterTrackSequenceDataChunk) { // "Mssq"
                  sequenceDataChunk = chunk;
              } else {
Debug.println(Level.WARNING, "unknown chunk: " + chunk.getClass());
              }
        }
    }

    @Override
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
