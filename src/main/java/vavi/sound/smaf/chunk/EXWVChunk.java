/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;


/**
 * EXWVChunk.
 * <pre>
 * "EXWV"
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-12-15 nsano initial version <br>
 */
public class EXWVChunk extends Chunk {

    private static final Logger logger = getLogger(EXWVChunk.class.getName());

    /** wave? */
    byte[] data;

    private static final String FOURCC = "EXWV";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key);
    }

    @Override
    public EXWVChunk init(byte[] id, int size) {
        return (EXWVChunk) super.init(id, size);
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        data = new byte[size];
        dis.readFully(data);
logger.log(Level.DEBUG, FOURCC + ": " + size + "\n" + StringUtil.getDump(data, 16));
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {

    }

    @Override
    public String toString() {
        return getDC().format(getId() + " " + data.length + " bytes");
    }
}
