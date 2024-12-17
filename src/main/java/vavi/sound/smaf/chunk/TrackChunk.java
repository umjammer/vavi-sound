/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.util.List;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;


/**
 * TrackChunk.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public abstract class TrackChunk extends Chunk {

    /**
     * <pre>
     * 0:   ma1
     * 1~4: ma2
     * 5:   ma3
     * 6:   ma5
     * 7:   ma7
     * </pre>
     */
    protected int trackNumber;

    /** */
    public TrackChunk(byte[] id, int size) {
        super(id, size);

        this.trackNumber = id[3] & 0xff;
    }

    /** */
    protected TrackChunk() {
    }

    /** */
    public int getTrackNumber() {
        return trackNumber;
    }

    /** */
    public void setTrackNumber(int trackNumber) {
        this.trackNumber = trackNumber;
        id[3] = (byte) trackNumber;
    }

    /** */
    protected ChannelStatus[] channelStatuses;

    /** */
    public enum FormatType {
        /** */
        HandyPhoneStandard(2, false),
        /** perform compression using Huffman encoding */
        MobileStandard_Compress(16, true),
        /** */
        MobileStandard_NoCompress(16, false),
//        /** TODO what? */
//        Unknown3(32, false);
        /** TODO = UNKNOWN3 ??? */
        SEQU(32, false);
        /** */
        final boolean compressed;
        /** size in file */
        public final int size;
        /** */
        FormatType(int size, boolean compressed) {
            this.size = size;
            this.compressed = compressed;
        }
    }

    /** */
    protected FormatType formatType;

    /**
     * @return Returns the formatType.
     */
    public FormatType getFormatType() {
        return formatType;
    }

    /**
     * This status defines the actual format of this Track Chunk. To reduce data size, it is possible to write in
     * LSI Native Format and other sequence formats assuming a powerful Control CPU in the future.
     * Compress specifies that compression is performed using Huffman encoding.
     */
    public void setFormatType(FormatType formatType) {
        this.formatType = formatType;
    }

    /** */
    public enum SequenceType {
        /**
         * Sequence Data is one continuous sequence data. Seek Point and Phrase List are used to refer to
         * meaningful positions in a sequence from the outside.
         */
        StreamSequence,
        /**
         * Sequence Data is a continuous representation of multiple phrase data. Phrase List is used to
         * recognize individual phrases from the outside.
         */
        SubSequence
    }

    /** */
    protected SequenceType sequenceType;

    /** */
    public void setSequenceType(SequenceType sequenceType) {
        this.sequenceType = sequenceType;
    }

    /** time bases */
    protected static final int[] timeBaseTable = {
        1, 2, 4, 5, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        10, 20, 40, 50
    };

    /**
     * <pre>
     * - formatType = 0x00
     * 0x00 1 msec
     * 0x01 2 msec
     * 0x02 4 msec
     * 0x03 5 msec
     * 0x04 ~ 0x0F Reserved
     * 0x10 10 msec
     * 0x11 20 msec
     * 0x12 40 msec
     * 0x13 50 msec
     * 0x14 ~ 0xFF Reserved
     * -
     * </pre>
     * @param timeBase real timeBase [msec]
     * @return index of timeBase
     */
    private static int findTimeBase(int timeBase) {
        for (int i = 0; i < timeBaseTable.length; i++) {
            if (timeBase == timeBaseTable[i]) {
                return i;
            }
        }
        throw new IllegalArgumentException(String.valueOf(timeBase));
    }

    /** */
    protected int durationTimeBase = 1;

    /**
     * @return Returns the durationTimeBase [msec].
     */
    public int getDurationTimeBase() {
        return timeBaseTable[durationTimeBase];
    }

    /**
     * Timebase_D and Timebase_G must be the same.
     * @param durationTimeBase in [msec]
     * @throws IllegalArgumentException wrong durationTimeBase
     */
    public void setDurationTimeBase(int durationTimeBase) {
        this.durationTimeBase = findTimeBase(durationTimeBase);
    }

    /** */
    protected int gateTimeTimeBase = 1;

    /**
     * @return Returns the gateTimeTimeBase [msec].
     */
    public int getGateTimeTimeBase() {
        return timeBaseTable[gateTimeTimeBase];
    }

    /**
     * @param gateTimeTimeBase in [msec]
     * @see #durationTimeBase
     * @throws IllegalArgumentException wrong gateTimeTimeBase
     */
    public void setGateTimeTimeBase(int gateTimeTimeBase) {
        this.gateTimeTimeBase = findTimeBase(gateTimeTimeBase);
    }

    /** */
    protected Chunk sequenceDataChunk;

    /** "[MA]tsq" */
    public void setSequenceDataChunk(SequenceDataChunk sequenceDataChunk) {
        this.sequenceDataChunk = sequenceDataChunk;
        size += sequenceDataChunk.getSize() + 8;
    }

    /**
     * MetaMessage (META_MACHINE_DEPEND) is placed at the beginning.
     * TrackChunk information is maintained in Properties.
     */
    public abstract List<SmafEvent> getSmafEvents()
        throws InvalidSmafDataException;
}
