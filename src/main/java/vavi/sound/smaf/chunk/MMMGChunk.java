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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.MetaMessage;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;


/**
 * MMMGChunk. (TODO long name)
 * <pre>
 * "MMMG"
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2024-12-14 nsano initial version <br>
 *          0.01 2026-09-11 nsano let the "VOIC" events out <br>
 */
public class MMMGChunk extends TrackChunk {

    private static final Logger logger = getLogger(MMMGChunk.class.getName());

    private static final String FOURCC = "MMMG";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key);
    }

    @Override
    public MMMGChunk init(byte[] id, int size) {
        super.init(id, size);
        formatType = FormatType.SEQU;
        return this;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        enigma = dis.readUnsignedShort();
        while (dis.available() > 0) {
//logger.log(Level.TRACE, "available: " + is.available() + ", " + available());
            Chunk chunk = readFrom(dis);
            chunks.add(chunk);
            if (chunk instanceof VoiceChunk vc) { // "VOIC"
                voiceChunk = vc;
            } else if (chunk instanceof SequenceDataChunk sdc) { // "SEQU"
                sequChunks.add(sdc);
            } else {
                logger.log(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }
    }

    /**
     * <pre>
     *  &lt;voice format&gt; 0x14 : 2 byte
     *  "VOIC", "SEQU" ...  : n byte
     * </pre>
     */
    @Override
    public void writeTo(OutputStream os) throws IOException {
        writeChunk(os, bos -> {
            DataOutputStream dos = new DataOutputStream(bos);

            dos.writeShort(enigma);

            for (Chunk chunk : chunks) {
                chunk.writeTo(dos);
            }
            dos.flush();
        });
    }

    // ----

    /**
     * {@code <voice format> 0x14}, the voice format being 1 for VMA (MA-1/MA-2) and
     * 2 for VM35 (MA-3/MA-5), which decides how "EXVO" is read.
     */
    private int enigma;

    private VoiceChunk voiceChunk;

    /** TODO multiple??? */
    private final List<SequenceDataChunk> sequChunks = new ArrayList<>();

    /**
     * The "SEQU" chunks, or one when there is none but a "VOIC" - the voices of a
     * "VOIC" have to go out even when this chunk carries no sequence of its own.
     */
    public int getTracks() {
        return Math.max(sequChunks.size(), voiceChunk != null ? 1 : 0);
    }

    /** adhoc */
    private int currentTrack;

    /** adhoc */
    public void setCurrentTrack(int currentTrack) {
        this.currentTrack = currentTrack;
    }

    @Override
    public List<SmafEvent> getSmafEvents() throws InvalidSmafDataException {
        List<SmafEvent> events = new ArrayList<>();

        //
        Map<String, Object> props = new HashMap<>();
        props.put("localType", ScoreTrackChunk.class);
        props.put("formatType", formatType);
        props.put("sequenceType", sequenceType);
        props.put("channelStatuses", channelStatuses);
        props.put("durationTimeBase", timeBaseTable[durationTimeBase]);
        props.put("gateTimeTimeBase", timeBaseTable[gateTimeTimeBase]);

        // internal use
        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MetaEvent.META_MACHINE_DEPEND.number(), props);
        events.add(new SmafEvent(metaMessage, 0L));

        // the "VOIC" voices belong to this whole chunk, not to one "SEQU", so they
        // go out once, ahead of the first track. Without this the "EXWV" wave and
        // the "EXVO" voices are parsed and then dropped, and a wave table voice
        // never reaches the synthesizer.
        if (voiceChunk != null && currentTrack == 0) {
            events.addAll(voiceChunk.getSmafEvents());
        }

        //
        if (!sequChunks.isEmpty()) {
            List<SmafMessage> messages = sequChunks.get(currentTrack).getSmafMessages(); // currentTrack ... adhoc
            for (SmafMessage message : messages) {
                events.add(new SmafEvent(message, 0L)); // TODO 0l
//logger.log(Level.TRACE, "SequenceDataChunk: " + message);
            }
        }

        return events;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getDC().format(getId() + " enigma: " + enigma));
        try (var dc = getDC().open()) {
            if (voiceChunk != null) sb.append(voiceChunk);
            sequChunks.forEach(sb::append);
        }

        return sb.toString();
    }
}
