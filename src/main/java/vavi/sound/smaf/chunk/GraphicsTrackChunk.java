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
import vavi.util.Debug;


/**
 * GraphicsTrack Chunk.
 * <pre>
 * "GTR*"
 *
 *  Format Type : 1byte
 *  Player Type : 1byte
 *  Text Encode Type : 1byte
 *  Color Type : 1byte
 *  TimeBase : 1byte
 *  Option Size : 1byte
 *  Option Data : Option Size で指定したサイズ(0〜255b)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class GraphicsTrackChunk extends TrackChunk {

    /** */
    public GraphicsTrackChunk(byte[] id, int size) {
        super(id, size);
Debug.println(Level.FINE, "Graphics[" + trackNumber + "]: " + size);
    }

    /** */
    public GraphicsTrackChunk() {
        System.arraycopy("GTR".getBytes(), 0, id, 0, 3);
        this.size = 5;
    }

    @Override
    protected void init(MyDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {
//skip(is, size);

        this.formatType = FormatType.values()[dis.readUnsignedByte()];

        this.playerType = dis.readUnsignedByte();
        this.textEncodeType = dis.readUnsignedByte();
        this.colorType = dis.readUnsignedByte();
        this.durationTimeBase = dis.readUnsignedByte();

        int optionSize = dis.readUnsignedByte();
        this.optionData = new byte[optionSize];
        dis.readFully(optionData);

        while (dis.available() > 0) {
            Chunk chunk = readFrom(dis);
            if (chunk instanceof GraphicsSetupDataChunk) {
                setupDataChunk = chunk;
            } else if (chunk instanceof GraphicsTrackSequenceDataChunk) {
                sequenceDataChunks.add(chunk);
            } else if (chunk instanceof FontDataChunk) {
                fontDataChunk = chunk;
            } else if (chunk instanceof ImageDataChunk) {
                imageDataChunk = chunk;
            } else {
Debug.println(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.write(id);
        dos.writeInt(size); // TODO add option, other chunks size

        setupDataChunk.writeTo(os);
        for (Chunk sequenceDataChunk : sequenceDataChunks) {
            sequenceDataChunk.writeTo(os);
        }
        if (fontDataChunk != null) {
            fontDataChunk.writeTo(os);
        }
        if (imageDataChunk != null) {
            imageDataChunk.writeTo(os);
        }
    }

    // header

    /**
     * 0x00 Handy Phone Standard
     * 0x01〜0xFF Reserved
     */
    private int playerType;
    /** */
    private int textEncodeType;
    /**
     * 0x00 Direct RGB:=3:3:2
     * 0x01 Index Color
     * 0x02〜FF Reserved
     */
    private int colorType;

    /** */
    private byte[] optionData;

    // ----

    /** */
    private Chunk setupDataChunk;

    /** "Gtsu" (required) */
    public void setSetupDataChunk(SetupDataChunk setupDataChunk) {
        if (this.setupDataChunk == null) {
            size += setupDataChunk.getSize() + 8;
        }
        this.setupDataChunk = setupDataChunk;
        setupDataChunk.id[0] = 'G';
    }

    /** > 1 */
    private List<Chunk> sequenceDataChunks = new ArrayList<>();

    /** (option) */
    private Chunk fontDataChunk;
    /** (option) */
    private Chunk imageDataChunk;

    /* */
    @Override
    public List<SmafEvent> getSmafEvents() throws InvalidSmafDataException {
        List<SmafEvent> events = new ArrayList<>();

        //
        Map<String, Object> props = new HashMap<>();
        props.put("localType", GraphicsTrackChunk.class);
        props.put("formatType", formatType);
        props.put("playerType", playerType);
        props.put("textEncodeType", textEncodeType);
        props.put("colorType", colorType);
        props.put("timeBase", durationTimeBase);

        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MetaEvent.META_MACHINE_DEPEND.number(), props);
        events.add(new SmafEvent(metaMessage, 0L));

        return null; // TODO
    }

    /** "Gftd" */
    public static class FontDataChunk extends Chunk {
        // "Ge**” ：Font Chunk
        // "Gu**” ：Unicode Font Chunk
        @Override
        protected void init(MyDataInputStream dis, Chunk parent)
            throws InvalidSmafDataException, IOException {
dis.skipBytes((int) (long) size); // TODO
        }

        /** TODO */
        @Override
        public void writeTo(OutputStream os) throws IOException {
        }
    }
}

/* */
