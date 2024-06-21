/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;

import static java.lang.System.getLogger;


/**
 * MasterTrack Chunk.
 * <pre>
 * "MSTR"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class MasterTrackChunk extends TrackChunk {

    private static final Logger logger = getLogger(MasterTrackChunk.class.getName());

    /** */
    public MasterTrackChunk(byte[] id, int size) {
        super(id, size);
logger.log(Level.DEBUG, "MasterTrack: " + size);
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        this.formatType = FormatType.values()[dis.readUnsignedByte()];
        this.sequenceType = SequenceType.values()[dis.readUnsignedByte()];
logger.log(Level.DEBUG, "sequenceType: " + sequenceType);
        this.durationTimeBase = dis.readUnsignedByte();
//logger.log(Level.DEBUG, "durationTimeBase: " + StringUtil.toHex2(durationTimeBase));
        int optionSize = dis.readUnsignedByte();
        this.optionData = new byte[optionSize];
        dis.readFully(optionData);

        while (dis.available() > 0) {
//logger.log(Level.DEBUG, "available: " + is.available() + ", " + available());
              Chunk chunk = readFrom(dis);
              if (chunk instanceof MasterTrackSequenceDataChunk) { // "Mssq"
                  sequenceDataChunk = chunk;
              } else {
logger.log(Level.WARNING, "unknown chunk: " + chunk.getClass());
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
