/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;
import vavi.util.Debug;


/**
 * StreamPcmData Chunk.
 * <pre>
 * "Mtsp"
 * </pre>
 * <li>TODO same as WaveDataChunk
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050101 nsano initial version <br>
 */
public class StreamPcmDataChunk extends Chunk {

    /** */
    public StreamPcmDataChunk(byte[] id, int size) {
        super(id, size);
Debug.println("StreamPcmData: " + size);
    }

    /** */
    public StreamPcmDataChunk() {
        System.arraycopy("Mtsp".getBytes(), 0, id, 0, 4);
        this.size = 0;
    }

    /** */
    protected void init(InputStream is, Chunk parent) throws InvalidSmafDataException, IOException {
        while (available() > 0) {
            Chunk chunk = readFrom(is);
            if (chunk instanceof StreamWaveDataChunk) {
                streamWaveDataChunks.add(chunk);
            } else {
Debug.println("unknown chunk: " + chunk.getClass());
            }
        }
    }

    /** */
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size);

        for (Chunk streamWaveDataChunk : streamWaveDataChunks) {
            streamWaveDataChunk.writeTo(os);
        }
    }

    /** */
    private List<Chunk> streamWaveDataChunks = new ArrayList<Chunk>();

    /** "Mwa*" */
    public void addWaveDataChunk(Chunk streamWaveDataChunk) {
        streamWaveDataChunks.add(streamWaveDataChunk);
        size += streamWaveDataChunk.getSize() + 8;
    }

    /** */
    public List<SmafEvent> getSmafEvents() {
        List<SmafEvent> events = new ArrayList<SmafEvent>();

        for (Chunk streamWaveDataChunk : streamWaveDataChunks) {
            SmafMessage smafMessage = ((StreamWaveDataChunk) streamWaveDataChunk).toSmafMessage();
            events.add(new SmafEvent(smafMessage, 0l));
        }

        return events;
    }
}

/* */
