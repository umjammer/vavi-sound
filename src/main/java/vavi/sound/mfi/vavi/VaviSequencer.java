/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Transmitter;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MetaEventListener;
import vavi.sound.mfi.MetaMessage;
import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.Sequencer;
import vavi.sound.midi.MidiUtil;

import static java.lang.System.getLogger;


/**
 * Sequencer implemented by vavi.
 * <p>
 * <li>{@code /vavi/sound/mfi/vavi/midi.properties#defaultSynthesizer} ... internal midi synthesizer</li>
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano midi compliant <br>
 *          1.00 020706 nsano use midi sequencer <br>
 *          1.01 030606 nsano change error trap <br>
 *          1.02 030819 nsano change sequence related <br>
 *          1.03 030902 nsano out source {@link MetaEventListener} <br>
 */
class VaviSequencer implements Sequencer {

    private static final Logger logger = getLogger(VaviSequencer.class.getName());

    /** the device information */
    static final MfiDevice.Info info =
        new MfiDevice.Info("Java MFi Sound Sequencer",
                           "vavi",
                           "Software sequencer using midi",
                           "Version " + VaviMfiDeviceProvider.version) {};

    /** sound source of this sequencer */
    private javax.sound.midi.Sequencer midiSequencer;

    /** the sequence of MFi */
    private Sequence sequence;

    @Override
    public MfiDevice.Info getDeviceInfo() {
        return info;
    }

    @Override
    public void close() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opened");
        }
        midiSequencer.close();
        off();
logger.log(Level.DEBUG, "★0 close: " + midiSequencer.hashCode());
    }

    @Override
    public boolean isOpen() {
        if (midiSequencer == null) {
            return false;
        }
        return midiSequencer.isOpen();
    }

    @Override
    public void open() throws MfiUnavailableException {
        try {
            if (this.midiSequencer == null) {
                this.midiSequencer = MidiUtil.getDefaultSequencer(vavi.sound.midi.VaviMidiDeviceProvider.class);
logger.log(Level.DEBUG, "midiSequencer: " + midiSequencer.getClass().getName());
logger.log(Level.DEBUG, "★0 init: " + midiSequencer.hashCode());
            }

logger.log(Level.DEBUG, "★0 open: " + midiSequencer.hashCode());
            midiSequencer.open();
        } catch (MidiUnavailableException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new MfiUnavailableException(e);
        }
    }

    @Override
    public void setSequence(Sequence sequence)
        throws InvalidMfiDataException {

        this.sequence = sequence;

        try {
            midiSequencer.setSequence(MfiSystem.toMidiSequence(sequence));
        } catch (InvalidMidiDataException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new InvalidMfiDataException(e);
        } catch (MfiUnavailableException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void setSequence(InputStream stream)
        throws IOException,
               InvalidMfiDataException {

        this.setSequence(MfiSystem.getSequence(stream));
    }

    @Override
    public Sequence getSequence() {
        return sequence;
    }

    @Override
    public void start() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opened");
        }
        on();
        midiSequencer.start();
    }

    @Override
    public void stop() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opened");
        }
        midiSequencer.stop();
        off();
    }

    @Override
    public boolean isRunning() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opened");
        }
        return midiSequencer.isRunning();
    }

    @Override
    public Transmitter getTransmitter() throws MfiUnavailableException {
        try {
            return midiSequencer.getTransmitter();
        } catch (MidiUnavailableException e) {
            throw new MfiUnavailableException(e);
        }
    }

    /** */
    private void on() {
        midiSequencer.addMetaEventListener(mel);
logger.log(Level.DEBUG, "★0 on: " + midiSequencer.hashCode());
    }

    /** */
    private void off() {
        midiSequencer.removeMetaEventListener(mel);
logger.log(Level.DEBUG, "★0 off: " + midiSequencer.hashCode());
    }

    // ----

    /** @see vavi.sound.mfi.SysexMessage MetaEvent */
    private final MetaSupport metaSupport = new MetaSupport();

    @Override
    public void addMetaEventListener(MetaEventListener l) {
        metaSupport.addMetaEventListener(l);
    }

    @Override
    public void removeMetaEventListener(MetaEventListener l) {
        metaSupport.removeMetaEventListener(l);
    }

    /** @see vavi.sound.mfi.MetaMessage MetaEvent */
    protected void fireMeta(MetaMessage meta) {
        metaSupport.fireMeta(meta);
    }

    /** meta 0x2f listener */
    private final javax.sound.midi.MetaEventListener mel = message -> {
logger.log(Level.DEBUG, "★0 meta: type: " + message.getType());
        switch (message.getType()) {
        case 0x2f: // java midi sequencer adds automatically
            try {
                MetaMessage metaMessage = new MetaMessage();
                metaMessage.setMessage(0x2f, new byte[0], 0);
                fireMeta(metaMessage);
                off();
            } catch (InvalidMfiDataException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            }
catch (RuntimeException | Error e) {
logger.log(Level.ERROR, e.getMessage(), e);
throw e;
}
            break;
        }
    };
}
