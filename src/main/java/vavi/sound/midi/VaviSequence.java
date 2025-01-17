/*
 * Copyright (c) 2009 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import javax.sound.midi.MetaEventListener;


/**
 * VaviSequence.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 090110 nsano initial version <br>
 */
public interface VaviSequence {

    /**
     * We use meta event listener for processing MFi/SMAF specific functionality internally.
     */
    MetaEventListener getMetaEventListener();
}
