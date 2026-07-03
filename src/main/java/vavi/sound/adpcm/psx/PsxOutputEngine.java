/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.psx;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.io.OutputEngine;


/**
 * PsxOutputEngine.
 * <p>
 * Decodes a headerless PS-ADPCM (SPU-ADPCM) stream into 16 bit PCM.
 * The stream is laid out in interleave block sets: {@code [ch0 block][ch1 block]...},
 * each block holding {@code interleaveBlockSize / 0x10} frames of 28 samples.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-03 nsano initial version <br>
 */
class PsxOutputEngine implements OutputEngine {

    /** frame is 0x10 bytes */
    private static final int BYTES_PER_FRAME = 0x10;

    /** frame decodes to 28 samples */
    private static final int SAMPLES_PER_FRAME = (BYTES_PER_FRAME - 0x02) * 2;

    /** */
    private final InputStream in;

    /** */
    private DataOutputStream out;

    private final int channels;
    private final int interleaveBlockSize;
    private final ByteOrder byteOrder;

    /** one interleave block set: channels * interleaveBlockSize */
    private byte[] packet;
    /** per channel frame output */
    private short[][] samples;
    /** per channel decoder state {hist1, hist2} */
    private int[][] hist;

    /** */
    public PsxOutputEngine(InputStream in, int channels, int interleaveBlockSize, ByteOrder byteOrder) {
        if (channels <= 0) {
            throw new IllegalArgumentException("channels " + channels + " makes no sense");
        }
        if (interleaveBlockSize < BYTES_PER_FRAME || interleaveBlockSize % BYTES_PER_FRAME != 0) {
            throw new IllegalArgumentException("interleaveBlockSize " + interleaveBlockSize + " makes no sense");
        }
        this.in = in;
        this.channels = channels;
        this.interleaveBlockSize = interleaveBlockSize;
        this.byteOrder = byteOrder;
    }

    @Override
    public void initialize(OutputStream out) throws IOException {
        if (this.out != null) {
            throw new IOException("Already initialized");
        } else {
            this.out = new DataOutputStream(out);

            packet = new byte[channels * interleaveBlockSize];
            samples = new short[channels][SAMPLES_PER_FRAME];
            hist = new int[channels][2];
        }
    }

    @Override
    public void execute() throws IOException {
        if (out == null) {
            throw new IOException("Not yet initialized");
        } else {
            int l = in.readNBytes(packet, 0, packet.length);
            if (l <= 0) {
                out.close();
                return;
            }

            // a trailing partial block set: only frame rows complete for all channels
            int frames = interleaveBlockSize / BYTES_PER_FRAME;
            if (l < packet.length) {
                frames = Math.min(frames, Math.max(l - (channels - 1) * interleaveBlockSize, 0) / BYTES_PER_FRAME);
                if (frames == 0) {
                    out.close();
                    return;
                }
            }

            for (int f = 0; f < frames; f++) {
                for (int ch = 0; ch < channels; ch++) {
                    Psx.decodePsxFrame(packet, ch * interleaveBlockSize + f * BYTES_PER_FRAME,
                            samples[ch], 0, 1, 0, SAMPLES_PER_FRAME, false, false, hist[ch]);
                }
                for (int i = 0; i < SAMPLES_PER_FRAME; i++) {
                    for (int ch = 0; ch < channels; ch++) {
                        if (ByteOrder.BIG_ENDIAN.equals(byteOrder)) {
                            out.writeShort(samples[ch][i]);
                        } else {
                            out.write( samples[ch][i] & 0x00ff);
                            out.write((samples[ch][i] & 0xff00) >> 8);
                        }
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
