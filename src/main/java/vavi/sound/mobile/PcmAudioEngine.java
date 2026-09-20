/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.sampled.AudioFormat;

import static java.lang.System.getLogger;


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
     *  from 240_2, a channel pair, the even channel L and the odd one R
     *   0 L
     *   1 R
     *  from 1_240_7, "adat", SMAF wave, one stream of its own, mono or L + R in it
     *   0 L + R
     * </pre>
     */
    public PcmAudioEngine() {
        data = new Data[32];
    }

    @Override
    public boolean accept(int format) {
        return format == 0x90; // TODO
    }

    @Override
    protected int getChannels(int streamNumber) {
        int channels = 1;
        if (data[streamNumber].channel == -1) {
            // from 1_240_7, an "adat" chunk or a SMAF wave: the message says itself how many
            // channels the stream has, the streams beside it are streams of their own and not
            // the other half of this one. pairing them by the parity of the stream number plays
            // the one before beside this one and never plays the one before by itself.
            channels = data[streamNumber].channels;
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

    @Override
    protected AudioFormat getAudioFormat(int sampleRate, int channels) {
        return new AudioFormat(
                AudioFormat.Encoding.PCM_UNSIGNED,
                sampleRate,
                8,
                channels,
                1 * channels,
                sampleRate,
                false);
    }

    // ----

    @Override
    protected OutputStream getOutputStream(OutputStream os) {
        return os;
    }
}
