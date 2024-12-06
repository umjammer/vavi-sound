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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MetaMessage;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


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
 * <li>{@link #getLength()} returns data + 10 ({@link MetaMessage#HEADER_LENGTH meta header} +
 *     {@link #SUB_TYPE_LENGTH type's string length}).
 * <li>TODO does it have to be {@link MetaMessage}? (although it looks like MIDI)
 * <li>TODO ↑ the first thing to put in {@link vavi.sound.mfi.Track}[0] is summarized as {@link MetaMessage}
 * <li>TODO ↑ then {@link vavi.sound.mfi.vavi.AudioDataMessage} should be a subclass of {@link MetaMessage}
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030819 nsano out source from {@link VaviMfiFileFormat} <br>
 *          0.10 030825 nsano merge {@link SubMessage} <br>
 *          0.11 030920 nsano extends {@link MetaMessage} <br>
 */
public abstract class SubMessage extends MetaMessage {

    private static final Logger logger = getLogger(SubMessage.class.getName());

    /** TODO use {@link vavi.sound.mfi.vavi.header.CodeMessage} */
    protected static String readingEncoding = "JISAutoDetect";

    /** TODO use {@link vavi.sound.mfi.vavi.header.CodeMessage} */
    protected static String writingEncoding = "Windows-31J";

    /** "port".length */
    protected static final int SUB_TYPE_LENGTH = 4;

    /** sequencer specific meta event */
    public static final int META_TYPE = 0x7f;

    /**
     * @param subType ex. {@link vavi.sound.mfi.vavi.header.ProtMessage#TYPE "prot"}
     */
    protected SubMessage(String subType, byte[] data) {
        try {
            byte[] message = getSubMessage(subType, data, data.length);
            setMessage(META_TYPE, message, message.length);
        } catch (InvalidMfiDataException e) {
            throw new IllegalStateException(e);
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
        } catch (UnsupportedEncodingException | InvalidMfiDataException e) {
            throw new IllegalStateException(e);
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
     * @return subType string (ex. "prot")
     */
    public String getSubType() {
        return new String(this.data, HEADER_LENGTH, SUB_TYPE_LENGTH);
    }

    /**
     * Data part only. It also does not include the {@link #getSubType() subType} string.
     * @return {@link #getLength()} - 10 ({@link MetaMessage#HEADER_LENGTH meta header} +
     *         {@link #SUB_TYPE_LENGTH type string length})
     */
    public int getDataLength() {
        return getLength() - (HEADER_LENGTH + SUB_TYPE_LENGTH);
    }

    /**
     * Data part only. It also does not include the {@link #getSubType() subType} string.
     * @return copied data
     */
    @Override
    public byte[] getData() {
        byte[] tmp = new byte[getDataLength()];
        System.arraycopy(this.data, HEADER_LENGTH + SUB_TYPE_LENGTH,
                         tmp, 0,
                         getDataLength());
        return tmp;
    }

    /**
     * Only the data part will be processed correctly if the length changes.
     * @param data not included {@link #getSubType() subType} string
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

    @Override
    public String toString() {
        return getSubType() + ": " + getDataLength() + ":\n" + StringUtil.getDump(getData());
    }

    // ----

    /** */
    public void writeTo(OutputStream os) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);

        dos.writeBytes(getSubType());
        dos.writeShort(getDataLength());
        dos.write(getData(), 0, getDataLength());
logger.log(Level.DEBUG, this);
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
        String subType = new String(cs);

        String key = null;
        String headerKey = "mfi.header." + subType;
        String audioKey = "mfi.audio." + subType;

        if (subChunkConstructors.containsKey(headerKey)) {
            key = headerKey;
        } else if (subChunkConstructors.containsKey(audioKey)) {
            key = audioKey;
        }

        SubMessage subChunk;

        int length = dis.readShort();
        byte[] subData = new byte[length];
        dis.readFully(subData, 0, length);

        if (key != null) {
            Constructor<SubMessage> constructor = subChunkConstructors.get(key);
            try {
                subChunk = constructor.newInstance(subType, subData);
            } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                throw new IllegalStateException(e);
            }
        } else {
logger.log(Level.WARNING, "unknown sub chunk: " + subType);
            subChunk = new SubMessage() {
                {
                    try {
                        byte[] message = getSubMessage(subType, subData, subData.length);
                        setMessage(META_TYPE, message, message.length);
                    } catch (InvalidMfiDataException e) {
                        throw new IllegalStateException(e);
                    }
                }
            };
        }

logger.log(Level.DEBUG, subChunk);
        return subChunk;
    }

    // ----

    /** {@link SubMessage} constructors */
    private static final Map<String, Constructor<SubMessage>> subChunkConstructors = new HashMap<>();

    static {
        try {
            // props
            Properties props = new Properties();
            final String path = "vavi.properties";
            props.load(SubMessage.class.getResourceAsStream(path));

            // header/audio sub chunks
            for (Object o : props.keySet()) {
                String key = (String) o;
                if (key.matches("mfi\\.(header|audio)\\.\\w+")) {
                    @SuppressWarnings("unchecked")
                    Class<SubMessage> clazz = (Class<SubMessage>) Class.forName(props.getProperty(key));
//logger.log(Level.TRACE, "sub class: " + StringUtil.getClassName(clazz));
                    Constructor<SubMessage> constructor = clazz.getConstructor(String.class, byte[].class);

                    subChunkConstructors.put(key, constructor);
                }
            }

            // encodings
            String value = props.getProperty("encoding.write");
            if (value != null) {
                writingEncoding = value;
logger.log(Level.DEBUG, "write encoding: " + writingEncoding);
            }
            value = props.getProperty("encoding.read");
            if (value != null) {
                readingEncoding = value;
logger.log(Level.DEBUG, "read encoding: " + readingEncoding);
            }
        } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }
}
