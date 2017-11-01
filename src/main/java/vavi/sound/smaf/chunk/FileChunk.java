/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.chunk;

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * File Chunk.
 * <pre>
 * "MMMD"
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class FileChunk extends Chunk {

    /** */
    public FileChunk(byte[] id, int size) {
        super(id, size);
    }

    /** */
    public FileChunk() {
        System.arraycopy("MMMD".getBytes(), 0, id, 0, 4);
        this.size = 2; // crc
    }

    /** */
    protected void init(InputStream is, Chunk parent)
        throws InvalidSmafDataException, IOException {

        while (available() > 2) {
            Chunk chunk = readFrom(is);
            if (chunk instanceof ContentsInfoChunk) {
                contentsInfoChunk = chunk;
            } else if (chunk instanceof OptionalDataChunk) {
                optionalDataChunk = chunk;
            } else if (chunk instanceof ScoreTrackChunk) {
Debug.println("TRACK: " + scoreTrackChunks.size());
                scoreTrackChunks.add((TrackChunk) chunk);
            } else if (chunk instanceof PcmAudioTrackChunk) {
                pcmAudioTrackChunks.add((TrackChunk) chunk);
            } else if (chunk instanceof GraphicsTrackChunk) {
                graphicsTrackChunks.add((TrackChunk) chunk);
            } else if (chunk instanceof MasterTrackChunk) {
                masterTrackChunk = (TrackChunk) chunk;
            } else {
Debug.println(Level.WARNING, "unsupported chunk: " + chunk.getClass());
            }
        }
//Debug.println("available: " + is.available());
        this.crc = readShort(is);
Debug.println("crc (orig): " + StringUtil.toHex4(crc));
        if (is.available() > 4) {
            int kddiCrc = readShort(is);
            int kddiMark = readShort(is);
Debug.println("has kddi crc: " + StringUtil.toHex4(kddiCrc) + ", " + StringUtil.toHex4(kddiMark));
        }
    }

    /** */
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
    private class Crc16OutputStream extends FilterOutputStream {
        /** */
        private CRC16 crc16 = new CRC16();
        /** */
        public Crc16OutputStream(OutputStream out) {
            super(out);
        }
        /** */
        public void write(int b) throws IOException {
            out.write(b);
            crc16.update((byte) b);
        }
        /** */
        public int getCrc() {
            return (int) crc16.getValue();
        }
    }

    //----

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
    private List<TrackChunk> scoreTrackChunks = new ArrayList<>();
    
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
    private List<TrackChunk> pcmAudioTrackChunks = new ArrayList<>();

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
    private List<TrackChunk> graphicsTrackChunks = new ArrayList<>();

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
     * Chunk Header 及び Body の Byte 列に対し、下記に示す割数で割り算した余り。(16 bit)
     */
    private int crc;

    /** CCITT X.25 */
    public int getCrc() {
        return crc;
    }
}

/* */
