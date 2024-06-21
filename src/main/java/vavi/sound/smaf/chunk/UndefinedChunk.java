/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
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


/**
 * Undefined Chunk.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080430 nsano initial version <br>
 */
public class UndefinedChunk extends Chunk {

    private static final Logger logger = getLogger(UndefinedChunk.class.getName());

    /** */
    public UndefinedChunk(byte[] id, int size) {
        super(id, size);
//new Exception("*** DUMMY ***").printStackTrace(System.err);
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        if (size > dis.available()) {
logger.log(Level.WARNING, "read size is larger than available stream");
//new Exception("*** DUMMY ***").printStackTrace(System.err);
            throw new InvalidSmafDataException("read size is larger than available stream");
        }
        byte[] data = new byte[size];
        dis.readFully(data);
logger.log(Level.WARNING, "Undefined: size: " + size + "\n" + StringUtil.getDump(data, 64));
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        // TODO
logger.log(Level.WARNING, "not implemented skip");
    }
}
