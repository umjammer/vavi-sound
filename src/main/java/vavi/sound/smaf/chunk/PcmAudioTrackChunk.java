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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import vavi.sound.midi.MidiConstants;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.MetaMessage;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;
import vavi.util.Debug;


/**
 * PcmAudioTrack Chunk.
 * <pre>
 * "ATR*"
 * 
 *      "Atsq" > 1
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class PcmAudioTrackChunk extends TrackChunk {

    /** */
    public PcmAudioTrackChunk(byte[] id, int size) {
        super(id, size);
Debug.println("PcmAudioTrack[" + trackNumber + "]: " + size + " bytes");
    }

    /** */
    public PcmAudioTrackChunk() {
        System.arraycopy("ATR".getBytes(), 0, id, 0, 3);
        this.size = 6;
    }

    /** */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {
//skip(is, size);

        this.formatType = FormatType.values()[read(is)];
Debug.println("formatType: " + formatType);
        this.sequenceType = SequenceType.values()[read(is)];
Debug.println("sequenceType: " + sequenceType);

        this.waveType = new WaveType(readShort(is));
Debug.println("waveType: " + waveType);

        this.durationTimeBase = read(is);
Debug.println("durationTimeBase: " + durationTimeBase + ", " + getDurationTimeBase() + " ms");
        this.gateTimeTimeBase = read(is);
Debug.println("gateTimeTimeBase: " + gateTimeTimeBase + ", " + getGateTimeTimeBase() + " ms");

        while (available() > 0) {
            Chunk chunk = readFrom(is);
            if (chunk instanceof SeekAndPhraseInfoChunk) {
                seekAndPhraseInfoChunk = chunk;
            } else if (chunk instanceof AudioSequenceDataChunk) {
                sequenceDataChunk = chunk;
            } else if (chunk instanceof SetupDataChunk) {
                setupDataChunk = chunk;
            } else if (chunk instanceof WaveDataChunk) {
                waveDataChunks.add(chunk);
            } else {
Debug.println(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }
    }

    /** */
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
    private List<Chunk> waveDataChunks = new ArrayList<Chunk>();

    /** "Awa*" TODO ホンマに複数か？ */
    public void addWaveDataChunk(Chunk waveDataChunk) {
        waveDataChunks.add(waveDataChunk);
        size += waveDataChunk.getSize() + 8;
    }

    /* */
    @Override
    public List<SmafEvent> getSmafEvents() throws InvalidSmafDataException {
        List<SmafEvent> events = new ArrayList<SmafEvent>();

        //
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("localType", PcmAudioTrackChunk.class);
        props.put("formatType", formatType);
        props.put("sequenceType", sequenceType);
        props.put("durationTimeBase", timeBaseTable[durationTimeBase]);
        props.put("gateTimeTimeBase", timeBaseTable[gateTimeTimeBase]);

        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MidiConstants.META_MACHINE_DEPEND, props);
        events.add(new SmafEvent(metaMessage, 0l));

        //
        for (Chunk waveDataChunk : waveDataChunks) {
            SmafMessage smafMessage = ((WaveDataChunk) waveDataChunk).toSmafMessage(waveType);
            events.add(new SmafEvent(smafMessage, 0l));
        }

        //
        List<SmafMessage> messages = ((SequenceDataChunk) sequenceDataChunk).getSmafMessages();
        for (SmafMessage message : messages) {
            events.add(new SmafEvent(message, 0l));
        }

        return events;
    }
}

/* */
