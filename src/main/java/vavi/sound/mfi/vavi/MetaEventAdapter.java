/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.util.logging.Level;

import javax.sound.midi.MetaEventListener;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.sequencer.MfiMessageStore;
import vavi.sound.mfi.vavi.sequencer.UnknownVenderSequencer;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.midi.MidiUtil;
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.util.Debug;


/**
 * a MIDI MetaEvent adapter implementation.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030902 nsano initial version <br>
 */
class MetaEventAdapter implements MetaEventListener, MfiDevice {

    /** the device information */
    private static final MfiDevice.Info info =
        new MfiDevice.Info("Java MFi ADPCM Sequencer",
                           "Vavisoft",
                           "Software sequencer using adpcm",
                           "Version " + VaviMfiDeviceProvider.version) {};

    @Override
    public MfiDevice.Info getDeviceInfo() {
        return info;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void open() {
    }

    /**
     * Implements a player using {@link MfiMessageStore}.
     * @see MachineDependentMessage#getMidiEvents(MidiContext)
     * @see vavi.sound.mfi.vavi.header.CopyMessage#getMidiEvents(MidiContext)
     * @see vavi.sound.mfi.vavi.header.ProtMessage#getMidiEvents(MidiContext)
     * @see vavi.sound.mfi.vavi.header.TitlMessage#getMidiEvents(MidiContext)
     * @see vavi.sound.mfi.vavi.track.TempoMessage#getMidiEvents(MidiContext)
     */
    @Override
    public void meta(javax.sound.midi.MetaMessage message) {
//Debug.println("type: " + message.getType());
        switch (MetaEvent.valueOf(message.getType())) {
        case META_MACHINE_DEPEND: // sequencer specific meta event
            try {
                processSpecial(message);
            } catch (InvalidMfiDataException e) {
                Debug.printStackTrace(e.getCause());
            }
catch (RuntimeException e) {
Debug.printStackTrace(e);
} catch (Error e) {
Debug.printStackTrace(e);
throw e;
}
            break;
        case META_TEXT_EVENT:     // text event
        case META_COPYRIGHT:      // copyright
        case META_NAME:           // sequence name or track name
Debug.println(Level.FINE, "meta " + message.getType() + ": " + MidiUtil.getDecodedMessage(message.getData()));
            break;
        case META_END_OF_TRACK:   // end of track
        case META_TEMPO:          // tempo was set
Debug.println(Level.FINE, "this handler ignore meta: " + message.getType());
            break;
        default:
Debug.println(Level.FINE, "no meta sub handler: " + message.getType());
            break;
        }
    }

    /**
     * <pre>
     * 0x7f
     * </pre>
     */
    private void processSpecial(javax.sound.midi.MetaMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getData();
        int manufacturerId = data[0];
        switch (manufacturerId) {
        case 0:     // 3 byte manufacturer id
Debug.printf(Level.WARNING, "unhandled manufacturer: %02x %02x %02x\n", data[0], data[1], data[2]);
            break;
        case VaviMidiDeviceProvider.MANUFACTURER_ID:
            processSpecial_Vavi(message);
            break;
        default:
Debug.printf(Level.WARNING, "unhandled manufacturer: %02x\n", manufacturerId);
            break;
        }
    }

    /**
     * <pre>
     * 0x5f
     * </pre>
     */
    private void processSpecial_Vavi(javax.sound.midi.MetaMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getData();
        int functionId = data[1];
        switch (functionId) {
        case MachineDependentSequencer.META_FUNCTION_ID_MACHINE_DEPEND:
            processSpecial_Vavi_MachineDependent(message);
            break;
        case AudioDataSequencer.META_FUNCTION_ID_MFi4:
            processSpecial_Vavi_Mfi4(message);
            break;
        default:
Debug.printf(Level.WARNING, "unhandled function: %02x\n", functionId);
            break;
        }
    }

    /**
     * <pre>
     * 0x5f 0x01
     * </pre>
     */
    private void processSpecial_Vavi_MachineDependent(javax.sound.midi.MetaMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getData();
        int id = (data[2] & 0xff) * 0xff + (data[3] & 0xff);
//Debug.println("message id: " + id);
        MachineDependentMessage mdm = (MachineDependentMessage) MfiMessageStore.get(id);

        int vendor = mdm.getVendor() | mdm.getCarrier();
        MachineDependentSequencer sequencer;
        try {
            sequencer = MachineDependentSequencer.factory.get(vendor);
        } catch (IllegalArgumentException | Error e) {
Debug.printStackTrace(e);
Debug.printf(Level.SEVERE, "error vendor: 0x%02x\n", vendor);
            sequencer = new UnknownVenderSequencer();
        }
        sequencer.sequence(mdm);
    }

    /**
     * <pre>
     * 0x5f 0x02
     * </pre>
     * @since MFi 4.0
     */
    private void processSpecial_Vavi_Mfi4(javax.sound.midi.MetaMessage message)
        throws InvalidMfiDataException {

        byte[] data = message.getData();
        int id = (data[2] & 0xff) * 0xff + (data[3] & 0xff);
//Debug.println("message id: " + id);
        AudioDataSequencer sequencer = (AudioDataSequencer) MfiMessageStore.get(id);

        sequencer.sequence();
    }
}
