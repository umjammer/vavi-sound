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
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.stream.StreamSupport;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import vavi.sound.mfi.MfiDevice.Info;
import vavi.sound.mfi.spi.MfiDeviceProvider;
import vavi.sound.mfi.spi.MfiFileReader;
import vavi.sound.mfi.spi.MfiFileWriter;

import static java.lang.System.getLogger;


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

    private static final Logger logger = getLogger(MfiSystem.class.getName());

    /** cannot be access */
    private MfiSystem() {
    }

    /** Gets all device from the default provider */
    public static MfiDevice.Info[] getMfiDeviceInfo() {
        List<MfiDevice.Info> result = new ArrayList<>();

        for (var provider : providers) {
            MfiDevice.Info[] infos = provider.getDeviceInfo();
            result.addAll(Arrays.asList(infos));
        }

        return result.toArray(MfiDevice.Info[]::new);
    }

    /** Get device by specified info from the default provider. */
    public static MfiDevice getMfiDevice(MfiDevice.Info info) throws MfiUnavailableException {

        for (var provider : providers) {
            MfiDevice.Info[] infos = provider.getDeviceInfo();
            for (MfiDevice.Info info_ : infos) {
                if (info_.equals(info)) {
                    return provider.getDevice(info);
                }
            }
        }

        throw new MfiUnavailableException("no sequencer available");
    }

    /** */
    private static <T extends MfiDevice> T getDevice(String clazz, String name, Class<T> deviceClass) throws MfiUnavailableException {
logger.log(Level.INFO, "name: " + name + ", clazz: " + clazz + ", deviceClass: " + deviceClass);
        for (var provider : providers) {
            MfiDevice.Info[] infos = provider.getDeviceInfo();
            for (MfiDevice.Info info : infos) {
                if (info.name.equals(name)) {
                    MfiDevice device = provider.getDevice(info);
                    if (clazz != null && device.getClass().getName().equals(clazz)) {
                        return (T) device;
                    } else if (deviceClass.isInstance(device)) {
                        return (T) device;
                    }
                }
            }
        }

        throw new MfiUnavailableException("no sequencer available");
    }

    /** */
    private static String toKey(MfiDevice.Info info) throws MfiUnavailableException {
        MfiDevice device = getMfiDevice(info);
        return device.getClass().getName() + "#" + info.name;
    }

    /**
     * Get a sequencer from the default provider.
     * <p>
     * when playing by {@link #getSequencer()},
     * set system property <code>vavi.sound.mfi.Sequencer</code> <code>"#Real Time Sequencer"</code>
     * unless if <code>"Java MFi Sound Sequencer"</code> become
     * default sequencer.
     * </p>
     */
    public static Sequencer getSequencer() throws MfiUnavailableException {
        return getSequencer(false);
    }

    /** */
    public static Sequencer getSequencer(boolean connected) throws MfiUnavailableException {
        try {
            String[] names = System.getProperty("vavi.sound.mfi.Sequencer", sequencerKey).split("#");

            Sequencer sequencer = getDevice(names[0], names[1], Sequencer.class);
            if (connected) {
                sequencer.open();
                sequencer.getTransmitter().setReceiver(getSynthesizer().getReceiver());
            }
            return sequencer;
        } catch (MidiUnavailableException e) {
            throw new MfiUnavailableException(e);
        }
    }

    /** Gets a listener to add MIDI sequencer. */
    public static Synthesizer getSynthesizer() throws MfiUnavailableException {

        String[] names = System.getProperty("vavi.sound.mfi.Synthesizer", synthesizerKey).split("#");

        return getDevice(names[0], names[1], Synthesizer.class);
    }

    /** Gets a MIDI - MFi converter from the default provider. */
    public static MidiConverter getMidiConverter() throws MfiUnavailableException {

        String[] names = System.getProperty("vavi.sound.mfi.MidiConverter", midiConverterKey).split("#");

        return getDevice(names[0], names[1], MidiConverter.class);
    }

    /** use #toMfiSequence(javax.sound.midi.Sequence sequence, int) */
    @Deprecated
    public static Sequence toMfiSequence(javax.sound.midi.Sequence sequence)
        throws InvalidMidiDataException, MfiUnavailableException {

        MidiConverter converter = getMidiConverter();
        return converter.toMfiSequence(sequence);
    }

    /**
     * Convert a MIDI sequence into a MFi sequence.
     * @param type    midi file type
     * @see MfiFileFormat#type
     */
    public static Sequence toMfiSequence(javax.sound.midi.Sequence sequence, int type)
        throws InvalidMidiDataException, MfiUnavailableException {

        MidiConverter converter = getMidiConverter();
        return converter.toMfiSequence(sequence, type);
    }

    /** Convert a MFi sequence into a MIDI sequence. */
    public static javax.sound.midi.Sequence toMidiSequence(Sequence sequence)
        throws InvalidMfiDataException, MfiUnavailableException {

        MidiConverter converter = getMidiConverter();
//logger.log(Level.TRACE, converter);
        return converter.toMidiSequence(sequence);
    }

    /** Gets MFi file format. */
    public static MfiFileFormat getMfiFileFormat(InputStream stream)
        throws InvalidMfiDataException, IOException {

//logger.log(Level.TRACE, "readers: " + readers.length);
        for (MfiFileReader reader : readers) {
            try {
                MfiFileFormat mff = reader.getMfiFileFormat(stream);
//logger.log(Level.TRACE, StringUtil.paramString(mff));
                return mff;
            } catch (Exception e) {
logger.log(Level.WARNING, e.getMessage(), e);
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

//logger.log(Level.TRACE, "readers: " + readers.length);
        for (MfiFileReader reader : readers) {
            try {
                Sequence sequence = reader.getSequence(stream);
//logger.log(Level.TRACE, StringUtil.paramString(sequence));
                return sequence;
            } catch (InvalidMfiDataException e) {
logger.log(Level.DEBUG, e);
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
logger.log(Level.WARNING, "no writer found for: " + fileType);
        return 0;
    }

    /** write MFi or MIDI */
    public static int write(Sequence in, int fileType, File out) throws IOException {

        return write(in, fileType, new BufferedOutputStream(Files.newOutputStream(out.toPath())));
    }

    // ----

    /** all providers */
    private static final ServiceLoader<MfiDeviceProvider> providers;
    /** all readers */
    private static final ServiceLoader<MfiFileReader> readers;
    /** all writers */
    private static final ServiceLoader<MfiFileWriter> writers;

    private static final String sequencerKey;
    private static final String synthesizerKey;
    private static final String midiConverterKey;

    private static String getKey(Class<?> clazz) throws Exception {
        Field field = clazz.getDeclaredField("info");
        field.setAccessible(true);
        return clazz.getName() + "#" + ((Info) field.get(null)).name;
    }

    /*
     * default sequencer, synthesizer, midiConverter classes are specified in MfiSystem.properties.
     */
    static {
        try {
            Properties props = new Properties();
            props.load(MfiSystem.class.getResourceAsStream("MfiSystem.properties"));
            sequencerKey = getKey(Class.forName(props.getProperty("vavi.sound.mfi.Sequencer")));
            synthesizerKey = getKey(Class.forName(props.getProperty("vavi.sound.mfi.Synthesizer")));
            midiConverterKey = getKey(Class.forName(props.getProperty("vavi.sound.mfi.MidiConverter")));
if (logger.isLoggable(Level.TRACE)) {
 System.err.println("sequencerKey: " + sequencerKey);
 System.err.println("synthesizerKey: " + synthesizerKey);
 System.err.println("midiConverterKey: " + midiConverterKey);
}

            providers = ServiceLoader.load(vavi.sound.mfi.spi.MfiDeviceProvider.class);
if (logger.isLoggable(Level.TRACE)) {
 providers.forEach(System.err::println);
}

            readers = ServiceLoader.load(vavi.sound.mfi.spi.MfiFileReader.class);
if (logger.isLoggable(Level.TRACE)) {
 readers.forEach(System.err::println);
}

            writers = ServiceLoader.load(vavi.sound.mfi.spi.MfiFileWriter.class);
if (logger.isLoggable(Level.TRACE)) {
 writers.forEach(System.err::println);
}
        } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }
}
