/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;


/**
 * Link Chunk.
 * <pre>
 * "Gln*"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class LinkChunk extends Chunk {

    /** */
    public LinkChunk(byte[] id, int size) {
        super(id, size);
Debug.println(Level.FINE, "Link: " + size + " bytes");
    }

    /** */
    public LinkChunk() {
        System.arraycopy("Gln".getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    /**  */
    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {
dis.skipBytes((int) (long) size); // TODO
    }

    /** TODO */
    @Override
    public void writeTo(OutputStream os) throws IOException {
    }
}
