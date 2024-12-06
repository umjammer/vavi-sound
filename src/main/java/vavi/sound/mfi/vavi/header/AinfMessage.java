/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi.header;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.vavi.SubMessage;
import vavi.util.StringUtil;


/**
 * MFi Header Sub Chunk for audio chunk information.
 * <pre>
 *  0: 7 6 543210
 *       ^ ~~~~~~
 *       | +- audio chunks ("adat") count
 *       +- is audio chunk only? 0: has track, 1: only
 *  1: audio info count
 *  2: audio info (*1)
 *  :   :
 *  N: audio info
 *
 *  audio info (*1)
 *  0: audio format
 *  1: audio info length H
 *  2: audio info length L
 *  3: audio info data
 *  :   :
 *  N: audio info data
 * </pre>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050721 nsano initial version <br>
 * @since MFi 4.0
 */
public class AinfMessage extends SubMessage {

    /** */
    public static final String TYPE = "ainf";

    /** */
    private final List<AudioInfo> audioInfos = new ArrayList<>();

    /**
     * for {@link SubMessage#readFrom(java.io.InputStream)}
     * @param type ignored
     */
    public AinfMessage(String type, byte[] data) {
        super(TYPE, data);

        // audio info ...
        data = getData();
        int l = 0;
        for (int i = 0; i < getAudioInfoCount(); i++) {
            AudioInfo audioInfo = new AudioInfo(i, data, l);
            audioInfos.add(audioInfo);

            l += 1 + 2 + audioInfo.length; // format + length + ...
        }
    }

    /** */
    public AinfMessage(boolean audioChunkOnly, int audioChunksCount, AudioInfo ... audioInfos) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int tmp = audioChunksCount;
            tmp |= audioChunkOnly ? 0x40 : 0x00;
            baos.write(tmp);
            baos.write(audioInfos.length);
            for (AudioInfo audioInfo : audioInfos) {
                baos.write(audioInfo.format);
                baos.write(audioInfo.length / 0x100);
                baos.write(audioInfo.length % 0x100);
                baos.write(audioInfo.data);
            }
            byte[] message = getSubMessage(TYPE, baos.toByteArray(), baos.size());
            setMessage(META_TYPE, message, message.length);
        } catch (InvalidMfiDataException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            assert false : e.toString();
        }
    }

    /** */
    public boolean isAudioChunkOnly() {
        byte[] data = getData();
        return (data[0] & 0x40) != 0;
    }

    /** 0 ~ 63 */
    public int getAudioChunksCount() {
        byte[] data = getData();
        return data[0] & 0x3f;
    }

    /** */
    public int getAudioInfoCount() {
        byte[] data = getData();
        return data[1] & 0xff;
    }

    /** */
    public static class AudioInfo {
        /** for {@link #toString()} */
        private int index;
        /** */
        private final int format;
        /** */
        private final int length;
        /** */
        private final byte[] data;
        /** for creation */
        public AudioInfo(int format, byte[] data) {
            this.format = format;
            this.length = data.length;
            this.data = data;
        }
        /** for reading */
        private AudioInfo(int index, byte[] in, int offset) {
            this.index = index;
            this.format = in[2 + offset] & 0xff;
            this.length = in[2 + offset + 1] * 0xff + in[2 + offset + 2];
            this.data = new byte[length];
            System.arraycopy(in, 2 + offset + 3, data, 0, length);
        }
        @Override
        public String toString() {
            String sb = "ainf inf[" + index + "]: " +
                    "%02x, %d".formatted(format, length) +
                    '\n' +
                    StringUtil.getDump(data);
            return sb;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ainf: ");
        sb.append(length);
        sb.append(": only: ");
        sb.append(isAudioChunkOnly());
        sb.append(", adat: ");
        sb.append(getAudioChunksCount());
        sb.append(", info: ");
        sb.append(getAudioInfoCount());
        sb.append('\n');
        for (AudioInfo audioInfo : audioInfos) {
            sb.append(audioInfo);
        }
        return sb.toString();
    }
}
