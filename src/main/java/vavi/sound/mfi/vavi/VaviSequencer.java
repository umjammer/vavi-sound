/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.IOException;
import java.io.InputStream;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Soundbank;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MetaEventListener;
import vavi.sound.mfi.MetaMessage;
import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.MfiUnavailableException;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.Sequencer;
import vavi.sound.mfi.Synthesizer;
import vavi.sound.midi.MidiUtil;
import vavi.util.Debug;


/**
 * Sequencer implemented by vavi.
 * <p>
 * このシーケンサクラスで再生する場合は
 * システムプロパティ <code>javax.sound.midi.Sequencer</code> に <code>"#Real Time Sequencer"</code>
 * を明示するようにしてください。<code>"Java MIDI(MFi/SMAF) ADPCM Sequencer"</code> が
 * デフォルトシーケンサになった場合、{@link #mea}が重複して登録されてしまいます。
 * </p>
 * <p>
 * {@link javax.sound.midi.MidiSystem} を
 * 使用しているため javax.sound.midi SPI のプログラム内で使用してはいけません。
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.10 020627 nsano midi compliant <br>
 *          1.00 020706 nsano use midi sequencer <br>
 *          1.01 030606 nsano change error trap <br>
 *          1.02 030819 nsano change sequence related <br>
 *          1.03 030902 nsano out source {@link MetaEventListener} <br>
 */
class VaviSequencer implements Sequencer, Synthesizer {

    /** the device information */
    private static final MfiDevice.Info info =
        new MfiDevice.Info("Java MFi Sound Sequencer",
                           "Vavisoft",
                           "Software sequencer using midi",
                           "Version " + VaviMfiDeviceProvider.version) {};

    /** sound source of this sequencer */
    private javax.sound.midi.Sequencer midiSequencer;

    /** */
    private javax.sound.midi.Synthesizer midiSynthesizer;

    /** the sequence of MFi */
    private Sequence sequence;

    public VaviSequencer() {
        try {
            this.midiSequencer = MidiUtil.getDefaultSequencer(vavi.sound.midi.VaviMidiDeviceProvider.class);
Debug.println("★0 midiSequencer: " + midiSequencer);
            midiSequencer.addMetaEventListener(mel);
            midiSequencer.addMetaEventListener(mea);

            this.midiSynthesizer = MidiSystem.getSynthesizer();
        } catch (MidiUnavailableException e) {
Debug.printStackTrace(e);
            throw new IllegalStateException(e);
        }
    }

    /* */
    public MfiDevice.Info getDeviceInfo() {
        return info;
    }

    /* */
    public void close() {
        midiSequencer.close();
        midiSynthesizer.close();
Debug.println("★0 close: " + midiSequencer.hashCode());
    }

    /* */
    public boolean isOpen() {
        return midiSequencer.isOpen();
    }

    /** ADPCM sequencer */
    private javax.sound.midi.MetaEventListener mea = new MetaEventAdapter();

    /*
     * {@link javax.sound.midi.MidiSystem} を
     * 使用しているため javax.sound.midi SPI のプログラム内で使用してはいけません。
     */
    public void open() throws MfiUnavailableException {
        try {
Debug.println("★0 open: " + midiSequencer.hashCode());
            midiSequencer.open();
            midiSynthesizer.open();
        } catch (MidiUnavailableException e) {
Debug.printStackTrace(e);
            throw new MfiUnavailableException(e);
        }
    }

    /** */
    public void setSequence(Sequence sequence)
        throws InvalidMfiDataException {

        this.sequence = sequence;

        try {
            midiSequencer.setSequence(MfiSystem.toMidiSequence(sequence));
        } catch (InvalidMidiDataException e) {
Debug.println(e);
            throw new InvalidMfiDataException(e);
        } catch (MfiUnavailableException e) {
Debug.println(e);
            throw new IllegalStateException(e);
        }
    }

    /** */
    public void setSequence(InputStream stream)
        throws IOException,
               InvalidMfiDataException {

        this.setSequence(MfiSystem.getSequence(stream));
    }

    /** */
    public Sequence getSequence() {
        return sequence;
    }

    /** */
    public void start() {
        midiSequencer.start();
Debug.println("★0 start: " + midiSequencer.hashCode());
    }

    /** */
    public void stop() {
        midiSequencer.stop();
Debug.println("★0 stop: " + midiSequencer.hashCode());
    }

    /** */
    public boolean isRunning() {
        return midiSequencer.isRunning();
    }

    //-------------------------------------------------------------------------

    /** {@link vavi.sound.mfi.MetaMessage MetaEvent} ユーティリティ。 */
    private MetaSupport metaSupport = new MetaSupport();

    /** {@link MetaEventListener} を登録します。 */
    public void addMetaEventListener(MetaEventListener l) {
        metaSupport.addMetaEventListener(l);
    }

    /** {@link MetaEventListener} を削除します。 */
    public void removeMetaEventListener(MetaEventListener l) {
        metaSupport.removeMetaEventListener(l);
    }

    /** {@link vavi.sound.mfi.MetaMessage MetaEvent} */
    protected void fireMeta(MetaMessage meta) {
        metaSupport.fireMeta(meta);
    }

    /** meta 0x2f listener */
    private javax.sound.midi.MetaEventListener mel = new javax.sound.midi.MetaEventListener() {
        /** */
        public void meta(javax.sound.midi.MetaMessage message) {
Debug.println("★0 meta: type: " + message.getType());
            switch (message.getType()) {
            case 0x2f:  // 自動的に最後につけてくれる
                try {
                    MetaMessage metaMessage = new MetaMessage();
                    metaMessage.setMessage(0x2f, new byte[0], 0);
                    fireMeta(metaMessage);
                } catch (InvalidMfiDataException e) {
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

    /* @see vavi.sound.mfi.Synthesizer#getChannels() */
    @Override
    public MidiChannel[] getChannels() throws MfiUnavailableException {
        return midiSynthesizer.getChannels(); // TODO MFiChannel?
    }

    /* @see vavi.sound.mfi.Synthesizer#loadAllInstruments(javax.sound.midi.Soundbank) */
    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        return midiSynthesizer.loadAllInstruments(soundbank);
    }

    /* @see vavi.sound.mfi.Synthesizer#getAvailableInstruments() */
    @Override
    public Instrument[] getAvailableInstruments() {
        return midiSynthesizer.getAvailableInstruments();
    }

    /* @see vavi.sound.mfi.Synthesizer#getDefaultSoundbank() */
    @Override
    public Soundbank getDefaultSoundbank() {
        return midiSynthesizer.getDefaultSoundbank();
    }

    /* @see vavi.sound.mfi.Synthesizer#unloadAllInstruments(javax.sound.midi.Soundbank) */
    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        midiSynthesizer.unloadAllInstruments(soundbank);
    }

    protected void finalize() {
        midiSequencer.removeMetaEventListener(mel);
        midiSequencer.removeMetaEventListener(mea);
    }
}

/* */
