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
    /**
     * @param pcm 16bit 
     * @return adpcm 4bit
     */
    int encode(int pcm);

    /**
     * @param adpcm 4bit 
     * @return pcm 16bit
     */
    int decode(int adpcm);
}

/* */
