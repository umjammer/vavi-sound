/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ima;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import vavi.io.LittleEndianDataInputStream;
import vavi.util.Debug;
import vavi.util.win32.WAVE;


/**
 * IMA InputStream
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030816 nsano initial version <br>
 */
public class ImaInputStream extends FilterInputStream {

    /** このクラスで取得するストリームのバイトオーダー */
    private ByteOrder byteOrder;

    /**
     * バイトオーダーは little endian
     */
    public ImaInputStream(InputStream in,
                          int samplesPerBlock,
                          int channels,
                          int blockSize)
        throws IOException {

        this(in,
             samplesPerBlock,
             channels,
             blockSize,
             ByteOrder.BIG_ENDIAN);
    }

    /**
     */
    public ImaInputStream(InputStream in,
                          final int samplesPerBlock,
                          final int channels,
                          int blockSize,
                          final ByteOrder byteOrder)
        throws IOException {

        super(new PipedInputStream());

        this.byteOrder = byteOrder;
Debug.println("byteOrder: " + this.byteOrder);

//Debug.println("samplesPerBlock: " + samplesPerBlock);
//Debug.println("channels: " + channels);
//Debug.println("blockSize: " + blockSize);

        //

        final Ima decoder = new Ima();

        final InputStream is = new BufferedInputStream(in);

        int bytesPerBlock =
            decoder.getBytesPerBlock(channels, samplesPerBlock);
Debug.println("bytesPerBlock: " + bytesPerBlock);
        final byte[] packet = new byte[blockSize];
        final int[] samples = new int[channels * samplesPerBlock];
        int bytesPerSample = 2;
        int numSamples = decoder.getSamplesIn(is.available(),
                                              channels,
                                              blockSize,
                                              samplesPerBlock);
//Debug.println("numSamples: " + numSamples);
        final int length = numSamples * channels;
        this.available = numSamples * channels * bytesPerSample;
//Debug.println("available: " + available);

        //

        final PipedOutputStream pos =
            new PipedOutputStream((PipedInputStream) this.in);

        Thread thread = new Thread(new Runnable() {
            /** */
            public void run() {

                DataOutputStream os = null;

                try {
                    // big endian
                    os = new DataOutputStream(pos);

                    int done = 0;
                    while (done < length) {

                        int l = 0;
                        while (l < packet.length && is.available() > 0) {
                            l += is.read(packet, l, packet.length - l);
                        }

                        int samplesThisBlock = samplesPerBlock;
                        if (l < packet.length) {
                            samplesThisBlock =
                                decoder.getSamplesIn(0, channels, l, 0);
                        }
//Debug.println("samplesThisBlock: " + samplesThisBlock + ", " + l);

                        decoder.decodeBlock(channels,
                                             packet,
                                             samples,
                                             samplesThisBlock);

                        for (int i = 0; i < samplesThisBlock; i++) {
                            if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                                os.writeShort(samples[i]);
                            } else {
                                os.write( samples[i] & 0x00ff);
                                os.write((samples[i] & 0xff00) >> 8);
                            }
                        }
                        done += samplesThisBlock;
//Debug.println("done: " + done);
                    }
                } catch (IOException e) {
Debug.printStackTrace(e);
                } finally {
                    try {
                        os.flush();
                        os.close();
                    } catch (IOException e) {
Debug.println(e);
                    }
                }
            }
        });

        thread.start();
    }

    /** */
    private int available;

    /** */
    public int available() throws IOException {
        return available;
    }

    /**
     */
    public int read() throws IOException {
        available--;
        return in.read();
    }

    /** */
    public int read(byte b[], int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException("byte[]");
        } else if ((off < 0) || (off > b.length) || (len < 0) ||
                 ((off + len) > b.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException("off: " + off + ", len: " + len);
        } else if (len == 0) {
            return 0;
        }

        int c = read();
        if (c == -1) {
            return -1;
        }
        b[off] = (byte) c;

        int i = 1;
        try {
            for (; i < len ; i++) {
                c = read();
                if (c == -1) {
                    break;
                }
                if (b != null) {
                    b[off + i] = (byte) c;
                }
            }
        } catch (IOException e) {
e.printStackTrace(System.err);
        }
        return i;
    }


    //-------------------------------------------------------------------------

    /**
     * Play .wav in IMA ADPCM.
     *
     * <pre>
     * IMA wave ext
     *  2 bytes
     *  samplesPerBlock (little endian)
     * </pre>
     * @param args 0:ima wave, 1:output pcm, 2:test or not, use "test"
     */
    public static void main(String[] args) throws Exception {

        final boolean isTest = args[2].equals("test");
        InputStream in = new BufferedInputStream(new FileInputStream(args[0]));
        WAVE wave = (WAVE) WAVE.readFrom(in);
        in.close();
        WAVE.fmt format = (WAVE.fmt) wave.findChildOf(WAVE.fmt.class);
        if (format.getFormatId() != 0x0011) {
            throw new IllegalArgumentException("not Intel DVI/IMA ADPCM");
        }
        LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(
            new ByteArrayInputStream(format.getExtended()));
Debug.println("ext size: " + ledis.available());
        int samplesPerBlock = ledis.readShort();
        WAVE.data data = (WAVE.data) wave.findChildOf(WAVE.data.class);
        in = new ByteArrayInputStream(data.getWave());
Debug.println("wave: " + in.available());

        //----

        int sampleRate = format.getSamplingRate();
        ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

        AudioFormat audioFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            byteOrder.equals(ByteOrder.BIG_ENDIAN));
System.err.println(audioFormat);

        InputStream is = new ImaInputStream(in,
                                            samplesPerBlock,
                                            format.getNumberChannels(),
                                            format.getBlockSize(),
                                            byteOrder);
OutputStream os =
 new BufferedOutputStream(new FileOutputStream(args[1]));

        int bufferSize = format.getBlockSize();

        DataLine.Info info =
            new DataLine.Info(SourceDataLine.class, audioFormat);
        SourceDataLine line =
            (SourceDataLine) AudioSystem.getLine(info);
        line.open(audioFormat);
        line.addLineListener(new LineListener() {
            public void update(LineEvent ev) {
Debug.println(ev.getType());
        		if (LineEvent.Type.STOP == ev.getType()) {
                    if (!isTest) {
                        System.exit(0);
                    }
        		}
            }
        });
        line.start();
        byte[] buf = new byte[bufferSize];
        int l = 0;
FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
double gain = .2d; // number between 0 and 1 (loudest)
float dB = (float) (Math.log(gain) / Math.log(10.0) * 20.0);
gainControl.setValue(dB);

        while (is.available() > 0) {
            l = is.read(buf, 0, bufferSize);
            line.write(buf, 0, l);
os.write(buf, 0, l);
        }
        line.drain();
        line.stop();
        line.close();
os.close();
    }
}

/* */
