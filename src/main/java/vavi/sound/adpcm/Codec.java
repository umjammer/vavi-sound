/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm;


/**
 * Codec. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 060427 nsano initial version <br>
 */
public interface Codec {
    /** @return adpcm */
    int encode(int pcm);

    /** @return pcm */
    int decode(int adpcm);
}

/* */
