/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;


/**
 * GraphicsSetupDataChunk Chunk.
 * <pre>
 * "Gtsu"
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class GraphicsSetupDataChunk extends Chunk {

    /** */
    public GraphicsSetupDataChunk(byte[] id, int size) {
        super(id, size);
Debug.println("GraphicsSetupData: " + size + " bytes");
    }

    /** */
    public GraphicsSetupDataChunk() {
        System.arraycopy("Gtsu".getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    /** */
    protected void init(InputStream is, Chunk parent) throws InvalidSmafDataException, IOException {
        while (available() > 0) {
            Chunk chunk = readFrom(is);
            if (chunk instanceof DisplayParameterDefinitionChunk) {
                displayParameterDefinitionChunk = chunk;
            } else if (chunk instanceof ColorPaletteDefinitionChunk) {
                colorPaletteDefinitionChunk = chunk;
            } else {
Debug.println("unknown chunk: " + chunk.getClass());
            }
        }

    }

    /** required */
    private Chunk displayParameterDefinitionChunk; 

    /** option */
    private Chunk colorPaletteDefinitionChunk; 

    /** */
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        displayParameterDefinitionChunk.writeTo(os);
        if (colorPaletteDefinitionChunk != null) {
            colorPaletteDefinitionChunk.writeTo(os);
        }
    }
}

/* */
