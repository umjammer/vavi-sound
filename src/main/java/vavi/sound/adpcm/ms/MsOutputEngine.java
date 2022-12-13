/*
 * Copyright (c) 2011 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ms;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.io.OutputEngine;


/**
 * MsOutputEngine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2011/10/16 umjammer initial version <br>
 */
class MsOutputEngine implements OutputEngine {

    /** */
    private InputStream in;

    /** */
    private Ms decoder = new Ms();

    /** */
    private DataOutputStream out;

    private int samplesPerBlock;
    private int nCoefs;
    private int[][] iCoefs;
    private int channels;
    private int blockSize;
    private ByteOrder byteOrder;

    private byte[] packet;
    private int[] samples;

    /** */
    public MsOutputEngine(InputStream in,
                          int samplesPerBlock,
                          int nCoefs,
                          int[][] iCoefs,
                          int channels,
                          int blockSize,
                          ByteOrder byteOrder) {
        this.in = in;
        this.samplesPerBlock = samplesPerBlock;
        if (nCoefs < 7 || nCoefs > 0x100) {
            throw new IllegalArgumentException("nCoefs " + nCoefs + " makes no sence");
        }
        this.nCoefs = nCoefs;
        this.iCoefs = iCoefs;
        this.channels = channels;
        this.blockSize = blockSize;
        this.byteOrder = byteOrder;
    }

    /* */
    public void initialize(OutputStream out) throws IOException {
        if (this.out != null) {
            throw new IOException("Already initialized");
        } else {
            this.out = new DataOutputStream(out);

            packet = new byte[blockSize];
            samples = new int[channels * samplesPerBlock];
        }
    }

    /* */
    public void execute() throws IOException {
        if (out == null) {
            throw new IOException("Not yet initialized");
        } else {
            int l = in.read(packet);
            if (l < 0) {
                out.close();
            } else {
                int samplesThisBlock = samplesPerBlock;
                if (l < packet.length) {
                    samplesThisBlock = Ms.getSamplesIn(0, channels, l, 0);
                }

                decoder.decodeBlock(channels,
                                    nCoefs,
                                    iCoefs,
                                    packet,
                                    samples,
                                    samplesThisBlock);

               for (int i = 0; i < samplesThisBlock; i++) {
                   if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                       out.writeShort(samples[i]);
                   } else {
                       out.write( samples[i] & 0x00ff);
                       out.write((samples[i] & 0xff00) >> 8);
                   }
               }
            }
        }
    }

    /* */
    public void finish() throws IOException {
        in.close();
    }
}
