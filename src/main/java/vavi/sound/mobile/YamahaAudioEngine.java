/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.ma.MaInputStream;
import vavi.sound.adpcm.ma.MaOutputStream;
import vavi.util.Debug;


/**
 * YAMAHA AudioEngine.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020829 nsano initial version <br>
 */
public class YamahaAudioEngine extends BasicAudioEngine {

    /**
     * <pre>
     *  L0 + L2 + ...
     *  R1 + R3 + ...
     * </pre>   
     */
    public YamahaAudioEngine() {
        datum = new Data[32];
    }

    /** */
    protected int getChannels(int streamNumber) {
        int channels = 1;
        if (datum[streamNumber].channel == -1) {
            // from 1_240_7
            if (datum[streamNumber].channels == 2) {
                channels = 2;
            } else {
                if (streamNumber % 2 == 1 && datum[streamNumber].channels != 2 && (datum[streamNumber - 1] != null && datum[streamNumber - 1].channels != 2)) {
Debug.println("always used: no: " + streamNumber + ", ch: " + datum[streamNumber].channel);
                    return -1;
                }
        
                if (streamNumber % 2 == 0 && datum[streamNumber].channels != 2 && (datum[streamNumber + 1] != null && datum[streamNumber + 1].channels != 2)) {
                    channels = 2;
                }
            }
        } else {
            // from 240_2, channels always 1
            
            if (streamNumber % 2 == 1 && datum[streamNumber].channel % 2 == 1 && (datum[streamNumber - 1] != null && datum[streamNumber - 1].channel % 2 == 0)) {
Debug.println("always used: no: " + streamNumber + ", ch: " + datum[streamNumber].channel);
                return -1;
            }
    
            if (streamNumber % 2 == 0 && datum[streamNumber].channel % 2 == 0 && (datum[streamNumber + 1] != null && datum[streamNumber + 1].channel % 2 == 1)) {
                channels = 2;
            }
        }
        return channels;
    }

    /** */
    protected InputStream[] getInputStreams(int streamNumber, int channels) {
        InputStream[] iss = new InputStream[2];
        if (datum[streamNumber].channels == 1) {
            InputStream in = new ByteArrayInputStream(datum[streamNumber].adpcm);
            iss[0] = new MaInputStream(in, ByteOrder.LITTLE_ENDIAN);
            if (channels != 1) {
                InputStream inR = new ByteArrayInputStream(datum[streamNumber + 1].adpcm);
                iss[1] = new MaInputStream(inR, ByteOrder.LITTLE_ENDIAN);
            }
        } else {
            InputStream in = new ByteArrayInputStream(datum[streamNumber].adpcm, 0, datum[streamNumber].adpcm.length / 2);
            iss[0] = new MaInputStream(in, ByteOrder.LITTLE_ENDIAN);
            InputStream inR = new ByteArrayInputStream(datum[streamNumber].adpcm, datum[streamNumber].adpcm.length / 2, datum[streamNumber].adpcm.length / 2);
            iss[1] = new MaInputStream(inR, ByteOrder.LITTLE_ENDIAN);
        }
        return iss;
    }

    //-------------------------------------------------------------------------

    /** */
    protected OutputStream getOutputStream(OutputStream os) {
        return new MaOutputStream(os, ByteOrder.LITTLE_ENDIAN);
    }
}

/* */
