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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.LinkedHashMap;
import java.util.Map;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.vavi.header.SorcMessage;
import vavi.sound.mfi.vavi.header.TitlMessage;
import vavi.sound.mfi.vavi.header.VersMessage;

import static java.lang.System.getLogger;


/**
 * HeaderChunk.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 070118 nsano initial version <br>
 */
class HeaderChunk {

    private static final Logger logger = getLogger(HeaderChunk.class.getName());

    /** {@value} */
    public static final String TYPE = "melo";

    /** = ({@link #majorType} + {@link #minorType} + {@link #tracksCount}) */
    public static final int HEADER_LENGTH = 3;

    /** ringtone data */
    public static final int MAJOR_TYPE_RING_TONE = 0x01;
    /** music data */
    public static final int MAJOR_TYPE_MUSIC = 0x02;
    /** when {@link #majorType} is {@link #MAJOR_TYPE_RING_TONE}, all music data */
    public static final int MINOR_TYPE_ALL = 0x01;
    /** when {@link #majorType} is {@link #MAJOR_TYPE_RING_TONE}, part of music data */
    public static final int MINOR_TYPE_PART = 0x02;
    /** when {@link #majorType} is {@link #MAJOR_TYPE_MUSIC}, fixed */
    public static final int MINOR_TYPE_MUSIC = 0x00;

    /** MFi data length ({@link #TYPE} + {@link #mfiDataLength} are excluded) */
    private int mfiDataLength;

    /** {@link #HEADER_LENGTH} + {@link #getSubChunksLength()}  */
    private int dataLength;

    /** */
    private int majorType = -1;
    /** */
    private int minorType = -1;

    /** max 4 */
    private int tracksCount;

    /** header, sub chunks */
    private final Map<String, SubMessage> subChunks = new LinkedHashMap<>();

    /** */
    private Support support;

    /** */
    public interface Support {
        /**
         * Gets {@link SubMessage} from {@link #support}.
         * Specs says {@link SubMessage} is located at top of {@link Sequence#getTracks()}[0].
         */
        void init(Map<String, SubMessage> subChunks);
        int getAudioDataLength();
        int getTracksLength();
        int getTracksCount();
    }

    /**
     * for reading
     */
    private HeaderChunk() {
    }

    /**
     * for writing
     */
    public HeaderChunk(Support support) {
        this.support = support;

        support.init(subChunks);
    }

    /**
     * MFi data length (except type, length)
     */
    public int getMfiDataLength() {
        return mfiDataLength;
    }

    /**
     * Chunk data length (except type, length)
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
     * Gets header chunk length.
     * <li>TODO scope
     */
    public int getSubChunksLength() {
        int length = 0;
        for (SubMessage subChunk : subChunks.values()) {
            length += 4 + 2 + subChunk.getDataLength(); // type + length + ...
//logger.log(Level.DEBUG, subChunk + ": " + subChunks.getSubLength());
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
     * @throws InvalidMfiDataException throws when minimum {@link SubMessage}s
     *         { {@link VaviMfiFileFormat#setSorc(int) "sorc"},
     *         {@link VaviMfiFileFormat#setTitle(String) "titl"},
     *         {@link VaviMfiFileFormat#setVersion(String) "vers"} }
     *         are not set
     */
    public void writeTo(OutputStream os)
        throws InvalidMfiDataException,
               IOException {

        // 1. check
        if (!isValid()) {
logger.log(Level.DEBUG, "majorType: " + majorType);
logger.log(Level.DEBUG, "minorType: " + minorType);
logger.log(Level.DEBUG, "[sorc]: "    + subChunks.get(SorcMessage.TYPE));
logger.log(Level.DEBUG, "[titl]: "    + subChunks.get(TitlMessage.TYPE));
logger.log(Level.DEBUG, "[vers]: "    + subChunks.get(VersMessage.TYPE));
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
logger.log(Level.DEBUG, "mfiDataLength: " + mfiDataLength);
logger.log(Level.DEBUG, "dataLength: "    + dataLength);
logger.log(Level.DEBUG, "majorType: "     + majorType);
logger.log(Level.DEBUG, "minorType: "     + minorType);
logger.log(Level.DEBUG, "numberTracks: "  + tracksCount);

        for (SubMessage subChunk : subChunks.values()) {
            subChunk.writeTo(os);
        }
    }

    /**
     * @throws InvalidMfiDataException first 4 bytes are not {@link #TYPE}
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
logger.log(Level.DEBUG, "mfiDataLength: " + headerChunk.mfiDataLength);

        // 1.3.1 offset to "trac" or "adat"
        headerChunk.dataLength = dis.readUnsignedShort();
logger.log(Level.DEBUG, "dataLength: " + headerChunk.dataLength);

        // 1.3.2.1 major type
        headerChunk.majorType = dis.readUnsignedByte();
logger.log(Level.DEBUG, "majorType: " + headerChunk.majorType);
        // 1.3.2.2 minor type
        headerChunk.setMinorType(dis.readUnsignedByte());
logger.log(Level.DEBUG, "minorType: " + headerChunk.minorType);
        // 1.3.3 number of tracks
        headerChunk.tracksCount = dis.readUnsignedByte();
logger.log(Level.DEBUG, "numberTracks: " + headerChunk.tracksCount);

        // 1.4 header sub chunks
        long l = 0;
        while (l < headerChunk.dataLength - HEADER_LENGTH) {
            SubMessage subChunk = SubMessage.readFrom(is);
            headerChunk.subChunks.put(subChunk.getSubType(), subChunk);
            l +=  4 + 2 + subChunk.getDataLength(); // type + length +
//logger.log(Level.DEBUG, "header subchunk length sum: " + l + " / " + (headerLength - 3));
        }

        return headerChunk;
    }
}
