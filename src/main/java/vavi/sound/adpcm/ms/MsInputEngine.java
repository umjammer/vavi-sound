/*
 * Copyright (c) 2011 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ms;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.io.IOStreamInputEngine;
import vavi.io.InputEngine;
import vavi.io.LittleEndianDataInputStream;
import vavi.util.Debug;


/**
 * MsInputEngine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2011/10/18 umjammer initial version <br>
 */
class MsInputEngine implements InputEngine {

    private OutputStream out;

    private Ms encoder = new Ms();

    private InputStream in;

    private int samplesPerBlock;
    private int channels;
    private ByteOrder byteOrder;

    /** */
    public MsInputEngine(OutputStream out,
                         int samplesPerBlock,
                         int channels,
                         final ByteOrder byteOrder) {
        this.out = out;
        this.samplesPerBlock = samplesPerBlock;
        this.channels = channels;
        this.byteOrder = byteOrder;
Debug.println("byteOrder: " + this.byteOrder);
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
     * {@link IOStreamInputEngine#IOStreamInputEngine(OutputStream, vavi.io.IOStreamInputEngine.InputStreamFactory, int)
     * の bufferSize を {@link #samplesPerBlock} * 2 にすること。
     */
    public void execute() throws IOException {
        if (in == null) {
            throw new IOException("Not yet initialized");
        } else {
            int bytesPerBlock = Ms.getBytesPerBlock(channels, samplesPerBlock);
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
//System.err.println(StringUtil.getDump(buffer, 128));
            if (l > 0) {
                byte[] adpcm = new byte[bytesPerBlock];
                int[] pcm = new int[l / 2];
//Debug.println("adpcm: " + bytesPerBlock  + ", pcm: " + pcm.length + ", " + l);
                LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(new ByteArrayInputStream(buffer));
                for (int i = 0; i < pcm.length; i++) {
                    pcm[i] = ledis.readShort();
                }
                encoder.encodeBlock(1, pcm, pcm.length, steps, adpcm, bytesPerBlock);
//System.err.println(StringUtil.getDump(adpcm, 128));
    
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
