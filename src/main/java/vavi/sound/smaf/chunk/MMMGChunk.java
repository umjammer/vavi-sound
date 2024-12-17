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
 */
public class MMMGChunk extends TrackChunk {

    private static final Logger logger = getLogger(MMMGChunk.class.getName());

    public MMMGChunk(byte[] id, int size) {
        super(id, size);
        formatType = FormatType.SEQU;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent) throws InvalidSmafDataException, IOException {
        enigma = dis.readUnsignedShort();
        while (dis.available() > 0) {
//logger.log(Level.TRACE, "available: " + is.available() + ", " + available());
            Chunk chunk = readFrom(dis);
            if (chunk instanceof VoiceChunk vc) { // "VOIC"
                voiceChunk = vc;
            } else if (chunk instanceof SequenceDataChunk sdc) { // "SEQU"
                sequChunks.add(sdc);
            } else {
                logger.log(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {

    }

    // ----

    private int enigma;

    private VoiceChunk voiceChunk;

    /** TODO multiple??? */
    private final List<SequenceDataChunk> sequChunks = new ArrayList<>();

    public int getTracks() {
        return sequChunks.size();
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

        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MetaEvent.META_MACHINE_DEPEND.number(), props);
        events.add(new SmafEvent(metaMessage, 0L));

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

        sb.append(super.toString());
        try (var dc = getDC().open()) {
            if (voiceChunk != null) sb.append(voiceChunk);
            sequChunks.forEach(sb::append);
        }

        return sb.toString();
    }
}
