/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;


/**
 * ImageData Chunk.
 * <pre>
 * "Gimd"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class ImageDataChunk extends Chunk {

    /** */
    public ImageDataChunk(byte[] id, int size) {
        super(id, size);
Debug.println("ImageData: " + size + " bytes");
    }

    /** */
    public ImageDataChunk() {
        System.arraycopy("Gimd".getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    /**  */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {

        while (available() > 0) {
            Chunk chunk = readFrom(is);
            if (chunk instanceof ImageChunk) { // "Gig*"
                imageDataChunks.add(chunk);
            } else if (chunk instanceof BitmapChunk) { // ""
                imageDataChunks.add(chunk);
            } else if (chunk instanceof LinkChunk) { // ""
                imageDataChunks.add(chunk);
            } else {
                Debug.println(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }
Debug.println("messages: " + imageDataChunks.size());
    }

    /** */
    private List<Chunk> imageDataChunks = new ArrayList<>();

    /** TODO */
    public void writeTo(OutputStream os) throws IOException {
    }
}

/* */
