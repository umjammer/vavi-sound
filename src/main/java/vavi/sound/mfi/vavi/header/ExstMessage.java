/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.SubMessage;


/**
 * �g���X�e�[�^�X A ���b�Z�[�W�� MFi Header Sub Chunk.
 * 
 * <pre>
 *  &quot;exst&quot; 2 bytes: extended status data length
 * </pre>
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class ExstMessage extends SubMessage {

    /** */
    public static final String TYPE = "exst";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public ExstMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** TODO check endian */
    public ExstMessage(int data) {
        super(TYPE, new byte[] {
            (byte) ((data & 0xff00) >> 8),
            (byte)  (data & 0x00ff)
        });
    }

    /** */
    public int getExst() {
        byte[] data = getData();
        return data[0] * 0xff + data[1];
    }

    /** TODO check endian */
    public void setExst(int data)
        throws InvalidMfiDataException {

        setData(new byte[] {
            (byte) ((data & 0xff00) >> 8),
            (byte)  (data & 0x00ff)
        });
    }

    /** */
    public String toString() {
        return TYPE + ": " + getDataLength() + ": " + getExst();
    }
}

/* */
