/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi;

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

import vavi.sound.midi.MidiUtil;
import vavi.sound.midi.VaviMidiDeviceProvider;
import vavi.sound.mobile.AudioEngine;
import vavi.sound.mobile.MobileExclusive;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafUnavailableException;
import vavi.sound.smaf.Synthesizer;
import vavi.sound.smaf.vavi.sequencer.WaveSequencer;
import vavi.util.StringUtil;

import static vavi.sound.mobile.MobileExclusive.unpack;


/**
 * SmafSynthesizer.
 * <p>
 * <li>{@code /vavi/sound/mfi/vavi/midi.properties#defaultSynthesizer} ... internal midi synthesizer</li>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-03-13 nsano initial version <br>
 */
public class VaviSmafSynthesizer implements Synthesizer {

    private static final Logger logger = System.getLogger(VaviSmafSynthesizer.class.getName());

    /** the device information */
    static final Info info =
            new Info("Java SMAF Sound Synthesizer",
                    "vavi",
                    "SMAF Software synthesizer with ADPCM",
                    "Version " + VaviSmafDeviceProvider.version) {};

    /** */
    private javax.sound.midi.Synthesizer midiSynthesizer;

    /** the synthesizer played here with the adpcm mixed in, null: it plays to its own line */
    private vavi.sound.mobile.MixingLine mixingLine;

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
            this.midiSynthesizer = MidiUtil.getDefaultSynthesizer(vavi.sound.midi.VaviMidiDeviceProvider.class);

            // none of the streams the song before stored is this song's
            AudioEngine.resetAll();

            // the adpcm is mixed into the synthesizer's line when it can be, see MixingLine
            mixingLine = vavi.sound.mobile.MixingLine.open(midiSynthesizer);
            if (mixingLine == null) {
                midiSynthesizer.open();
            }
        } catch (MidiUnavailableException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new SmafUnavailableException(e);
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

    /**
     * A Receiver w/ ADPCM driver
     */
    public static class VaviSmafReceiver implements MidiDeviceReceiver {
        boolean isOpen;

        /** */
        private final javax.sound.midi.Synthesizer midiSynthesizer;

        public VaviSmafReceiver(javax.sound.midi.Synthesizer midiSynthesizer) {
            this.midiSynthesizer = midiSynthesizer;
            try {
                AudioEngine.Sync.setSynthesizerLatency(midiSynthesizer.getLatency() / 1000);
logger.log(Level.DEBUG, "synthesizer latency: " + midiSynthesizer.getLatency() / 1000 + " ms");
            } catch (Exception e) {
logger.log(Level.DEBUG, "getting synthesizer latency: " + e);
            }
            isOpen = true;
        }

        @Override
        public void send(MidiMessage message, long timeStamp) {
            if (!isOpen) return;

            if (message instanceof SysexMessage sysexMessage) {
                try {
                    processSpecial(sysexMessage, this);
                } catch (InvalidSmafDataException e) {
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

        // ----

        /**
         * sysex
         * <pre>
         * 0xf0 manufacturerId
         * </pre>
         */
        private static void processSpecial(javax.sound.midi.SysexMessage message, Receiver receiver) throws InvalidSmafDataException {

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
         * manufacturer id: vavi, function id: 7f: packed
         * <pre>
         * 0xf0 0x43 ... smaf sysex
         * 0xf0 0x45 0x03 sub-functionId ... 03: wave
         * </pre>
         * @param data unpacked sysex
         */
        private static void processSpecial_Vavi_Packed(byte[] data, Receiver receiver) throws InvalidSmafDataException {

            int functionId = data[1];
            switch (functionId) {
                case 0x43: // yamaha smaf message
                    processSpecial_Vavi_Yamaha(data, receiver);
                    break;
                case WaveSequencer.SMAF_SYSEX_FUNCTION_ID_WAVE:
                    processSpecial_Vavi_Wave(data, receiver);
                    break;
                default:
                    logger.log(Level.WARNING, "unhandled function: %02x".formatted(functionId));
                    break;
            }
        }

        // ----

        /**
         * yamaha sysex
         * <p>
         * vendor is yamaha only, so process is same as the {@link #processSpecial_Vavi_Wave}
         * <pre>
         * 0xf0 0x43 ...
         * </pre>
         */
        private static void processSpecial_Vavi_Yamaha(byte[] data, Receiver receiver) throws InvalidSmafDataException {
        }

        /**
         * vavi: function id: wave (message is yamaha message for wave)
         * <pre>
         * 0xf0 0x45 0x03
         * </pre>
         */
        private static void processSpecial_Vavi_Wave(byte[] data, javax.sound.midi.Receiver receiver) throws InvalidSmafDataException {
            WaveSequencer sequencer = WaveSequencer.factory(data);
logger.log(Level.DEBUG, "wave sysex received at: " + System.nanoTime() + " ns");
            sequencer.sequence(Arrays.copyOfRange(data, 2, data.length), receiver);
        }
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        Receiver receiver = new VaviSmafReceiver(midiSynthesizer);
        // the listener's volume is of the whole mix when the adpcm is mixed into the line
        return mixingLine != null ? mixingLine.receiver(receiver) : receiver;
    }
}
