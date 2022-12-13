/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;

import javax.imageio.ImageIO;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;


/**
 * Image Chunk.
 * <pre>
 * "Gig*"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class ImageChunk extends Chunk {

    /** */
    private int imageNumer;

    /** */
    public ImageChunk(byte[] id, int size) {
        super(id, size);

        this.imageNumer = id[3];
Debug.println(Level.FINE, "Image[" + imageNumer + "]: " + size + " bytes");
    }

    /** */
    public ImageChunk() {
        System.arraycopy("Gig".getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    /** */
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        this.imageData = new byte[size];
        dis.readFully(imageData);

Debug.println(Level.FINE, "image: " + getImage());
    }

    /** */
    private byte[] imageData;

    /** */
    public BufferedImage getImage() {
        try {
            return ImageIO.read(new ByteArrayInputStream(imageData));
        } catch (IOException e) {
            assert false;
            return null;
        }
    }

    /** TODO */
    public void writeTo(OutputStream os) throws IOException {
    }
}

/* */
