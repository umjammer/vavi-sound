/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.MetaEventListener;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.midi.MidiUtil;
import vavi.sound.midi.VaviMidiDeviceProvider;

import static java.lang.System.getLogger;
import static vavi.sound.mfi.vavi.header.AinfMessage.META_FUNCTION_ID_AudioEngine;


/**
 * a MIDI MetaEvent adapter implementation.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030902 nsano initial version <br>
 */
class MetaEventAdapter implements MetaEventListener {

    private static final Logger logger = getLogger(MetaEventAdapter.class.getName());

    /**
     * @see vavi.sound.mfi.vavi.header.CopyMessage#getMidiEvents(MidiContext)
     * @see vavi.sound.mfi.vavi.header.ProtMessage#getMidiEvents(MidiContext)
     * @see vavi.sound.mfi.vavi.header.TitlMessage#getMidiEvents(MidiContext)
     * @see vavi.sound.mfi.vavi.track.TempoMessage#getMidiEvents(MidiContext)
     */
    @Override
    public void meta(javax.sound.midi.MetaMessage message) {
//logger.log(Level.TRACE, "type: " + message.getType());
        switch (MetaEvent.valueOf(message.getType())) {
        case META_TEXT_EVENT:     // text event
        case META_COPYRIGHT:      // copyright
        case META_NAME:           // sequence name or track name
logger.log(Level.DEBUG, "meta " + message.getType() + ": " + MidiUtil.getDecodedMessage(message.getData()));
            break;
        case META_END_OF_TRACK:   // end of track
        case META_TEMPO:          // tempo was set
logger.log(Level.DEBUG, "this handler ignore meta: " + message.getType());
            break;
        case META_MACHINE_DEPEND:
            byte[] data = message.getData();
            if (data.length > 2 &&
                    data[0] == VaviMidiDeviceProvider.MANUFACTURER_ID &&
                    data[1] == META_FUNCTION_ID_AudioEngine
            ) {
                int format = (data[2] & 0xff) * 0x100 + (data[3] & 0xff);
logger.log(Level.INFO, "audio engine: " + format);
            }
            break;
        default:
logger.log(Level.DEBUG, "no meta sub handler: " + message.getType());
            break;
        }
    }
}
