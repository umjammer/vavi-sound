/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;


/**
 * MfiFileFormat.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 */
public class MfiFileFormat {

    /** total file length */
    protected int byteLength;

    /** */
    protected final int type;

    /** */
    public MfiFileFormat(int type, int bytes) {
        this.type = type;
        this.byteLength = bytes;
    }

    /** Gets total file length. */
    public int getByteLength() {
        return byteLength;
    }

    /** */
    public int getType() {
        return type;
    }
}
