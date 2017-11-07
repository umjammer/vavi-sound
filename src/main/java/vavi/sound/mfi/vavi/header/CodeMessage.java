/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.SubMessage;


/**
 * 文字コード情報 MFi Header Sub Chunk.
 * <pre>
 *  &quot;code&quot; 4 bytes: code
 * </pre>
 * @see TitlMessage
 * @see CopyMessage
 * @see ProtMessage
 * @see AuthMessage
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 070125 nsano initial version <br>
 * @since MFi 5.0
 */
public class CodeMessage extends SubMessage {

    /** */
    public static final String TYPE = "exst";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public CodeMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** TODO check endian */
    public CodeMessage(int data) {
        super(TYPE, new byte[] {
            (byte) ((data & 0xff000000) >> 24),
            (byte) ((data & 0x00ff0000) >> 16),
            (byte) ((data & 0x0000ff00) >> 8),
            (byte)  (data & 0x000000ff)
        });
    }

    /** */
    public int getCode() {
        byte[] data = getData();
        return (data[0] << 24) & 0xff |
               (data[1] << 16) & 0xff |
               (data[2] <<  8) & 0xff |
                data[3];
    }

    /** TODO check endian */
    public void setCode(int data)
        throws InvalidMfiDataException {

        setData(new byte[] {
            (byte) ((data & 0xff000000) >> 24),
            (byte) ((data & 0x00ff0000) >> 16),
            (byte) ((data & 0x0000ff00) >> 8),
            (byte)  (data & 0x000000ff)
        });
    }

    /** */
    public String toString() {
        return TYPE + ": " + getDataLength() + ": " + getCode();
    }
}

/* */
