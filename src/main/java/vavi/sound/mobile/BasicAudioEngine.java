/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;


/**
 * Abstract AudioEngine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020903 nsano initial version <br>
 */
public abstract class BasicAudioEngine implements AudioEngine {

    private static final Logger logger = getLogger(BasicAudioEngine.class.getName());

    /** */
    protected Data[] data;

    @Override
    public void setData(int streamNumber,
                        int channel,
                        int sampleRate,
                        int bits,
                        int channels,
                        byte[] adpcm, boolean continued) {
        Data datum;
        if (this.data[streamNumber] == null) {
            datum = new Data();
            datum.channel = channel;
            datum.sampleRate = sampleRate;
            datum.bits = bits;
            datum.channels = channels;
        } else {
            datum = this.data[streamNumber];
        }
        if (datum.continued) {
            byte[] temp = new byte[datum.adpcm.length + adpcm.length];
            System.arraycopy(datum.adpcm, 0, temp, 0, datum.adpcm.length);
            System.arraycopy(adpcm, 0, temp, datum.adpcm.length, adpcm.length);
            datum.adpcm = temp;
        } else {
            datum.adpcm = adpcm;
        }
        datum.continued = continued;
        this.data[streamNumber] = datum;
//debug1();
    }

    @Override
    public void stop(int streamNumber) {
    }

    /** */
    protected abstract int getChannels(int streamNumber);

    /** */
    protected abstract InputStream[] getInputStreams(int streamNumber, int channels);

    @Override
    public void start(int streamNumber) {

        int channels = getChannels(streamNumber);
        if (channels == -1) {
logger.log(Level.INFO, "always used: no: " + streamNumber + ", ch: " + this.data[streamNumber].channel);
            return;
        }

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            this.data[streamNumber].sampleRate,
            16,
            channels,
            2 * channels,
            this.data[streamNumber].sampleRate,
            false);
logger.log(Level.DEBUG, audioFormat);

        try {

//logger.log(Level.TRACE, data.length);
            InputStream[] iss = getInputStreams(streamNumber, channels);

//logger.log(Level.TRACE, "is: " + is.available());
//OutputStream os = debug2();

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();
            volume(line, volume);
            byte[] buf = new byte[1024];
            while (iss[0].available() > 0) {
                if (channels == 1) {
                    int l = iss[0].read(buf, 0, 1024);
logger.log(Level.TRACE, "data:\n" + StringUtil.getDump(buf, 64));
                    line.write(buf, 0, l);
//debug3(os);
                } else {
                    int lL = iss[0].read(buf, 0, 512);
                    int lR = iss[1].read(buf, 512, 512);
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
//debug4(os);
        } catch (IOException | LineUnavailableException e) {
            throw new IllegalStateException(e);
        }
    }

    // ----

    /** */
    protected abstract OutputStream getOutputStream(OutputStream os);

    @Override
    public byte[] encode(int bits, int channels, byte[] pcm) {
        try {
            if (channels == 1) {
                // monaural
                InputStream is = new ByteArrayInputStream(pcm);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStream os = getOutputStream(baos);
logger.log(Level.DEBUG, "pcm length: " + is.available());
                while (is.available() > 0) {
                    int c = is.read();
                    if (c == -1) {
logger.log(Level.DEBUG, "read returns -1");
                        break;
                    }
                    os.write(c);
                }

                return baos.toByteArray();
            } else {
                // stereo
                byte[][] monos = Util.toMono(pcm, 16, ByteOrder.LITTLE_ENDIAN);
                // L
                InputStream is = new ByteArrayInputStream(monos[0]);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStream os = getOutputStream(baos);
logger.log(Level.DEBUG, "pcm L length: " + is.available());
                while (is.available() > 0) {
                    int c = is.read();
                    if (c == -1) {
logger.log(Level.DEBUG, "Illegal EOF L: " + is.available());
                        break;
                    }
                    os.write(c);
                }
                byte[] monoL = baos.toByteArray();
                // R
                is = new ByteArrayInputStream(monos[1]);
                baos = new ByteArrayOutputStream();
                os = getOutputStream(baos);
logger.log(Level.DEBUG, "pcm R length: " + is.available());
                while (is.available() > 0) {
                    int c = is.read();
                    if (c == -1) {
logger.log(Level.DEBUG, "Illegal EOF R: " + is.available());
                        break;
                    }
                    os.write(c);
                }
                byte[] monoR = baos.toByteArray();
                return Util.concatenate(monoL, monoR);
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
