/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import java.io.UnsupportedEncodingException;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.SubMessage;


/**
 * MFi Header Sub Chunk for version information.
 *
 * <pre>
 *  &quot;vers&quot; 4 bytes: mld version
 *  format mmnn (ex. 0100)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030822 nsano initial version <br>
 */
public class VersMessage extends SubMessage {

    /** */
    public static final String TYPE = "vers";

    @Override
    public boolean accept(String key) {
        return TYPE.equals(key);
    }

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * <li>TODO length 4 check
     *
     * @param type ignored
     * @return
     */
    @Override
    public SubMessage init(String type, byte[] data) {
        return super.init(TYPE, data);
    }

    /**
     * TODO length 4 check
     * @param data 4 bytes number (ex. { '0', '3', '0', '1' })
     */
    public SubMessage init(String data) {
        return super.init(TYPE, data);
    }

    /** */
    public String getVersion() {
        try {
            return new String(getData(), readingEncoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * TODO length 4 check
     * @param version 4 bytes number as string (ex. "0301")
     */
    public void setVersion(String version) throws InvalidMfiDataException {
        try {
            setData(version.getBytes(readingEncoding));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return "vers: " + getDataLength() + ": " + getVersion();
    }
}
