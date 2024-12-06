/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import vavi.sound.smaf.InvalidSmafDataException;

import static java.lang.System.getLogger;


/**
 * File Chunk.
 * <pre>
 * "MMMD"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class FileChunk extends Chunk {

    private static final Logger logger = getLogger(FileChunk.class.getName());

    /** */
    public FileChunk(byte[] id, int size) {
        super(id, size);
    }

    /** */
    public FileChunk() {
        System.arraycopy("MMMD".getBytes(), 0, id, 0, 4);
        this.size = 2; // crc
    }

    @Override
    protected void init(CrcDataInputStream dis, Chunk parent)
        throws InvalidSmafDataException, IOException {

        while (dis.available() > 2) {
            Chunk chunk = readFrom(dis);
            if (chunk instanceof ContentsInfoChunk) {
                contentsInfoChunk = chunk;
            } else if (chunk instanceof OptionalDataChunk) {
                optionalDataChunk = chunk;
            } else if (chunk instanceof ScoreTrackChunk) {
logger.log(Level.DEBUG, "TRACK: " + scoreTrackChunks.size());
                scoreTrackChunks.add((TrackChunk) chunk);
            } else if (chunk instanceof PcmAudioTrackChunk) {
                pcmAudioTrackChunks.add((TrackChunk) chunk);
            } else if (chunk instanceof GraphicsTrackChunk) {
                graphicsTrackChunks.add((TrackChunk) chunk);
            } else if (chunk instanceof MasterTrackChunk) {
                masterTrackChunk = (TrackChunk) chunk;
            } else {
logger.log(Level.WARNING, "unsupported chunk: " + chunk.getId());
            }
        }
//logger.log(Level.TRACE, "available: " + is.available());
        this.crc = dis.readUnsignedShort();
logger.log(Level.DEBUG, "crc (orig): %04x".formatted(crc));
        if (dis.available() > 4) {
            int kddiCrc = dis.readUnsignedShort();
            int kddiMark = dis.readUnsignedShort();
logger.log(Level.DEBUG, "has kddi crc: %04x, %04x".formatted(kddiCrc, kddiMark));
        }
    }

    @Override
    public void writeTo(OutputStream os) throws IOException {
        Crc16OutputStream cos = new Crc16OutputStream(os);

        DataOutputStream dos = new DataOutputStream(cos);

        dos.write(id);
        dos.writeInt(size);

        contentsInfoChunk.writeTo(cos);
        if (optionalDataChunk != null) {
            optionalDataChunk.writeTo(cos);
        }
        for (Chunk scoreTrackChunk : scoreTrackChunks) {
            scoreTrackChunk.writeTo(cos);
        }
        for (Chunk pcmAudioTrackChunk : pcmAudioTrackChunks) {
            pcmAudioTrackChunk.writeTo(cos);
        }
        for (Chunk graphicsTrackChunk : graphicsTrackChunks) {
            graphicsTrackChunk.writeTo(cos);
        }
        if (masterTrackChunk != null) {
            masterTrackChunk.writeTo(cos);
        }

        dos.writeShort(~cos.getCrc());
    }

    /** */
    private static class Crc16OutputStream extends FilterOutputStream {
        /** */
        private final CRC16 crc16 = new CRC16();
        /** */
        public Crc16OutputStream(OutputStream out) {
            super(out);
        }
        @Override
        public void write(int b) throws IOException {
            out.write(b);
            crc16.update((byte) b);
        }
        /** */
        public int getCrc() {
            return crc16.getValue();
        }
    }

    // ----

    /** */
    private Chunk contentsInfoChunk;

    /** */
    public ContentsInfoChunk getContentsInfoChunk() {
        return (ContentsInfoChunk) contentsInfoChunk;
    }

    /** "CNTI" (required) */
    public void setContentsInfoChunk(ContentsInfoChunk contentsInfoChunk) {
        if (this.contentsInfoChunk == null) {
            size += contentsInfoChunk.getSize() + 8;
        }
        this.contentsInfoChunk = contentsInfoChunk;
    }

    /** */
    private Chunk optionalDataChunk;

    /** */
    public OptionalDataChunk getOptionalDataChunk() {
        return (OptionalDataChunk) optionalDataChunk;
    }

    /** "OPDA" (option) */
    public void setOptionalDataChunk(OptionalDataChunk optionalDataChunk) {
        if (this.optionalDataChunk == null) {
            size += optionalDataChunk.getSize() + 8;
        }
        this.optionalDataChunk = optionalDataChunk;
    }

    /** "MTR*" */
    private final List<TrackChunk> scoreTrackChunks = new ArrayList<>();

    /** */
    public List<TrackChunk> getScoreTrackChunks() {
        return scoreTrackChunks;
    }

    /** */
    public void addScoreTrackChunk(ScoreTrackChunk scoreTrackChunk) {
        scoreTrackChunks.add(scoreTrackChunk);
        size += scoreTrackChunk.getSize() + 8;
    }

    /** "ATR*" */
    private final List<TrackChunk> pcmAudioTrackChunks = new ArrayList<>();

    /** */
    public List<TrackChunk> getPcmAudioTrackChunks() {
        return pcmAudioTrackChunks;
    }

    /** */
    public void addPcmAudioTrackChunk(PcmAudioTrackChunk pcmAudioTrackChunk) {
        pcmAudioTrackChunks.add(pcmAudioTrackChunk);
        size += pcmAudioTrackChunk.getSize() + 8;
    }

    /** "GTR*" */
    private final List<TrackChunk> graphicsTrackChunks = new ArrayList<>();

    /** */
    public List<TrackChunk> getGraphicsTrackChunks() {
        return graphicsTrackChunks;
    }

    /** */
    public void addGraphicsTrackChunk(GraphicsTrackChunk graphicsTrackChunk) {
        graphicsTrackChunks.add(graphicsTrackChunk);
        size += graphicsTrackChunk.getSize() + 8;
    }

    /** "MSTR" (option) */
    private TrackChunk masterTrackChunk;

    /** */
    public MasterTrackChunk getMasterTrackChunk() {
        return (MasterTrackChunk) masterTrackChunk;
    }

    /** */
    public void setMasterTrackChunk(MasterTrackChunk masterTrackChunk) {
        if (this.masterTrackChunk == null) {
            size += masterTrackChunk.getSize() + 8;
        }
        this.masterTrackChunk = masterTrackChunk;
    }

    /**
     * the remainder when the Byte columns of Chunk Header and Body are divided by the divisor shown below. (16 bit)
     */
    private int crc;

    /** CCITT X.25 */
    public int getCrc() {
        return crc;
    }
}
