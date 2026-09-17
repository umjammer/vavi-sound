/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

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

import vavi.sound.smaf.SmafDevice.Info;
import vavi.sound.smaf.spi.SmafDeviceProvider;
import vavi.sound.smaf.spi.SmafFileReader;
import vavi.sound.smaf.spi.SmafFileWriter;
import vavi.sound.smaf.spi.SmafMidiConverter;

import static java.lang.System.getLogger;


/**
 * SMAF (*.mmf)
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 1.00 041223 nsano initial version <br>
 */
public final class SmafSystem {

    private static final Logger logger = getLogger(SmafSystem.class.getName());

    /** */
    private SmafSystem() {
    }

    /** Gets all device from the default provider */
    public static SmafDevice.Info[] getSmafDeviceInfo() {
        List<Info> result = new ArrayList<>();

        for (var provider : providers) {
            SmafDevice.Info[] infos = provider.getDeviceInfo();
            result.addAll(Arrays.asList(infos));
        }

        return result.toArray(SmafDevice.Info[]::new);
    }

    /** Get device by specified info from the default provider. */
    public static SmafDevice getSmafDevice(SmafDevice.Info info) throws SmafUnavailableException {

        for (var provider : providers) {
            SmafDevice.Info[] infos = provider.getDeviceInfo();
            for (SmafDevice.Info info_ : infos) {
                if (info_.equals(info)) {
                    return provider.getDevice(info);
                }
            }
        }

        throw new SmafUnavailableException("no sequencer available");
    }

    /** */
    private static <T extends SmafDevice> T getDevice(String clazz, String name, Class<T> deviceClass) throws SmafUnavailableException {
        logger.log(Level.INFO, "name: " + name + ", clazz: " + clazz + ", deviceClass: " + deviceClass);
        for (var provider : providers) {
            SmafDevice.Info[] infos = provider.getDeviceInfo();
            for (SmafDevice.Info info : infos) {
                if (info.name.equals(name)) {
                    SmafDevice device = provider.getDevice(info);
                    if (clazz != null && device.getClass().getName().equals(clazz)) {
                        return (T) device;
                    } else if (deviceClass.isInstance(device)) {
                        return (T) device;
                    }
                }
            }
        }

        throw new SmafUnavailableException("no sequencer available");
    }

    /** */
    private static String toKey(SmafDevice.Info info) throws SmafUnavailableException {
        SmafDevice device = getSmafDevice(info);
        return device.getClass().getName() + "#" + info.name;
    }

    /** Gets a sequencer. */
    public static Sequencer getSequencer() throws SmafUnavailableException {

        return getSequencer(false);
    }

    /** Gets a sequencer. */
    public static Sequencer getSequencer(boolean connected) throws SmafUnavailableException {
        try {
            String[] names = System.getProperty("vavi.sound.smaf.Sequencer", sequencerKey).split("#");

            vavi.sound.smaf.Sequencer sequencer = getDevice(names[0], names[1], vavi.sound.smaf.Sequencer.class);
            if (connected) {
                sequencer.open();
                sequencer.getTransmitter().setReceiver(getSynthesizer().getReceiver());
            }
            return sequencer;
        } catch (MidiUnavailableException e) {
            throw new SmafUnavailableException(e);
        }
    }

    /** Gets a listener to attach to a MIDI sequencer. */
    public static Synthesizer getSynthesizer() throws SmafUnavailableException {

        String[] names = System.getProperty("vavi.sound.samf.Synthesizer", synthesizerKey).split("#");

        return getDevice(names[0], names[1], vavi.sound.smaf.Synthesizer.class);
    }

    /** use #toSmafSequence(javax.sound.midi.Sequence sequence, int) */
    @Deprecated
    public static vavi.sound.smaf.Sequence toSmafSequence(javax.sound.midi.Sequence sequence)
            throws InvalidMidiDataException, SmafUnavailableException {

        for (SmafMidiConverter converter : converters) {
            if (converter.isFileTypeSupported(sequence)) {
                return converter.toSmafSequence(sequence);
            }
        }
        throw new InvalidMidiDataException();
    }

    /**
     * Convert a MIDI sequence into a MFi sequence.
     * @param type    midi file type
     * @see SmafFileFormat#type
     */
    public static vavi.sound.smaf.Sequence toSmafSequence(javax.sound.midi.Sequence sequence, int type)
            throws InvalidMidiDataException, SmafUnavailableException {

        for (SmafMidiConverter converter : converters) {
            if (converter.isFileTypeSupported(sequence)) {
                return converter.toSmafSequence(sequence, type);
            }
        }
        throw new InvalidMidiDataException();
    }

    /** Convert a MFi sequence into a MIDI sequence. */
    public static javax.sound.midi.Sequence toMidiSequence(vavi.sound.smaf.Sequence sequence)
            throws InvalidSmafDataException, SmafUnavailableException {

        for (SmafMidiConverter converter : converters) {
            if (converter.isFileTypeSupported(sequence)) {
                return converter.toMidiSequence(sequence);
            }
        }
        throw new InvalidSmafDataException();
    }

    /**
     * @param stream sequencer
     * @return SmafFileFormat
     */
    public static SmafFileFormat getSmafFileFormat(InputStream stream) throws InvalidSmafDataException, IOException {

//logger.log(Level.TRACE, "readers: " + readers.length);
        for (SmafFileReader reader : readers) {
            try {
                SmafFileFormat mff = reader.getSmafFileFormat(stream);
//logger.log(Level.TRACE, StringUtil.paramString(mff));
                return mff;
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getMessage(), e);
            }
        }

        throw new InvalidSmafDataException("unsupported stream: " + stream);
    }

    /** Gets MFi file format. */
    public static SmafFileFormat getSmafFileFormat(File file) throws InvalidSmafDataException, IOException {

        return getSmafFileFormat(new BufferedInputStream(Files.newInputStream(file.toPath())));
    }

    /** Gets MFi file format. */
    public static SmafFileFormat getSmafFileFormat(URL url) throws InvalidSmafDataException, IOException {

        return getSmafFileFormat(new BufferedInputStream(url.openStream()));
    }

    /**
     * @param stream sequencer
     * @return SMAF Sequence
     */
    public static Sequence getSequence(InputStream stream) throws InvalidSmafDataException, IOException {

//logger.log(Level.TRACE, "readers: " + readers.length);
        for (SmafFileReader reader : readers) {
            try {
                vavi.sound.smaf.Sequence sequence = reader.getSequence(stream);
//logger.log(Level.TRACE, StringUtil.paramString(sequence));
                return sequence;
            } catch (InvalidSmafDataException e) {
                logger.log(Level.DEBUG, e.getMessage(), e);
            }
        }

        throw new InvalidSmafDataException("unsupported stream: " + stream);
    }

    /** Gets a SMAF sequence. */
    public static vavi.sound.smaf.Sequence getSequence(File file) throws InvalidSmafDataException, IOException {

        return getSequence(new BufferedInputStream(Files.newInputStream(file.toPath())));
    }

    /** Gets a MFi sequence. */
    public static vavi.sound.smaf.Sequence getSequence(URL url) throws InvalidSmafDataException, IOException {

        return getSequence(new BufferedInputStream(url.openStream()));
    }

    /** Gets supported MFi file types. */
    public static int[] getSmafFileTypes() {
        List<Integer> types = new ArrayList<>();
        for (SmafFileWriter writer : writers) {
            int[] ts = writer.getSmafFileTypes();
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
    public static int[] getSmafFileTypes(vavi.sound.smaf.Sequence sequence) {
        List<Integer> types = new ArrayList<>();
        for (SmafFileWriter writer : writers) {
            int[] ts = writer.getSmafFileTypes(sequence);
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
    public static boolean isFileTypeSupported(int fileType, vavi.sound.smaf.Sequence sequence) {
        return StreamSupport.stream(writers.spliterator(), false).anyMatch(w -> w.isFileTypeSupported(fileType, sequence));
    }

    /** write MFi or MIDI */
    public static int write(vavi.sound.smaf.Sequence in, int fileType, OutputStream out) throws IOException {

        for (SmafFileWriter writer : writers) {
            if (writer.isFileTypeSupported(fileType, in)) {
                return writer.write(in, fileType, out);
            }
        }
        logger.log(Level.WARNING, "no writer found for: " + fileType);
        return 0;
    }

    /** write MFi or MIDI */
    public static int write(vavi.sound.smaf.Sequence in, int fileType, File out) throws IOException {

        return write(in, fileType, new BufferedOutputStream(Files.newOutputStream(out.toPath())));
    }

    // ----

    /** all providers */
    private static final ServiceLoader<SmafDeviceProvider> providers;
    /** all readers */
    private static final ServiceLoader<SmafFileReader> readers;
    /** all writers */
    private static final ServiceLoader<SmafFileWriter> writers;
    /** all converters */
    private static final ServiceLoader<SmafMidiConverter> converters;

    private static final String sequencerKey;
    private static final String synthesizerKey;

    private static String getKey(Class<?> clazz) throws Exception {
        Field field = clazz.getDeclaredField("info");
        field.setAccessible(true);
        return clazz.getName() + "#" + ((Info) field.get(null)).name;
    }

    /*
     * default sequencer, synthesizer, midiConverter classes are specified in SmafSystem.properties.
     */
    static {
        try {
            Properties props = new Properties();
            props.load(SmafSystem.class.getResourceAsStream("SmafSystem.properties"));
            sequencerKey = getKey(Class.forName(props.getProperty("vavi.sound.smaf.Sequencer")));
            synthesizerKey = getKey(Class.forName(props.getProperty("vavi.sound.smaf.Synthesizer")));
if (logger.isLoggable(Level.TRACE)) {
 System.err.println("sequencerKey: " + sequencerKey);
 System.err.println("synthesizerKey: " + synthesizerKey);
}

            providers = ServiceLoader.load(vavi.sound.smaf.spi.SmafDeviceProvider.class);
if (logger.isLoggable(Level.TRACE)) {
 providers.forEach(System.err::println);
}

            readers = ServiceLoader.load(vavi.sound.smaf.spi.SmafFileReader.class);
if (logger.isLoggable(Level.TRACE)) {
 readers.forEach(System.err::println);
}

            writers = ServiceLoader.load(vavi.sound.smaf.spi.SmafFileWriter.class);
if (logger.isLoggable(Level.TRACE)) {
 writers.forEach(System.err::println);
}
            converters = ServiceLoader.load(vavi.sound.smaf.spi.SmafMidiConverter.class);
if (logger.isLoggable(Level.TRACE)) {
 converters.forEach(System.err::println);
}
        } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }
}
