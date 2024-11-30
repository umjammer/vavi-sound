/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
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
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.vavi.audio.AdpmMessage;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mfi.vavi.sequencer.MfiMessageStore;
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.mobile.AudioEngine;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * AudioDataMessage.
 * <pre>
 *  adat
 *   type       4       "adat"
 *   length     4
 *   header     x 1     *1
 *
 *  header (*1)
 *   length     2
 *   format     1
 *   attribute  1
 *   sub chunk  x N     *2
 *
 *  sub chunk (*2)
 *   type       4
 *   length     2
 *   data       L
 * </pre>
 * <li>{@link #data} doesn't contain header, sub chunk part. it seems to be pure ADPCM data.
 * <li>{@link #length} is total length of AudioData Chunk
 * <li>TODO "extends {@link MfiMessage}" is needed? that should be AudioDataChunk isn't it?
 * <li>TODO this class should be merge into {@link vavi.sound.mfi.Track}? → extends {@link MetaMessage}？
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050721 nsano initial version <br>
 * @since MFi 4.0
 */
public class AudioDataMessage extends MfiMessage
    implements MidiConvertible, AudioDataSequencer {

    private static final Logger logger = getLogger(AudioDataMessage.class.getName());

    /** {@value} */
    public static final String TYPE = "adat";

    /** index of "adat" */
    private int audioDataNumber;

    /**
     * @since MFi 5.0
     */
    public static final int FORMAT_ADPCM_TYPE2 = 0x81;

    /**
     * @see #FORMAT_ADPCM_TYPE2
     */
    private int format;

    /**
     * <pre>
     * 76543 2 1 0
     * ~~~~~ ~ ~ ~
     *     | | | +- pitch change control identifier (0: not affected by pitch changes, 1: affected by pitch changes)
     *     | | +--- tempo change control identifier (0: not affected by tempo changes, 1: affected by tempo changes)
     *     | +----- 3D identifier (0: not 3D processed, 1: 3D 3D processed)
     *     +------- reserved (0 fixed)
     * </pre>
     */
    private int attribute;

    /** */
    private final Map<String, SubMessage> subChunks = new LinkedHashMap<>();

    /**
     * @see #FORMAT_ADPCM_TYPE2
     */
    public int getFormat() {
        return format;
    }

    /** */
    public boolean is3D() {
        return (attribute & 0x04) != 0;
    }

    /**
     * @return Returns the index.
     */
    public int getAudioDataNumber() {
        return audioDataNumber;
    }

    /** for writer */
    public AudioDataMessage(int format, int attribute, SubMessage ... subChunks) {
        super(new byte[0]);
        this.format = format;
        this.attribute = attribute;
        for (SubMessage subChunk : subChunks) {
            this.subChunks.put(subChunk.getSubType(), subChunk);
        }
    }

    /** for reader */
    public AudioDataMessage(int index) {
        super(new byte[0]);
        this.audioDataNumber = index;
    }

    /** */
    public void writeTo(OutputStream os)
        throws IOException {

        // 1. recalc
        int dataLength = data.length;
logger.log(Level.DEBUG, "dataLength: " + dataLength);
        int subChunksLength = 0;
        for (SubMessage subChunk : subChunks.values()) {
            subChunksLength += 4 + 2 + subChunk.getDataLength(); // type + length + ...
        }
logger.log(Level.DEBUG, "subChunksLength: " + subChunksLength);
        int headerLength = 1 + 1 + subChunksLength; // format + attribute + ...
        int audioDataLength = 2 + headerLength + dataLength; // headerLength + ...
logger.log(Level.DEBUG, "audioDataLength: " + audioDataLength);

        // 2. write
        DataOutputStream dos = new DataOutputStream(os);

        dos.writeBytes(TYPE);
        dos.writeInt(audioDataLength);

        dos.writeShort(headerLength);
        dos.writeByte(format);
        dos.writeByte(attribute);
        for (SubMessage subChunk : subChunks.values()) {
            subChunk.writeTo(os);
        }

        dos.write(data);
    }

    /**
     * @after {@link #length} will be set (total length of AudioData Chunk)
     * @after {@link #data} will be set, not contains header, sub chunks
     * @throws InvalidMfiDataException beginning of <code>is</code> is not {@link #TYPE}
     */
    public void readFrom(InputStream is)
        throws InvalidMfiDataException,
               IOException {

        DataInputStream dis = new DataInputStream(is);

        // type
        byte[] bytes = new byte[4];
        dis.readFully(bytes, 0, 4);
        String string = new String(bytes);
        if (!TYPE.equals(string)) {
            throw new InvalidMfiDataException("invalid audio data: " + string);
        }

        // length
        int audioDataLength = dis.readInt();

        // header
        int headerLength = dis.readUnsignedShort();
        this.format = dis.readUnsignedByte();
        this.attribute = dis.readUnsignedByte();
logger.log(Level.DEBUG, String.format("adat header: %d: f: %02x, a: %02x", headerLength, format, attribute));

        // sub chunks
        int l = 0;
        while (l < headerLength - (1 + 1)) { // - (format + attribute)
            SubMessage subChunk = SubMessage.readFrom(is);
            subChunks.put(subChunk.getSubType(), subChunk);
            l += subChunk.getDataLength() + 4 + 2; // + type + length
logger.log(Level.DEBUG, "audio subchunk length sum: " + l + " / " + (headerLength - 2));
        }

        // data
        int dataLength = audioDataLength - (headerLength + 1 + 1); // + format + attribute
        data = new byte[dataLength]; // TODO while data should be included
        dis.readFully(data, 0, dataLength);
logger.log(Level.DEBUG, "adat length[" + audioDataNumber + "]: " + dataLength + " bytes\n" + StringUtil.getDump(data, 16));

        //
        this.length = audioDataLength + 4 + 4; // + type + length
    }

    /**
     * excludes header, sub chunks (pure ADPCM)
     * data is stored in L R order without interleaving.
     * <li>TODO Chunk interface
     */
    public void setData(byte[] data) {
        this.data = data;

        // calc
        int dataLength = data.length;
logger.log(Level.DEBUG, "dataLength: " + dataLength);
        int subChunksLength = 0;
        for (SubMessage subChunk : subChunks.values()) {
            subChunksLength += 4 + 2 + subChunk.getDataLength(); // type + length + ...
        }
logger.log(Level.DEBUG, "subChunksLength: " + subChunksLength);
        int headerLength = 1 + 1 + subChunksLength; // format + attribute + ...
        int audioDataLength = 2 + headerLength + dataLength; // headerLength + ...
logger.log(Level.DEBUG, "audioDataLength: " + audioDataLength);
        this.length = audioDataLength + 4 + 4; // + type + length
    }

    /**
     * excludes header, sub chunks (pure ADPCM)
     * data is stored in L R order without interleaving.
     * <li>TODO Chunk interface
     */
    public byte[] getData() {
        return data;
    }

    /** @throws UnsupportedOperationException no mean for this class. */
    @Override
    public byte[] getMessage() {
        throw new UnsupportedOperationException("no mean");
    }

    // ----

    @Override
    public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
        MetaMessage metaMessage = new MetaMessage();

        int id = MfiMessageStore.put(this);
        byte[] data = {
            VaviMidiDeviceProvider.MANUFACTURER_ID,
            META_FUNCTION_ID_MFi4,
            (byte) ((id / 0x100) & 0xff),
            (byte) ((id % 0x100) & 0xff)
        };
        metaMessage.setMessage(0x7f,    // sequencer specific meta event
                               data,
                               data.length);

        return new MidiEvent[] {
            new MidiEvent(metaMessage, context.getCurrent())
        };
    }

    @Override
    public void sequence() throws InvalidMfiDataException {
        int id = getAudioDataNumber();
        int format = getFormat();
        byte[] data = getData();

        AdpmMessage adpm = (AdpmMessage) subChunks.get(AdpmMessage.TYPE);
        int samplingRate = adpm.getSamplingRate() * 1000;
        int samplingBits = adpm.getSamplingBits();
        int channels = adpm.getChannels();

        AudioEngine engine = Factory.getAudioEngine(format);
        engine.setData(id, -1, samplingRate, samplingBits, channels, data, false);
    }
}
