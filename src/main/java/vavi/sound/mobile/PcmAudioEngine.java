/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;


/**
 * PCM AudioEngine.
 * <pre>
 * WSC-MAX_DLL
 * DLL
 *                  Input File                                                               Output File
 * wscma2_dll       File Format         Bit              Sampling Frequency      Form        Conversion Format   File Format
 * wscma3_dll       WAVE                16bit            4kHz or 8kHz            mono        4bit ADPCM          SMAF/MA-2
 *                  WAVE                16bit            4kHz~16kHz              mono        4bit ADPCM          SMAF/MA-3
 * wscma5_dll       WAVE                16bit or 8bit    4kHz~ 8kHz              mono        8bit PCM            SMAF/MA-3
 *                  WAVE or AIFF        16bit            4kHz~24kHz              mono        4bit ADPCM          SMAF/MA-5
 *                  WAVE or AIFF        16bit or 8bit    4kHz~12kHz              mono        8bit PCM            SMAF/MA-5
 *                  WAVE or AIFF        16bit            4kHz~12kHz             stereo       4bit ADPCM          SMAF/MA-5
 *                  WAVE or AIFF        16bit or 8bit    4kHz~6kHz              stereo       8bit PCM            SMAF/MA-5
 * </pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020829 nsano initial version <br>
 * @see "WSC-MAx.pdf"
 */
public class PcmAudioEngine extends BasicAudioEngine {

    private static final Logger logger = getLogger(PcmAudioEngine.class.getName());

    /**
     * <pre>
     *  L0 + L2 + ...
     *  R1 + R3 + ...
     * </pre>
     */
    public PcmAudioEngine() {
        data = new Data[32];
    }

    @Override
    protected int getChannels(int streamNumber) {
        int channels = 1;
        if (data[streamNumber].channel == -1) {
            // from 1_240_7
            if (data[streamNumber].channels == 2) {
                channels = 2;
            } else {
                if (streamNumber % 2 == 1 && data[streamNumber].channels != 2 && (data[streamNumber - 1] != null && data[streamNumber - 1].channels != 2)) {
logger.log(Level.DEBUG, "always used: no: " + streamNumber + ", ch: " + data[streamNumber].channel);
                    return -1;
                }

                if (streamNumber % 2 == 0 && data[streamNumber].channels != 2 && (data[streamNumber + 1] != null && data[streamNumber + 1].channels != 2)) {
                    channels = 2;
                }
            }
        } else {
            // from 240_2, channels always 1

            if (streamNumber % 2 == 1 && data[streamNumber].channel % 2 == 1 && (data[streamNumber - 1] != null && data[streamNumber - 1].channel % 2 == 0)) {
logger.log(Level.DEBUG, "always used: no: " + streamNumber + ", ch: " + data[streamNumber].channel);
                return -1;
            }

            if (streamNumber % 2 == 0 && data[streamNumber].channel % 2 == 0 && (data[streamNumber + 1] != null && data[streamNumber + 1].channel % 2 == 1)) {
                channels = 2;
            }
        }
        return channels;
    }

    @Override
    protected InputStream[] getInputStreams(int streamNumber, int channels) {
        InputStream[] iss = new InputStream[2];
        if (data[streamNumber].channels == 1) {
            InputStream in = new ByteArrayInputStream(data[streamNumber].adpcm);
            iss[0] = in;
            if (channels != 1) {
                InputStream inR = new ByteArrayInputStream(data[streamNumber + 1].adpcm);
                iss[1] = inR;
            }
        } else {
            InputStream in = new ByteArrayInputStream(data[streamNumber].adpcm, 0, data[streamNumber].adpcm.length / 2);
            iss[0] = in;
            InputStream inR = new ByteArrayInputStream(data[streamNumber].adpcm, data[streamNumber].adpcm.length / 2, data[streamNumber].adpcm.length / 2);
            iss[1] = inR;
        }
        return iss;
    }

    // ----

    @Override
    protected OutputStream getOutputStream(OutputStream os) {
        return os;
    }

    @Override
    public void start(int streamNumber) {

        int channels = getChannels(streamNumber);
        if (channels == -1) {
logger.log(Level.DEBUG, "always used: no: " + streamNumber + ", ch: " + this.data[streamNumber].channel);
            return;
        }

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_UNSIGNED,
            this.data[streamNumber].sampleRate,
            8,
            channels,
            1 * channels,
            this.data[streamNumber].sampleRate,
            false);
logger.log(Level.DEBUG, audioFormat);

        try {

//logger.log(Level.TRACE, data.length);
            InputStream[] iss = getInputStreams(streamNumber, channels);

//logger.log(Level.TRACE, "is: " + is.available());

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();
            volume(line, volume);
            byte[] buf = new byte[1024];
            while (iss[0].available() > 0) {
                if (channels == 1) {
                    int l = iss[0].read(buf, 0, 1024);
//logger.log(Level.TRACE, "\n" + StringUtil.getDump(buf, 64));
                    line.write(buf, 0, l);
                } else {
                    int lL = iss[0].read(buf, 0, 512);
                    /*int lR = */iss[1].read(buf, 512, 512);
//logger.log(Level.TRACE, "l : " + lL + ", r: " + lR);
                    for (int i = 0; i < lL / 2; i++) {
                        byte[] temp = new byte[4];
                        temp[0] = buf[i * 2];
                        temp[1] = buf[i * 2 + 1];
                        temp[2] = buf[512 + i * 2];
                        temp[3] = buf[512 + i * 2 + 1];
                        line.write(temp, 0, 4);
                    }
                }
            }
            line.drain();
            line.stop();
            line.close();

        } catch (IOException | LineUnavailableException e) {
            throw new IllegalStateException(e);
        }
    }
}
