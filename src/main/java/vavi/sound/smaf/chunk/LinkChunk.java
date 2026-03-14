/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;

import static java.lang.System.getLogger;


/**
 * Link Chunk.
 * <pre>
 * "Gln*"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class LinkChunk extends Chunk {

    private static final Logger logger = getLogger(LinkChunk.class.getName());

    private static final String FOURCC = "Gln";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key.substring(0, 3));
    }

    @Override
    public LinkChunk init(byte[] id, int size) {
        super.init(id, size);
logger.log(Level.DEBUG, "Link: " + size + " bytes");
        return this;
    }

    /** */
    public LinkChunk() {
        System.arraycopy(FOURCC.getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {
dis.skipBytes((int) (long) size); // TODO
    }

    /** TODO */
    @Override
    public void writeTo(OutputStream os) throws IOException {
    }
}
