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



/**
 * WaveDivider.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050401 nsano initial version <br>
 */
public interface WaveDivider {

    /** �������ꂽ PCM �f�[�^��\���N���X�ł��B */
    class Chunk {
        /** ���� */
        public int sequence;
        /** �T���v�����O���[�g */
        public int samplingRate;
        /** �T���v�����O bits */
        public int bits;
        /** �`�����l���� */
        public int channels;
        /** PCM �f�[�^ */
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
            // TODO �����ƃv���p�e�B�Ƃ�����I��
            if (audioInputStream.getFormat().getEncoding().equals(Encoding.PCM_SIGNED)) {
                if (audioInputStream.getFormat().getChannels() == 1) {
                    return new Pcm16BitMonauralWaveDivider(audioInputStream);
                } else {
                    return new Pcm16BitMonauralWaveDivider(new MonauralInputFilter().doFilter(audioInputStream));
                }
            } else {
System.err.println("unsupported type: " + audioInputStream.getFormat());
                throw new IllegalArgumentException("only pcm mono is supported.");
            }
        }
    }

    /** */
    interface Event {
        void exec(Chunk chunk) throws IOException;
    }

    /**
     * WAVE �t�@�C������ PCM �f�[�^�𕪊����܂��B
     * @param seconds time for divide
     * @param event event for each chunks
     */
    void divide(float seconds, Event event) throws IOException;
}

/* */
