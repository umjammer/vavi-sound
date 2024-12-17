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
import java.util.StringJoiner;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


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

    byte[] exclusive;

    public EXWVChunk(byte[] id, int size) {
        super(id, size);
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        exclusive = new byte[size];
        dis.readFully(exclusive);
logger.log(Level.DEBUG, "EXWV: " + size + "\n" + StringUtil.getDump(exclusive, 64));
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {

    }

    @Override
    public String toString() {
        return new StringJoiner(", ", EXWVChunk.class.getSimpleName() + "[", "]")
                .toString();
    }
}
