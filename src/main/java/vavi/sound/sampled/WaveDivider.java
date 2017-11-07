/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.AudioFormat.Encoding;

import vavi.util.Debug;


/**
 * WaveDivider.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050401 nsano initial version <br>
 */
public interface WaveDivider {

    /** 分割された PCM データを表すクラスです。 */
    class Chunk {
        /** 順番 */
        public int sequence;
        /** サンプリングレート */
        public int samplingRate;
        /** サンプリング bits */
        public int bits;
        /** チャンネル数 */
        public int channels;
        /** PCM データ */
        public byte[] buffer;
        /** */
        Chunk(int sequence, byte[] buffer, int samplingRate, int bits, int channels) {
            this.sequence = sequence;
            this.buffer = buffer;
            this.samplingRate = samplingRate;
            this.bits = bits;
            this.channels = channels;
        }
    }

    /** */
    class Factory {
        public static WaveDivider getWaveDivider(AudioInputStream audioInputStream) throws IOException, UnsupportedAudioFileException {
            // TODO ちゃんとプロパティとかから選ぶ
            if (audioInputStream.getFormat().getEncoding().equals(Encoding.PCM_SIGNED)) {
                if (audioInputStream.getFormat().getChannels() == 1) {
                    return new Pcm16BitMonauralWaveDivider(audioInputStream);
                } else {
                    return new Pcm16BitMonauralWaveDivider(new MonauralInputFilter().doFilter(audioInputStream));
                }
            } else {
Debug.println("unsupported type: " + audioInputStream.getFormat());
                throw new IllegalArgumentException("only pcm mono is supported.");
            }
        }
    }

    /** */
    interface Event {
        void exec(Chunk chunk) throws IOException;
    }

    /**
     * WAVE ファイル中の PCM データを分割します。
     * @param seconds time for divide
     * @param event event for each chunks
     */
    void divide(float seconds, Event event) throws IOException;
}

/* */
