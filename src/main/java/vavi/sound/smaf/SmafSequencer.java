/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Soundbank;

import vavi.sound.midi.MidiUtil;

import static java.lang.System.getLogger;


/**
 * Sequencer implemented for SMAF.
 * <p>
 * don't use {@link javax.sound.midi.MidiSystem#getSequencer()},
 * {@link javax.sound.midi.MidiSystem#getSequencer(boolean)} in this program,
 * because this is the {@link javax.sound.midi.Sequencer}.
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
class SmafSequencer implements Sequencer, Synthesizer {

    private static final Logger logger = getLogger(SmafSequencer.class.getName());

    /** the device information */
    private static final SmafDevice.Info info =
        new SmafDevice.Info("Java SMAF Sound Sequencer",
                           "vavi",
                           "Software sequencer using midi",
                           "Version " + SmafDeviceProvider.version) {};

    /** sound source of this sequencer */
    private javax.sound.midi.Sequencer midiSequencer;

    /** */
    private javax.sound.midi.Synthesizer midiSynthesizer;

    /** the sequence of SMAF */
    private Sequence sequence;

    @Override
    public SmafDevice.Info getDeviceInfo() {
        return info;
    }

    @Override
    public void close() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opend");
        }
        midiSequencer.close();
        midiSynthesizer.close();
        off();
    }

    @Override
    public boolean isOpen() {
        if (midiSequencer == null) {
            return false;
        }
        return midiSequencer.isOpen();
    }

    /** ADPCM sequencer, TODO should be {@link javax.sound.midi.Transmitter} */
    private final javax.sound.midi.MetaEventListener mea = new MetaEventAdapter();

    @Override
    public void open() throws SmafUnavailableException {
        try {
            if (midiSequencer == null) {
                this.midiSequencer = MidiUtil.getDefaultSequencer(vavi.sound.midi.VaviMidiDeviceProvider.class);
                this.midiSynthesizer = MidiSystem.getSynthesizer();
            }

            midiSequencer.open();
            midiSynthesizer.open();
            midiSequencer.getTransmitter().setReceiver(midiSynthesizer.getReceiver());
        } catch (MidiUnavailableException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new SmafUnavailableException(e);
        }
    }

    @Override
    public void setSequence(Sequence sequence)
        throws InvalidSmafDataException {

        this.sequence = sequence;

        try {
            midiSequencer.setSequence(SmafSystem.toMidiSequence(sequence));
        } catch (InvalidMidiDataException e) {
logger.log(Level.DEBUG, e);
            throw new InvalidSmafDataException(e);
        } catch (SmafUnavailableException e) {
logger.log(Level.DEBUG, e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setSequence(InputStream stream)
        throws IOException,
               InvalidSmafDataException {

        this.setSequence(SmafSystem.getSequence(stream));
    }

    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void start() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opend");
        }
        on();
        midiSequencer.start();
    }

    @Override
    public void stop() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opend");
        }
        midiSequencer.stop();
        off();
    }

    @Override
    public boolean isRunning() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opend");
        }
        return midiSequencer.isRunning();
    }

    private void on() {
        midiSequencer.addMetaEventListener(mel);
        midiSequencer.addMetaEventListener(mea);
    }

    private void off() {
        midiSequencer.removeMetaEventListener(mel);
        midiSequencer.removeMetaEventListener(mea);
    }

    // ----

    /** {@link MetaMessage MetaEvent} utility */
    private final MetaSupport metaSupport = new MetaSupport();

    /* Adds a {@link MetaEventListener}. */
    @Override
    public void addMetaEventListener(MetaEventListener l) {
        metaSupport.addMetaEventListener(l);
    }

    /* Removes a {@link MetaEventListener}. */
    @Override
    public void removeMetaEventListener(MetaEventListener l) {
        metaSupport.removeMetaEventListener(l);
    }

    /** {@link MetaMessage MetaEvent} */
    protected void fireMeta(MetaMessage meta) {
        metaSupport.fireMeta(meta);
    }

    /** meta 0x2f listener */
    private final javax.sound.midi.MetaEventListener mel = message -> {
//logger.log(Level.TRACE, "type: " + message.getType());
        switch (message.getType()) {
        case 0x2f:  // added automatically at the end of the sequence
            try {
                MetaMessage metaMessage = new MetaMessage();
                metaMessage.setMessage(0x2f, null);
                fireMeta(metaMessage);
                off();
            } catch (InvalidSmafDataException e) {
logger.log(Level.DEBUG, e);
            }
catch (RuntimeException | Error e) {
logger.log(Level.ERROR, e.getMessage(), e);
throw e;
}
            break;
        }
    };

    // synthesizer

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
        return midiSynthesizer.getReceiver();
    }
}
