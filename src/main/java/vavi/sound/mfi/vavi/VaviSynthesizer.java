/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

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

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.mfi.Synthesizer;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.sequencer.MfiMessageStore;
import vavi.sound.mfi.vavi.sequencer.UnknownVenderSequencer;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.midi.VaviMidiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * VaviSynthesizer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-13 nsano initial version <br>
 */
public class VaviSynthesizer implements Synthesizer {

    private static final Logger logger = getLogger(VaviSequencer.class.getName());

    /** the device information */
    static final MfiDevice.Info info =
            new MfiDevice.Info("Java MFi Sound Synthesizer",
                    "vavi",
                    "MFi Software synthesizer with ADPCM",
                    "Version " + VaviMfiDeviceProvider.version) {};

    /** */
    private javax.sound.midi.Synthesizer midiSynthesizer;

    @Override
    public MidiChannel[] getChannels() {
        return midiSynthesizer.getChannels();
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        return midiSynthesizer.getDefaultSoundbank();
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return midiSynthesizer.getAvailableInstruments();
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        return midiSynthesizer.loadAllInstruments(soundbank);
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        midiSynthesizer.loadAllInstruments(soundbank);
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        // Implements a player using {@link MfiMessageStore}.
        // @see MachineDependentMessage#getMidiEvents(MidiContext)
        return new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof SysexMessage sysexMessage) {
                    try {
                        processSpecial(sysexMessage);
                    } catch (InvalidMfiDataException e) {
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

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void open() throws MfiUnavailableException {
        try {
            this.midiSynthesizer = MidiSystem.getSynthesizer();
logger.log(Level.DEBUG, "midiSynthesizer: " + midiSynthesizer.getClass().getName());

            midiSynthesizer.open();
        } catch (MidiUnavailableException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new MfiUnavailableException(e);
        }
    }

    @Override
    public void close() {
        midiSynthesizer.close();
    }

    // ----

    /**
     * <pre>
     * 0x7f
     * </pre>
     */
    private static void processSpecial(javax.sound.midi.SysexMessage message)
            throws InvalidMfiDataException {

        byte[] data = message.getData();
        int manufacturerId = data[0];
        switch (manufacturerId) {
            case 0:     // 3 byte manufacturer id
logger.log(Level.WARNING, "unhandled manufacturer: %02x %02x %02x".formatted(data[0], data[1], data[2]));
                break;
            case VaviMidiDeviceProvider.MANUFACTURER_ID:
                processSpecial_Vavi(message);
                break;
            default:
logger.log(Level.WARNING, "unhandled manufacturer: %02x".formatted(manufacturerId));
                break;
        }
    }

    /**
     * <pre>
     * 0x5f
     * </pre>
     */
    private static void processSpecial_Vavi(javax.sound.midi.SysexMessage message) throws InvalidMfiDataException {

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
logger.log(Level.WARNING, "unhandled function: %02x".formatted(functionId));
                break;
        }
    }

    /**
     * <pre>
     * 0x5f 0x01
     * </pre>
     */
    private static void processSpecial_Vavi_MachineDependent(javax.sound.midi.SysexMessage message) throws InvalidMfiDataException {

        byte[] data = message.getData();
        int id = (data[2] & 0xff) * 0xff + (data[3] & 0xff);
//logger.log(Level.TRACE, "message id: " + id);
        MachineDependentMessage mdm = (MachineDependentMessage) MfiMessageStore.get(id);

        int vendor = mdm.getVendor() | mdm.getCarrier(); // TODO carrier works?
        MachineDependentSequencer sequencer;
        try {
            sequencer = MachineDependentSequencer.Factory.getSequencer(vendor);
        } catch (IllegalArgumentException | Error e) {
logger.log(Level.ERROR, e.getMessage(), e);
logger.log(Level.ERROR, "error vendor: 0x%02x".formatted(vendor));
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
    private static void processSpecial_Vavi_Mfi4(javax.sound.midi.SysexMessage message) throws InvalidMfiDataException {

        byte[] data = message.getData();
        int id = (data[2] & 0xff) * 0xff + (data[3] & 0xff);
//logger.log(Level.TRACE, "message id: " + id);
        AudioDataSequencer sequencer = (AudioDataSequencer) MfiMessageStore.get(id);

        sequencer.sequence();
    }
}
