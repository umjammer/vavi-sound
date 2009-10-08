/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import vavi.sound.smaf.chunk.Chunk;
import vavi.sound.smaf.chunk.FileChunk;
import vavi.sound.smaf.message.TrackMessage;


/**
 * File Chunk.
 * <pre>
 * "MMMD"
 * </pre>
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class SmafFileFormat {

    /** {@value} */
    public static final String TYPE = "MMMD";

    /**
     * @see "vavi/sound/midi/package.html"
     */
    public static final int FILE_TYPE = 0x84;

    /** */
    public SmafFileFormat(int byteLength) {
        this.byteLength = byteLength;
    }

    /** */
    private int byteLength;

    /**
     * @return byte length
     */
    public int getByteLength() {
        return byteLength;
    }

    //----
    
    /** SMAF Sequence */
    private Sequence sequence;

    /**
     * @param sequence SMAF Sequence
     */
    SmafFileFormat(Sequence sequence) {
        this.sequence = sequence;
    }

    /**
     * @return SMAF Sequence
     */
    Sequence getSequence() {
        return sequence;
    }

    /** */
    static SmafFileFormat readFrom(InputStream is) throws InvalidSmafDataException, IOException {
        Chunk chunk = Chunk.readFrom(is, null, true);
        if (FileChunk.class.isInstance(chunk)) {
            FileChunk fileChunk = FileChunk.class.cast(chunk);
            SmafFileFormat sff = new SmafFileFormat(fileChunk.getSize());
            sff.sequence = new SmafSequence(fileChunk);
            return sff;
        } else {
            throw new InvalidSmafDataException("stream is not smaf");
        }
    }

    /**
     * ストリームに書き込みます。事前にシーケンスを設定しておくこと。
     * @after out は {@link java.io.OutputStream#flush() flush} されます
     * @throws IllegalStateException シーケンスが設定されていない場合スローされます
     */
    void writeTo(OutputStream out)
        throws InvalidSmafDataException, IOException {

        if (sequence == null) {
            throw new IllegalStateException("no sequence");
        }

        int smafDataLength = 0;
        Track[] tracks = sequence.getTracks();
        for (int t = 0; t < tracks.length; t++) {
            TrackMessage track = new TrackMessage(t, tracks[t]);
            smafDataLength += track.getDataLength();
        }

        DataOutputStream dos = new DataOutputStream(out);

        //
        dos.writeBytes(TYPE);
        dos.writeInt(smafDataLength);

        // 3. tracks
        for (int t = 0; t < tracks.length; t++) {
            TrackMessage track = new TrackMessage(t, tracks[t]);
            track.writeTo(out);
        }

        out.flush(); // TODO いる？
    }

    /** TODO */
    public static boolean isIgnored(SmafMessage message) {
        return false;
    }
    
    //----
    
    /**
     * load only
     * @param args 0: input mmf
     */
    public static void main(String[] args) throws Exception {
        SmafSystem.getSmafFileFormat(new BufferedInputStream(new FileInputStream(args[0])));
        System.exit(0);
    }
}

/* */
