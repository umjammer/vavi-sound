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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.MetaMessage;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;

import static java.lang.System.getLogger;


/**
 * PcmAudioTrack Chunk.
 * <pre>
 * "ATR*"
 *
 *      "Atsq" > 1
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class PcmAudioTrackChunk extends TrackChunk {

    private static final Logger logger = getLogger(PcmAudioTrackChunk.class.getName());

    /** */
    public PcmAudioTrackChunk(byte[] id, int size) {
        super(id, size);
logger.log(Level.DEBUG, "PcmAudioTrack[" + trackNumber + "]: " + size + " bytes");
    }

    /** */
    public PcmAudioTrackChunk() {
        System.arraycopy("ATR".getBytes(), 0, id, 0, 3);
        this.size = 6;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {
//skip(is, size);

        this.formatType = FormatType.values()[dis.readUnsignedByte()];
logger.log(Level.DEBUG, "formatType: " + formatType);
        this.sequenceType = SequenceType.values()[dis.readUnsignedByte()];
logger.log(Level.DEBUG, "sequenceType: " + sequenceType);

        this.waveType = new WaveType(dis.readUnsignedShort());
logger.log(Level.DEBUG, "waveType: " + waveType);

        this.durationTimeBase = dis.readUnsignedByte();
logger.log(Level.DEBUG, "durationTimeBase: " + durationTimeBase + ", " + getDurationTimeBase() + " ms");
        this.gateTimeTimeBase = dis.readUnsignedByte();
logger.log(Level.DEBUG, "gateTimeTimeBase: " + gateTimeTimeBase + ", " + getGateTimeTimeBase() + " ms");

        while (dis.available() > 0) {
            Chunk chunk = readFrom(dis);
            if (chunk instanceof SeekAndPhraseInfoChunk) {
                seekAndPhraseInfoChunk = chunk;
            } else if (chunk instanceof AudioSequenceDataChunk) {
                sequenceDataChunk = chunk;
            } else if (chunk instanceof SetupDataChunk) {
                setupDataChunk = chunk;
            } else if (chunk instanceof WaveDataChunk) {
                waveDataChunks.add(chunk);
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

        dos.writeByte(formatType.ordinal());
        dos.writeByte(sequenceType.ordinal());
        dos.writeShort(waveType.intValue());
        dos.writeByte(durationTimeBase);
        dos.writeByte(gateTimeTimeBase);
        if (seekAndPhraseInfoChunk != null) {
            seekAndPhraseInfoChunk.writeTo(os);
        }
        if (sequenceDataChunk != null) {
            sequenceDataChunk.writeTo(os);
        }
        if (setupDataChunk != null) {
            setupDataChunk.writeTo(os);
        }
        for (Chunk waveDataChunk : waveDataChunks) {
            waveDataChunk.writeTo(os);
        }
    }

    /** */
    private WaveType waveType;

    /**
     * @param waveType the waveType to set
     */
    public void setWaveType(WaveType waveType) {
        this.waveType = waveType;
    }

    /** */
    private Chunk seekAndPhraseInfoChunk;

    /** "AspI" (option) */
    public void setSeekAndPhraseInfoChunk(SeekAndPhraseInfoChunk seekAndPhraseInfoChunk) {
        if (this.seekAndPhraseInfoChunk == null) {
            size += seekAndPhraseInfoChunk.getSize() + 8;
        }
        this.seekAndPhraseInfoChunk = seekAndPhraseInfoChunk;
        seekAndPhraseInfoChunk.id[0] = 'A';
    }

    /** */
    private Chunk setupDataChunk;

    /** "Atsu" (option) */
    public void setSetupDataChunk(SetupDataChunk setupDataChunk) {
        if (this.setupDataChunk == null) {
            size += setupDataChunk.getSize() + 8;
        }
        this.setupDataChunk = setupDataChunk;
    }

    /** */
    private final List<Chunk> waveDataChunks = new ArrayList<>();

    /** "Awa*" TODO is there really more than one? */
    public void addWaveDataChunk(Chunk waveDataChunk) {
        waveDataChunks.add(waveDataChunk);
        size += waveDataChunk.getSize() + 8;
    }

    @Override
    public List<SmafEvent> getSmafEvents() throws InvalidSmafDataException {
        List<SmafEvent> events = new ArrayList<>();

        //
        Map<String, Object> props = new HashMap<>();
        props.put("localType", PcmAudioTrackChunk.class);
        props.put("formatType", formatType);
        props.put("sequenceType", sequenceType);
        props.put("durationTimeBase", timeBaseTable[durationTimeBase]);
        props.put("gateTimeTimeBase", timeBaseTable[gateTimeTimeBase]);

        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MetaEvent.META_MACHINE_DEPEND.number(), props);
        events.add(new SmafEvent(metaMessage, 0L));

        //
        for (Chunk waveDataChunk : waveDataChunks) {
            SmafMessage smafMessage = ((WaveDataChunk) waveDataChunk).toSmafMessage(waveType);
            events.add(new SmafEvent(smafMessage, 0L));
        }

        //
        List<SmafMessage> messages = ((SequenceDataChunk) sequenceDataChunk).getSmafMessages();
        for (SmafMessage message : messages) {
            events.add(new SmafEvent(message, 0L));
        }

        return events;
    }
}
