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

    private static final String FOURCC = "VOIC";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key);
    }

    @Override
    public VoiceChunk init(byte[] id, int size) {
        return (VoiceChunk) super.init(id, size);
    }

    @Override
    public List<SmafEvent> getSmafEvents() throws InvalidSmafDataException {
        List<SmafEvent> events = new ArrayList<>();
        if (sequenceDataChunk != null) {
            events.addAll(sequenceDataChunk.getSmafMessages().stream().map(m -> new SmafEvent(m, m.getDuration())).toList());
        }
        if (exwvChunk != null) {
            events.add(new SmafEvent(exwvChunk.getSmafMessage(), 0));
        }
        if (!exclusiveVoiceChunks.isEmpty()) {
            events.addAll(exclusiveVoiceChunks.stream().map(c -> new SmafEvent(c.getSmafMessage(), 0)).toList());
        }
        return events;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        while (dis.available() > 0) {
//logger.log(Level.TRACE, "available: " + is.available() + ", " + available());
            Chunk chunk = readFrom(dis);
            switch (chunk) {
                case SequenceDataChunk subChunk -> this.sequenceDataChunk = subChunk; // "Mssq"
                case EXWVChunk subChunk -> this.exwvChunk = subChunk; // "EXWV"
                case ExclusiveVoiceChunk subChunk -> exclusiveVoiceChunks.add(subChunk); // "EXVC"
                default -> logger.log(Level.WARNING, "unknown chunk: " + chunk.getClass());
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
            if (exwvChunk != null) sb.append(exwvChunk);
            exclusiveVoiceChunks.stream().map(cs -> dc.format(cs.toString())).forEach(sb::append);
        }

        return sb.toString();
    }
}
