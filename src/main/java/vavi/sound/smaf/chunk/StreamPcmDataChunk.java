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
import java.util.ArrayList;
import java.util.List;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;

import static java.lang.System.getLogger;


/**
 * StreamPcmData Chunk.
 * <pre>
 * "Mtsp"
 * </pre>
 * <li>TODO same as WaveDataChunk
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050101 nsano initial version <br>
 */
public class StreamPcmDataChunk extends Chunk {

    private static final Logger logger = getLogger(StreamPcmDataChunk.class.getName());

    /** */
    public StreamPcmDataChunk(byte[] id, int size) {
        super(id, size);
logger.log(Level.DEBUG, "StreamPcmData: " + size);
    }

    /** */
    public StreamPcmDataChunk() {
        System.arraycopy("Mtsp".getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        while (dis.available() > 0) {
            Chunk chunk = readFrom(dis);
            if (chunk instanceof StreamWaveDataChunk) {
                streamWaveDataChunks.add(chunk);
            } else {
logger.log(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        for (Chunk streamWaveDataChunk : streamWaveDataChunks) {
            streamWaveDataChunk.writeTo(os);
        }
    }

    /** */
    private final List<Chunk> streamWaveDataChunks = new ArrayList<>();

    /** "Mwa*" */
    public void addWaveDataChunk(Chunk streamWaveDataChunk) {
        streamWaveDataChunks.add(streamWaveDataChunk);
        size += streamWaveDataChunk.getSize() + 8;
    }

    /** */
    public List<SmafMessage> getSmafMessages() {
        List<SmafMessage> messages = new ArrayList<>();

        for (Chunk streamWaveDataChunk : streamWaveDataChunks) {
            SmafMessage smafMessage = ((StreamWaveDataChunk) streamWaveDataChunk).toSmafMessage();
            messages.add(smafMessage);
        }

        return messages;
    }
}
