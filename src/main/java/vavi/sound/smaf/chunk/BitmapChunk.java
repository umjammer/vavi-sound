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
 * Bitmap Chunk.
 * <pre>
 * "Gbm*"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class BitmapChunk extends Chunk {

    private static final Logger logger = getLogger(BitmapChunk.class.getName());

    private static final String FOURCC = "Gbm";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key.substring(0, 3));
    }

    @Override
    public BitmapChunk init(byte[] id, int size) {
        super.init(id, size);
logger.log(Level.DEBUG, "Bitmap: " + size + " bytes");
        return this;
    }

    /** */
    public BitmapChunk() {
        System.arraycopy(FOURCC.getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {
        // TODO the body is not parsed yet, keep it as it is so it can be written back
        this.data = new byte[size];
        dis.readFully(data);
    }

    /** the bitmap body, not parsed yet */
    private byte[] data = new byte[0];

    /** the bitmap body, not parsed yet */
    public byte[] getData() {
        return data;
    }

    /** the bitmap body, not parsed yet */
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        writeChunk(os, bos -> bos.write(data));
    }
}
