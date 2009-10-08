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
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MetaMessage;
import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * MFi Sub Chunk.
 * <pre>
 * real [MFi sub chunk]
 * +--+--+--+--+--+--+--+--+-
 * |XX XX XX XX|LL LL|DD DD ...
 * +--+--+--+--+--+--+--+--+-
 * 
 * this class {@link #data} in [MFi meta]
 *     spec of {@link MetaMessage}
 *          |
 * |<-------+------->|<---- spec of {@link SubMessage} ---- ...
 * +--+--+--+--+--+--+--+--+--+--+--+-
 * |00 ff fd|LL LL|7f|XX XX XX XX|DD DD ...
 * +--+--+--+--+--+--+--+--+--+--+--+-
 * </pre>
 * <li>{@link #getLength()} �̓f�[�^ + 10 ({@link MetaMessage#HEADER_LENGTH meta header} +
 *     {@link #SUB_TYPE_LENGTH type �̕�������}) ��Ԃ��܂��B
 * <li>TODO {@link MetaMessage} �ł���K�v������̂��H (MIDI ���ۂ��͂Ȃ��Ă邪)
 * <li>�� {@link vavi.sound.mfi.Track}[0] �̍ŏ��ɓ����̂� {@link MetaMessage} �Ƃ܂Ƃ߂���
 * <li>�� ��������� {@link vavi.sound.mfi.vavi.AudioDataMessage} �� {@link MetaMessage} �̃T�u�N���X�ł���ׂ�
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030819 nsano out source from {@link VaviMfiFileFormat} <br>
 *          0.10 030825 nsano merge {@link SubMessage} <br>
 *          0.11 030920 nsano extends {@link MetaMessage} <br>
 */
public abstract class SubMessage extends MetaMessage {

    /** TODO use {@link vavi.sound.mfi.vavi.header.CodeMessage} */
    protected static String readingEncoding = "JISAutoDetect";

    /** TODO use {@link vavi.sound.mfi.vavi.header.CodeMessage} */
    protected static String writingEncoding = "Windows-31J";

    /** "port".length */
    protected static final int SUB_TYPE_LENGTH = 4;

    /** �V�[�P���T�ŗL�̃��^�C�x���g */
    public static final int META_TYPE = 0x7f;

    /**
     * @param subType ex. {@link vavi.sound.mfi.vavi.header.ProtMessage#TYPE "prot"} 
     */
    protected SubMessage(String subType, byte[] data) {
        try {
            byte[] message = getSubMessage(subType, data, data.length);
            setMessage(META_TYPE, message, message.length);
        } catch (InvalidMfiDataException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }

    /**
     * @param subType ex. {@link vavi.sound.mfi.vavi.header.ProtMessage#TYPE "prot"} 
     */
    protected SubMessage(String subType, String data) {
        try {
            byte[] tmp = data.getBytes(writingEncoding);
            byte[] message = getSubMessage(subType, tmp, tmp.length);
            setMessage(META_TYPE, message, message.length);
        } catch (UnsupportedEncodingException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        } catch (InvalidMfiDataException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }

    /** for {@link MfiConvertible} and {@link vavi.sound.mfi.vavi.header.AinfMessage} */
    protected SubMessage() {
    }

    /** 
     * @param subType ex. "prot" 
     * @return <pre>
     * +--+--+--+--+--+--+--+--+-
     * |XX XX XX XX|DD DD ...
     * +--+--+--+--+--+--+--+--+-
     * </pre>
     */
    protected byte[] getSubMessage(String subType, byte[] data, int length) {

        byte[] tmp = new byte[SUB_TYPE_LENGTH + length];

        tmp[0] = (byte) subType.charAt(0);
        tmp[1] = (byte) subType.charAt(1);
        tmp[2] = (byte) subType.charAt(2);
        tmp[3] = (byte) subType.charAt(3);
        System.arraycopy(data, 0, tmp, SUB_TYPE_LENGTH, length);

        return tmp;
    }

    /**
     * @return subType ������ (ex. "prot") 
     */
    public String getSubType() {
        return new String(this.data, HEADER_LENGTH, SUB_TYPE_LENGTH);
    }

    /**
     * �f�[�^�����̂݁B{@link #getSubType() subType} ��������܂݂܂���B
     * @return {@link #getLength()} - 10 ({@link MetaMessage#HEADER_LENGTH meta header} +
     *         {@link #SUB_TYPE_LENGTH type �̕�������})
     */
    public int getDataLength() {
        return getLength() - (HEADER_LENGTH + SUB_TYPE_LENGTH);
    }

    /**
     * �f�[�^�����̂݁B{@link #getSubType() subType} ��������܂݂܂���B
     * @return �R�s�[
     */
    public byte[] getData() {
        byte[] tmp = new byte[getDataLength()];
        System.arraycopy(this.data, HEADER_LENGTH + SUB_TYPE_LENGTH,
                         tmp, 0,
                         getDataLength());
        return tmp;
    }

    /**
     * �f�[�^�����̂݁A�������ς�����ꍇ���������������B
     * @param data {@link #getSubType() subType} ��������܂߂Ȃ�
     */
    public void setData(byte[] data)
        throws InvalidMfiDataException {

        if (data.length == getDataLength()) {
            System.arraycopy(data, 0,
                             this.data, HEADER_LENGTH + SUB_TYPE_LENGTH,
                             data.length);
        } else {
            byte[] message = getSubMessage(getSubType(), data, data.length);
            setMessage(getType(), message, message.length);
        }
    }

    /** */
    public String toString() {
        return getSubType() + ": " + getDataLength() + ":\n" + StringUtil.getDump(getData());
    }

    //----

    /** */
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.writeBytes(getSubType());
        dos.writeShort(getDataLength());
        dos.write(getData(), 0, getDataLength());
Debug.println(this);
    }

    /**
     * @return anonymous {@link SubMessage} when input is an unknown type 
     * @throws IllegalStateException when {@link SubMessage} instantiation failed
     */
    public static SubMessage readFrom(InputStream is)
        throws InvalidMfiDataException,
               IOException {

        DataInputStream dis = new DataInputStream(is);

        byte[] cs = new byte[SUB_TYPE_LENGTH];
        dis.read(cs, 0, SUB_TYPE_LENGTH);       // type
        final String subType = new String(cs);

        String key = null;
        String headerKey = "mfi.header." + subType;
        String audioKey = "mfi.audio." + subType;

        if (subChunkConstructors.containsKey(headerKey)) {
            key = headerKey;
        } else if (subChunkConstructors.containsKey(audioKey)) {
            key = audioKey;
        }

        SubMessage subChunk = null;

        int length = dis.readShort();
        final byte[] subData = new byte[length];
        dis.readFully(subData, 0, length);

        if (key != null) {
            Constructor<SubMessage> constructor = subChunkConstructors.get(key);
            try {
                subChunk = constructor.newInstance(subType, subData);
            } catch (Exception e) {
Debug.printStackTrace(e);
                throw (RuntimeException) new IllegalStateException().initCause(e);
            }
        } else {
Debug.println(Level.WARNING, "unknown sub chunk: " + subType);
            subChunk = new SubMessage() {
                {
                    try {
                        byte[] message = getSubMessage(subType, subData, subData.length);
                        setMessage(META_TYPE, message, message.length);
                    } catch (InvalidMfiDataException e) {
                        throw (RuntimeException) new IllegalStateException().initCause(e);
                    }
                }
            };
        }

Debug.println(subChunk);
        return subChunk;
    }

    //----

    /** {@link SubMessage} �̃R���X�g���N�^�W */
    private static Map<String, Constructor<SubMessage>> subChunkConstructors = new HashMap<String, Constructor<SubMessage>>();

    static {
        try {
            // props
            Properties props = new Properties();
            final String path = "vavi.properties";
            props.load(SubMessage.class.getResourceAsStream(path));
            
            // header/audio sub chunks
            Iterator<?> i = props.keySet().iterator();
            while (i.hasNext()) {
                String key = (String) i.next();
                if (key.matches("mfi\\.(header|audio)\\.\\w+")) {
                    @SuppressWarnings("unchecked")
                    Class<SubMessage> clazz = (Class<SubMessage>) Class.forName(props.getProperty(key));
//Debug.println("sub class: " + StringUtil.getClassName(clazz));
                    Constructor<SubMessage> constructor = clazz.getConstructor(String.class, byte[].class);

                    subChunkConstructors.put(key, constructor);
                }
            }

            // encodings
            String value = props.getProperty("encoding.write");
            if (value != null) {
                writingEncoding = value;
Debug.println("write encoding: " + writingEncoding);
            }
            value = props.getProperty("encoding.read");
            if (value != null) {
                readingEncoding = value;
Debug.println("read encoding: " + readingEncoding);
            }
        } catch (Exception e) {
Debug.printStackTrace(e);
            System.exit(1);
        }
    }
}

/* */
