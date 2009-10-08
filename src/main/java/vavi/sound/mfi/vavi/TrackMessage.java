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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
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
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * TrackMessage.
 * <p>
 * {@link #getLength()} �� Track Chunk ���ׂĂ̒���
 * </p>
 * <li>TODO <code>extends {@link MfiMessage}</code> ����́H TrackChunk ����Ȃ��́H
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030825 nsano initial version <br>
 *          0.01 030826 nsano refactoring <br>
 */
public class TrackMessage extends MfiMessage {

    /** */
    public static final String TYPE = "trac";

    /** */
    private Track track;

    /** */
    private int trackNumber;

    /** �ǂݍ��ݗp */
    private int noteLength = -1;

    /** �ǂݍ��ݗp */
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
Debug.println("noteLength: " + noteLength);
    }

    /** */
    public void setExst(int exst) {
        this.exst = exst;
Debug.println("exst: " + exst);
    }

    /**
     * {@link Track}[0] �̕s�K�v�ȃf�[�^�͏Ȃ���܂��B
     * @see VaviMfiFileFormat#isIgnored(MfiMessage) �ˑ��֌W�����܂�������
     */
    public void writeTo(OutputStream os) throws IOException {

        DataOutputStream dos = new DataOutputStream(os);

        dos.writeBytes(TYPE);
        dos.writeInt(getDataLength());
Debug.println("track: " + trackNumber + ": " + getDataLength());
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
     * �����o���p
     * {@link Track}[0] �̕s�K�v�ȃf�[�^�͏Ȃ���܂��B
     * @see VaviMfiFileFormat#isIgnored(MfiMessage) �ˑ��֌W�����܂�������
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
 Debug.printStackTrace(e);
 Debug.println("j: " + j + ", track.size: " + track.size() + ", " + track.get(j));
 throw e;
}
        }

        return trackLength;
    }

    /**
     * @before {@link #noteLength}, {@link #exst} ���ݒ肳��Ă��邱��
     * @after {@link #length} ���ݒ肳��� Track Chunk �̒���
     * @throws IllegalStateException {@link vavi.sound.mfi.vavi.header.NoteMessage} �̒���
     *         �������� {@link #exst} ���ݒ肳��Ă��Ȃ��ꍇ
     * @throws InvalidMfiDataException is �̎n�܂肪 {@link #TYPE} �Ŏn�܂��Ă��Ȃ��ꍇ
     */
    public void readFrom(InputStream is)
        throws InvalidMfiDataException,
               IOException {
//Debug.dump(is, 512);

        if (noteLength == -1 || exst == -1) {
            throw new IllegalStateException("noteLength and exst must be set.");
        }

        DataInputStream dis = new DataInputStream(is);

        // type
        byte[] bytes = new byte[4];
        dis.readFully(bytes, 0, 4);
        String string = new String(bytes);
        if (!TYPE.equals(string)) {
//Debug.println("dump:\n" + StringUtil.getDump(is, 64));
            throw new InvalidMfiDataException("invalid track: " + string);
        }

        // length
        int trackLength = dis.readInt();
Debug.println("trackLength[" + trackNumber + "]: " + trackLength);

        // events
        int l = 0;
        while (l < trackLength) {
            MfiMessage message = getMessage(dis);
            track.add(new MfiEvent(message, 0l));

            l += message.getLength();
//Debug.println("track[" + trackNumber + "] event length sum: " + l + " / " + trackLlength);
        }

        //
        this.length = trackLength + 4 + 4; // + type + length
    }

    /**
     * Reads a message from stream
     * TODO MfiMessage#readFrom �̂悤�ȋC������̂��� vavi �p�b�P�[�W�Ƃ̈ˑ��֌W�����܂�����... 
     */
    private MfiMessage getMessage(DataInputStream dis)
        throws IOException {

        int delta  = dis.readUnsignedByte();
        int status = dis.readUnsignedByte();

        switch (status) {
        case MfiMessage.STATUS_CLASS_A: // Class A (0x3f)
        case MfiMessage.STATUS_CLASS_B: // Class B (0x7f)
        case MfiMessage.STATUS_CLASS_C: // Class C (0xbf)
        case MfiMessage.STATUS_NORMAL:  // Normal (0xff)
            return getClassOrNormalMessage(delta, status, dis);
        default:                        // �m�[�g���b�Z�[�W
            return NoteMessageFactory.getMessage(delta, status, dis, noteLength);
        }
    }

    /**
     * Class or Normal.
     * @see "vavi.properties"
     * @param status 0x3f: Class A, 0x7f: Class B, 0xbf: Class C, 0xff: Normal
     * @param dis data1 ~
     * TODO MfiMessage#readFrom �̂悤�ȋC������̂��� vavi �p�b�P�[�W�Ƃ̈ˑ��֌W�����܂�����... 
     */
    private MfiMessage getClassOrNormalMessage(int delta,
                                               int status,
                                               DataInputStream dis)
        throws IOException {

        int data1 = dis.readUnsignedByte();    // �g���X�e�[�^�X

        MfiMessage message = null;
        if (data1 >= 0x00 && data1 <= 0x7f) {
            // �g���X�e�[�^�X A ... LongMessage
            message = LongMessageFactory.getMessage(delta, status, data1, dis, exst);
        } else if (data1 >= 0x80 && data1 <= 0xef) {
            // �g���X�e�[�^�X B ... ShortMessage
            message = ShortMessageFactory.getMessage(delta, status, data1, dis);
        } else {
            // �g����� 0xf# ... SysexMessage
            message = SysexMessageFactory.getMessage(delta, status, data1, dis);
        }

        return message;
    }

    /** @throws UnsupportedOperationException */
    public byte[] getMessage() {
        throw new UnsupportedOperationException("no mean");
    }

    //----

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
         * TODO NoteMessage#readFrom �̂悤�ȋC������̂��� vavi �p�b�P�[�W�Ƃ̈ˑ��֌W�����܂�����... 
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
                    throw (RuntimeException) new IllegalStateException().initCause(e);
                }
            } else {
                int data1 = dis.readUnsignedByte();

                try {
                    return noteMessageConstructor1.newInstance(delta, status, data1);
                } catch (Exception e) {
                    throw (RuntimeException) new IllegalStateException().initCause(e);
                }
            }
        }

        /** �m�[�g {@link NoteMessage} �I�u�W�F�N�g�̃C���X�^���X���擾����R���X�g���N�^ 1 */
        private static Constructor<NoteMessage> noteMessageConstructor1;

        /** �m�[�g {@link NoteMessage} �I�u�W�F�N�g�̃C���X�^���X���擾����R���X�g���N�^ 2 */
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
//Debug.println("note class: " + StringUtil.getClassName(clazz));
                    noteMessageConstructor1 = clazz.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE);
                    noteMessageConstructor2 = clazz.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                }
            } catch (Exception e) {
Debug.printStackTrace(e);
                throw (RuntimeException) new IllegalStateException().initCause(e);
            }
        }
    }

    /** sysex */
    private static class SysexMessageFactory {
        
        /**
         * �g����� 0xf# ~ 0xf#
         * <pre>
         *  length = dis.readShort();
         *  data ...
         * </pre>
         * @param dis data2 ~
         * TODO SysexMessage#readFrom �̂悤�ȋC������̂��� vavi �p�b�P�[�W�Ƃ̈ˑ��֌W�����܂�����... 
         */
        public static MfiMessage getMessage(int delta,
                                            int status,
                                            int data1,
                                            DataInputStream dis) throws IOException {
//Debug.println("delta: " + StringUtil.toHex2(delta));

            String key = String.format("mfi.track.%d.%c.%d", status, 'e', data1);

            if (sysexMessageInstantiators.containsKey(key)) {
                Method method = sysexMessageInstantiators.get(key);

                try {
                    return (SysexMessage) method.invoke(null, delta, status, data1, dis);
                } catch (Exception e) {
                    throw (RuntimeException) new IllegalStateException().initCause(e);
                }
            } else {
                // TODO clean up
                int length = dis.readUnsignedShort();
                byte[] data2 = new byte[length + 2];
                data2[0] = (byte) ((length / 0x100) & 0xff);
                data2[1] = (byte) ((length % 0x100) & 0xff);
                dis.readFully(data2, 2, length);

Debug.println("sysex unhandled: delta: " + StringUtil.toHex2(delta) + ", status: " + StringUtil.toHex2(status) + ", extended status: " + StringUtil.toHex2(data1));
                return UnknownMessageFactory.getMessage(delta, status, data1, data2);
            }
        }

        /** �g���X�e�[�^�X A {@link SysexMessage} �I�u�W�F�N�g�̃C���X�^���X���擾���郁�\�b�h�W */
        private static Map<String, Method> sysexMessageInstantiators = new HashMap<String, Method>();

        static {
            try {
                // props
                Properties props = new Properties();

                final String path = "vavi.properties";
                props.load(TrackMessage.class.getResourceAsStream(path));

                // 
                Iterator<?> i = props.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
                    if (key.matches("mfi\\.track\\.\\d+\\.e\\.\\d+")) {
                        Class<?> clazz = Class.forName(props.getProperty(key));
//Debug.println("sysex class: " + StringUtil.getClassName(clazz));
                        Method method = clazz.getMethod("readFrom", Integer.TYPE, Integer.TYPE, Integer.TYPE, InputStream.class);

                        sysexMessageInstantiators.put(key, method);
                    }
                }
            } catch (Exception e) {
Debug.printStackTrace(e);
                throw (RuntimeException) new IllegalStateException().initCause(e);
            }
        }
    }

    /** short */
    private static class ShortMessageFactory {
        
        /**
         * �g���X�e�[�^�X B (0x80 ~ 0xef)
         * <pre>
         *  length 1 fixed
         *  data 1 byte
         * </pre>
         * @param dis data2 ~
         * TODO ShortMessage#readFrom �̂悤�ȋC������̂��� vavi �p�b�P�[�W�Ƃ̈ˑ��֌W�����܂�����... 
         */
        public static MfiMessage getMessage(int delta,
                                            int status,
                                            int data1,
                                            DataInputStream dis) throws IOException {
            //
            int data2 = dis.readUnsignedByte();

            //
            String key = String.format("mfi.track.%d.%c.%d", status, 'b', data1);

            Constructor<? extends MfiMessage> constructor = null;
            if (shortMessageConstructors.containsKey(key)) {
                constructor = shortMessageConstructors.get(key);
            } else {
Debug.println("short unhandled: delta: " + StringUtil.toHex2(delta) + ", status: " + StringUtil.toHex2(status) + ", extended status: " + StringUtil.toHex2(data1));
                return UnknownMessageFactory.getMessage(delta, status, data1, data2);
            }

            try {
                return constructor.newInstance(delta, status, data1, data2);
            } catch (Exception e) {
                throw (RuntimeException) new IllegalStateException().initCause(e);
            }
        }

        /** �g���X�e�[�^�X B {@link ShortMessage} �I�u�W�F�N�g�̃C���X�^���X���擾����R���X�g���N�^�W */
        private static Map<String, Constructor<ShortMessage>> shortMessageConstructors = new HashMap<String, Constructor<ShortMessage>>();

        static {
            try {
                // props
                Properties props = new Properties();

                final String path = "vavi.properties";
                props.load(TrackMessage.class.getResourceAsStream(path));

                //
                Iterator<?> i = props.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
                    if (key.matches("mfi\\.track\\.\\d+\\.b\\.\\d+")) {
                        @SuppressWarnings("unchecked")
                        Class<ShortMessage> shortMessageClass = (Class<ShortMessage>) Class.forName(props.getProperty(key));
//Debug.println("short class: " + StringUtil.getClassName(shortMessageClass));
                        Constructor<ShortMessage> constructor = shortMessageClass.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);

                        shortMessageConstructors.put(key, constructor);
                    }
                }

            } catch (Exception e) {
Debug.printStackTrace(e);
                throw (RuntimeException) new IllegalStateException().initCause(e);
            }
        }
    }

    /** long */
    private static class LongMessageFactory {
        
        /**
         * �g���X�e�[�^�X A (0x00 ~ 0x7f)
         * <pre>
         *  length is 'exst'
         *  data ...
         * </pre>
         * @param dis data2 ~
         * @see #exst
         * TODO LongMessage#readFrom �̂悤�ȋC������̂��� vavi �p�b�P�[�W�Ƃ̈ˑ��֌W�����܂�����... 
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
            String key = String.format("mfi.track.%d.%c.%d", status, 'a', data1);

            Constructor<? extends MfiMessage> constructor = null;
            if (longMessageConstructors.containsKey(key)) {
                constructor = longMessageConstructors.get(key);
            } else {
Debug.println("long unhandled: delta: " + StringUtil.toHex2(delta) + ", status: " + StringUtil.toHex2(status) + ", extended status: " + StringUtil.toHex2(data1));
                return UnknownMessageFactory.getMessage(delta, status, data1, data2);
            }

            try {
                return constructor.newInstance(delta, status, data1, data2);
            } catch (Exception e) {
                throw (RuntimeException) new IllegalStateException().initCause(e);
            }
        }

        /** �g����� {@link LongMessage} �I�u�W�F�N�g�̃C���X�^���X���擾����R���X�g���N�^�W */
        private static Map<String, Constructor<LongMessage>> longMessageConstructors = new HashMap<String, Constructor<LongMessage>>();

        static {
            try {
                // props
                Properties props = new Properties();

                final String path = "vavi.properties";
                props.load(TrackMessage.class.getResourceAsStream(path));

                // 
                Iterator<?> i = props.keySet().iterator();
                while (i.hasNext()) {
                    String key = (String) i.next();
                    if (key.matches("mfi\\.track\\.\\d+\\.a\\.\\d+")) {
                        @SuppressWarnings("unchecked")
                        Class<LongMessage> longMessageClass = (Class<LongMessage>) Class.forName(props.getProperty(key));
//Debug.println("long class: " + StringUtil.getClassName(longMessageClass));
                        Constructor<LongMessage> constructor = longMessageClass.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, byte[].class);

                        longMessageConstructors.put(key, constructor);
                    }
                }
            } catch (Exception e) {
Debug.printStackTrace(e);
                throw (RuntimeException) new IllegalStateException().initCause(e);
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
                throw (RuntimeException) new IllegalStateException().initCause(e);
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
                throw (RuntimeException) new IllegalStateException().initCause(e);
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
//Debug.println("unknown class: " + StringUtil.getClassName(clazz));
                    unknownMessageConstructor1 = clazz.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                    unknownMessageConstructor2 = clazz.getConstructor(Integer.TYPE, Integer.TYPE, Integer.TYPE, byte[].class);
                }

            } catch (Exception e) {
Debug.printStackTrace(e);
                throw (RuntimeException) new IllegalStateException().initCause(e);
            }
        }
    }
}

/* */
