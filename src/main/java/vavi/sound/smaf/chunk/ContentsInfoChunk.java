/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

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
 * ContentsInfo Chunk.
 * <pre>
 * "CNTI"
 *
 *  Contents Class ：1 byte (required)
 *  Contents Type ：1 byte (required)
 *  Contents Code Type ：1 byte (required)
 *  Copy Status ：1 byte (required)
 *  Copy Counts ：1 byte (required)
 *  Option ：n byte (option)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class ContentsInfoChunk extends Chunk {

    /** */
    public ContentsInfoChunk(byte[] id, int size) {
        super(id, size);
    }

    /** */
    public ContentsInfoChunk() {
        System.arraycopy("CNTI".getBytes(), 0, id, 0, 4);
        this.size = 5;
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        this.contentsClass = dis.readUnsignedByte();
Debug.println(Level.FINE, "contentsClass: " + (contentsClass == 0 ? "YAMAHA" : "Vender ID(" + contentsClass + ")"));
        this.contentsType = dis.readUnsignedByte();
Debug.printf(Level.FINE, "contentsType: 0x%02x\n", contentsType);
        this.contentsCodeType = dis.readUnsignedByte();
Debug.printf(Level.FINE, "contentsCodeType: 0x%02x\n", contentsCodeType);
        this.copyStatus = dis.readUnsignedByte();
Debug.println(Level.FINE, "copyStatus: " + StringUtil.toBits(copyStatus, 8));
        this.copyCounts = dis.readUnsignedByte();
Debug.println(Level.FINE, "copyCounts: " + copyCounts);
        byte[] option = new byte[size - 5];
        dis.readFully(option);
Debug.println(Level.FINE, "option: " + option.length + " bytes (subData)");
        int i = 0;
        while (i < option.length) {
Debug.println(Level.FINER, i + " / " + option.length + "\n" + StringUtil.getDump(option, i, option.length - i));
            SubData subDatum = new SubData(option, i, contentsCodeType);
            subData.put(subDatum.getTag(), subDatum);
Debug.println(Level.FINE, "ContentsInfo: subDatum: " + subDatum);
            i += 2 + 1 + subDatum.getData().length + 1; // tag ':' data ','
Debug.println(Level.FINER, i + " / " + option.length + "\n" + StringUtil.getDump(option, i, option.length - i));
        }
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        dos.writeByte(contentsClass);
        dos.writeByte(contentsType);
        dos.writeByte(contentsCodeType);
        dos.writeByte(copyStatus);
        dos.writeByte(copyCounts);
        for (SubData subDatum : subData.values()) {
            subDatum.writeTo(os);
        }
    }

    /** */
    public static final int CONTENT_CLASS_YAMAHA = 0x00;

    /** */
    private int contentsClass;

    /**
     * Represents a class of content.
     * *Used to differentiate when similar data formats are used in future
     *  multi-function terminals such as PDA terminals.
     */
    public void setContentsClass(int contentsClass) {
        this.contentsClass = contentsClass;
    }

    /** */
    private int contentsCodeType;

    /**
     * <pre>
     * 0x01 ISO 8859-1 (Latin-1) English, French, German Italian, Spanish, Portuguese
     * 0x02 EUC-KR(KS) Korean
     * 0x03 HZ-GB-2312 Chinese(Simplified)
     * 0x04 Big5 Chinese(Traditional)
     * 0x05 KOI8-R Russian etc.
     * 0x06 TCVN-5773:1993 Vietnamese
     * 0x07 ~ 0x1F Reserved Reserved
     * 0x20 UCS-2 Unicode
     * 0x21 UCS-4 Unicode
     * 0x22 UTF-7 Unicode
     * 0x23 UTF-8 Unicode
     * 0x24 UTF-16 Unicode
     * 0x25 UTF-32 Unicode
     * 0x26 ~ 0xFF Reserved Reserved
     * </pre>
     * However, for all types of Option in Contents Info Chunk, ",(0x2C)" is defined as the Option delimiter,
     * so it is used together with backslash "\(0x5C)" as an escape character.
     * <pre>
     *  Notation Function
     *  \,       represents “,”
     *  \\       represents "\"
     *  \        ignored
     * </pre>
     * If a character code that is assumed to be unable to correctly identify the delimiter is set,
     * data shall not be written in Option of Contents Info Chunk, but data shall be written in Optional Data Chunk.
     */
    public void setContentsCodeType(int contentsCodeType) {
        this.contentsCodeType = contentsCodeType;
    }

    /** */
    private int contentsType;

    /**
     * Represents the type of content.
     * <pre>
     * 0x00 ~ 0x0F, 0x30 ~ 0x33 ringtone melody
     * 0x10 ~ 0x1F, 0x40 ~ 0x42 karaoke type
     * 0x20 ~ 0x2F, 0x50 ~ 0x53 CM type
     * unused values other than the above reserved
     * </pre>
     */
    public void setContentsType(int contentsType) {
        this.contentsType = contentsType;
    }

    /** */
    private int copyCounts;

    /**
     * Expresses the number of copies.
     * <pre>
     * Copy Counts Description
     * 0x00 ~ 0xFE Copy Counts
     * 0xFF Copy Counts(255 or more)
     * </pre>
     * *increments the count by 1 each time a copy/move occurs.
     */
    public void setCopyCounts(int copyCounts) {
        this.copyCounts = copyCounts;
    }

    /** */
    private int copyStatus;

    /**
     * Expresses the copy definition of the content.
     * <pre>
     *                | b7       | b6       | b5       | b4       | b3       | b2       | b1      |  b0
     * ---------------+----------+----------+----------+----------+----------+----------+---------+----------
     * impossible bit | Reserved | Reserved | Reserved | Reserved | Reserved | edit     | save    | transfer
     * </pre>
     * b0 defines whether it can be transferred, b1 whether it can be saved or not, and b2 whether it can be edited.
     * (0: Possible 1: Not possible) Reserved bit is filled with “1”.
     */
    public void setCopyStatus(int copyStatus) {
        this.copyStatus = copyStatus;
    }

    /** */
    private Map<String, SubData> subData = new TreeMap<>();

    /**
     * @return null when specified sub chunk is not found
     */
    public String getSubDataByTag(String tag) {
        SubData subDatum = subData.get(tag);
        if (subDatum == null) {
            return null;
        }
        try {
            return new String(subDatum.getData(), "Windows-31J"); // use contentsCodeType
        } catch (UnsupportedEncodingException e) {
            return new String(subDatum.getData());
        }
    }

    /**
     * Option (Contents Type 0x00, for Contents Class 0x00 ~ 0xFF)
     * <p>
     * Stores genre name, song title, artist name, lyricist/composer name, etc.
     * It is not necessarily used for display, but for recognition of individual data.
     * The data has a variable length and is written separated by "Tag(2byte)" + ":(0x3A)" + "Data" + ",(0x2C)".
     * The tag name is as follows. The tag is a fixed 2-byte byte string.
     * The escape character when using ",(0x2C)" as a character in "Data" is defined in §4.2.3.
     * </p>
     * <pre>
     *  name               | tag name | Hex
     * --------------------+----------+-----------
     *  vendor name        | VN       | 0x56 0x4E
     *  carrier name       | CN       | 0x43 0x4E
     *  category name      | CA       | 0x43 0x41
     *  song title         | ST       | 0x53 0x54
     *  artist name        | AN       | 0x41 0x4E
     *  words writer       | WW       | 0x57 0x57
     *  song writer        | SW       | 0x53 0x57
     *  arrangement writer | AW       | 0x41 0x57
     *  copyright&copy;    | CR       | 0x43 0x52
     *  group name         | GR       | 0x47 0x52
     *  management info    | MI       | 0x4D 0x49
     *  creation date      | CD       | 0x43 0x44
     *  update date        | UD       | 0x55 0x44
     * </pre>
     * when adding unicode, Optional Data Chunk was added because there are character codes
     * that cannot identify the delimiter in option.
     */
    public void addSubData(String tag, String data) {
        SubData subDatum;
        try {
            subDatum = new SubData(tag, data.getBytes("Windows-31J")); // use contentsCodeType
        } catch (UnsupportedEncodingException e) {
            subDatum = new SubData(tag, data.getBytes());
        }
        subData.put(tag, subDatum);
        size += subDatum.getSize();
    }
}
