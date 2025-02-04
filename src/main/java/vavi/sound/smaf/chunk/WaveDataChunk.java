/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.message.WaveDataMessage;

import static java.lang.System.getLogger;


/**
 * WaveData Chunk.
 * <pre>
 * "Awa*" *: chunk number
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050101 nsano initial version <br>
 */
public class WaveDataChunk extends Chunk {

    private static final Logger logger = getLogger(WaveDataChunk.class.getName());

    /** */
    public WaveDataChunk(byte[] id, int size) {
        super(id, size);

        waveNumber = id[3] & 0xff;
logger.log(Level.DEBUG, "WaveData[" + waveNumber + "]: " + size + " bytes");
    }

    /** */
    public WaveDataChunk() {
        System.arraycopy("Awa".getBytes(), 0, id, 0, 3);
        size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        data = new byte[size];
        dis.readFully(data);
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        os.write(data);
    }

    /** */
    protected int waveNumber;

    /** */
    public int getWaveNumber() {
        return waveNumber;
    }

    /** */
    public void setWaveNumber(int waveNumber) {
        this.waveNumber = waveNumber;
        id[3] = (byte) waveNumber;
    }

    /** */
    protected byte[] data;

    /** */
    public byte[] getWaveData() {
        return data;
    }

    /** */
    public void setWaveData(byte[] data) {
        this.data = data;
        size = data.length;
    }

    /** */
    SmafMessage toSmafMessage(WaveType waveType) {
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
