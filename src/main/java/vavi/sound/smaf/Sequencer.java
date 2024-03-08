/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.IOException;
import java.io.InputStream;


/**
 * Sequencer.
 * <p>
 * {@link javax.sound.midi} subset compatible.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public interface Sequencer extends SmafDevice {

    /** */
    void setSequence(Sequence sequence)
        throws InvalidSmafDataException;

    /** */
    void setSequence(InputStream stream)
        throws IOException,
               InvalidSmafDataException;

    /** */
    Sequence getSequence();

    /** */
    void start();

    /** */
    void stop();

    /** */
    boolean isRunning();

    /** Adds {@link MetaEventListener Listener}. */
    void addMetaEventListener(MetaEventListener l);

    /** Removes {@link MetaEventListener Listener}. */
    void removeMetaEventListener(MetaEventListener l);
}
