/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;


/**
 * OptionalData Chunk.
 * <pre>
 * "OPDA"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class OptionalDataChunk extends Chunk {

    /** */
    public OptionalDataChunk(byte[] id, int size) {
        super(id, size);
Debug.println(Level.FINE, "OptionalData: " + size + " bytes");
    }

    /** */
    public OptionalDataChunk() {
        System.arraycopy("OPDA".getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        while (dis.available() > 0) {
            Chunk data = readFrom(dis);
            // TODO "Pro*"
            dataChunks.add(data);
        }
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        for (Chunk dataChunk : dataChunks) {
            dataChunk.writeTo(os);
        }
    }

    /** DataChunk "Dch*", ... */
    private List<Chunk> dataChunks = new ArrayList<>();

    /**
     * @return Returns the subChunks.
     */
    public List<Chunk> getDataChunks() {
        return dataChunks;
    }

    /** */
    public void addDataChunks(DataChunk dataChunk) {
        dataChunks.add(dataChunk);
        size += dataChunk.getSize();
    }

    /** ???Chunk "Pro*", ... */
    private Chunk proChunk;

    /**
     * @return Returns the "Pro*" chunk.
     */
    public Chunk getProChunk() {
        return proChunk;
    }

    // Pro* chunk (not in specification 3.06)
    //  start 4 bytes
    //  stop 4 bytes
    //  ??? 4 bytes
}

/* */
