/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.message.WaveDataMessage;
import vavi.util.Debug;


/**
 * Stream WaveData Chunk.
 * <pre>
 * "Mwa*" *: chunk number
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050101 nsano initial version <br>
 */
public class StreamWaveDataChunk extends WaveDataChunk {

    /** */
    public StreamWaveDataChunk(byte[] id, int size) {
        super(id, size);
Debug.println(Level.FINE, "StreamWaveData: " + size);
    }

    /** */
    public StreamWaveDataChunk() {
        System.arraycopy("Mwa".getBytes(), 0, id, 0, 3);
        size = 0;
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        byte[] weveTypeBytes = new byte[3];
        dis.readFully(weveTypeBytes);
        this.waveType = new WaveType(weveTypeBytes);

        data = new byte[dis.available()];
        dis.readFully(data);
    }

    /** */
    private WaveType waveType;

    /** */
    SmafMessage toSmafMessage() {
        int waveNumber = this.getWaveNumber();
        byte[] waveData = this.getWaveData();
        WaveDataMessage waveDataMessage = new WaveDataMessage(
              waveNumber,
              waveType.getWaveFormat(),
              waveData,
              waveType.getWaveSamplingFreq(),
              waveType.getWaveBaseBit(),
              waveType.getWaveChannels());

        return waveDataMessage;
    }
}

/* */
