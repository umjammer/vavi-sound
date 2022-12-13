/*
 * Copyright (c) 2011 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ima;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.logging.Level;

import vavi.io.InputEngine;
import vavi.io.LittleEndianDataInputStream;
import vavi.util.Debug;

import vavix.io.IOStreamInputEngine;


/**
 * ImaInputEngine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2011/10/18 umjammer initial version <br>
 */
class ImaInputEngine implements InputEngine {

    private OutputStream out;

    private Ima encoder = new Ima();

    private InputStream in;

    private int samplesPerBlock;
    private int channels;
    private ByteOrder byteOrder;

    /** */
    public ImaInputEngine(OutputStream out,
                          int samplesPerBlock,
                          int channels,
                          ByteOrder byteOrder) {
        this.out = out;
        this.samplesPerBlock = samplesPerBlock;
        this.channels = channels;
        this.byteOrder = byteOrder;
Debug.println(Level.FINE, "byteOrder: " + this.byteOrder);
    }

    /* */
    public void initialize(InputStream in) throws IOException {
        if (this.in != null) {
            throw new IOException("Already initialized");
        } else {
            this.in = in;
        }
    }

    /** */
    private int[] steps = new int[16];

    /**
     * {@link #in} が {@link #samplesPerBlock} * 2 しか読まないので {@link IOStreamInputEngine} の
     * {@link IOStreamInputEngine#IOStreamInputEngine(OutputStream, vavix.io.IOStreamInputEngine.InputStreamFactory, int)}
     * の bufferSize を {@link #samplesPerBlock} * 2 にすること。
     */
    public void execute() throws IOException {
        if (in == null) {
            throw new IOException("Not yet initialized");
        } else {
            int bytesPerBlock = Ima.getBytesPerBlock(channels, samplesPerBlock);
//System.err.println("bytesPerBlock: " + bytesPerBlock + ", samplesPerBlock: " + samplesPerBlock);
            byte[] buffer = new byte[samplesPerBlock * 2];
            int l = 0;
            while (l < buffer.length) {
                int r = in.read(buffer, l, buffer.length - l);
                if (r < 0) {
                    break;
                }
                l += r;
            }
            if (l > 0) {
                byte[] adpcm = new byte[bytesPerBlock];
                int[] pcm = new int[l / 2];
//Debug.println("adpcm: " + bytesPerBlock  + ", pcm: " + pcm.length + ", " + l);
                LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(new ByteArrayInputStream(buffer));
                for (int i = 0; i < pcm.length; i++) {
                    pcm[i] = ledis.readShort();
                }
                ledis.close();
                encoder.encodeBlock(1, pcm, pcm.length, steps, adpcm, 9);

//Debug.println("adpcm: " + adpcm.length);
                out.write(adpcm);
            }
        }
    }

    /* */
    public void finish() throws IOException {
        out.flush();
        out.close();
    }
}

/* */
