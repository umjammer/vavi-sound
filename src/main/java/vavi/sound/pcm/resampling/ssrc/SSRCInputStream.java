/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.ssrc;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.sound.sampled.AudioFormat;

import vavix.io.AdvancedPipedInputStream;


/**
 * SSRC InputStream
 * 
 * @author <a href="mailto:vaddvivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030816 nsano initial version <br>
 */
public class SSRCInputStream extends FilterInputStream {

    /** TODO parameter from {@link AudioFormat#properties()} */
    public SSRCInputStream(AudioFormat in, AudioFormat out, InputStream is) throws IOException {

        super(init(is,
                   in.getChannels(),
                   (int) in.getSampleRate(),
                   in.getFrameSize() / in.getChannels(),
                   (int) out.getSampleRate(),
                   in.getFrameSize() / in.getChannels()));
    }

    /**
     * TODO pipe
     *
     * @param in source stream
     * @param ch number of channels
     * @param ifrq input frequency
     * @param ibps input bits per sample
     * @param ofrq output frequency
     * @param obps output bits per second
     * @return resampled stream
     */
    static InputStream init2(InputStream in, int ch, int ifrq, int ibps, int ofrq, int obps) throws IOException {
        Pipe pipe = Pipe.open();
        pipe.sink().configureBlocking(false);
//        new Thread() {
//            public void run() {
//                try {
                    SSRC ssrc = new SSRC();
                    ssrc.io(Channels.newChannel(in), pipe.sink(), in.available(), ch, ifrq, ibps, ofrq, obps, true, true);
//                    pipe.sink().close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }.start();
        return Channels.newInputStream(pipe.source());
    }

    /**
     * 
     * @param in source stream
     * @param ch number of channels
     * @param ifrq input frequency
     * @param ibps input bits per sample
     * @param ofrq output frequency
     * @param obps output bits per second
     * @return resampled stream
     */
    static InputStream init(InputStream in, int ch, int ifrq, int ibps, int ofrq, int obps) {
        AdvancedPipedInputStream source = new AdvancedPipedInputStream();
        final AdvancedPipedInputStream.OutputStreamEx sink = source.getOutputStream();
        new Thread() {
            public void run() {
                try {
                    SSRC ssrc = new SSRC();
                    ReadableByteChannel rbc = Channels.newChannel(in);
                    WritableByteChannel wbc = Channels.newChannel(sink);
                    ssrc.io(rbc, wbc, in.available(), ch, ifrq, ibps, ofrq, obps, true, true);
                    sink.close();
                } catch (IOException ex) {
                    try {
                        sink.setException(ex);
                    } catch (IOException ignored) {
                    }
                }
            }
        }.start();
        return source;
    }
}

/* */
