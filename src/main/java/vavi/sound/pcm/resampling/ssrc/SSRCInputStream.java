/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.pcm.resampling.ssrc;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.sound.sampled.AudioFormat;

import static java.lang.System.getLogger;


/**
 * SSRC InputStream
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
public class SSRCInputStream extends FilterInputStream {

    private static final Logger logger = getLogger(SSRCInputStream.class.getName());

    /** use in properties */
    public SSRCInputStream(AudioFormat in, AudioFormat out, InputStream is) throws IOException {

        super(init(is,
                   in.getChannels(),
                   (int) in.getSampleRate(),
                   in.getFrameSize() / in.getChannels(),
                   (int) out.getSampleRate(),
                   in.getFrameSize() / in.getChannels(),
                   in.properties()));
    }

    private static final ExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * @param in source stream
     * @param ch number of channels
     * @param iFrq input frequency
     * @param iBps input bytes per sample
     * @param oFrq output frequency
     * @param oBps output bytes per second
     * @return resampled stream
     */
    private static InputStream init(InputStream in, int ch, int iFrq, int iBps, int oFrq, int oBps,
                                    Map<String, Object> props) throws IOException {
        Pipe pipe = Pipe.open();
        pipe.sink().configureBlocking(true);
        executorService.submit(() -> {
            try {
                SSRC ssrc = new SSRC();
                ssrc.io(Channels.newChannel(in), pipe.sink(), in.available(), ch, iFrq, iBps, oFrq, oBps, props);
                pipe.sink().close();
            } catch (IOException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        });
        return Channels.newInputStream(pipe.source());
    }
}
