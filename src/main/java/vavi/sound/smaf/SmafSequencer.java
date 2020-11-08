/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Soundbank;

import vavi.sound.midi.MidiUtil;
import vavi.util.Debug;


/**
 * Sequencer implemented for SMAF.
 * <p>
 * このシーケンサクラスで再生する場合は
 * システムプロパティ <code>javax.sound.midi.Sequencer</code> に <code>"#Real Time Sequencer"</code>
 * を明示するようにしてください。<code>"Java MIDI(MFi/SMAF) ADPCM Sequencer"</code> が
 * デフォルトシーケンサになった場合、{@link #mea}が重複して登録されてしまいます。
 * </p>
 * <p>
 * {@link javax.sound.midi.MidiSystem#getSequencer()} を
 * 使用しているため javax.sound.midi SPI のプログラム内で使用してはいけません。
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
class SmafSequencer implements Sequencer, Synthesizer {

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

    /** */
    public SmafDevice.Info getDeviceInfo() {
        return info;
    }

    /* */
    public void close() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opend");
        }
        midiSequencer.close();
        midiSynthesizer.close();
    }

    /* */
    public boolean isOpen() {
        if (midiSequencer == null) {
            return false;
        }
        return midiSequencer.isOpen();
    }

    /** ADPCM sequencer */
    private javax.sound.midi.MetaEventListener mea = new MetaEventAdapter();

    /* */
    public void open() throws SmafUnavailableException {
        try {
            if (midiSequencer == null) {
                this.midiSequencer = MidiUtil.getDefaultSequencer(vavi.sound.midi.VaviMidiDeviceProvider.class);
                this.midiSynthesizer = MidiSystem.getSynthesizer();
            }

            midiSequencer.open();
            midiSynthesizer.open();
        } catch (MidiUnavailableException e) {
Debug.printStackTrace(e);
            throw new SmafUnavailableException(e);
        }
    }

    /* */
    public void setSequence(Sequence sequence)
        throws InvalidSmafDataException {

        this.sequence = sequence;

        try {
            midiSequencer.setSequence(SmafSystem.toMidiSequence(sequence));
        } catch (InvalidMidiDataException e) {
Debug.println(e);
            throw new InvalidSmafDataException(e);
        } catch (SmafUnavailableException e) {
Debug.println(e);
            throw new IllegalStateException(e);
        }
    }

    /* */
    public void setSequence(InputStream stream)
        throws IOException,
               InvalidSmafDataException {

        this.setSequence(SmafSystem.getSequence(stream));
    }

    /* */
    public Sequence getSequence() {
        return sequence;
    }

    /* */
    public void start() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opend");
        }
        on();
        midiSequencer.start();
    }

    /* */
    public void stop() {
        if (midiSequencer == null) {
            throw new IllegalStateException("not opend");
        }
        midiSequencer.stop();
        off();
    }

    /* */
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

    //-------------------------------------------------------------------------

    /** {@link MetaMessage MetaEvent} ユーティリティ。 */
    private MetaSupport metaSupport = new MetaSupport();

    /* {@link MetaEventListener} を登録します。 */
    public void addMetaEventListener(MetaEventListener l) {
        metaSupport.addMetaEventListener(l);
    }

    /* {@link MetaEventListener} を削除します。 */
    public void removeMetaEventListener(MetaEventListener l) {
        metaSupport.removeMetaEventListener(l);
    }

    /** {@link MetaMessage MetaEvent} */
    protected void fireMeta(MetaMessage meta) {
        metaSupport.fireMeta(meta);
    }

    /** meta 0x2f listener */
    private javax.sound.midi.MetaEventListener mel = new javax.sound.midi.MetaEventListener() {
        /** */
        public void meta(javax.sound.midi.MetaMessage message) {
//Debug.println("type: " + message.getType());
            switch (message.getType()) {
            case 0x2f:  // 自動的に最後につけてくれる
                try {
                    MetaMessage metaMessage = new MetaMessage();
                    metaMessage.setMessage(0x2f, null);
                    fireMeta(metaMessage);
                    off();
                } catch (InvalidSmafDataException e) {
Debug.println(e);
                }
catch (RuntimeException e) {
Debug.printStackTrace(e);
 throw e;
} catch (Error e) {
Debug.printStackTrace(e);
 throw e;
}
                break;
            }
        }
    };

    // synthesizer

    /* @see vavi.sound.smaf.Synthesizer#getChannels() */
    @Override
    public MidiChannel[] getChannels() throws SmafUnavailableException {
        return midiSynthesizer.getChannels(); // TODO MFiChannel?
    }

    /* @see vavi.sound.smaf.Synthesizer#loadAllInstruments(javax.sound.midi.Soundbank) */
    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        return midiSynthesizer.loadAllInstruments(soundbank);
    }

    /* @see vavi.sound.smaf.Synthesizer#getAvailableInstruments() */
    @Override
    public Instrument[] getAvailableInstruments() {
        return midiSynthesizer.getAvailableInstruments();
    }

    /* @see vavi.sound.smaf.Synthesizer#getDefaultSoundbank() */
    @Override
    public Soundbank getDefaultSoundbank() {
        return midiSynthesizer.getDefaultSoundbank();
    }

    /* @see vavi.sound.smaf.Synthesizer#unloadAllInstruments(javax.sound.midi.Soundbank) */
    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        midiSynthesizer.unloadAllInstruments(soundbank);
    }

    /* */
    protected void finalize() {
        off();
    }
}

/* */
