/*
 * Copyright (c) 2011 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ima;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.io.OutputEngine;


/**
 * ImaOutputEngine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2011/10/16 umjammer initial version <br>
 */
class ImaOutputEngine implements OutputEngine {

    /** */
    private final InputStream in;

    /** */
    private final Ima decoder = new Ima();

    /** */
    private DataOutputStream out;

    private final int samplesPerBlock;
    private final int channels;
    private final int blockSize;
    private final ByteOrder byteOrder;

    private byte[] packet;
    private int[] samples;

    /** */
    public ImaOutputEngine(InputStream in,
                           int samplesPerBlock,
                           int channels,
                           int blockSize,
                           ByteOrder byteOrder) {
        this.in = in;
        this.samplesPerBlock = samplesPerBlock;
        this.channels = channels;
        this.blockSize = blockSize;
        this.byteOrder = byteOrder;
    }

    @Override
    public void initialize(OutputStream out) throws IOException {
        if (this.out != null) {
            throw new IOException("Already initialized");
        } else {
            this.out = new DataOutputStream(out);

            packet = new byte[blockSize];
            samples = new int[channels * samplesPerBlock];
        }
    }

    @Override
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
                    samplesThisBlock = Ima.getSamplesIn(0, channels, l, 0);
                }

                decoder.decodeBlock(channels,
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

    @Override
    public void finish() throws IOException {
        in.close();
    }
}
