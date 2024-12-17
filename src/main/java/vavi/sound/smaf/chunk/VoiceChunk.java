/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.chunk.ExclusiveVoiceChunk.ExclusiveType;

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;


/**
 * VoiceChunk.
 * <pre>
 * "VOIC"
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-12-14 nsano initial version <br>
 */
public class VoiceChunk extends TrackChunk {

    private static final Logger logger = getLogger(VoiceChunk.class.getName());

    enum VoiceType {
        FM,
        PCM,
        AL
    }

    static class VMAVoicePC {}
    static class VM35VoicePC {}

    static class Exclusive {
        boolean variableLength;
        ExclusiveType type;
        VoiceType voiceType;
        VMAVoicePC vmaVoicePC;
        VM35VoicePC vm35VoicePC;
        byte[] Data;
    }

    public VoiceChunk(byte[] id, int size) {
        super(id, size);
    }

    @Override
    public List<SmafEvent> getSmafEvents() throws InvalidSmafDataException {
        return List.of();
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        while (dis.available() > 0) {
//logger.log(Level.TRACE, "available: " + is.available() + ", " + available());
            Chunk chunk = readFrom(dis);
            if (chunk instanceof SequenceDataChunk) { // "Mssq"
                sequenceDataChunk = chunk;
            } else if (chunk instanceof EXWVChunk ewvc) { // "EXWV"
                exwvChunk = ewvc;
            } else if (chunk instanceof ExclusiveVoiceChunk evc) { // "EXVC"
                exclusiveVoiceChunks.add(evc);
            } else {
logger.log(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {

    }

    // ----

    private EXWVChunk exwvChunk;

    private final List<ExclusiveVoiceChunk> exclusiveVoiceChunks = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(super.toString());
        try (var dc = getDC().open()) {
            if (exwvChunk != null) sb.append(dc.format(exwvChunk.toString()));
            exclusiveVoiceChunks.stream().map(cs -> dc.format(cs.toString())).forEach(sb::append);
        }

        return sb.toString();
    }
}
