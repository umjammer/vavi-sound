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

import vavi.sound.smaf.InvalidSmafDataException;

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;


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

    private static final String FOURCC = "Gimd";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key);
    }

    @Override
    public ImageDataChunk init(byte[] id, int size) {
        super.init(id, size);
logger.log(Level.DEBUG, "ImageData: " + size + " bytes");
        return this;
    }

    /** */
    public ImageDataChunk() {
        System.arraycopy(FOURCC.getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        while (dis.available() > 0) {
            Chunk chunk = readFrom(dis);
            chunks.add(chunk);
            if (!(chunk instanceof ImageChunk) &&    // "Gig*"
                !(chunk instanceof BitmapChunk) &&   // "Gbm*"
                !(chunk instanceof LinkChunk)) {     // "Gln*"
                logger.log(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }
logger.log(Level.DEBUG, "messages: " + chunks.size());
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        writeChunk(os, bos -> {
            for (Chunk imageDataChunk : chunks) {
                imageDataChunk.writeTo(bos);
            }
        });
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getDC().format(getId()));
        try (var dc = getDC().open()) {
            for (var imageDataChunk : chunks) sb.append(dc.format(imageDataChunk.toString()));
        }

        return sb.toString();
    }
}
