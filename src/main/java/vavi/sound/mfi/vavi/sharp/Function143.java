/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.sharp;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.sequencer.MachineDependentFunction;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * Sharp System exclusive message function 0x8f processor.
 * (Wave Setup)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 051111 nsano initial version <br>
 */
public class Function143 implements MachineDependentFunction {

    /** このデータのヘッダ分長さ */
    private static final int HEADER_LENGTH = 7;

    /**
     * 0x8f Wave Setup
     *
     * @param message see below
     * <pre>
     *  0    delta
     *  1    ff
     *  2    ff
     *  3-4  length
     *  5    vendor
     *  6    0x8f
     * </pre>
     */
    @Override
    public void process(MachineDependentMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getMessage();

        int index = HEADER_LENGTH;
        while (index < data.length) {
            int subId  = data[index++];
            int subIdLength = data[index++];
            byte[] subIdData = new byte[subIdLength];
            System.arraycopy(data, index, subIdData, 0, subIdLength);
            index += subIdLength;
Debug.printf(Level.FINER, "ADPCM subId: %d\n%s", subId, StringUtil.getDump(subIdData));
            subIds.add(new SubIdChunk(subId, subIdData));
        }
    }

    /** */
    private enum SubId {
        /** 1byte, 0x01: 8kHz, 0x02: 16kHz, 0x03: 32kHz */
        MAX_SAMPLE(0x81),
        /** 1byte, 0x01: 8kHz, 0x02: 16kHz, 0x03: 32kHz */
        MAX_SAMPLE_CUE(0x89),
        /** 1byte, 0x01: 8kHz, 0x02: 16kHz, 0x03: 32kHz */
        MIN_SAMPLE(0x82),
        /** 1byte, 0x01: 8kHz, 0x02: 16kHz, 0x03: 32kHz */
        MIN_SAMPLE_CUE(0x8a),
        /** max kHz / 8 kHz */
        MAX_PARALLEL(0x83),
        /** max kHz / 8 kHz */
        MAX_PARALLEL_CUE(0x8b);
        final int value;
        SubId(int value) {
            this.value = value;
        }
    }

    /**
     * @param sample 8000, 16000, 32000 are available
     */
    public void setMaxSample(int sample) {
        subIds.add(new SubIdChunk(SubId.MAX_SAMPLE, sample / 8000));
    }

    /**
     * @param sample 8000, 16000, 32000 are available
     */
    public void setMaxSampleCue(int sample) {
        subIds.add(new SubIdChunk(SubId.MAX_SAMPLE_CUE, sample / 8000));
    }

    /**
     * @param sample 8000, 16000, 32000 are available
     */
    public void setMinSample(int sample) {
        subIds.add(new SubIdChunk(SubId.MIN_SAMPLE, sample / 8000));
    }

    /**
     * @param sample 8000, 16000, 32000 are available
     */
    public void setMinSampleCue(int sample) {
        subIds.add(new SubIdChunk(SubId.MIN_SAMPLE_CUE, sample / 8000));
    }

    /**
     * @param sample summed sampling rate
     */
    public void setMaxParallel(int sample) {
        subIds.add(new SubIdChunk(SubId.MAX_PARALLEL, sample / 8000));
    }

    /**
     * @param sample summed sampling rate
     */
    public void setMaxParallelCue(int sample) {
        subIds.add(new SubIdChunk(SubId.MAX_PARALLEL_CUE, sample / 8000));
    }

    /** */
    private List<SubIdChunk> subIds = new ArrayList<>();

    /** */
    private static class SubIdChunk {
        SubIdChunk(SubId id, int value) {
            this.id = id.value;
            this.data = new byte[] { (byte) value };
        }
        SubIdChunk(int id, byte[] data) {
            this.id = id;
            this.data = data;
        }
        /** */
        int id;
        /** */
        byte[] data;
    }

    /** */
    private int getSubIdsLength() {
        int length = 0;
        for (SubIdChunk subId : subIds) {
            length += (1 + 1 + subId.data.length);
        }
        return length;
    }

    /** */
    public byte[] getMessage()
        throws InvalidMfiDataException {

        byte[] tmp = new byte[2 + getSubIdsLength()];
        tmp[0] = (byte) (VENDOR_SHARP | CARRIER_DOCOMO);
        tmp[1] = (byte) 0x8f;
        int index = 2;
        for (SubIdChunk subId : subIds) {
            tmp[index++] = (byte) subId.id;
            tmp[index++] = (byte) subId.data.length;
            System.arraycopy(subId.data, 0, tmp, index, subId.data.length);
            index += subId.data.length;
        }
        return tmp;
    }
}
