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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.LongMessage;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.NoteMessage;
import vavi.sound.mfi.ShortMessage;
import vavi.sound.mfi.SysexMessage;
import vavi.sound.mfi.Track;

import static java.lang.System.getLogger;
import static vavi.sound.mfi.vavi.VaviMfiFileFormat.DumpContext.getDC;


/**
 * TrackMessage.
 * <p>
 * {@link #getLength()} is total length of Track Chunk.
 * </p>
 * <li>TODO <code>extends {@link MfiMessage}</code> is really needed? is it TrackChunk, isn't it?
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030825 nsano initial version <br>
 *          0.01 030826 nsano refactoring <br>
 */
public class TrackMessage extends MfiMessage {

    private static final Logger logger = getLogger(TrackMessage.class.getName());

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

    /** */
    public TrackMessage(int trackNumber, Track track) {
        super(new byte[0]);

        this.trackNumber = trackNumber;
        this.track = track;
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
//logger.log(Level.TRACE, "track[" + trackNumber + "] event length sum: " + l + " / " + trackLength);
        }

        //
        this.length = trackLength + 4 + 4; // + type + length
    }

    /**
     * Reads a message from stream
     * TODO it seems like MfiMessage#readFrom, but the dependency with the vavi package is not good enough...
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
                    NoteMessageFactory.getMessage(delta, status, dis, noteLength);
        };
    }

    /**
     * Class or Normal.
     * @see "vavi.properties"
     * @param status 0x3f: Class A, 0x7f: Class B, 0xbf: Class C, 0xff: Normal
     * @param dis data1 ~
     * TODO it seems like MfiMessage#readFrom, but the dependency with the vavi package is not good enough...
     */
    private MfiMessage getClassOrNormalMessage(int delta, int status, DataInputStream dis) throws IOException {

        int data1 = dis.readUnsignedByte();    // extended status

        MfiMessage message;
        if (data1 >= 0x00 && data1 <= 0x7f) {
            // Extended Status A ... LongMessage
            message = LongMessageFactory.getMessage(delta, status, data1, dis, exst);
        } else if (data1 >= 0x80 && data1 <= 0xef) {
            // Extended Status B ... ShortMessage
            message = ShortMessageFactory.getMessage(delta, status, data1, dis);
        } else {
            // Extended Information 0xf# ... SysexMessage
            message = SysexMessageFactory.getMessage(delta, status, data1, dis);
        }

        return message;
    }

    /** @throws UnsupportedOperationException no mean */
    @Override
    public byte[] getMessage() {
        throw new UnsupportedOperationException("no mean");
    }

    // ----

    /** note */
    private static class NoteMessageFactory {

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
         * TODO seems should be located at NoteMessage#readFrom, but it makes relationship with vavi package wrong...
         */
        public static MfiMessage getMessage(int delta,
                                            int status,
                                            DataInputStream dis,
                                            int noteLength)
            throws IOException {

            if (noteLength == 1) {
                int data1 = dis.readUnsignedByte();
                int data2 = dis.readUnsignedByte();

                try {
                    return noteMessageConstructor2.newInstance(delta, status, data1, data2);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                int data1 = dis.readUnsignedByte();

                try {
                    return noteMessageConstructor1.newInstance(delta, status, data1);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }

        /** A constructor that creates {@link NoteMessage} object instance. 1 */
        private static Constructor<NoteMessage> noteMessageConstructor1;

        /** A constructor that creates {@link NoteMessage} object instance. 2 */
        private static Constructor<NoteMessage> noteMessageConstructor2;

        static {
            try {
                // props
                Properties props = new Properties();

                final String path = "vavi.properties";
                props.load(TrackMessage.class.getResourceAsStream(path));

                //
                String key = "mfi.track.note";
                if (props.containsKey(key)) {
                    @SuppressWarnings("unchecked")
                    Class<NoteMessage> clazz = (Class<NoteMessage>) Class.forName(props.getProperty(key));
//logger.log(Level.TRACE, "note class: " + StringUtil.getClassName(clazz));
                    noteMessageConstructor1 = clazz.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE);
                    noteMessageConstructor2 = clazz.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                }
            } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                throw new IllegalStateException(e);
            }
        }
    }

    /** sysex */
    private static class SysexMessageFactory {

        /**
         * Extended Information 0xf# ~ 0xf#
         * <pre>
         *  length = dis.readShort();
         *  data ...
         * </pre>
         * @param dis data2 ~
         * TODO it seems like SysexMessage#readFrom, but the dependency with the vavi package is not good enough...
         */
        public static MfiMessage getMessage(int delta,
                                            int status,
                                            int data1,
                                            DataInputStream dis) throws IOException {
//logger.log(Level.TRACE, "delta: " + StringUtil.toHex2(delta));

            String key = "mfi.track.%d.%c.%d".formatted(status, 'e', data1);

            if (sysexMessageInstantiators.containsKey(key)) {
                Method method = sysexMessageInstantiators.get(key);

                try {
                    return (SysexMessage) method.invoke(null, delta, status, data1, dis);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                // TODO clean up
                int length = dis.readUnsignedShort();
                byte[] data2 = new byte[length + 2];
                data2[0] = (byte) ((length / 0x100) & 0xff);
                data2[1] = (byte) ((length % 0x100) & 0xff);
                dis.readFully(data2, 2, length);

logger.log(Level.WARNING, "sysex unhandled: delta: %02x, status: %02x, extended status: %02x".formatted(delta, status, data1));
                return UnknownMessageFactory.getMessage(delta, status, data1, data2);
            }
        }

        /** methods for creates Extended Status A {@link SysexMessage} objects instance */
        private static final Map<String, Method> sysexMessageInstantiators = new HashMap<>();

        static {
            try {
                // props
                Properties props = new Properties();

                final String path = "vavi.properties";
                props.load(TrackMessage.class.getResourceAsStream(path));

                //
                for (Object o : props.keySet()) {
                    String key = (String) o;
                    if (key.matches("mfi\\.track\\.\\d+\\.e\\.\\d+")) {
                        Class<?> clazz = Class.forName(props.getProperty(key));
//logger.log(Level.TRACE, "sysex class: " + StringUtil.getClassName(clazz));
                        Method method = clazz.getMethod("readFrom", Integer.TYPE, Integer.TYPE, Integer.TYPE, InputStream.class);

                        sysexMessageInstantiators.put(key, method);
                    }
                }
            } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                throw new IllegalStateException(e);
            }
        }
    }

    /** short */
    private static class ShortMessageFactory {

        /**
         * Extended Status B (0x80 ~ 0xef)
         * <pre>
         *  length 1 fixed
         *  data 1 byte
         * </pre>
         * @param dis data2 ~
         * TODO it seems like ShortMessage#readFrom, but the dependency with the vavi package is not good enough...
         */
        public static MfiMessage getMessage(int delta,
                                            int status,
                                            int data1,
                                            DataInputStream dis) throws IOException {
            //
            int data2 = dis.readUnsignedByte();

            //
            String key = "mfi.track.%d.%c.%d".formatted(status, 'b', data1);

            Constructor<? extends MfiMessage> constructor;
            if (shortMessageConstructors.containsKey(key)) {
                constructor = shortMessageConstructors.get(key);
            } else {
logger.log(Level.WARNING, "short unhandled: delta: %02x, status: %02x, extended status: %02x".formatted(delta, status, data1));
                return UnknownMessageFactory.getMessage(delta, status, data1, data2);
            }

            try {
                return constructor.newInstance(delta, status, data1, data2);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /** constructors for Extended Status B {@link ShortMessage} object instance */
        private static final Map<String, Constructor<ShortMessage>> shortMessageConstructors = new HashMap<>();

        static {
            try {
                // props
                Properties props = new Properties();

                final String path = "vavi.properties";
                props.load(TrackMessage.class.getResourceAsStream(path));

                //
                for (Object o : props.keySet()) {
                    String key = (String) o;
                    if (key.matches("mfi\\.track\\.\\d+\\.b\\.\\d+")) {
                        @SuppressWarnings("unchecked")
                        Class<ShortMessage> shortMessageClass = (Class<ShortMessage>) Class.forName(props.getProperty(key));
//logger.log(Level.TRACE, "short class: " + StringUtil.getClassName(shortMessageClass));
                        Constructor<ShortMessage> constructor = shortMessageClass.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);

                        shortMessageConstructors.put(key, constructor);
                    }
                }

            } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                throw new IllegalStateException(e);
            }
        }
    }

    /** long */
    private static class LongMessageFactory {

        /**
         * Extended Status A (0x00 ~ 0x7f)
         * <pre>
         *  length is 'exst'
         *  data ...
         * </pre>
         * @param dis data2 ~
         * @see #exst
         * TODO it seems like LongMessage#readFrom, but the dependency with the vavi package is not good enough...
         */
        public static MfiMessage getMessage(int delta,
                                            int status,
                                            int data1,
                                            DataInputStream dis,
                                            int exst) throws IOException {
            //
            byte[] data2 = new byte[1 + exst];
            dis.readFully(data2, 0, 1 + exst);

            //
            String key = "mfi.track.%d.%c.%d".formatted(status, 'a', data1);

            Constructor<? extends MfiMessage> constructor;
            if (longMessageConstructors.containsKey(key)) {
                constructor = longMessageConstructors.get(key);
            } else {
logger.log(Level.WARNING, "long unhandled: delta: %02x, status: %02x, extended status: %02x".formatted(delta, status, data1));
                return UnknownMessageFactory.getMessage(delta, status, data1, data2);
            }

            try {
                return constructor.newInstance(delta, status, data1, data2);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /** constructors for creating Extended Information {@link LongMessage} object instance */
        private static final Map<String, Constructor<LongMessage>> longMessageConstructors = new HashMap<>();

        static {
            try {
                // props
                Properties props = new Properties();

                final String path = "vavi.properties";
                props.load(TrackMessage.class.getResourceAsStream(path));

                //
                for (Object o : props.keySet()) {
                    String key = (String) o;
                    if (key.matches("mfi\\.track\\.\\d+\\.a\\.\\d+")) {
                        @SuppressWarnings("unchecked")
                        Class<LongMessage> longMessageClass = (Class<LongMessage>) Class.forName(props.getProperty(key));
//logger.log(Level.TRACE, "long class: " + StringUtil.getClassName(longMessageClass));
                        Constructor<LongMessage> constructor = longMessageClass.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, byte[].class);

                        longMessageConstructors.put(key, constructor);
                    }
                }
            } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                throw new IllegalStateException(e);
            }
        }
    }

    /** unknown */
    private static class UnknownMessageFactory {

        /** */
        public static MfiMessage getMessage(int delta,
                                            int status,
                                            int data1,
                                            int data2) {
            try {
                return unknownMessageConstructor1.newInstance(delta, status, data1, data2);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /** */
        public static MfiMessage getMessage(int delta,
                                            int status,
                                            int data1,
                                            byte[] data2) {
            try {
                return unknownMessageConstructor2.newInstance(delta, status, data1, data2);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        /** unknown 1 */
        private static Constructor<MfiMessage> unknownMessageConstructor1;

        /** unknown 2 */
        private static Constructor<MfiMessage> unknownMessageConstructor2;

        static {
            try {
                // props
                Properties props = new Properties();

                final String path = "vavi.properties";
                props.load(TrackMessage.class.getResourceAsStream(path));

                // unknown
                String key = "mfi.track.unknown";
                if (props.containsKey(key)) {
                    @SuppressWarnings("unchecked")
                    Class<MfiMessage> clazz = (Class<MfiMessage>) Class.forName(props.getProperty(key));
//logger.log(Level.TRACE, "unknown class: " + StringUtil.getClassName(clazz));
                    unknownMessageConstructor1 = clazz.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                    unknownMessageConstructor2 = clazz.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, byte[].class);
                }

            } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                throw new IllegalStateException(e);
            }
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
}
