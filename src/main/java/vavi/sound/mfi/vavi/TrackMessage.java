/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.DataInputStream;
import java.io.IOException;


/**
 * TrackMessage.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-14 nsano initial version <br>
 */
public interface TrackMessage {

    /** caution, name is conflicted with {@link TrackMessage#accept(String)} */
    boolean accept(String key);

    /** Represents a message found in a track. */
    interface SysexTrackMessage extends TrackMessage {

        /** */
        SysexTrackMessage init(int delta, int status, int data1, DataInputStream dis) throws IOException;
    }
}
