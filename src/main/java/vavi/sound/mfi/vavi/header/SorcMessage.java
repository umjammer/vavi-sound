/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import vavi.sound.mfi.vavi.SubMessage;


/**
 * 再配布不可識別子 MFi Header Sub Chunk.
 *
 * <pre>
 *
 *  &quot;sorc&quot; 1 byte: protect information *2
 *
 *   *2 sorc
 *      7654321.
 *      msb 7   0000000     from network
 *              0000001     from terminal
 *              0000010     from external i/f
 *      .......0
 *      lsb     0: no copyright, 1: has copyright
 *
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 *          0.01 030825 nsano complete <br>
 */
public class SorcMessage extends SubMessage {

    /** */
    public static final String TYPE = "sorc";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public SorcMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** */
    public SorcMessage(int sorc) {
        super(TYPE, new byte[] {
            (byte) sorc
        });
    }

    /** @return 0: no copyright, 1: has copyright */
    public int getSorc() {
        return getData()[0];
    }

    /** @param sorc 0: no copyright, 1: has copyright */
    public void setSorc(int sorc) {
        getData()[0] = (byte) sorc;
    }

    /** */
    public String toString() {
        return String.format("sorc: %d: 0x%02x", getDataLength(), getSorc());
    }
}

/* */
