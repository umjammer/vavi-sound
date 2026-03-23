/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import vavi.sound.mfi.vavi.SubMessage;


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

    /** @see ContentsInfoChunk#setContentsCodeType */
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

    /** tag and description */
    static final Properties tags = new Properties();

    /** tags which data is string */
    static final List<String> stringTags = new ArrayList<>();

    static {
        try {
            Properties props = new Properties();
            props.load(SubMessage.class.getResourceAsStream("/vavi/sound/smaf/chunk/tag.properties"));

            for (String name : props.stringPropertyNames()) {
                String[] pair = props.getProperty(name).split(",");
                if (Boolean.parseBoolean(pair[1])) stringTags.add(name);
                tags.put(name, pair[0]);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return "SubData(" + tag + ", " + data.length + ",lang: " + contentsCodeType + ", " + tags.getProperty(tag) + "): " +
                (stringTags.contains(tag) ? new String(data, Charset.forName("Windows-31J")) : Arrays.toString(data));
    }
}
