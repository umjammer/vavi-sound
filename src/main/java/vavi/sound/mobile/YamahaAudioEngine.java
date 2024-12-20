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
import java.nio.ByteOrder;

import vavi.sound.adpcm.ma.MaInputStream;
import vavi.sound.adpcm.ma.MaOutputStream;

import static java.lang.System.getLogger;


/**
 * YAMAHA AudioEngine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020829 nsano initial version <br>
 */
public class YamahaAudioEngine extends BasicAudioEngine {

    private static final Logger logger = getLogger(YamahaAudioEngine.class.getName());

    /**
     * <pre>
     *  L0 + L2 + ...
     *  R1 + R3 + ...
     * </pre>
     */
    public YamahaAudioEngine() {
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
            iss[0] = new MaInputStream(in, ByteOrder.LITTLE_ENDIAN);
            if (channels != 1) {
                InputStream inR = new ByteArrayInputStream(data[streamNumber + 1].adpcm);
                iss[1] = new MaInputStream(inR, ByteOrder.LITTLE_ENDIAN);
            }
        } else {
            InputStream in = new ByteArrayInputStream(data[streamNumber].adpcm, 0, data[streamNumber].adpcm.length / 2);
            iss[0] = new MaInputStream(in, ByteOrder.LITTLE_ENDIAN);
            InputStream inR = new ByteArrayInputStream(data[streamNumber].adpcm, data[streamNumber].adpcm.length / 2, data[streamNumber].adpcm.length / 2);
            iss[1] = new MaInputStream(inR, ByteOrder.LITTLE_ENDIAN);
        }
        return iss;
    }

    // ----

    @Override
    protected OutputStream getOutputStream(OutputStream os) {
        return new MaOutputStream(os, ByteOrder.LITTLE_ENDIAN);
    }
}
