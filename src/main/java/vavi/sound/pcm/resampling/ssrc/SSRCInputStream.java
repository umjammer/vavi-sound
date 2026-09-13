/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.ssrc;

import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;


/**
 * SSRC InputStream
 * <p>
 * the conversion runs on the reading thread, a read converts only a chunk of the input.
 * in two pass mode (default) the first read converts the whole input by the 1st pass.
 * </p>
 * <p>
 * formats are little endian linear PCM, 8bit may be signed or unsigned.
 * the output is 8, 16 or 24 bit. the input length is taken from {@link AudioInputStream#getFrameLength()},
 * when it is unknown the stream is converted until its end.
 * </p>
 * <p>
 * properties
 * <li>twopass ... boolean, default true</li>
 * <li>normalize ... boolean, default true</li>
 * <li>dither ... int: {0 ~ 3}</li>
 * <li>pdf ... int: {0 ~ 1}</li>
 * <li>profile ... String: {"standard", "fast"}, default "standard"</li>
 * </p>
 * @author <a href="mailto:vaddvivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030816 nsano initial version <br>
 */
public class SSRCInputStream extends InputStream {

    /** source stream */
    private final InputStream in;

    /** the converter */
    private final SSRC.Converter converter;

    /** receives the result of a pull */
    private final ByteArrayOutputStream produced = new ByteArrayOutputStream();

    /** a channel to {@link #produced} */
    private final WritableByteChannel sink = Channels.newChannel(produced);

    /** converted bytes not read yet */
    private byte[] buffer = new byte[0];

    /** read position in {@link #buffer} */
    private int position;

    /** the converter has finished */
    private boolean eof;

    /** {@link #close()} has been called */
    private boolean closed;

    /** output is signed 8bit, ssrc makes unsigned 8bit */
    private final boolean flipOutput;

    /**
     * @param in format of the source, properties are used as options
     * @param out format of the result, only sample rate and sample size are used
     * @param is source stream
     * @throws IllegalArgumentException the formats are not supported
     */
    public SSRCInputStream(AudioFormat in, AudioFormat out, InputStream is) throws IOException {
        if (in.getChannels() != out.getChannels()) {
            throw new IllegalArgumentException("channels must be same: " + in.getChannels() + ", " + out.getChannels());
        }
        int bps = bytesPerSample(in);
        int dbps = bytesPerSample(out);

        long length = -1;
        if (is instanceof AudioInputStream ais && ais.getFrameLength() != AudioSystem.NOT_SPECIFIED) {
            length = ais.getFrameLength() * bps * in.getChannels();
        }

        this.in = isSigned8bit(in) ? new FilterInputStream(is) {
            @Override
            public int read() throws IOException {
                int c = super.read();
                return c < 0 ? c : c ^ 0x80;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int r = super.read(b, off, len);
                flip(b, off, r);
                return r;
            }
        } : is;
        this.flipOutput = isSigned8bit(out);
        this.converter = new SSRC().new Converter(Channels.newChannel(this.in),
                length,
                in.getChannels(),
                (int) in.getSampleRate(),
                bps,
                (int) out.getSampleRate(),
                dbps,
                in.properties());
    }

    /** @throws IllegalArgumentException not supported */
    private static int bytesPerSample(AudioFormat format) {
        AudioFormat.Encoding encoding = format.getEncoding();
        int bits = format.getSampleSizeInBits();
        if (bits % 8 != 0 || bits < 8 || bits > 32) {
            throw new IllegalArgumentException("unsupported sample size: " + format);
        }
        if (bits == 8) {
            if (!AudioFormat.Encoding.PCM_SIGNED.equals(encoding) && !AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)) {
                throw new IllegalArgumentException("unsupported encoding: " + format);
            }
        } else {
            if (!AudioFormat.Encoding.PCM_SIGNED.equals(encoding) || format.isBigEndian()) {
                throw new IllegalArgumentException("only signed little endian PCM is supported: " + format);
            }
        }
        return bits / 8;
    }

    /** */
    private static boolean isSigned8bit(AudioFormat format) {
        return format.getSampleSizeInBits() == 8 && AudioFormat.Encoding.PCM_SIGNED.equals(format.getEncoding());
    }

    /** signed 8bit <-> unsigned 8bit */
    private static void flip(byte[] b, int off, int len) {
        for (int i = 0; i < len; i++) {
            b[off + i] ^= (byte) 0x80;
        }
    }

    /** @return false when no more data */
    private boolean fill() throws IOException {
        ensureOpen();
        while (position == buffer.length) {
            if (eof) {
                return false;
            }
            produced.reset();
            eof = !converter.pull(sink);
            buffer = produced.toByteArray();
            position = 0;
            if (flipOutput) {
                flip(buffer, 0, buffer.length);
            }
        }
        return true;
    }

    @Override
    public int read() throws IOException {
        if (!fill()) {
            return -1;
        }
        return buffer[position++] & 0xff;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        if (!fill()) {
            return -1;
        }
        int l = Math.min(len, buffer.length - position);
        System.arraycopy(buffer, position, b, off, l);
        position += l;
        return l;
    }

    @Override
    public int available() throws IOException {
        ensureOpen();
        return buffer.length - position;
    }

    /** @throws IOException closed */
    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * deletes the temporary file of the two pass mode and closes the source stream,
     * same as other format conversion streams do.
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        buffer = new byte[0];
        try {
            converter.close();
        } finally {
            in.close();
        }
    }
}
