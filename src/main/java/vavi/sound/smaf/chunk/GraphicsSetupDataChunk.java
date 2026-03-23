/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;


/**
 * GraphicsSetupDataChunk Chunk.
 * <pre>
 * "Gtsu"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class GraphicsSetupDataChunk extends Chunk {

    private static final Logger logger = getLogger(GraphicsSetupDataChunk.class.getName());

    private static final String FOURCC = "Gtsu";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key);
    }

    @Override
    public GraphicsSetupDataChunk init(byte[] id, int size) {
        super.init(id, size);
logger.log(Level.DEBUG, "GraphicsSetupData: " + size + " bytes");
        return this;
    }

    /** */
    public GraphicsSetupDataChunk() {
        System.arraycopy(FOURCC.getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        while (dis.available() > 0) {
            Chunk chunk = readFrom(dis);
            if (chunk instanceof DisplayParameterDefinitionChunk) {
                displayParameterDefinitionChunk = chunk;
            } else if (chunk instanceof ColorPaletteDefinitionChunk) {
                colorPaletteDefinitionChunk = chunk;
            } else {
logger.log(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }

    }

    /** required */
    private Chunk displayParameterDefinitionChunk;

    /** option */
    private Chunk colorPaletteDefinitionChunk;

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        displayParameterDefinitionChunk.writeTo(os);
        if (colorPaletteDefinitionChunk != null) {
            colorPaletteDefinitionChunk.writeTo(os);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getDC().format(getId()));
        try (var dc = getDC().open()) {
            if (displayParameterDefinitionChunk != null) sb.append(displayParameterDefinitionChunk);
            if (colorPaletteDefinitionChunk != null) sb.append(colorPaletteDefinitionChunk);
        }

        return sb.toString();
    }
}
