/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.message.WaveDataMessage;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;


/**
 * EXWVChunk.
 * <pre>
 * "EXWV"
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-12-15 nsano initial version <br>
 */
public class EXWVChunk extends Chunk {

    private static final Logger logger = getLogger(EXWVChunk.class.getName());

    /** adpcm */
    byte[] data;

    private static final String FOURCC = "EXWV";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key);
    }

    @Override
    public EXWVChunk init(byte[] id, int size) {
        return (EXWVChunk) super.init(id, size);
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        data = new byte[size];
        dis.readFully(data);
logger.log(Level.DEBUG, FOURCC + ": " + size + "\n" + StringUtil.getDump(data, 16));
//Files.write(Path.of("tmp", "exwv.bin"), data, StandardOpenOption.CREATE_NEW);
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        dos.write(data);
    }

    public SmafMessage getSmafMessage() {
        WaveType waveType = new WaveType(1, 1, 8000, 4); // TODO
        WaveDataMessage waveDataMessage = new WaveDataMessage(
                -1, // TODO
                waveType.getWaveFormat(),
                data,
                waveType.getWaveSamplingFreq(),
                waveType.getWaveBaseBit(),
                waveType.getWaveChannels());

        return waveDataMessage;
    }

    @Override
    public String toString() {
        return getDC().format(getId() + " " + data.length + " bytes");
    }
}
