/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.io.Serializable;


/**
 * System exclusive message.
 * <p>
 * Represents MFi specs. "Extended Status Information".
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020703 nsano initial version <br>
 *          0.01 030820 nsano implements Serializable <br>
 */
public abstract class SysexMessage extends MfiMessage implements Serializable {

    /** */
    protected SysexMessage(byte[] message) {
        super(message);
    }
}
