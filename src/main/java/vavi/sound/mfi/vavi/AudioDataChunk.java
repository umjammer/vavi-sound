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
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Receiver;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.audio.AdpmChunk;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.MobileExclusive;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.mfi.vavi.VaviMfiFileFormat.DumpContext.getDC;


/**
 * AudioDataChunk.
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
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050721 nsano initial version <br>
 * @since MFi 4.0
 */
public class AudioDataChunk {

    private static final Logger logger = getLogger(AudioDataChunk.class.getName());

    /** {@value} */
    public static final String TYPE = "adat";

    private final AudioDataMessage audioDataMessage;

    /** for reader */
    public AudioDataChunk(int index) {
        this.audioDataMessage = new AudioDataMessage();
        this.audioDataMessage.audioDataNumber = index;
    }

    public AudioDataMessage getAudioDataMessage() {
        return audioDataMessage;
    }

    /**
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
        this.audioDataMessage.format = dis.readUnsignedByte();
        this.audioDataMessage.attribute = dis.readUnsignedByte();
logger.log(Level.DEBUG, "adat header: %d: f: %02x, a: %02x".formatted(headerLength, audioDataMessage.format, audioDataMessage.attribute));

        // sub chunks
        int l = 0;
        while (l < headerLength - (1 + 1)) { // - (format + attribute)
            SubChunk subChunk = SubChunk.readFrom(is);
            audioDataMessage.subChunks.put(subChunk.getSubType(), subChunk);
            l += subChunk.getDataLength() + 4 + 2; // + type + length
logger.log(Level.DEBUG, "audio subchunk length sum: " + l + " / " + (headerLength - 2));
        }

        // data
        int dataLength = audioDataLength - (headerLength + 1 + 1); // + format + attribute
        byte[] data = new byte[dataLength]; // TODO while data should be included
        dis.readFully(data, 0, dataLength);
logger.log(Level.DEBUG, "adat length[" + audioDataMessage.audioDataNumber + "]: " + dataLength + " bytes\n" + StringUtil.getDump(data, 16));

        //
        int length = audioDataLength + 4 + 4; // + type + length TODO check

        audioDataMessage.init(data, length);
    }

    /**
     * <li>{@link #data} doesn't contain header, sub chunk part. it seems to be pure ADPCM data.
     * <li>{@link #length} is total length of AudioData Chunk
     */
    public static class AudioDataMessage extends vavi.sound.mfi.SysexMessage
            implements MidiConvertible, AudioDataSequencer {

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
         *     | +----- 3D identifier (0: not 3D processed, 1: 3D processed)
         *     +------- reserved (0 fixed)
         * </pre>
         */
        private int attribute;

        /** */
        private final Map<String, SubChunk> subChunks = new LinkedHashMap<>();

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

        /**
         * @after {@link #length} will be set (total length of AudioData Chunk)
         * @after {@link #data} will be set, not contains header, sub chunks
         */
        AudioDataMessage init(byte[] data, int length) {
            super.init(data);
            this.length = length;

            return this;
        }

        /** for writer */
        public AudioDataMessage init(int format, int attribute, SubChunk... subChunks) {
            this.format = format;
            this.attribute = attribute;
            for (SubChunk subChunk : subChunks) {
                this.subChunks.put(subChunk.getSubType(), subChunk);
            }

            return this;
        }

        /** */
        public void writeTo(OutputStream os) throws IOException {

            // 1. recalc
            int dataLength = this.getData().length;
            logger.log(Level.DEBUG, "dataLength: " + dataLength);
            int subChunksLength = 0;
            for (SubChunk subChunk : this.subChunks.values()) {
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
            dos.writeByte(this.format);
            dos.writeByte(this.attribute);
            for (SubChunk subChunk : this.subChunks.values()) {
                subChunk.writeTo(os);
            }

            dos.write(this.getData());
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
            for (SubChunk subChunk : subChunks.values()) {
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

        /**
         * an "adat" chunk is not a track event, it has no Δ. {@link #data} is pure ADPCM,
         * the super's one reading data[0] as Δ puts the wave after the play of it.
         * @return 0 always
         */
        @Override
        public int getDelta() {
            return 0;
        }

        // ----

        @Override
        public MidiEvent[] getMidiEvents(MidiContext context) throws InvalidMidiDataException {
            SysexMessage sysexMessage;

            AdpmChunk adpm = (AdpmChunk) subChunks.get(AdpmChunk.TYPE);
            sysexMessage = MobileExclusive.packedSystex(MobileExclusive.wave(
                    MFi_SYSEX_FUNCTION_ID_MFi4,
                    audioDataNumber,
                    format,
                    adpm.getChannels(),
                    adpm.getSamplingBits(),
                    adpm.getSamplingRate() * 1000,
                    getData()));

            return new MidiEvent[] {
                new MidiEvent(sysexMessage, context.getCurrent())
            };
        }

        /**
         * @param data 10 id fm ch bt sr adpcm ...
         * @throws IllegalArgumentException when audio engine does not found
         * @see MobileExclusive#wave 
         */
        @Override
        public void sequence(byte[] data, Receiver receiver) throws InvalidMfiDataException {
            assert data[0] == 0x10 : "illegal command";
            int id = data[1] & 0x7f;
            int format = data[2] & 0xff;

            int samplingRate = (data[5] & 0xff) * 0x100 + (data[6] & 0xff);
            int samplingBits = data[4];
            int channels = data[3];
            byte[] adpcm = Arrays.copyOfRange(data, 7, data.length - 1);

            try {
                AudioEngine engine = AudioEngineFactory.getAudioEngine(format);
                engine.setData(id, -1, samplingRate, samplingBits, channels, adpcm, false);
            } catch (IllegalArgumentException e) {
logger.log(Level.ERROR, "cannot retrieve audio engine for: " + e.getMessage());
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(TYPE).append("\n");
        try (var dc = getDC().open()) {
            audioDataMessage.subChunks.values().forEach(sc -> sb.append(dc.format(sc.toString())));
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
