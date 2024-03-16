/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Level;
import java.util.stream.StreamSupport;

import javax.sound.midi.InvalidMidiDataException;

import vavi.sound.mfi.spi.MfiDeviceProvider;
import vavi.sound.mfi.spi.MfiFileReader;
import vavi.sound.mfi.spi.MfiFileWriter;
import vavi.util.Debug;


/**
 * MfiSystem.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 021222 nsano use META-INF/services files <br>
 *          0.02 030817 nsano uncomment isFileTypeSupported <br>
 *          0.03 030819 nsano add toMidiSequence, toMfiSequence <br>
 *          0.04 031212 nsano add toMfiSequence(Sequence, int) <br>
 */
public final class MfiSystem {

    /** cannot be access */
    private MfiSystem() {
    }

    /** Gets all device from the default provider */
    public static MfiDevice.Info[] getMfiDeviceInfo() {
        return provider.getDeviceInfo(); // TODO should search all providers
    }

    /** Get device by specified info from the default provider. */
    public static MfiDevice getMfiDevice(MfiDevice.Info info)
        throws MfiUnavailableException {

        return provider.getDevice(info); // TODO should search all providers
    }

    /**
     * Get a sequencer from the default provider.
     * <p>
     * when playing by {@link #getSequencer()},
     * set system property <code>javax.sound.midi.Sequencer</code> <code>"#Real Time Sequencer"</code>
     * unless if <code>"Java MIDI(MFi/SMAF) ADPCM Sequencer"</code> become
     * default sequencer, listeners get by {@link #getMetaEventListener()} are
     * registered duplicate.
     * </p>
     */
    public static Sequencer getSequencer()
        throws MfiUnavailableException {

        MfiDevice.Info[] infos = provider.getDeviceInfo();
        for (MfiDevice.Info info : infos) {
            MfiDevice device = provider.getDevice(info);
            if (device instanceof Sequencer) {
                return (Sequencer) device;
            }
        }
        // TODO should search other providers when not found
        throw new MfiUnavailableException("no sequencer available");
    }

    /** Gets a listener to add MIDI sequencer. */
    public static javax.sound.midi.MetaEventListener getMetaEventListener()
        throws MfiUnavailableException {

        MfiDevice.Info[] infos = provider.getDeviceInfo();
        for (MfiDevice.Info info : infos) {
            MfiDevice device = provider.getDevice(info);
            if (device instanceof javax.sound.midi.MetaEventListener) {
                return (javax.sound.midi.MetaEventListener) device;
            }
        }

        // TODO should search other providers when not found
        throw new MfiUnavailableException("no MetaEventListener available");
    }

    /** Gets a MIDI - MFi converter from the default provider. */
    public static MidiConverter getMidiConverter()
        throws MfiUnavailableException {

        MfiDevice.Info[] infos = provider.getDeviceInfo();
        for (MfiDevice.Info info : infos) {
            MfiDevice device = provider.getDevice(info);
            if (device instanceof MidiConverter) {
                return (MidiConverter) device;
            }
        }
        // TODO should search other providers when not found
        throw new MfiUnavailableException("no midiConverter available");
    }

    /** use #toMfiSequence(javax.sound.midi.Sequence sequence, int) */
    @Deprecated
    public static Sequence toMfiSequence(javax.sound.midi.Sequence sequence)
        throws InvalidMidiDataException, MfiUnavailableException {

        MidiConverter converter = MfiSystem.getMidiConverter();
        return converter.toMfiSequence(sequence);
    }

    /**
     * Convert a MIDI sequence into a MFi sequence.
     * @param type    midi file type
     * @see MfiFileFormat#type
     */
    public static Sequence toMfiSequence(javax.sound.midi.Sequence sequence, int type)
        throws InvalidMidiDataException, MfiUnavailableException {

        MidiConverter converter = MfiSystem.getMidiConverter();
        return converter.toMfiSequence(sequence, type);
    }

    /** Convert a MFi sequence into a MIDI sequence. */
    public static javax.sound.midi.Sequence toMidiSequence(Sequence sequence)
        throws InvalidMfiDataException, MfiUnavailableException {

        MidiConverter converter = MfiSystem.getMidiConverter();
//Debug.println(converter);
        return converter.toMidiSequence(sequence);
    }

    /** Gets MFi file format. */
    public static MfiFileFormat getMfiFileFormat(InputStream stream)
        throws InvalidMfiDataException, IOException {

//Debug.println("readers: " + readers.length);
        for (MfiFileReader reader : readers) {
            try {
                MfiFileFormat mff = reader.getMfiFileFormat(stream);
//Debug.println(StringUtil.paramString(mff));
                return mff;
            } catch (Exception e) {
Debug.println(Level.WARNING, e);
            }
        }

        throw new InvalidMfiDataException("unsupported stream: " + stream);
    }

    /** Gets MFi file format. */
    public static MfiFileFormat getMfiFileFormat(File file) throws InvalidMfiDataException, IOException {

        return getMfiFileFormat(new BufferedInputStream(Files.newInputStream(file.toPath())));
    }

    /** Gets MFi file format. */
    public static MfiFileFormat getMfiFileFormat(URL url) throws InvalidMfiDataException, IOException {

        return getMfiFileFormat(new BufferedInputStream(url.openStream()));
    }

    /** Gets a MFi sequence. */
    public static Sequence getSequence(InputStream stream) throws InvalidMfiDataException, IOException {

//Debug.println("readers: " + readers.length);
        for (MfiFileReader reader : readers) {
            try {
                Sequence sequence = reader.getSequence(stream);
//Debug.println(StringUtil.paramString(sequence));
                return sequence;
            } catch (InvalidMfiDataException e) {
Debug.println(Level.FINE, e);
                continue;
            }
        }

        throw new InvalidMfiDataException("unsupported stream: " + stream);
    }

    /** Gets a MFi sequence. */
    public static Sequence getSequence(File file) throws InvalidMfiDataException, IOException {

        return getSequence(new BufferedInputStream(Files.newInputStream(file.toPath())));
    }

    /** Gets a MFi sequence. */
    public static Sequence getSequence(URL url) throws InvalidMfiDataException, IOException {

        return getSequence(new BufferedInputStream(url.openStream()));
    }

    /** Gets supported MFi file types. */
    public static int[] getMfiFileTypes() {
        List<Integer> types = new ArrayList<>();
        for (MfiFileWriter writer : writers) {
            int[] ts = writer.getMfiFileTypes();
            for (int t : ts) {
                types.add(t);
            }
        }

        int [] result = new int[types.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = types.get(i);
        }

        return result;
    }

    /** Get a MFi file type by specified sequence. */
    public static int[] getMfiFileTypes(Sequence sequence) {
        List<Integer> types = new ArrayList<>();
        for (MfiFileWriter writer : writers) {
            int[] ts = writer.getMfiFileTypes(sequence);
            for (int t : ts) {
                types.add(t);
            }
        }

        int [] result = new int[types.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = types.get(i);
        }

        return result;
    }

    /** Returns if a file type is supported or not. */
    public static boolean isFileTypeSupported(int fileType) {
        return StreamSupport.stream(writers.spliterator(), false).anyMatch(w -> w.isFileTypeSupported(fileType));
    }

    /** Returns if a file type is supported or not by specified sequence. */
    public static boolean isFileTypeSupported(int fileType, Sequence sequence) {
        return StreamSupport.stream(writers.spliterator(), false).anyMatch(w -> w.isFileTypeSupported(fileType, sequence));
    }

    /** write MFi or MIDI */
    public static int write(Sequence in, int fileType, OutputStream out) throws IOException {

        for (MfiFileWriter writer : writers) {
            if (writer.isFileTypeSupported(fileType, in)) {
                return writer.write(in, fileType, out);
            }
        }
Debug.println(Level.WARNING, "no writer found for: " + fileType);
        return 0;
    }

    /** write MFi or MIDI */
    public static int write(Sequence in, int fileType, File out) throws IOException {

        return write(in, fileType, new BufferedOutputStream(Files.newOutputStream(out.toPath())));
    }

    //----

    /** all providers */
    private static ServiceLoader<MfiDeviceProvider> providers;
    /** all readers */
    private static ServiceLoader<MfiFileReader> readers;
    /** all writers */
    private static ServiceLoader<MfiFileWriter> writers;

    /** default provider */
    private static MfiDeviceProvider provider;

    /*
     * default is specified by MfiSystem.properties.
     * <li>vavi.sound.mfi.spi.MfiDeviceProvider
     */
    static {
        Properties mfiSystemProps = new Properties();

        try {
            Class<?> clazz = MfiSystem.class;

            mfiSystemProps.load(clazz.getResourceAsStream("MfiSystem.properties"));
            String defaultProvider = mfiSystemProps.getProperty("default.provider");

            providers = ServiceLoader.load(vavi.sound.mfi.spi.MfiDeviceProvider.class);
providers.forEach(System.err::println);
            provider = StreamSupport.stream(providers.spliterator(), false).filter(p -> p.getClass().getName().equals(defaultProvider)).findFirst().get();
Debug.println(Level.FINE, "default provider: " + provider.getClass().getName());

            readers = ServiceLoader.load(vavi.sound.mfi.spi.MfiFileReader.class);
providers.forEach(System.err::println);

            writers = ServiceLoader.load(vavi.sound.mfi.spi.MfiFileWriter.class);
providers.forEach(System.err::println);
        } catch (Exception e) {
Debug.println(Level.SEVERE, e);
            throw new IllegalStateException(e);
        }
    }
}
