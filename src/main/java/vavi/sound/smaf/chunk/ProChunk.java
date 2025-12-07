/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
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
 * ProChunk.
 * <pre>
 * "PRO*"
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-03-21 nsano initial version <br>
 */
public class ProChunk extends Chunk {

    private static final Logger logger = getLogger(ProChunk.class.getName());

    int pro;

    int start;
    int stop;
    int unknown;

    public ProChunk(byte[] id, int size) {
        super(id, size);

        this.pro = id[3] & 0xff;
logger.log(Level.DEBUG, "Pro: ???: " + pro + ", size: " + size);
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        start = dis.readInt();
        stop = dis.readInt();
        unknown = dis.readInt();
logger.log(Level.TRACE, "Pro: start: " + start + ", stop: " + stop + ", unknown: " + unknown);
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {

    }

    @Override
    public String toString() {
        return getDC().format(getId() + pro + " start: " + start + ", stop: " + stop + ", unknown: " + unknown);
    }
}
