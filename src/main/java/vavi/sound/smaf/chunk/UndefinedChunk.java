/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Undefined Chunk.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080430 nsano initial version <br>
 */
public class UndefinedChunk extends Chunk {

    /** */
    public UndefinedChunk(byte[] id, int size) {
        super(id, size);
//new Exception("*** DUMMY ***").printStackTrace(System.err);
    }

    /** */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {

        byte[] data = new byte[size];
        read(is, data);
Debug.println(Level.WARNING, "Undefined: size: " + size + "\n" + StringUtil.getDump(data, 64));
    }

    /** */
    public void writeTo(OutputStream os) throws IOException {
        // TODO
    }
}

/* */
