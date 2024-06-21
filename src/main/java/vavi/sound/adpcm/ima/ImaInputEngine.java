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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteOrder;

import vavi.io.InputEngine;
import vavi.io.LittleEndianDataInputStream;
import vavix.io.IOStreamInputEngine;

import static java.lang.System.getLogger;


/**
 * ImaInputEngine.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2011/10/18 umjammer initial version <br>
 */
class ImaInputEngine implements InputEngine {

    private static final Logger logger = getLogger(ImaInputEngine.class.getName());

    private final OutputStream out;

    private final Ima encoder = new Ima();

    private InputStream in;

    private final int samplesPerBlock;
    private final int channels;
    private final ByteOrder byteOrder;

    /** */
    public ImaInputEngine(OutputStream out,
                          int samplesPerBlock,
                          int channels,
                          ByteOrder byteOrder) {
        this.out = out;
        this.samplesPerBlock = samplesPerBlock;
        this.channels = channels;
        this.byteOrder = byteOrder;
logger.log(Level.DEBUG, "byteOrder: " + this.byteOrder);
    }

    @Override
    public void initialize(InputStream in) throws IOException {
        if (this.in != null) {
            throw new IOException("Already initialized");
        } else {
            this.in = in;
        }
    }

    /** */
    private final int[] steps = new int[16];

    /**
     * because {@link #in} reads only {@link #samplesPerBlock} * 2 bytes, bufferSize for
     * {@link IOStreamInputEngine#IOStreamInputEngine(OutputStream, vavix.io.IOStreamInputEngine.InputStreamFactory, int)}
     * of {@link IOStreamInputEngine} must be set {@link #samplesPerBlock} * 2.
     */
    @Override
    public void execute() throws IOException {
        if (in == null) {
            throw new IOException("Not yet initialized");
        } else {
            int bytesPerBlock = Ima.getBytesPerBlock(channels, samplesPerBlock);
//logger.log(Level.DEBUG, "bytesPerBlock: " + bytesPerBlock + ", samplesPerBlock: " + samplesPerBlock);
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
//logger.log(Level.DEBUG, "adpcm: " + bytesPerBlock  + ", pcm: " + pcm.length + ", " + l);
                LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(new ByteArrayInputStream(buffer));
                for (int i = 0; i < pcm.length; i++) {
                    pcm[i] = ledis.readShort();
                }
                ledis.close();
                encoder.encodeBlock(1, pcm, pcm.length, steps, adpcm, 9);

//logger.log(Level.DEBUG, "adpcm: " + adpcm.length);
                out.write(adpcm);
            }
        }
    }

    @Override
    public void finish() throws IOException {
        out.flush();
        out.close();
    }
}
