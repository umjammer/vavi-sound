/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm;

import java.io.InputStream;


/**
 * AdpcmInputStreamFactory.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-09-05 nsano initial version <br>
 */
public interface AdpcmInputStreamFactory {

    /** */
    AdpcmInputStream factory(InputStream in);
}
