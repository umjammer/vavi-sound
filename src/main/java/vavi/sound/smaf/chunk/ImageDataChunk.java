/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import vavi.sound.smaf.InvalidSmafDataException;

import static java.lang.System.getLogger;


/**
 * ImageData Chunk.
 * <pre>
 * "Gimd"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class ImageDataChunk extends Chunk {

    private static final Logger logger = getLogger(ImageDataChunk.class.getName());

    /** */
    public ImageDataChunk(byte[] id, int size) {
        super(id, size);
logger.log(Level.DEBUG, "ImageData: " + size + " bytes");
    }

    /** */
    public ImageDataChunk() {
        System.arraycopy("Gimd".getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        while (dis.available() > 0) {
            Chunk chunk = readFrom(dis);
            if (chunk instanceof ImageChunk) { // "Gig*"
                imageDataChunks.add(chunk);
            } else if (chunk instanceof BitmapChunk) { // ""
                imageDataChunks.add(chunk);
            } else if (chunk instanceof LinkChunk) { // ""
                imageDataChunks.add(chunk);
            } else {
                logger.log(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }
logger.log(Level.DEBUG, "messages: " + imageDataChunks.size());
    }

    /** */
    private final List<Chunk> imageDataChunks = new ArrayList<>();

    /** TODO */
    @Override
    public void writeTo(OutputStream os) throws IOException {
    }
}
