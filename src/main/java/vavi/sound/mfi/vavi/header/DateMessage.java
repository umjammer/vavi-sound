/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import java.util.Date;

import vavi.sound.mfi.vavi.SubMessage;


/**
 * 日付情報 MFi Header Sub Chunk.
 *
 * <pre>
 *  &quot;date&quot; 8 bytes: date created
 *  format yyyymmdd (ex. 19990716)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class DateMessage extends SubMessage {

    /** */
    public static final String TYPE = "date";

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * <li>TODO format, 8 byte check
     * @param type ignored
     */
    public DateMessage(String type, byte[] data) {
        super(TYPE, data);
    }

    /** TODO format, 8 byte check */
    public DateMessage(Date date) {
        super(TYPE, date.toString());
    }

    /** */
    public String toString() {
        int length = getDataLength();
        byte[] data = getData();

        return "date: " + length + ": " + new String(data);
    }
}

/* */
