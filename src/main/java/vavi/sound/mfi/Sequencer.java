/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.io.IOException;
import java.io.InputStream;


/**
 * Sequencer.
 * <p>
 * {@link javax.sound.midi} subset compatible.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 */
public interface Sequencer extends MfiDevice {

    /** */
    void setSequence(Sequence sequence)
        throws InvalidMfiDataException;

    /** */
    void setSequence(InputStream stream)
        throws IOException,
               InvalidMfiDataException;

    /** */
    Sequence getSequence();

    /** */
    void start();

    /** */
    void stop();

    /** */
    boolean isRunning();

    /** {@link MetaEventListener Listener} を登録します。 */
    void addMetaEventListener(MetaEventListener l);

    /** {@link MetaEventListener Listener} を削除します。 */
    void removeMetaEventListener(MetaEventListener l);
}
