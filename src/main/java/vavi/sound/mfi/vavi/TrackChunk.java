/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ServiceLoader;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.LongMessage;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.SysexMessage;
import vavi.sound.mfi.Track;
import vavi.sound.mfi.vavi.TrackMessage.SysexTrackMessage;
import vavi.sound.mfi.vavi.track.UndefinedMessage;

import static java.lang.System.getLogger;
import static vavi.sound.mfi.vavi.VaviMfiFileFormat.DumpContext.getDC;


/**
 * TrackMessage.
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030825 nsano initial version <br>
 *          0.01 030826 nsano refactoring <br>
 */
public class TrackChunk {

    private static final Logger logger = getLogger(TrackChunk.class.getName());

    /** */
    public static final String TYPE = "trac";

    /** */
    private final Track track;

    /** */
    private final int trackNumber;

    /** for reading */
    private int noteLength = -1;

    /** for writing */
    private int exst = -1;

    /** total length */
    private int length;

    /** */
    public TrackChunk(int trackNumber, Track track) {
        this.trackNumber = trackNumber;
        this.track = track;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    /** */
    public void setNoteLength(int noteLength) {
        this.noteLength = noteLength;
logger.log(Level.DEBUG, "noteLength: " + noteLength);
    }

    /** */
    public void setExst(int exst) {
        this.exst = exst;
logger.log(Level.DEBUG, "exst: " + exst);
    }

    /**
     * Unnecessary data in {@link Track}[0] will be removed.
     * @see VaviMfiFileFormat#isIgnored(MfiMessage) dependencies may be lacking
     */
    public void writeTo(OutputStream os) throws IOException {

        DataOutputStream dos = new DataOutputStream(os);

        dos.writeBytes(TYPE);
        dos.writeInt(getDataLength());
logger.log(Level.DEBUG, "track: " + trackNumber + ": " + getDataLength());
        for (int j = 0; j < track.size(); j++) {
            MfiEvent event = track.get(j);
            MfiMessage message = event.getMessage();
            if (!VaviMfiFileFormat.isIgnored(message)) {
                byte[] data = message.getMessage();
                dos.write(data, 0, data.length);
            }
        }
    }

    /**
     * for writing
     * Unnecessary data in {@link Track}[0] will be removed.
     * @see VaviMfiFileFormat#isIgnored(MfiMessage) dependencies may be lacking
     */
    public int getDataLength() {
        int trackLength = 0;

        for (int j = 0; j < track.size(); j++) {
try {
            MfiEvent event = track.get(j);
            MfiMessage message = event.getMessage();
            if (!VaviMfiFileFormat.isIgnored(message)) {
                trackLength += message.getLength();
            }
} catch (RuntimeException e) {
 logger.log(Level.ERROR, e.getMessage(), e);
 logger.log(Level.ERROR, "j: " + j + ", track.size: " + track.size() + ", " + track.get(j));
 throw e;
}
        }

        return trackLength;
    }

    /**
     * @before {@link #noteLength}, {@link #exst} are must be set
     * @after {@link #length} will be set as Track Chunk length
     * @throws IllegalStateException {@link vavi.sound.mfi.vavi.header.NoteMessage} length or
     *         {@link #exst} is not set
     * @throws InvalidMfiDataException at the beginning of <code>is</code> is not {@link #TYPE}
     */
    public void readFrom(InputStream is) throws InvalidMfiDataException, IOException {
//logger.log(Level.TRACE, "\n" + StringUtil.getDump(is, 512));

        if (noteLength == -1 || exst == -1) {
            throw new IllegalStateException("noteLength and exst must be set.");
        }

        DataInputStream dis = new DataInputStream(is);

        // type
        byte[] bytes = new byte[4];
        dis.readFully(bytes, 0, 4);
        String string = new String(bytes);
        if (!TYPE.equals(string)) {
//logger.log(Level.TRACE, "dump:\n" + StringUtil.getDump(is, 64));
            throw new InvalidMfiDataException("invalid track: " + string);
        }

        // length
        int trackLength = dis.readInt();
logger.log(Level.DEBUG, "trackLength[" + trackNumber + "]: " + trackLength);

        // events
        int l = 0;
        while (l < trackLength) {
            MfiMessage message = getMessage(dis);
            track.add(new MfiEvent(message, 0L));

            l += message.getLength();
//logger.log(Level.TRACE, "track[" + trackNumber + "] event: " + message.getClass().getSimpleName() + ", length: " + message.getLength());
//logger.log(Level.TRACE, "track[" + trackNumber + "] event length sum: " + l + " / " + trackLength + ", available: " + is.available());
        }

        //
        this.length = trackLength + 4 + 4; // + type + length
    }

    /**
     * Reads a message from stream
     */
    private MfiMessage getMessage(DataInputStream dis)
        throws IOException {

        int delta  = dis.readUnsignedByte();
        int status = dis.readUnsignedByte();

        return switch (status) {
            case MfiMessage.STATUS_CLASS_A,   // Class A (0x3f)
                 MfiMessage.STATUS_CLASS_B,   // Class B (0x7f)
                 MfiMessage.STATUS_CLASS_C,   // Class C (0xbf)
                 MfiMessage.STATUS_NORMAL ->  // Normal (0xff)
                    getClassOrNormalMessage(delta, status, dis);
            default ->                        // note message
                    getNoteMessage(delta, status, dis, noteLength);
        };
    }

    /**
     * Class or Normal.
     * @param status 0x3f: Class A, 0x7f: Class B, 0xbf: Class C, 0xff: Normal
     * @param dis data1 ~
     */
    private MfiMessage getClassOrNormalMessage(int delta, int status, DataInputStream dis) throws IOException {

        int data1 = dis.readUnsignedByte();    // extended status

        MfiMessage message;
        if (data1 >= 0x00 && data1 <= 0x7f) {
            // Extended Status A ... LongMessage
            message = getLongMessage(delta, status, data1, dis, exst);
        } else if (data1 >= 0x80 && data1 <= 0xef) {
            // Extended Status B ... ShortMessage
            message = getShortMessage(delta, status, data1, dis);
        } else {
            // Extended Information 0xf# ... SysexMessage
            message = getSysexMessage(delta, status, data1, dis);
        }

        return message;
    }

    // ----

    /**
     * Note.
     * @see #noteLength
     * @param status <pre>
     * 0x00 ~ 0x3e
     * 0x40 ~ 0x7e
     * 0x80 ~ 0xbe
     * 0xc0 ~ 0xfe
     * </pre>
     * @param dis data1 ~
     */
    private static MfiMessage getNoteMessage(int delta,
                                             int status,
                                             DataInputStream dis,
                                             int noteLength)
        throws IOException {

        if (noteLength == 1) {
            int data1 = dis.readUnsignedByte();
            int data2 = dis.readUnsignedByte();

            return new VaviNoteMessage(delta, status, data1, data2);
        } else {
            int data1 = dis.readUnsignedByte();

            return new VaviNoteMessage(delta, status, data1);
        }
    }

    /**
     * Extended Information 0xf# ~ 0xf#
     * <pre>
     *  length = dis.readShort();
     *  data ...
     * </pre>
     * @param dis data2 ~
     */
    private static MfiMessage getSysexMessage(int delta,
                                              int status,
                                              int data1,
                                              DataInputStream dis) throws IOException {
//logger.log(Level.TRACE, "delta: " + StringUtil.toHex2(delta));

        String key = "%d.e.%d".formatted(status, data1);

        SysexMessage sysexMessage = (SysexMessage) factory(key);
        if (sysexMessage != null) {
            return (MfiMessage) ((SysexTrackMessage) sysexMessage).init(delta, status, data1, dis);
        } else {
            int length = dis.readUnsignedShort();
            byte[] data2 = new byte[length + 2];
            data2[0] = (byte) ((length / 0x100) & 0xff);
            data2[1] = (byte) ((length % 0x100) & 0xff);
            dis.readFully(data2, 2, length);

logger.log(Level.WARNING, "sysex unhandled: delta: %02x, status: %02x, extended status: %02x".formatted(delta, status, data1));
            return new UndefinedMessage().init(delta, status, data1, data2);
        }
    }

    /**
     * Extended Status B (0x80 ~ 0xef)
     * <pre>
     *  length 1 fixed
     *  data 1 byte
     * </pre>
     * @param dis data2 ~
     */
    private static MfiMessage getShortMessage(int delta,
                                              int status,
                                              int data1,
                                              DataInputStream dis) throws IOException {
        //
        int data2 = dis.readUnsignedByte();

        //
        String key = "%d.b.%d".formatted(status, data1);

        ShortMessage shortMessage = (ShortMessage) factory(key);
        if (shortMessage != null) {
            return shortMessage.init(delta, status, data1, data2);
        } else {
logger.log(Level.WARNING, "short unhandled: delta: %02x, status: %02x, extended status: %02x".formatted(delta, status, data1));
            return new UndefinedMessage().init(delta, status, data1, data2);
        }
    }

    /**
     * Extended Status A (0x00 ~ 0x7f)
     * <pre>
     *  length is 'exst'
     *  data ...
     * </pre>
     * @param dis data2 ~
     * @see #exst
     */
    private static MfiMessage getLongMessage(int delta,
                                             int status,
                                             int data1,
                                             DataInputStream dis,
                                             int exst) throws IOException {
        //
        byte[] data2 = new byte[1 + exst];
        dis.readFully(data2, 0, 1 + exst);

        //
        String key = "%d.a.%d".formatted(status, data1);

        LongMessage longMessage = (LongMessage) factory(key);
        if (longMessage != null) {
            return longMessage.init(delta, status, data1, data2);
        } else {
logger.log(Level.WARNING, "long unhandled: delta: %02x, status: %02x, extended status: %02x".formatted(delta, status, data1));
            return new UndefinedMessage().init(delta, status, data1, data2);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TYPE).append("\n");
        try (var dc = getDC().open()) {
            track.stream()
                    .filter(e -> !(e.getMessage() instanceof SubMessage))
                    .filter(e -> !(e.getMessage() instanceof AudioDataMessage))
                    .forEach(e -> sb.append(dc.format(e.getMessage().toString())));
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }

    /** */
    private static TrackMessage factory(String key) {
        for (TrackMessage message : ServiceLoader.load(TrackMessage.class)) {
            if (message.accept(key)) {
                return message;
            }
        }
logger.log(Level.WARNING, "no matched track message for: " + key);
        return null;
    }
}
