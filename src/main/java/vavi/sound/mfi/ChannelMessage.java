/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;


/**
 * Represents {@link MfiMessage} depends on channel number.
 * <p>
 * using this name to match MIDI api instead of Voice ...
 * </p>
 * <li>is this really needed? no class in the package javax.sound.midi
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 031203 nsano initial version <br>
 */
public interface ChannelMessage {

    /** */
    int getVoice();

    /** */
    void setVoice(int voice);
}
