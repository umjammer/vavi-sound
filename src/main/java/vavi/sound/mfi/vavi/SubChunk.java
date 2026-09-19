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
import java.util.Arrays;
import java.util.ServiceLoader;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MetaMessage;
import vavi.sound.mfi.vavi.sub.CodeChunk;
import vavi.sound.mfi.vavi.sub.ProtChunk;
import vavi.util.Debug;
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
 * |<-------+------->|<---- spec of {@link SubChunk} ---- ...
 * +--+--+--+--+--+--+--+--+--+--+--+-
 * |00 ff fd|LL LL|7f|XX XX XX XX|DD DD ...
 * +--+--+--+--+--+--+--+--+--+--+--+-
 * </pre>
 * <li>{@link #getLength()} returns data + 10 ({@link MetaMessage#HEADER_LENGTH meta header} +
 *     {@link #SUB_TYPE_LENGTH type's string length}).
 * <p>
 * <h4>system property</h4>
 * <li>{@code vavi.sound.mfi.encoding.write} ... encoding for writing, default {@code Windows-31J}</li>
 * <li>{@code vavi.sound.mfi.encoding.read} ... encoding for reading, default {@code JISAutoDetect}</li>
 * <p>
 * <li>TODO the first thing to put in {@link vavi.sound.mfi.Track}[0] is summarized as {@link MetaMessage}
 * <li>TODO all subclasses make MfiMessage class inside like AudioDataChunk </li>
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030819 nsano out source from {@link VaviMfiFileFormat} <br>
 *          0.10 030825 nsano merge {@link SubChunk} <br>
 *          0.11 030920 nsano extends {@link MetaMessage} <br>
 */
public abstract class SubChunk extends MetaMessage {

    private static final Logger logger = getLogger(SubChunk.class.getName());

    /** TODO use {@link CodeChunk} */
    protected static final String readingEncoding;

    /** TODO use {@link CodeChunk} */
    protected static final String writingEncoding;

    /** "port".length */
    protected static final int SUB_TYPE_LENGTH = 4;

    /** sequencer specific meta event */
    public static final int META_TYPE = 0x7f;

    /** */
    public abstract boolean accept(String subType);

    /**
     * @param subType ex. {@link ProtChunk#TYPE "prot"}
     * @return this
     */
    protected SubChunk init(String subType, byte[] data) {
        try {
            byte[] message = getSubMessage(subType, data, data.length);
            setMessage(META_TYPE, message, message.length);
            return this;
        } catch (InvalidMfiDataException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * @param subType ex. {@link ProtChunk#TYPE "prot"}
     */
    protected SubChunk init(String subType, String data) {
        try {
            byte[] tmp = data.getBytes(writingEncoding);
            byte[] message = getSubMessage(subType, tmp, tmp.length);
            setMessage(META_TYPE, message, message.length);
            return this;
        } catch (UnsupportedEncodingException | InvalidMfiDataException e) {
            throw new IllegalStateException(e);
        }
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
Debug.print(getDataLength() + "\n" + StringUtil.getDump(this.data, 32));
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
     * @return anonymous {@link SubChunk} when input is an unknown type
     * @throws IllegalStateException when {@link SubChunk} instantiation failed
     */
    public static SubChunk readFrom(InputStream is)
        throws InvalidMfiDataException,
               IOException {

        DataInputStream dis = new DataInputStream(is);

        byte[] cs = new byte[SUB_TYPE_LENGTH];
        dis.readNBytes(cs, 0, SUB_TYPE_LENGTH); // type
        String subType = new String(cs);

        int length = dis.readShort();
        byte[] subData = new byte[length];
        dis.readFully(subData, 0, length);

logger.log(Level.TRACE, "subType: " + subType + ", data.length: " + subData.length);
        SubChunk subChunk = factory(subType);

        if (subChunk != null) {
            subChunk.init(subType, subData);
        } else {
logger.log(Level.WARNING, "unknown sub chunk: " + subType);
            subChunk = new SubChunk() {
                {
                    try {
                        byte[] message = getSubMessage(subType, subData, subData.length);
                        setMessage(META_TYPE, message, message.length);
                    } catch (InvalidMfiDataException e) {
                        throw new IllegalStateException(e);
                    }
                }
                @Override public boolean accept(String subType) { return false; }
            };
        }

logger.log(Level.DEBUG, subChunk);
        return subChunk;
    }

    // ----

    protected static byte[] concat(byte[]... arrays) {
        int length = Arrays.stream(arrays)
                .mapToInt(a -> a.length)
                .sum();

        byte[] result = new byte[length];
        int pos = 0;

        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }

        return result;
    }

    /** @return new instance */
    public static SubChunk factory(String subType) {
        for (SubChunk subChunk : ServiceLoader.load(SubChunk.class)) {
            if (subChunk.accept(subType)) {
                // ServiceLoader caches provider instances.  SubMessage holds
                // the parsed payload, so returning that cached instance makes
                // every chunk share the last chunk's data (e.g. all ADPM
                // entries become the final 2-bit header).  Create a fresh
                // provider instance for every parsed subchunk.
                return subChunk;
            }
        }
logger.log(Level.WARNING, "no matched sub chunk: " + subType);
        return null;
    }

    static {
        // encodings
        writingEncoding = System.getProperty("vavi.sound.mfi.encoding.write", "Windows-31J");
logger.log(Level.DEBUG, "write encoding: " + writingEncoding);
        readingEncoding = System.getProperty("vavi.sound.mfi.encoding.read","JISAutoDetect");
logger.log(Level.DEBUG, "read encoding: " + readingEncoding);
    }
}
