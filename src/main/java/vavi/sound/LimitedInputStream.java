/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;


/**
 * MsWaveAudioFileReaderTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 240330 nsano initial version <br>
 */
public class LimitedInputStream extends FilterInputStream {

    private static final Logger logger = Logger.getLogger(LimitedInputStream.class.getName());

    public static final String ERROR_MESSAGE_REACHED_TO_LIMIT = "stop reading, prevent form eof";

    private final int limit;

    public LimitedInputStream(InputStream in) throws IOException {
        this(in, in.available());
    }

    public LimitedInputStream(InputStream in, int limit) throws IOException {
        super(in);
        this.limit = limit;
logger.finer("limit: " + limit);
    }

    private void check(int r) throws IOException {
        if (in.available() < r) {
logger.fine("reached to limit");
            throw new IOException(ERROR_MESSAGE_REACHED_TO_LIMIT);
        }
    }

    @Override
    public int read() throws IOException {
        check(1);
        return super.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        check(b.length);
        return super.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        check(len);
        return super.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        check((int) n);
        return super.skip(n);
    }
}
