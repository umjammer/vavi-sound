/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Data Chunk.
 * <pre>
 * "Dch*" *: language code
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041226 nsano initial version <br>
 */
public class DataChunk extends Chunk {

    /** */
    public DataChunk(byte[] id, int size) {
        super(id, size);

        this.languageCode = id[3] & 0xff;
Debug.println("Data: lang: " + languageCode + ", size: " + size);
    }

    /** */
    public DataChunk() {
        System.arraycopy("Dch".getBytes(), 0, id, 0, 3);
        this.size = 0;
    }

    /** */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {

//Debug.println("available: " + available());
        while (available() > 4) { // TODO 正常ファイルは 0 でいい
            SubData subDatum = new SubData(is);
Debug.println(subDatum);
            subData.put(subDatum.tag, subDatum);
//Debug.println("SubData: " + subDatum.tag + ", " + subDatum.data.length + ", " + available());
        }
        skip(is, available()); // TODO 正常ファイルなら必要なし
    }

    /** */
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
    private Map<String, SubData> subData = new TreeMap<String, SubData>();

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
     * 記述する文字コードがUnicode の場合、それぞれの文字群先頭にBOM (バイトオーダーマーク) を設定
     * すること。BOM 無しの場合、ビッグエンディアンとして解釈する。
     */
    boolean isUnicode(int languageCode) {
        return languageCode >= 0x20 && languageCode <= 0x25; 
    }
    
    /**
     * <pre>
     * tag  2byte (固定)
     * size 2byte (固定)
     * data n byte (可変)
     * </pre>
     */
    class SubData {
        
        /** */
        SubData(InputStream is) throws IOException {
            byte[] temp = new byte[2];
            read(is, temp);
            this.tag = new String(temp);
            
            int size = readShort(is);
            
            this.data = new byte[size];
            read(is, this.data);
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

        /** タグ */
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
                    return "SubData(" + new String(tag) + ", lang: " + getLanguageCode() + ", size: " + data.length + "): " + string;
                } else {
                    return "SubData(" + new String(tag) + ", lang: " + getLanguageCode() + ", size: " + data.length + ")\n" + StringUtil.getDump(data);
                }
            } catch (UnsupportedEncodingException e) {
                assert false;
                return null;
            }
        }
    }
}

/* */
