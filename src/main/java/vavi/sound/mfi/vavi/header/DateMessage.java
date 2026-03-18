/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import java.util.Date;

import vavi.sound.mfi.vavi.SubMessage;


/**
 * MFi Header Sub Chunk for date information.
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

    @Override
    public boolean accept(String key) {
        return TYPE.equals(key);
    }

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * <li>TODO format, 8 byte check
     *
     * @param type ignored
     * @return this
     */
    @Override
    public SubMessage init(String type, byte[] data) {
        return super.init(TYPE, data);
    }

    /** TODO format, 8 byte check */
    public SubMessage init(Date date) {
        return super.init(TYPE, date.toString());
    }

    @Override
    public String toString() {
        int length = getDataLength();
        byte[] data = getData();

        return "date: " + length + ": " + new String(data);
    }
}
