/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MetaEventListener;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.message.MidiContext;
import vavi.sound.smaf.message.WaveMessage;
import vavi.sound.smaf.sequencer.SmafMessageStore;

import static java.lang.System.getLogger;


/**
 * a MIDI MetaEvent adapter implementation.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
class MetaEventAdapter implements MetaEventListener {

    private static final Logger logger = getLogger(MetaEventAdapter.class.getName());

    /**
     * Implements playback mechanism using {@link SmafMessageStore}.
     * @see WaveMessage#getMidiEvents(MidiContext)
     */
    @Override
    public void meta(javax.sound.midi.MetaMessage message) {
//logger.log(Level.TRACE, "type: " + message.getType());
        switch (MetaEvent.valueOf(message.getType())) {
        case META_TEXT_EVENT:     // text event
        case META_COPYRIGHT:      // show copyright
        case META_NAME:           // sequence name or track name
logger.log(Level.DEBUG, "meta " + message.getType() + ": " + MidiUtil.getDecodedMessage(message.getData()));
            break;
        case META_END_OF_TRACK:   // end of track
        case META_TEMPO:          // tempo was set
logger.log(Level.DEBUG, "this handler ignore meta: " + message.getType());
            break;
        default:
logger.log(Level.DEBUG, "no meta sub handler: " + message.getType());
            break;
        }
    }
}
