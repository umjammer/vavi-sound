/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mobile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;

import vavi.sound.adpcm.rohm.RohmInputStream;
import vavi.sound.adpcm.rohm.RohmOutputStream;


/**
 * ROHM Audio Engine.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 051121 nsano initial version <br>
 */
public class RohmAudioEngine extends BasicAudioEngine {

    /** */
    private static final int MAX_ID = 16;

    /**
     * <pre>
     *  (c: continued, e: end)
     *  from Function131, Function134
     *   0 Lc + Lc + Le
     *   1 Rc + Rc + Re
     *  from AudioDataMessage
     *   0 L + R
     * </pre>   
     */
    public RohmAudioEngine() {
        data = new Data[MAX_ID];
    }

    /** */
    protected int getChannels(int streamNumber) {
        int channels = 1;
        if (data[streamNumber].channel != -1) { 
            // from MachineDependent
            if (streamNumber % 2 == 1 && data[streamNumber].channel % 2 == 1 && (data[streamNumber - 1] != null && data[streamNumber - 1].channel % 2 == 0)) {
                return -1;
            }
    
            if (streamNumber % 2 == 0 && data[streamNumber].channel % 2 == 0 && (data[streamNumber + 1] != null && data[streamNumber + 1].channel % 2 == 1)) {
                channels = 2;
            }
        } else {
            // from AudioData
            if (data[streamNumber].channels == 2) {
                channels = 2;
            }
        }
        return channels;
    }

    /** */
    protected InputStream[] getInputStreams(int streamNumber, int channels) {
        InputStream[] iss = new InputStream[2];
        if (data[streamNumber].channels == 1) {
            InputStream in = new ByteArrayInputStream(data[streamNumber].adpcm); 
            iss[0] = new RohmInputStream(in, ByteOrder.LITTLE_ENDIAN);
            if (channels != 1) {
                InputStream inR = new ByteArrayInputStream(data[streamNumber + 1].adpcm);
                iss[1] = new RohmInputStream(inR, ByteOrder.LITTLE_ENDIAN);
            }
        } else {
            InputStream in = new ByteArrayInputStream(data[streamNumber].adpcm, 0, data[streamNumber].adpcm.length / 2); 
            iss[0] = new RohmInputStream(in, ByteOrder.LITTLE_ENDIAN);
            InputStream inR = new ByteArrayInputStream(data[streamNumber].adpcm, data[streamNumber].adpcm.length / 2, data[streamNumber].adpcm.length / 2);
            iss[1] = new RohmInputStream(inR, ByteOrder.LITTLE_ENDIAN);
        }
        return iss;
    }

    //-------------------------------------------------------------------------

    /** */
    protected OutputStream getOutputStream(OutputStream os) {
        return new RohmOutputStream(os, ByteOrder.LITTLE_ENDIAN);
    }
}

/* */
