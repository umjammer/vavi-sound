/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.vavi.header.SorcMessage;
import vavi.sound.mfi.vavi.header.TitlMessage;
import vavi.sound.mfi.vavi.header.VersMessage;
import vavi.util.Debug;


/**
 * HeaderChunk.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070118 nsano initial version <br>
 */
class HeaderChunk {
    /** {@value} */
    public static final String TYPE = "melo";

    /** = ({@link #majorType} + {@link #minorType} + {@link #tracksCount}) */
    public static final int HEADER_LENGTH = 3;

    /** 着信メロディデータ */
    public static final int MAJOR_TYPE_RING_TONE = 0x01;
    /** 音楽データ */
    public static final int MAJOR_TYPE_MUSIC = 0x02;
    /** {@link #majorType} が {@link #MAJOR_TYPE_RING_TONE} の場合、全曲データ */
    public static final int MINOR_TYPE_ALL = 0x01;
    /** {@link #majorType} が {@link #MAJOR_TYPE_RING_TONE} の場合、部分データ  */
    public static final int MINOR_TYPE_PART = 0x02;
    /** {@link #majorType} が {@link #MAJOR_TYPE_MUSIC} の場合、固定  */
    public static final int MINOR_TYPE_MUSIC = 0x00;

    /** MFi data length ({@link #TYPE} + {@link #mfiDataLength} are excluded) */
    private int mfiDataLength;

    /** {@link #HEADER_LENGTH} + {@link #getSubChunksLength()}  */
    private int dataLength;

    /** */
    private int majorType = -1;
    /** */
    private int minorType = -1;

    /** 最大 4 やって */
    private int tracksCount;

    /** ヘッダ・サブチャンク */
    private Map<String, SubMessage> subChunks = new LinkedHashMap<>();

    /** */
    private Support support;

    /** */
    public interface Support {
        /**
         * {@link #support} から {@link SubMessage} を取り出します。
         * {@link SubMessage} は {@link Sequence#getTracks()}[0] の先頭にあるのが仕様
         */
        void init(Map<String, SubMessage> subChunks);
        int getAudioDataLength();
        int getTracksLength();
        int getTracksCount();
    }

    /**
     * 読み込み用
     */
    private HeaderChunk() {
    }

    /**
     * 書き出し用
     */
    public HeaderChunk(Support support) {
        this.support = support;

        support.init(subChunks);
    }

    /**
     * MFi データの長さ (type, length は除く)
     */
    public int getMfiDataLength() {
        return mfiDataLength;
    }

    /**
     * Chunk データの長さ (type, length は除く)
     * <li>TODO -> interface Chunk
     */
    public int getDataLength() {
        return dataLength;
    }

    /**
     * @see #MAJOR_TYPE_RING_TONE
     * @see #MAJOR_TYPE_MUSIC
     */
    public int getMajorType() {
        return majorType;
    }

    /** */
    public void setMajorType(int majorType) {
        this.majorType = majorType;
    }

    /**
     * @see #MINOR_TYPE_MUSIC
     * @see #MINOR_TYPE_ALL
     * @see #MINOR_TYPE_PART
     */
    public int getMinorType() {
        return minorType;
    }

    /** */
    public void setMinorType(int minorType) {
        this.minorType = minorType;
    }

    /** */
    public int getTracksCount() {
        return tracksCount;
    }

    /** */
    public Map<String, SubMessage> getSubChunks() {
        return subChunks;
    }

    /**
     * ヘッダサブチャンクの長さを取得します。
     * <li>TODO scope
     */
    public int getSubChunksLength() {
        int length = 0;
        for (SubMessage subChunk : subChunks.values()) {
            length += 4 + 2 + subChunk.getDataLength(); // type + length + ...
//Debug.println(subChunk + ": " + subChunks.getSubLength());
        }
        return length;
    }

    /** */
    private boolean isValid() {
        return (majorType != -1 &&
                minorType != -1 &&
                subChunks.containsKey(SorcMessage.TYPE) &&
                subChunks.containsKey(TitlMessage.TYPE) &&
                subChunks.containsKey(VersMessage.TYPE));
    }

    /**
     * @throws InvalidMfiDataException 最低限の {@link SubMessage}
     *         { {@link VaviMfiFileFormat#setSorc(int) "sorc"},
     *         {@link VaviMfiFileFormat#setTitle(String) "titl"},
     *         {@link VaviMfiFileFormat#setVersion(String) "vers"} }
     *         が設定されていない場合スローされます
     */
    public void writeTo(OutputStream os)
        throws InvalidMfiDataException,
               IOException {

        // 1. check
        if (!isValid()) {
Debug.println(Level.FINE, "majorType: " + majorType);
Debug.println(Level.FINE, "minorType: " + minorType);
Debug.println(Level.FINE, "[sorc]: "    + subChunks.get(SorcMessage.TYPE));
Debug.println(Level.FINE, "[titl]: "    + subChunks.get(TitlMessage.TYPE));
Debug.println(Level.FINE, "[vers]: "    + subChunks.get(VersMessage.TYPE));
            throw new InvalidMfiDataException("fields are not filled");
        }

        // 2. recalc
        this.dataLength = HEADER_LENGTH + getSubChunksLength();
        int headerChunkLengthDash = 2 + dataLength; // type, length excluded
        int audioChunksLength = support.getAudioDataLength();
        int trackChunksLength = support.getTracksLength();
        this.mfiDataLength = headerChunkLengthDash + audioChunksLength + trackChunksLength;
        this.tracksCount = support.getTracksCount();

        // 3. write
        DataOutputStream dos = new DataOutputStream(os);

        //
        dos.writeBytes(TYPE);
        dos.writeInt(mfiDataLength);

        dos.writeShort(dataLength);
        dos.writeByte(majorType);
        dos.writeByte(minorType);
        dos.writeByte(tracksCount);
Debug.println(Level.FINE, "mfiDataLength: " + mfiDataLength);
Debug.println(Level.FINE, "dataLength: "    + dataLength);
Debug.println(Level.FINE, "majorType: "     + majorType);
Debug.println(Level.FINE, "minorType: "     + minorType);
Debug.println(Level.FINE, "numberTracks: "  + tracksCount);

        for (SubMessage subChunk : subChunks.values()) {
            subChunk.writeTo(os);
        }
    }

    /**
     * @throws InvalidMfiDataException 最初の 4 bytes が {@link #TYPE} で無い場合
     */
    public static HeaderChunk readFrom(InputStream is)
        throws InvalidMfiDataException,
               IOException {

        HeaderChunk headerChunk = new HeaderChunk();

        DataInputStream dis = new DataInputStream(is);

        // 1.1. type
        byte[] bytes = new byte[4];
        dis.readFully(bytes, 0, 4);  // type
        String string = new String(bytes);
        if (!TYPE.equals(string)) {
            throw new InvalidMfiDataException("invalid type: " + string);
        }

        // 1.2 length
        headerChunk.mfiDataLength = dis.readInt();
Debug.println(Level.FINE, "mfiDataLength: " + headerChunk.mfiDataLength);

        // 1.3.1 offset to "trac" or "adat"
        headerChunk.dataLength = dis.readUnsignedShort();
Debug.println(Level.FINE, "dataLength: " + headerChunk.dataLength);

        // 1.3.2.1 major type
        headerChunk.majorType = dis.readUnsignedByte();
Debug.println(Level.FINE, "majorType: " + headerChunk.majorType);
        // 1.3.2.2 minor type
        headerChunk.setMinorType(dis.readUnsignedByte());
Debug.println(Level.FINE, "minorType: " + headerChunk.minorType);
        // 1.3.3 number of tracks
        headerChunk.tracksCount = dis.readUnsignedByte();
Debug.println(Level.FINE, "numberTracks: " + headerChunk.tracksCount);

        // 1.4 header sub chunks
        long l = 0;
        while (l < headerChunk.dataLength - HEADER_LENGTH) {
            SubMessage subChunk = SubMessage.readFrom(is);
            headerChunk.subChunks.put(subChunk.getSubType(), subChunk);
            l +=  4 + 2 + subChunk.getDataLength(); // type + length +
//Debug.println("header subchunk length sum: " + l + " / " + (headerLength - 3));
        }

        return headerChunk;
    }
}
