/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.MetaMessage;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;
import vavi.util.Debug;


/**
 * ScoreTrack Chunk.
 * <pre>
 * "MTR*"
 *
 *  Format Type              : 1 byte (必須)
 *  Sequence Type            : 1 byte (必須)
 *  TimeBase_D               : 1 byte (必須)
 *  TimeBase_G               : 1 byte (必須)
 *  Channel Status           : n byte (必須)(Format Type に依存)
 *  Seek &amp; Phrase Info Chunk : n byte (Option)
 *  Setup Data Chunk         : n byte (Option)
 *  Sequence Data Chunk      : n byte (必須)
 *  Stream PCM Data Chunk    : n byte (Option) (Format Type = &quot;Mobile Standard&quot; の場合のみ)
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class ScoreTrackChunk extends TrackChunk {

    /** */
    public ScoreTrackChunk(byte[] id, int size) {
        super(id, size);
Debug.println("ScoreTrack[" + trackNumber + "]: " + size + " bytes");
    }

    /** */
    public ScoreTrackChunk() {
        System.arraycopy("MTR".getBytes(), 0, id, 0, 3);
        this.size = 4;
    }

    /** */
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        this.formatType = FormatType.values()[dis.readUnsignedByte()];
        this.sequenceType = SequenceType.values()[dis.readUnsignedByte()];
Debug.println("sequenceType: " + sequenceType);
        this.durationTimeBase = dis.readUnsignedByte();
Debug.println("durationTimeBase: " + durationTimeBase + ", " + getDurationTimeBase() + " ms");
        this.gateTimeTimeBase = dis.readUnsignedByte();
Debug.println("gateTimeTimeBase: " + gateTimeTimeBase + ", " + getGateTimeTimeBase() + " ms");

        switch (formatType) {
        case HandyPhoneStandard: {
            byte[] buffer = new byte[2];
            dis.readFully(buffer);
//Debug.println(StringUtil.getDump(channelStatus));
            this.channelStatuses = new ChannelStatus[4];
            for (int i = 0; i < 4; i++) {
                channelStatuses[i] = new ChannelStatus(i, (byte) ((buffer[i / 2] & (0xf0 >> (4 * (i % 2)))) >> (4 * ((i + 1) % 2))));
//Debug.println(channelStatuses[i]);
            }
          } break;
        case MobileStandard_Compress:
        case MobileStandard_NoCompress: {
            byte[] buffer = new byte[16];
            dis.readFully(buffer);
            this.channelStatuses = new ChannelStatus[16];
            for (int i = 0; i < 16; i++) {
                channelStatuses[i] = new ChannelStatus(i, (int) buffer[i]);
//Debug.println(channelStatuses[i]);
            }
          } break;
        case Unknown3: {
            byte[] buffer = new byte[32];
            dis.readFully(buffer);
            // TODO implement
          } break;
        }
Debug.println("formatType: " + formatType);

        while (dis.available() > 0) {
//Debug.println("available: " + is.available() + ", " + available());
            Chunk chunk = readFrom(dis);
            if (chunk instanceof SeekAndPhraseInfoChunk) {
                seekAndPhraseInfoChunk = chunk;
            } else if (chunk instanceof SequenceDataChunk) {
                sequenceDataChunk = chunk;
            } else if (chunk instanceof SetupDataChunk) {
                setupDataChunk = chunk;
            } else if (chunk instanceof StreamPcmDataChunk) {
                streamPcmDataChunk = chunk;
            } else {
Debug.println(Level.WARNING, "unsupported chunk: " + chunk.getClass());
            }
        }
    }

    /** */
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size + formatType.size);

        dos.writeByte(formatType.ordinal());
        dos.writeByte(sequenceType.ordinal());
        dos.writeByte(durationTimeBase);
        dos.writeByte(gateTimeTimeBase);
        for (ChannelStatus channelStatus : channelStatuses) {
            channelStatus.writeTo(os);
        }
        if (seekAndPhraseInfoChunk != null) {
            seekAndPhraseInfoChunk.writeTo(os);
        }
        if (sequenceDataChunk != null) {
            sequenceDataChunk.writeTo(os);
        }
        if (setupDataChunk != null) {
            setupDataChunk.writeTo(os);
        }
        if (streamPcmDataChunk != null) {
            streamPcmDataChunk.writeTo(os);
        }
    }

    /** */
    private ChannelStatus[] channelStatuses;

    /** */
    private Chunk seekAndPhraseInfoChunk;

    /** "MspI" (option) */
    public void setSeekAndPhraseInfoChunk(SeekAndPhraseInfoChunk seekAndPhraseInfoChunk) {
        if (this.seekAndPhraseInfoChunk == null) {
            size += seekAndPhraseInfoChunk.getSize() + 8;
        }
        this.seekAndPhraseInfoChunk = seekAndPhraseInfoChunk;
        seekAndPhraseInfoChunk.id[0] = 'M';
    }

    /** */
    private Chunk setupDataChunk;

    /** "Mtsu" (option) */
    public void setSetupDataChunk(SetupDataChunk setupDataChunk) {
        if (this.setupDataChunk == null) {
            size += setupDataChunk.getSize() + 8;
        }
        this.setupDataChunk = setupDataChunk;
        setupDataChunk.id[0] = 'M';
    }

    /** */
    private Chunk streamPcmDataChunk;

    /** "Mtsp" (option) */
    public void setStreamPcmDataChunk(StreamPcmDataChunk streamPcmDataChunk) {
        if (this.streamPcmDataChunk == null) {
            size += streamPcmDataChunk.getSize() + 8;
        }
        this.streamPcmDataChunk = streamPcmDataChunk;
    }

    /* */
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
        if (setupDataChunk != null) {
            List<SmafMessage> messages = ((SetupDataChunk) setupDataChunk).getSmafMessages();
            for (SmafMessage message : messages) {
                events.add(new SmafEvent(message, 0L));
//Debug.println("SetupDataChunk: " + message);
            }
        }

        //
        if (streamPcmDataChunk != null) {
            List<SmafMessage> messages = ((StreamPcmDataChunk) streamPcmDataChunk).getSmafMessages();
            for (SmafMessage message : messages) {
                events.add(new SmafEvent(message, 0L)); // TODO 0l
//Debug.println("StreamPcmDataChunk: " + message);
            }
        }

        //
        List<SmafMessage> messages = ((SequenceDataChunk) sequenceDataChunk).getSmafMessages();
        for (SmafMessage message : messages) {
            events.add(new SmafEvent(message, 0L)); // TODO 0l
//Debug.println("SequenceDataChunk: " + message);
        }

        return events;
    }
}

/* */
