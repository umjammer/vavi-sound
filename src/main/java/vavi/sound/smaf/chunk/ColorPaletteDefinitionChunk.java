/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import vavi.sound.smaf.InvalidSmafDataException;


/**
 * ColorPaletteDefinition Chunk.
 * <pre>
 * "Gcpd"
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class ColorPaletteDefinitionChunk extends Chunk {

    /** */
    public ColorPaletteDefinitionChunk(byte[] id, int size) {
        super(id, size);
//Debug.println("ColorPaletteDefinition: " + size);
    }

    /** */
    public ColorPaletteDefinitionChunk() {
        System.arraycopy("Gcpd".getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    /** */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {
skip(is, size);
    }

    /** */
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        // TODO
    }
}

/* */
