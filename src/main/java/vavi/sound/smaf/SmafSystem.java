/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;

import vavi.util.Debug;


/**
 * SMAF (*.mmf)
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 1.00 041223 nsano initial version <br>
 */
public final class SmafSystem {

    /** TODO SmafDeviceProvider */
    private static Sequencer sequencer;
    /** TODO SmafDeviceProvider */
    private static SmafMidiConverter converter;
    /** TODO SmafDeviceProvider */
    private static MetaEventListener waveSequencer;
    /** */
    private static SmafFileReader reader;
    /** */
    private static SmafFileWriter writer;

    /** */
    static {
        // TODO SmafDeviceProvider
        sequencer = new SmafSequencer();
        converter = new SmafMidiConverter();
        waveSequencer = new MetaEventAdapter();

        reader = new SmafFileReader();
        writer = new SmafFileWriter();
    }

    /** */
    private SmafSystem() {
    }

    /** シーケンサを取得します。 */
    public static Sequencer getSequencer()
        throws SmafUnavailableException {

        return sequencer;
    }

    /** MIDI シーケンサに付加するリスナを取得します。 */
    public static MetaEventListener getMetaEventListener()
        throws SmafUnavailableException {

        return waveSequencer;
    }

    /**
     * @param is
     * @return SmafFileFormat
     */
    public static SmafFileFormat getSmafFileFormat(InputStream is)
        throws InvalidSmafDataException, IOException {

        SmafFileFormat ff = reader.getSmafFileFormat(is);
        return ff;
    }

    /**
     * @param is
     * @return SMAF Sequence
     */
    public static vavi.sound.smaf.Sequence getSequence(InputStream is)
        throws InvalidSmafDataException, IOException {

        SmafFileFormat ff = getSmafFileFormat(is);
        return ff.getSequence();
    }

    /** SMAF シーケンスを取得します。 */
    public static vavi.sound.smaf.Sequence getSequence(File file)
        throws InvalidSmafDataException, IOException {

        return getSequence(new BufferedInputStream(new FileInputStream(file)));
    }

    /**
     * @param sequence SMAF Sequence
     * @return MIDI Sequence
     */
    public static Sequence toMidiSequence(vavi.sound.smaf.Sequence sequence)
        throws InvalidSmafDataException, SmafUnavailableException {

        try {
            return converter.convert(sequence);
        } catch (IOException e) {
Debug.printStackTrace(e);
            throw (InvalidSmafDataException) new InvalidSmafDataException().initCause(e);
        } catch (InvalidMidiDataException e) {
Debug.printStackTrace(e);
            throw (InvalidSmafDataException) new InvalidSmafDataException().initCause(e);
        }
    }

    /**
     * @param in MIDI Sequence
     * @param fileType
     * @return SMAF sequence
     */
    public static vavi.sound.smaf.Sequence toSmafSequence(Sequence in, int fileType)
        throws InvalidMidiDataException, SmafUnavailableException {

        try {
            return converter.convert(in, fileType);
        } catch (IOException e) {
Debug.printStackTrace(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        } catch (InvalidSmafDataException e) {
Debug.printStackTrace(e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        }
    }

    /**
     * @param smafSequence
     * @param fileType
     * @param out
     * @return SMAF sequence
     */
    public static int write(vavi.sound.smaf.Sequence smafSequence, int fileType, OutputStream out)
        throws IOException {

        return writer.write(smafSequence, fileType, out);
    }

    /** SMAF or MIDI で書き出します。 */
    public static int write(vavi.sound.smaf.Sequence smafSequence, int fileType, File out)
        throws IOException {

        return writer.write(smafSequence, fileType, out);
    }

    //-------------------------------------------------------------------------

    /**
     * Tests this class.
     * <pre>
     * usage:
     *  % java -Djavax.sound.midi.Sequencer="#Java MIDI(MFi/SMAF) ADPCM Sequencer" SmafSystem -p in_mmf_file
     *  % java SmafSystem -c in_mmf_file out_mid_file
     * </pre>
     */
    public static void main(String[] args) throws Exception {
//try {
        boolean convert = false;
        boolean play = false;
        
        if (args[0].equals("-c")) {
            convert = true;
        } else if (args[0].equals("-p")) {
            play = true;
        } else {
            throw new IllegalArgumentException(args[0]);
        }

        File file = new File(args[1]);
        vavi.sound.smaf.Sequence smafSequence = SmafSystem.getSequence(new BufferedInputStream(new FileInputStream(file)));
        Sequence midiSequence = SmafSystem.toMidiSequence(smafSequence);

        if (play) {
            javax.sound.midi.Sequencer midiSequencer = MidiSystem.getSequencer();
            midiSequencer.open();
            midiSequencer.setSequence(midiSequence);

            midiSequencer.start();
            while (midiSequencer.isRunning()) {
                try { Thread.sleep(100); } catch (Exception e) {}
            }
            midiSequencer.stop();

            midiSequencer.close();
        }
        
        if (convert) {
//Debug.println("☆☆☆ here: " + midiSequence);
            int ts[] = MidiSystem.getMidiFileTypes(midiSequence);
//Debug.println("★★★ here");
//Debug.println("types: " + ts.length);
            if (ts.length == 0) {
                throw new IllegalArgumentException("no support type");
            }
            for (int i = 0; i < ts.length; i++) {
//Debug.println("type: 0x" + StringUtil.toHex2(ts[i]));
            }

            file = new File(args[2]);
            int r = MidiSystem.write(midiSequence, 0, file);
Debug.println("write: " + r + " bytes as '" + args[2] + "'");
        }

        System.exit(0);
//} catch (Throwable t) {
// Debug.printStackTrace(t);
// System.exit(1);
//}
    }
}

/* */

