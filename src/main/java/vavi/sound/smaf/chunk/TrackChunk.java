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
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
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
    public enum FormatType {
        /** */
        HandyPhoneStandard(2, false),
        /** ハフマン符号化による圧縮を行う */
        MobileStandard_Compress(16, true),
        /** */
        MobileStandard_NoCompress(16, false),
        /** TODO what? */
        Unknown3(32, false);
        /** */
        boolean compressed;
        /** size in file */
        int size;
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
     * このステータスでこの Track Chunk の実フォーマットを定義する。データ・サイズ削減のため、LSI
     * Native Format での記述や、将来パワフルな Control CPU を想定して、その他のシーケンス・フォーマ
     * ットを記述可能とする。Compress はハフマン符号化による圧縮を行うことと定める。
     */
    public void setFormatType(FormatType formatType) {
        this.formatType = formatType;
    }

    /** */
    public enum SequenceType {
        /**
         * Sequence Data は１つの連続したシーケンス・データである。Seek Point や Phrase List はシーケン
         * ス中の意味のある位置を外部から参照する目的で利用する。
         */
        StreamSequence,
        /**
         * Sequence Data は複数のフレーズデータを連続で表記したものである。Phrase List は外部から個
         * 別フレーズを認識する為に用いる。
         */
        SubSequence;
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
     * 0x04〜0x0F Reserved
     * 0x10 10 msec
     * 0x11 20 msec
     * 0x12 40 msec
     * 0x13 50 msec
     * 0x14〜0xFF Reserved
     * - 
     * </pre>
     * @param timeBase real timeBase [msec]
     * @return index of timeBase
     */
    private int findTimeBase(int timeBase) {
        for (int i = 0; i < timeBaseTable.length; i++) {
            if (timeBase == timeBaseTable[i]) {
                return i;
            }
        }
        throw new IllegalArgumentException(String.valueOf(timeBase));
    }

    /** */
    protected int durationTimeBase;

    /**
     * @return Returns the durationTimeBase [msec].
     */
    public int getDurationTimeBase() {
        return timeBaseTable[durationTimeBase];
    }

    /**
     * Timebase_D と Timebase_G は同一であること。
     * @param durationTimeBase in [msec]
     * @throws IllegalArgumentException wrong durationTimeBase
     */
    public void setDurationTimeBase(int durationTimeBase) {
        this.durationTimeBase = findTimeBase(durationTimeBase);
    }

    /** */
    protected int gateTimeTimeBase;

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
     * 先頭に MetaMessage (META_MACHINE_DEPEND) が入る。 
     * TrackChunk の情報を Properties で保持している。
     */
    public abstract List<SmafEvent> getSmafEvents()
        throws InvalidSmafDataException;
}

/* */
