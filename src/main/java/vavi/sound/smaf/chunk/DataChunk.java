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
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Data Chunk.
 * <pre>
 * "Dch*" *: language code
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041226 nsano initial version <br>
 */
public class DataChunk extends Chunk {

    /** */
    public DataChunk(byte[] id, int size) {
        super(id, size);

        this.languageCode = id[3] & 0xff;
Debug.println(Level.FINE, "Data: lang: " + languageCode + ", size: " + size);
    }

    /** */
    public DataChunk() {
        System.arraycopy("Dch".getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

Debug.println(Level.FINER, "available: " + dis.available());
        while (dis.available() > 4) { // TODO normal files should be 0.
            SubData subDatum = new SubData(dis);
Debug.println(Level.FINE, subDatum);
            subData.put(subDatum.tag, subDatum);
Debug.println(Level.FINER, "SubData: " + subDatum.tag + ", " + subDatum.data.length + ", " + dis.available());
        }
        dis.skipBytes(dis.available()); // TODO not necessary if the file is normal
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
    private Map<String, SubData> subData = new TreeMap<>();

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
        private String tag;

        /** Data */
        private byte[] data;

        /** */
        public String toString() {
            try {
                String string = new String(data, defaultEncoding);
                boolean printable = true;
//System.err.print("@@@: ");
                for (char c : string.toCharArray()) {
//System.err.print(c);
                    if (!StringUtil.isPrintableChar(c)) {
                        printable = false;
                        break;
                    }
                }
//System.err.println();
                if (printable) {
                    return "SubData(" + tag + ", lang: " + getLanguageCode() + ", size: " + data.length + "): " + string;
                } else {
                    return "SubData(" + tag + ", lang: " + getLanguageCode() + ", size: " + data.length + ")\n" + StringUtil.getDump(data, 128);
                }
            } catch (UnsupportedEncodingException e) {
                assert false;
                return null;
            }
        }
    }
}
