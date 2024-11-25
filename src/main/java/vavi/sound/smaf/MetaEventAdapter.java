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
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.smaf.message.MidiContext;
import vavi.sound.smaf.message.WaveMessage;
import vavi.sound.smaf.sequencer.MachineDependentSequencer;
import vavi.sound.smaf.sequencer.SmafMessageStore;
import vavi.sound.smaf.sequencer.WaveSequencer;

import static java.lang.System.getLogger;


/**
 * a MIDI MetaEvent adapter implementation.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
class MetaEventAdapter implements MetaEventListener, SmafDevice {

    private static final Logger logger = getLogger(MetaEventAdapter.class.getName());

    /** the device information */
    private static final SmafDevice.Info info =
        new SmafDevice.Info("Java SMAF WAVE Sequencer",
                           "Vavisoft",
                           "Software sequencer using wave",
                           "Version " + SmafDeviceProvider.version) {};

    /* */
    @Override
    public SmafDevice.Info getDeviceInfo() {
        return info;
    }

    /* */
    @Override
    public void close() {
    }

    /* */
    @Override
    public boolean isOpen() {
        return true;
    }

    /* */
    @Override
    public void open() {
    }

    /**
     * Implements playback mechanism using {@link SmafMessageStore}.
     * @see WaveMessage#getMidiEvents(MidiContext)
     */
    @Override
    public void meta(javax.sound.midi.MetaMessage message) {
//logger.log(Level.TRACE, "type: " + message.getType());
        switch (MetaEvent.valueOf(message.getType())) {
        case META_MACHINE_DEPEND: // sequencer specific meta event
            try {
                processSpecial(message);
            } catch (InvalidSmafDataException e) {
                logger.log(Level.ERROR, e.getCause().getMessage(), e.getCause());
            }
catch (RuntimeException e) {
logger.log(Level.ERROR, e.getMessage(), e);
} catch (Error e) {
logger.log(Level.ERROR, e.getMessage(), e);
throw e;
}
            break;
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

    /**
     * meta machine depend: 0x7f
     * <pre>
     * 0x7f manufacturerId
     * </pre>
     */
    private void processSpecial(javax.sound.midi.MetaMessage message)
        throws InvalidSmafDataException {

        byte[] data = message.getData();
        int manufacturerId = data[0];
        switch (manufacturerId) {
        case 0:     // 3 byte manufacturer id
logger.log(Level.WARNING, String.format("unhandled manufacturer: %02x %02x %02x", data[0], data[1], data[2]));
            break;
        case VaviMidiDeviceProvider.MANUFACTURER_ID: // 0x5f vavi
            processSpecial_Vavi(message);
            break;
        default:
logger.log(Level.WARNING, String.format("unhandled manufacturer: %02x", manufacturerId));
            break;
        }
    }

    /**
     * manufacturerId 0x5f: vavi
     * <pre>
     * 0x5f functionId
     * </pre>
     */
    private static void processSpecial_Vavi(javax.sound.midi.MetaMessage message)
        throws InvalidSmafDataException {

        byte[] data = message.getData();
        int functionId = data[1];
        switch (functionId) {
        case MachineDependentSequencer.META_FUNCTION_ID_MACHINE_DEPEND:
            processSpecial_Vavi_MachineDependent(message);
            break;
        case WaveSequencer.META_FUNCTION_ID_SMAF:
            processSpecial_Vavi_Wave(message);
            break;
        default:
logger.log(Level.WARNING, String.format("unhandled function: %02x", functionId));
            break;
        }
    }

    /**
     * <pre>
     * 0x5f 0x01
     * </pre>
     */
    private static void processSpecial_Vavi_MachineDependent(javax.sound.midi.MetaMessage message)
        throws InvalidSmafDataException {

        byte[] data = message.getData();
        int id = (data[2] & 0xff) * 0xff + (data[3] & 0xff);
//logger.log(Level.TRACE, "message id: " + id);
        MachineDependentSequencer sequencer = (MachineDependentSequencer) SmafMessageStore.get(id);
        sequencer.sequence();
    }

    /**
     * functionId 0x03: wave
     * <pre>
     * 0x5f 0x03 id(H) id(L)
     * </pre>
     */
    private static void processSpecial_Vavi_Wave(javax.sound.midi.MetaMessage message)
        throws InvalidSmafDataException {

        byte[] data = message.getData();
        int id = (data[2] & 0xff) * 0x100 + (data[3] & 0xff);
//logger.log(Level.TRACE, "message id: " + id);
        WaveSequencer sequencer = (WaveSequencer) SmafMessageStore.get(id);
        sequencer.sequence();
    }
}
