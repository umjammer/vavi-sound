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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.imageio.ImageIO;

import vavi.sound.smaf.InvalidSmafDataException;

import static java.lang.System.getLogger;


/**
 * Image Chunk.
 * <pre>
 * "Gig*"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class ImageChunk extends Chunk {

    private static final Logger logger = getLogger(ImageChunk.class.getName());

    /** */
    private int imageNumer;

    /** */
    public ImageChunk(byte[] id, int size) {
        super(id, size);

        this.imageNumer = id[3];
logger.log(Level.DEBUG, "Image[" + imageNumer + "]: " + size + " bytes");
    }

    /** */
    public ImageChunk() {
        System.arraycopy("Gig".getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        this.imageData = new byte[size];
        dis.readFully(imageData);

logger.log(Level.DEBUG, "image: " + getImage());
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
    @Override
    public void writeTo(OutputStream os) throws IOException {
    }
}
