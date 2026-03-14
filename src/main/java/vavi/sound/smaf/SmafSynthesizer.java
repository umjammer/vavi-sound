/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SysexMessage;

import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.smaf.sequencer.MachineDependentSequencer;
import vavi.sound.smaf.sequencer.SmafMessageStore;
import vavi.sound.smaf.sequencer.WaveSequencer;


/**
 * SmafSynthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-13 nsano initial version <br>
 */
public class SmafSynthesizer implements Synthesizer {

    private static final Logger logger = System.getLogger(SmafSynthesizer.class.getName());

    /** the device information */
    static final SmafDevice.Info info =
            new SmafDevice.Info("Java SMAF Sound Synthesizer",
                    "vavi",
                    "SMAF Software synthesizer with ADPCM",
                    "Version " + SmafDeviceProvider.version) {};

    /** */
    private javax.sound.midi.Synthesizer midiSynthesizer;

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void open() throws SmafUnavailableException {
        try {
            this.midiSynthesizer = MidiSystem.getSynthesizer();

            midiSynthesizer.open();
        } catch (MidiUnavailableException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new SmafUnavailableException(e);
        }
    }

    @Override
    public void close() {
        midiSynthesizer.close();
    }

    @Override
    public MidiChannel[] getChannels() {
        return midiSynthesizer.getChannels(); // TODO SmafChannel?
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        return midiSynthesizer.loadAllInstruments(soundbank);
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return midiSynthesizer.getAvailableInstruments();
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        return midiSynthesizer.getDefaultSoundbank();
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        midiSynthesizer.unloadAllInstruments(soundbank);
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof SysexMessage sysexMessage) {
                    try {
                        processSpecial(sysexMessage);
                    } catch (InvalidSmafDataException e) {
                        logger.log(Level.ERROR, e.getCause().getMessage(), e.getCause());
} catch (RuntimeException e) {
 logger.log(Level.ERROR, e.getMessage(), e);
} catch (Error e) {
 logger.log(Level.ERROR, e.getMessage(), e);
 throw e;
}
                }
                try {
                    midiSynthesizer.getReceiver().send(message, timeStamp);
                } catch (MidiUnavailableException e) {
                    logger.log(Level.ERROR, e.getMessage(), e);
                }
            }

            @Override
            public void close() {

            }
        };
    }

    // ----

    /**
     * meta machine depend: 0x7f
     * <pre>
     * 0x7f manufacturerId
     * </pre>
     */
    private static void processSpecial(javax.sound.midi.SysexMessage message)
            throws InvalidSmafDataException {

        byte[] data = message.getData();
        int manufacturerId = data[0];
        switch (manufacturerId) {
            case 0:     // 3 byte manufacturer id
                logger.log(Level.WARNING, "unhandled manufacturer: %02x %02x %02x".formatted(data[0], data[1], data[2]));
                break;
            case VaviMidiDeviceProvider.MANUFACTURER_ID: // 0x5f vavi
                processSpecial_Vavi(message);
                break;
            default:
                logger.log(Level.WARNING, "unhandled manufacturer: %02x".formatted(manufacturerId));
                break;
        }
    }

    /**
     * manufacturerId 0x5f: vavi
     * <pre>
     * 0x5f functionId
     * </pre>
     */
    private static void processSpecial_Vavi(javax.sound.midi.SysexMessage message)
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
                logger.log(Level.WARNING, "unhandled function: %02x".formatted(functionId));
                break;
        }
    }

    /**
     * <pre>
     * 0x5f 0x01
     * </pre>
     */
    private static void processSpecial_Vavi_MachineDependent(javax.sound.midi.SysexMessage message)
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
    private static void processSpecial_Vavi_Wave(javax.sound.midi.SysexMessage message)
            throws InvalidSmafDataException {

        byte[] data = message.getData();
        int id = (data[2] & 0xff) * 0x100 + (data[3] & 0xff);
//logger.log(Level.TRACE, "message id: " + id);
        WaveSequencer sequencer = (WaveSequencer) SmafMessageStore.get(id);
        sequencer.sequence();
    }
}
