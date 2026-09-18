/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Arrays;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDeviceReceiver;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.mfi.Synthesizer;
import vavi.sound.mfi.vavi.sequencer.AudioDataSequencer;
import vavi.sound.mfi.vavi.sequencer.MfiValueExclusive;
import vavi.sound.mfi.vavi.sequencer.MachineDependentSequencer;
import vavi.sound.mfi.vavi.track.MachineDependentMessage;
import vavi.sound.midi.MidiUtil;
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.MobileExclusive;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;
import static vavi.sound.mobile.MobileExclusive.unpack;


/**
 * Vavi MFi Synthesizer.
 * <p>
 * <li>{@code /vavi/sound/mfi/vavi/midi.properties#defaultSynthesizer} ... internal midi synthesizer</li>
 * <p>
 * system property
 * <li>{@code vavi.sound.mobile.AudioEngine.syncLead} ... control midi latency, default 120</li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-13 nsano initial version <br>
 *          0.01 2026-09-11 nsano pass packed smaf exclusives through <br>
 */
public class VaviMfiSynthesizer implements Synthesizer {

    private static final Logger logger = getLogger(VaviMfiSynthesizer.class.getName());

    /** the device information */
    static final MfiDevice.Info info =
            new MfiDevice.Info("Java MFi Sound Synthesizer",
                    "vavi",
                    "MFi Software synthesizer with ADPCM",
                    "Version " + VaviMfiDeviceProvider.version) {};

    /** */
    private javax.sound.midi.Synthesizer midiSynthesizer;

    /** the synthesizer played here with the adpcm mixed in, null: it plays to its own line */
    private vavi.sound.mobile.MixingLine mixingLine;

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

    /**
     * A Receiver w/ ADPCM driver.
     * <p>
     * @see MachineDependentMessage#getMidiEvents(MidiContext)
     */
    public static class VaviMfiReceiver implements MidiDeviceReceiver {
        boolean isOpen;

        /** */
        private final javax.sound.midi.Synthesizer midiSynthesizer;

        public VaviMfiReceiver(javax.sound.midi.Synthesizer midiSynthesizer) {
            this.midiSynthesizer = midiSynthesizer;
            try {
                long reportedLatency = midiSynthesizer.getLatency() / 1000;
                // Gervill's reported latency includes a sizable output
                // buffer.  ADPCM is written directly to a SourceDataLine and
                // does not need the full value; compensating it in full made
                // short MFi percussion audibly late.  Keep the lead tunable
                // for sound cards with a different hardware path.
                long lead = Long.getLong("vavi.sound.mobile.AudioEngine.syncLead", 120);
                long effectiveLatency = Math.max(0, reportedLatency - lead);
                AudioEngine.Sync.setSynthesizerLatency(effectiveLatency);
logger.log(Level.DEBUG, "synthesizer latency: reported=" + reportedLatency + " ms, effective=" + effectiveLatency + " ms");            } catch (Exception e) {
            }
            isOpen = true;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!isOpen) return;

            if (message instanceof SysexMessage sysexMessage) {
                try {
                    processSpecial(sysexMessage, this);
                } catch (InvalidMfiDataException e) {
                    logger.log(Level.ERROR, e.getCause().getMessage(), e.getCause());
} catch (RuntimeException e) {
 logger.log(Level.ERROR, e.getMessage(), e);
} catch (Error e) {
 logger.log(Level.ERROR, e.getMessage(), e);
 throw e;
}
            }
            // TODO MetaMessage 0x2f closing engine
            try {
                receiver().send(message, timeStamp);
            } catch (MidiUnavailableException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }

        /** the receiver of the synthesizer behind this, one for all the messages */
        private Receiver receiver;

        /**
         * Gets the receiver of the synthesizer once: a synthesizer may well hand out a new
         * receiver each time it is asked, which every message would leave behind in its list.
         * Not in the constructor, the synthesizer may not be open yet then.
         */
        private synchronized Receiver receiver() throws MidiUnavailableException {
            if (receiver == null) {
                receiver = midiSynthesizer.getReceiver();
            }
            return receiver;
        }

        @Override
        public synchronized void close() {
            isOpen = false;
            if (receiver != null) {
                receiver.close();
                receiver = null;
            }
        }

        @Override
        public MidiDevice getMidiDevice() {
            return midiSynthesizer;
        }
    }

    // ----

    /**
     * sysex
     * <pre>
     * 0xf0 manufacturerId
     * </pre>
     */
    public static void processSpecial(javax.sound.midi.SysexMessage message, Receiver receiver) throws InvalidMfiDataException {

        byte[] data = message.getData();
        int manufacturerId = data[0];
        switch (manufacturerId) {
            case 0:     // 3 byte manufacturer id
                logger.log(Level.DEBUG, "unhandled manufacturer: %02x %02x %02x".formatted(data[0], data[1], data[2]));
                break;
            case VaviMidiDeviceProvider.MANUFACTURER_ID: // 0x45 vavi
                int functionId = data[1];
                if (functionId == MobileExclusive.MIDI_SYSEX_FUNCTION_ID_PACKED) {
                    processSpecial_Vavi_Packed(unpack(message.getData()), receiver);
                } else if (functionId == MfiValueExclusive.MFi_SYSEX_FUNCTION_ID_VALUE) {
                    // mfi values for a synthesizer of an mfi sound source, the midi ones are enough here
                } else {
                    logger.log(Level.WARNING, "unhandled function: %02x".formatted(functionId) + "\n" + StringUtil.getDump(message.getData(), 32));
                }
                break;
            case 0x7f:
                logger.log(Level.DEBUG, "unhandled Realtime Universal: %02x".formatted(manufacturerId) + "\n" + StringUtil.getDump(message.getData(), 32));
                break;
            default:
                logger.log(Level.DEBUG, "unhandled manufacturer: %02x".formatted(manufacturerId) + "\n" + StringUtil.getDump(message.getData(), 32));
                break;
        }
    }

    /**
     * manufacturer id: vavi 0x45
     * <pre>
     * 0x45 0x01 ... MACHINE_DEPEND
     * 0x45 0x02 ... MFi4
     * </pre>
     */
    private static void processSpecial_Vavi_Packed(byte[] data, Receiver receiver) throws InvalidMfiDataException {
logger.log(Level.TRACE, "\n" + StringUtil.getDump(data, 32));

        int functionId = data[1];
        switch (functionId) {
            case MachineDependentSequencer.MFi_SYSEX_FUNCTION_ID_MACHINE_DEPENDENT:
                processSpecial_Vavi_MachineDependent(data, receiver);
                break;
            case AudioDataSequencer.MFi_SYSEX_FUNCTION_ID_MFi4:
                processSpecial_Vavi_Mfi4(data, receiver);
                break;
            default:
                logger.log(Level.WARNING, "unhandled function: %02x".formatted(functionId & 0xff));
                break;
        }
    }

    /**
     * sysex function id: machine dependent (message has vendor and carrier id)
     * <pre>
     * 0x45 0x01 ... mfi sysex
     * </pre>
     */
    private static void processSpecial_Vavi_MachineDependent(byte[] data, Receiver receiver) throws InvalidMfiDataException {
        MachineDependentSequencer sequencer = MachineDependentSequencer.Factory.getSequencer(data);
        sequencer.sequence(Arrays.copyOfRange(data, 2, data.length), receiver);
    }

    /**
     * sysex function id: mfi4 (message is mfi4)
     * <pre>
     * 0x45 0x02 ... mfi4 wave
     * </pre>
     * @since MFi 4.0
     */
    private static void processSpecial_Vavi_Mfi4(byte[] data, Receiver receiver) throws InvalidMfiDataException {
        AudioDataSequencer sequencer = AudioDataSequencer.factory(data);
logger.log(Level.DEBUG, "audio sysex received at: " + System.nanoTime() + " ns");
        sequencer.sequence(Arrays.copyOfRange(data, 2, data.length), receiver);
    }

    // ----

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new VaviMfiSynthesizer.VaviMfiReceiver(midiSynthesizer);
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
            this.midiSynthesizer = MidiUtil.getDefaultSynthesizer(vavi.sound.midi.VaviMidiDeviceProvider.class);
logger.log(Level.DEBUG, "midiSynthesizer: " + midiSynthesizer.getClass().getName());

            // the adpcm is mixed into the synthesizer's line when it can be, see MixingLine
            mixingLine = vavi.sound.mobile.MixingLine.open(midiSynthesizer);
            if (mixingLine == null) {
                midiSynthesizer.open();
            }
        } catch (MidiUnavailableException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new MfiUnavailableException(e);
        }
    }

    @Override
    public void close() {
        if (mixingLine != null) {
            mixingLine.close();
            mixingLine = null;
        } else {
            midiSynthesizer.close();
        }
    }
}
