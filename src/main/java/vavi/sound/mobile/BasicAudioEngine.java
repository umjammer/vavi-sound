/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import vavi.util.Debug;


/**
 * Abstract AudioEngine.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020903 nsano initial version <br>
 */
public abstract class BasicAudioEngine implements AudioEngine {

    /** */
    protected Data[] datum;

    /* */
    public void setData(int streamNumber,
                        int channel,
                        int sampleRate,
                        int bits,
                        int channels,
                        byte[] adpcm, boolean continued) {
        Data data;
        if (datum[streamNumber] == null) {
            data = new Data();
            data.channel = channel;
            data.sampleRate = sampleRate;
            data.bits = bits;
            data.channels = channels;
        } else {
            data = datum[streamNumber];
        }
        if (data.continued) {
            byte[] temp = new byte[data.adpcm.length + adpcm.length];
            System.arraycopy(data.adpcm, 0, temp, 0, data.adpcm.length);
            System.arraycopy(adpcm, 0, temp, data.adpcm.length, adpcm.length);
            data.adpcm = temp;
        } else {
            data.adpcm = adpcm;
        }
        data.continued = continued;
        datum[streamNumber] = data;

try {
 OutputStream os = null;
 if (fileName != null) {
Debug.println("šššššššš adpcm out to file: " + fileName);
  os = new BufferedOutputStream(new FileOutputStream(fileName, true));
  os.write(adpcm, 0, adpcm.length);
  os.flush();
  os.close();
 }
} catch (IOException e) {
Debug.printStackTrace(e);
}
    }

    /** */
    public void stop(int streamNumber) {
    }

    /** */
    protected abstract int getChannels(int streamNumber);

    /** */
    protected abstract InputStream[] getInputStreams(int streamNumber, int channels);

    /** */
    public void start(int streamNumber) {

        int channels = getChannels(streamNumber);
        if (channels == -1) {
Debug.println("always used: no: " + streamNumber + ", ch: " + datum[streamNumber].channel);
            return;
        }

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            datum[streamNumber].sampleRate,
            16,
            channels,
            2 * channels,
            datum[streamNumber].sampleRate,
            false);
Debug.println(audioFormat);

        try {

//Debug.println(data.length);
            InputStream[] iss = getInputStreams(streamNumber, channels);

//Debug.println("is: " + is.available());
OutputStream os = null;
if (pcmFileName != null) {
Debug.println("šššššššš output PCM to file: " + pcmFileName);
 os = new BufferedOutputStream(new FileOutputStream(pcmFileName));
}

            DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(audioFormat);
            line.start();
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);
            byte[] buf = new byte[1024];
            while (iss[0].available() > 0) {
                if (channels == 1) {
                    int l = iss[0].read(buf, 0, 1024);
//Debug.dump(buf, 64);
                    line.write(buf, 0, l);
if (os != null) {
 os.write(buf, 0, l);
}
                } else {
                    int lL = iss[0].read(buf, 0, 512);
                    /*int lR = */iss[1].read(buf, 512, 512);
//System.err.println("l : " + lL + ", r: " + lR);
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
if (os != null) {
 os.flush();
 os.close();
}
        } catch (IOException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        } catch (LineUnavailableException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }

    //-------------------------------------------------------------------------

    /** */
    protected abstract OutputStream getOutputStream(OutputStream os);

    /* */
    public byte[] encode(int bits, int channels, byte[] pcm) {
        try {
            if (channels == 1) {
                // monaural
                InputStream is = new ByteArrayInputStream(pcm);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                OutputStream os = getOutputStream(baos);
Debug.println("pcm length: " + is.available());
                while (is.available() > 0) {
                    int c = is.read();
                    if (c == -1) {
Debug.println("read returns -1");
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
Debug.println("pcm L length: " + is.available());
                while (is.available() > 0) {
                    int c = is.read();
                    if (c == -1) {
Debug.println("Illegal EOF L: " + is.available());
                        break;
                    }
                    os.write(c);
                }
                byte[] monoL = baos.toByteArray();
                // R
                is = new ByteArrayInputStream(monos[1]);
                baos = new ByteArrayOutputStream();
                os = getOutputStream(baos);
Debug.println("pcm R length: " + is.available());
                while (is.available() > 0) {
                    int c = is.read();
                    if (c == -1) {
Debug.println("Illegal EOF R: " + is.available());
                        break;
                    }
                    os.write(c);
                }
                byte[] monoR = baos.toByteArray();
                return Util.concatenate(monoL, monoR);
            }
        } catch (IOException e) {
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }

    //-------------------------------------------------------------------------

    /** */
    private static String fileName;

    /** */
    private static String pcmFileName;

    /**
     * Tests this class.
     *
     * usage: java $0 mfi_file adpcm
     */
    public static void main(String[] args) throws Exception {

        if (args.length >= 2) {
            fileName = args[1];
        }
        if (args.length >= 3) {
            pcmFileName = args[2];
        }

        vavi.sound.mfi.Sequencer sequencer = vavi.sound.mfi.MfiSystem.getSequencer();
        sequencer.open();
        vavi.sound.mfi.Sequence sequence = vavi.sound.mfi.MfiSystem.getSequence(new File(args[0]));
        sequencer.setSequence(sequence);
        sequencer.addMetaEventListener(new vavi.sound.mfi.MetaEventListener() {
            public void meta(vavi.sound.mfi.MetaMessage meta) {
Debug.println(meta.getType());
                if (meta.getType() == 47) {
                    System.exit(0);
                }
            }
        });
        sequencer.start();
    }
}

/* */
