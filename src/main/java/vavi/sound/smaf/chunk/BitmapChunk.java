/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;


/**
 * Bitmap Chunk.
 * <pre>
 * "Gbm*"
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class BitmapChunk extends Chunk {

    /** */
    public BitmapChunk(byte[] id, int size) {
        super(id, size);
Debug.println("Bitmap: " + size + " bytes");
    }

    /** */
    public BitmapChunk() {
        System.arraycopy("Gbm".getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    /**  */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {
skip(is, size); // TODO
    }

    /** TODO */
    public void writeTo(OutputStream os) throws IOException {
    }
}

/* */
