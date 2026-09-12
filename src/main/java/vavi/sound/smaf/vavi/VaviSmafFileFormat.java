/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;

import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.Sequence;
import vavi.sound.smaf.SmafFileFormat;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.Track;
import vavi.sound.smaf.vavi.chunk.Chunk;
import vavi.sound.smaf.vavi.chunk.FileChunk;
import vavi.sound.smaf.vavi.message.TrackMessage;


/**
 * File Chunk.
 * <pre>
 * "MMMD"
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class VaviSmafFileFormat extends SmafFileFormat {

    private static final Logger logger = System.getLogger(VaviSmafFileFormat.class.getName());

    /** {@value} */
    public static final String TYPE = "MMMD";

    /**
     * @see "vavi/sound/midi/package.html"
     */
    public static final int FILE_TYPE = 0x84;

    /** */
    public VaviSmafFileFormat(int byteLength) {
        super(FILE_TYPE, byteLength);
    }

    // ----

    /** SMAF Sequence */
    private Sequence sequence;

    /**
     * @param sequence SMAF Sequence
     */
    VaviSmafFileFormat(Sequence sequence) {
        super(FILE_TYPE, 0); // TODO byteLength
        this.sequence = sequence;
    }

    /**
     * TODO byteLength
     *
     * @return SMAF Sequence
     */
    Sequence getSequence() {
        return sequence;
    }

    /** factory */
    static VaviSmafFileFormat readFrom(InputStream is) throws InvalidSmafDataException, IOException {
        try {
            Chunk chunk = Chunk.readFrom(is, null);
            if (chunk instanceof FileChunk fileChunk) {
                VaviSmafFileFormat sff = new VaviSmafFileFormat(fileChunk.getSize());
                sff.sequence = new SmafSequence(fileChunk);
                return sff;
            } else {
                throw new InvalidSmafDataException("stream is not smaf: first chunk: " + chunk.getId());
            }
        } catch (IOException | InvalidSmafDataException e) {
            throw e;
        } catch (Exception e) {
logger.log(Level.DEBUG, e.getMessage(), e);
            throw new InvalidSmafDataException(e);
        }
    }

    /**
     * Writes to the stream. Set the sequence in advance.
     * @after out has been {@link java.io.OutputStream#flush() flush}-ed
     * @throws IllegalStateException throws if no sequence is set
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

        out.flush(); // TODO is needed?

        byteLength = smafDataLength + 4 + 4;
    }

    /** TODO */
    public static boolean isIgnored(SmafMessage message) {
        return false;
    }
}
