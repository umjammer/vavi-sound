/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;


/**
 * MfiFileFormat.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 */
public class MfiFileFormat {

    /** �t�@�C�����ׂĂ̒��� */
    protected int byteLength;

    /** */
    protected int type;

    /** */
    public MfiFileFormat(int type, int bytes) {
        this.type = type;
        this.byteLength = bytes;
    }

    /** �t�@�C�����ׂĂ̒������擾���܂��B */
    public int getByteLength() {
        return byteLength;
    }

    /** */
    public int getType() {
        return type;
    }
}

/* */
