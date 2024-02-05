/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Undefined Chunk.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080430 nsano initial version <br>
 */
public class UndefinedChunk extends Chunk {

    /** */
    public UndefinedChunk(byte[] id, int size) {
        super(id, size);
//new Exception("*** DUMMY ***").printStackTrace(System.err);
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        if (size > dis.available()) {
Debug.println(Level.WARNING, "read size is larger than available stream");
//new Exception("*** DUMMY ***").printStackTrace(System.err);
            throw new InvalidSmafDataException("read size is larger than available stream");
        }
        byte[] data = new byte[size];
        dis.readFully(data);
Debug.println(Level.WARNING, "Undefined: size: " + size + "\n" + StringUtil.getDump(data, 64));
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        // TODO
Debug.println(Level.WARNING, "not implemented skip");
    }
}
