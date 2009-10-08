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

import vavi.sound.midi.MidiConstants;
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
 *  Option Data : Option Size Ç≈éwíËÇµÇΩÉTÉCÉY(0Å`255b)
 * </pre>
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class GraphicsTrackChunk extends TrackChunk {

    /** */
    public GraphicsTrackChunk(byte[] id, int size) {
        super(id, size);
Debug.println("Graphics[" + trackNumber + "]: " + size);
    }

    /** */
    public GraphicsTrackChunk() {
        System.arraycopy("GTR".getBytes(), 0, id, 0, 3);
        this.size = 5;
    }

    /** */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {
//skip(is, size);

        this.formatType = FormatType.values()[read(is)];

        this.playerType = read(is);
        this.textEncodeType = read(is);
        this.colorType = read(is);
        this.durationTimeBase = read(is);

        int optionSize = read(is);
        this.optionData = new byte[optionSize];
        read(is, optionData);

        while (available() > 0) {
            Chunk chunk = readFrom(is);
            if (chunk instanceof GraphicsSetupDataChunk) {
                setupDataChunk = chunk;
            } else if (chunk instanceof GraphicsTrackSequenceDataChunk) {
                sequenceDataChunks.add(chunk);
            } else if (chunk instanceof FontDataChunk) {
                fontDataChunk = chunk;
            } else if (chunk instanceof ImageDataChunk) {
                imageDataChunk = chunk;
            } else {
Debug.println("unknown chunk: " + chunk.getClass());
            }
        }
    }

    /** */
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
     * 0x01Å`0xFF Reserved
     */
    private int playerType;
    /** */
    private int textEncodeType;
    /**
     * 0x00 Direct RGB:=3:3:2
     * 0x01 Index Color
     * 0x02Å`FF Reserved
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
    private List<Chunk> sequenceDataChunks = new ArrayList<Chunk>(); 

    /** (option) */
    private Chunk fontDataChunk;
    /** (option) */
    private Chunk imageDataChunk;

    /* */
    @Override
    public List<SmafEvent> getSmafEvents() throws InvalidSmafDataException {
        List<SmafEvent> events = new ArrayList<SmafEvent>();

        //
        Map<String, Object> props = new HashMap<String, Object>();
        props.put("localType", GraphicsTrackChunk.class);
        props.put("formatType", formatType);
        props.put("playerType", playerType);
        props.put("textEncodeType", textEncodeType);
        props.put("colorType", colorType);
        props.put("timeBase", durationTimeBase);

        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MidiConstants.META_MACHINE_DEPEND, props);
        events.add(new SmafEvent(metaMessage, 0l));

        return null; // TODO
    }

    /** "Gftd" */
    public static class FontDataChunk extends Chunk {
        // "Ge**Åh ÅFFont Chunk
        // "Gu**Åh ÅFUnicode Font Chunk
        /** */
        protected void init(InputStream is, Chunk parent)
            throws InvalidSmafDataException, IOException {
skip(is, size); // TODO
        }

        /** TODO */
        public void writeTo(OutputStream os) throws IOException {
        }
    }
}

/* */
