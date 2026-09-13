/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;


/**
 * SmafFileFormat.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260912 nsano initial version <br>
 */
public class SmafFileFormat {

    /** total file length */
    protected int byteLength;

    /** */
    protected final int type;

    /** */
    public SmafFileFormat(int type, int bytes) {
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
