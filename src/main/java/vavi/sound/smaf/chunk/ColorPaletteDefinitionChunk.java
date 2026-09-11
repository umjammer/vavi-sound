/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;

import vavi.sound.smaf.InvalidSmafDataException;


/**
 * ColorPaletteDefinition Chunk.
 * <pre>
 * "Gcpd"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080517 nsano initial version <br>
 */
public class ColorPaletteDefinitionChunk extends Chunk {

    private static final String FOURCC = "Gcpd";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key);
    }

    @Override
    public ColorPaletteDefinitionChunk init(byte[] id, int size) {
        super.init(id, size);
//logger.log(Level.TRACE, "ColorPaletteDefinition: " + size);
        return this;
    }

    /** */
    public ColorPaletteDefinitionChunk() {
        System.arraycopy(FOURCC.getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {
        // TODO the palette is not parsed yet, keep it as it is so it can be written back
        this.data = new byte[size];
        dis.readFully(data);
    }

    /** the palette body, not parsed yet */
    private byte[] data = new byte[0];

    /** the palette body, not parsed yet */
    public byte[] getData() {
        return data;
    }

    /** the palette body, not parsed yet */
    public void setData(byte[] data) {
        this.data = data;
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        writeChunk(os, bos -> bos.write(data));
    }
}
