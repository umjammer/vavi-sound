/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import vavi.util.StringUtil;


/**
 * SubData.
 * <pre>
 * tag ':' data ','
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080417 nsano initial version <br>
 */
class SubData {

    /** */
    private int contentsCodeType;

    /** */
    SubData(String tag, byte[] data) {
        this.tag = tag;
        this.data = data;
    }

    /** */
    SubData(byte[] buffer, int offset) throws IOException {
        this(buffer, offset, 0); // TODO 0 ?
    }

    /** TODO about charset */
    SubData(byte[] buffer, int offset, int contentsCodeType) throws IOException {
        this.contentsCodeType = contentsCodeType;

        this.tag = new String(buffer, offset, 2);
        if (buffer[offset + 2] != ':') {
            throw new IllegalArgumentException("not ':' but '" + (char) buffer[offset + 2] + "'");
        }
        List<Byte> temp = new ArrayList<>();
        int i = offset + 3;
        while (i < buffer.length) {
            if (buffer[i] == '\\') {
                i++;
                if (buffer[i] != '\\') {
                    temp.add((byte) '\\');
                    i++;
                } else if (buffer[i] == ',') {
                    temp.add((byte) ',');
                    i++;
                } else {
                    // TODO ignore
                }
            } else if (buffer[i] == ',') {
                break;
            } else {
                temp.add(buffer[i]);
                i++;
            }
        }
        data = new byte[temp.size()];
        for (i = 0; i < data.length; i++) {
            data[i] = temp.get(i);
        }
    }

    /** */
    public void writeTo(OutputStream os) throws IOException {
        os.write(tag.getBytes());
        os.write(':');
        os.write(data);
        os.write(',');
    }

    /** get data size in file */
    public int getSize() {
        return 2 + 1 + data.length + 1;
    }

    /** */
    private final String tag;

    /** tag */
    public String getTag() {
        return tag;
    }

    /** */
    private final byte[] data;

    /** Data */
    public byte[] getData() {
        return data;
    }

    /** */
    public String toString() {
        try {
            return tag + "(" + data.length + "): lang: " + contentsCodeType + ": " + new String(data, "Windows-31J")
                   /* + "\n" + StringUtil.getDump(data) */;
        } catch (UnsupportedEncodingException e) {
            return tag + "(" + data.length + "): lang: " + contentsCodeType + ":\n" + StringUtil.getDump(data);
        }
    }
}
