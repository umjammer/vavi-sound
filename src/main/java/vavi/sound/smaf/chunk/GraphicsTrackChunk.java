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

import static java.lang.System.getLogger;
import static vavi.sound.smaf.chunk.Chunk.DumpContext.getDC;


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
 *  Option Data : size specified in Option Size (0 ~ 255b)
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class GraphicsTrackChunk extends TrackChunk {

    private static final Logger logger = getLogger(GraphicsTrackChunk.class.getName());

    private static final String FOURCC = "GTR";

    @Override
    protected boolean accept(String key) {
        return FOURCC.equals(key.substring(0, 3));
    }

    @Override
    public GraphicsTrackChunk init(byte[] id, int size) {
        super.init(id, size);
logger.log(Level.DEBUG, "Graphics[" + trackNumber + "]: " + size);
        return this;
    }

    /** */
    public GraphicsTrackChunk() {
        System.arraycopy(FOURCC.getBytes(), 0, id, 0, 3);
        this.size = 5;
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        this.formatType = FormatType.values()[dis.readUnsignedByte()];
logger.log(Level.DEBUG, "formatType: " + formatType);

        this.playerType = dis.readUnsignedByte();
        this.textEncodeType = dis.readUnsignedByte();
        this.colorType = dis.readUnsignedByte();
        this.durationTimeBase = dis.readUnsignedByte();

        int optionSize = dis.readUnsignedByte();
        this.optionData = new byte[optionSize];
        dis.readFully(optionData);

        while (dis.available() > 0) {
            Chunk chunk = readFrom(dis);
            chunks.add(chunk);
            if (chunk instanceof GraphicsSetupDataChunk) {
                setupDataChunk = chunk;
            } else if (chunk instanceof GraphicsTrackSequenceDataChunk) {
                sequenceDataChunks.add(chunk);
            } else if (chunk instanceof FontDataChunk) {
                fontDataChunk = chunk;
            } else if (chunk instanceof ImageDataChunk) {
                imageDataChunk = chunk;
            } else {
logger.log(Level.WARNING, "unknown chunk: " + chunk.getClass());
            }
        }
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        writeChunk(os, bos -> {
            DataOutputStream dos = new DataOutputStream(bos);

            dos.writeByte(formatType.ordinal());
            dos.writeByte(playerType);
            dos.writeByte(textEncodeType);
            dos.writeByte(colorType);
            dos.writeByte(durationTimeBase);
            dos.writeByte(optionData.length);
            dos.write(optionData);

            for (Chunk chunk : chunks) {
                chunk.writeTo(dos);
            }
            dos.flush();
        });
    }

    // header

    /**
     * 0x00 Handy Phone Standard
     * 0x01 ~ 0xFF Reserved
     */
    private int playerType;
    /** */
    private int textEncodeType;
    /**
     * 0x00 Direct RGB:=3:3:2
     * 0x01 Index Color
     * 0x02 ~ FF Reserved
     */
    private int colorType;

    /** */
    private byte[] optionData = new byte[0];

    // ----

    /** */
    private Chunk setupDataChunk;

    /** "Gtsu" (required) */
    public void setSetupDataChunk(SetupDataChunk setupDataChunk) {
        if (this.setupDataChunk == null) {
            size += setupDataChunk.getSize() + 8;
        }
        replaceChunk(this.setupDataChunk, setupDataChunk);
        this.setupDataChunk = setupDataChunk;
        setupDataChunk.id[0] = 'G';
    }

    /** > 1 */
    private final List<Chunk> sequenceDataChunks = new ArrayList<>();

    /** (option) */
    private Chunk fontDataChunk;
    /** (option) */
    private Chunk imageDataChunk;

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

        // internal use
        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MetaEvent.META_MACHINE_DEPEND.number(), props);
        events.add(new SmafEvent(metaMessage, 0L));

        return events;
    }

    /** "Gftd" */
    public static class FontDataChunk extends Chunk {

        private static final String FOURCC = "Gftd";

        @Override
        protected boolean accept(String key) {
            return FOURCC.equals(key);
        }

        // "Ge**” ：Font Chunk
        // "Gu**” ：Unicode Font Chunk
        @Override
        protected void init(CrcDataInputStream dis, Chunk parent)
            throws InvalidSmafDataException, IOException {
            // TODO the fonts are not parsed yet, keep them as they are so they can be written back
            this.data = new byte[size];
            dis.readFully(data);
        }

        /** the font body, not parsed yet */
        private byte[] data = new byte[0];

        /** the font body, not parsed yet */
        public byte[] getData() {
            return data;
        }

        @Override
        public void writeTo(OutputStream os) throws IOException {
            writeChunk(os, bos -> bos.write(data));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(getDC().format(getId() + " " + formatType + ", " + playerType + ", durationTimeBase: " + durationTimeBase + ", gateTimeTimeBase: " + gateTimeTimeBase));
        try (var dc = getDC().open()) {
            if (setupDataChunk != null) sb.append(setupDataChunk);
            if (fontDataChunk != null) sb.append(fontDataChunk);
            if (imageDataChunk != null) sb.append(imageDataChunk);
            for (var sequenceDataChunk : sequenceDataChunks) sb.append(sequenceDataChunk);
        }

        return sb.toString();
    }
}
