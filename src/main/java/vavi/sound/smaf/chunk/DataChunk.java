/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;
import static vavi.sound.smaf.chunk.SubData.stringTags;
import static vavi.sound.smaf.chunk.SubData.tags;


/**
 * Data Chunk.
 * <pre>
 * "Dch*" *: language code
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041226 nsano initial version <br>
 */
public class DataChunk extends Chunk {

    private static final Logger logger = getLogger(DataChunk.class.getName());

    private static final String FOURCC = "Dch";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key.substring(0, 3));
    }

    @Override
    public DataChunk init(byte[] id, int size) {
        super.init(id, size);

        this.languageCode = id[3] & 0xff;
logger.log(Level.DEBUG, "Data: lang: " + languageCode + ", size: " + size);

        return this;
    }

    /** */
    public DataChunk() {
        System.arraycopy(FOURCC.getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

logger.log(Level.TRACE, "available: " + dis.available());
        while (dis.available() > 4) { // TODO normal files must be 0. 4 means there is a case additional 4 bytes are exists at end
            SubData subDatum = new SubData(dis);
logger.log(Level.TRACE, subDatum);
            subData.put(subDatum.tag, subDatum);
logger.log(Level.TRACE, "SubData: " + subDatum.tag + ", " + subDatum.data.length + ", " + dis.available());
        }
logger.log(Level.INFO, "skip unexpected bytes left: " + dis.available());
        dis.skipBytes(dis.available()); // TODO not necessary for a normal file, for illegal case that has additional 4 bytes are exists at end
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        for (SubData subDatum : subData.values()) {
            subDatum.writeTo(os);
        }
    }

    /** */
    private static final String defaultEncoding = "Windows-31J";

    /** */
    private final Map<String, SubData> subData = new TreeMap<>();

    /** */
    String getSubDataByTag(String tag) {
        try {
            return new String(subData.get(tag).data, defaultEncoding); // use #getLnaguageCode()
        } catch (UnsupportedEncodingException e) {
            return new String(subData.get(tag).data);
        }
    }

    /** */
    void addSubData(String tag, String data) {
        SubData subDatum;
        try {
            subDatum = new SubData(tag, data.getBytes(defaultEncoding)); // use #getLnaguageCode()
        } catch (UnsupportedEncodingException e) {
            subDatum = new SubData(tag, data.getBytes());
        }
        subData.put(tag, subDatum);
        size += subDatum.getSize();
    }

    /** */
    private int languageCode;

    /** */
    public int getLanguageCode() {
        return languageCode;
    }

    /** */
    public void setLanguageCode(int languageCode) {
        this.languageCode = languageCode;
        id[3] = (byte) languageCode;
    }

    /**
     * if the character code to be written is Unicode, set a BOM (byte order mark) at the beginning of
     * each character group. If there is no BOM, it will be interpreted as big endian.
     */
    boolean isUnicode(int languageCode) {
        return languageCode >= 0x20 && languageCode <= 0x25;
    }

    /**
     * <pre>
     * tag  2byte (fixed)
     * size 2byte (fixed)
     * data n byte (variable)
     * </pre>
     */
    class SubData {

        /** */
        SubData(DataInput di) throws IOException {
            byte[] temp = new byte[2];
            di.readFully(temp);
            this.tag = new String(temp);

            int size = di.readUnsignedShort();

            this.data = new byte[size];
            di.readFully(this.data);
        }

        /** */
        SubData(String tag, byte[] data) {
            this.tag = tag;
            this.data = data;
        }

        /** */
        public void writeTo(OutputStream os) throws IOException {
            DataOutputStream dos = new DataOutputStream(os);
            os.write(tag.getBytes());
            dos.writeShort(data.length);
            os.write(data);
        }

        /** get data size in file */
        public int getSize() {
            return 2 + 2 + data.length;
        }

        /** tag */
        private final String tag;

        /** Data */
        private final byte[] data;

        @Override
        public String toString() {
            if (stringTags.contains(tag)) {
                return "SubData(" + tag + ", lang: " + getLanguageCode() + ", size: " + data.length + ", " + tags.getProperty(tag) + "): " + new String(data, Charset.forName(defaultEncoding));
            } else {
                return "SubData(" + tag + ", lang: " + getLanguageCode() + ", size: " + data.length + ", " + tags.getProperty(tag) + "): " + Arrays.toString(data); // \n" + StringUtil.getDump(data, 128);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getDC().format(getId() + languageCode));
        try (var dc = getDC().open()) {
            subData.values().stream().map(sd -> dc.format(sd.toString())).forEach(sb::append);
        }

        return sb.toString();
    }
}
