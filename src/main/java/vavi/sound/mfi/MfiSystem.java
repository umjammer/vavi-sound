/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;

import javax.sound.midi.InvalidMidiDataException;

import vavi.sound.mfi.spi.MfiDeviceProvider;
import vavi.sound.mfi.spi.MfiFileReader;
import vavi.sound.mfi.spi.MfiFileWriter;
import vavi.util.Debug;


/**
 * MfiSystem.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.01 021222 nsano use META-INF/services files <br>
 *          0.02 030817 nsano uncomment isFileTypeSupported <br>
 *          0.03 030819 nsano add toMidiSequence, toMfiSequence <br>
 *          0.04 031212 nsano add toMfiSequence(Sequence, int) <br>
 */
public final class MfiSystem {

    /** アクセスできません。 */
    private MfiSystem() {
    }

    /** デフォルトプロバイダからすべてのデバイスを取得します。 */
    public static MfiDevice.Info[] getMfiDeviceInfo() {
        return provider.getDeviceInfo(); // TODO プロバイダまわすべき
    }

    /** デフォルトプロバイダから指定した情報のデバイスを取得します。 */
    public static MfiDevice getMfiDevice(MfiDevice.Info info)
        throws MfiUnavailableException {

        return provider.getDevice(info); // TODO プロバイダまわすべき
    }

    /**
     * デフォルトプロバイダからシーケンサを取得します。
     * <p>
     * {@link #getSequencer()} で再生する場合は
     * システムプロパティ <code>javax.sound.midi.Sequencer</code> に <code>"#Real Time Sequencer"</code>
     * を明示するようにしてください。<code>"Java MIDI(MFi/SMAF) ADPCM Sequencer"</code> が
     * デフォルトシーケンサになった場合、{@link #getMetaEventListener()}で取得できるリスナー
     * が重複して登録されてしまいます。
     * </p>
     */
    public static Sequencer getSequencer()
        throws MfiUnavailableException {

        MfiDevice.Info[] infos = provider.getDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            MfiDevice device = provider.getDevice(infos[i]);
            if (device instanceof Sequencer) {
                return (Sequencer) device;
            }
        }
        // TODO なければ他のプロバイダを探すべき
        throw new MfiUnavailableException("no sequencer available");
    }

    /** MIDI シーケンサに付加するリスナを取得します。 */
    public static javax.sound.midi.MetaEventListener getMetaEventListener()
        throws MfiUnavailableException {

        MfiDevice.Info[] infos = provider.getDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            MfiDevice device = provider.getDevice(infos[i]);
            if (device instanceof javax.sound.midi.MetaEventListener) {
                return (javax.sound.midi.MetaEventListener) device;
            }
        }

        // TODO なければ他のプロバイダを探すべき
        throw new MfiUnavailableException("no MetaEventListener available");
    }

    /** デフォルトプロバイダから MIDI - MFi コンバータを取得します。 */
    public static MidiConverter getMidiConverter()
        throws MfiUnavailableException {

        MfiDevice.Info[] infos = provider.getDeviceInfo();
        for (int i = 0; i < infos.length; i++) {
            MfiDevice device = provider.getDevice(infos[i]);
            if (device instanceof MidiConverter) {
                return (MidiConverter) device;
            }
        }
        // TODO なければ他のプロバイダを探すべき
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
     * MIDI シーケンスを MFi シーケンスに変換します。
     * @param type    midi file type
     * @see MfiFileFormat#type
     */
    public static Sequence toMfiSequence(javax.sound.midi.Sequence sequence,
                                         int type)
        throws InvalidMidiDataException, MfiUnavailableException {

        MidiConverter converter = MfiSystem.getMidiConverter();
        return converter.toMfiSequence(sequence, type);
    }

    /** MFi シーケンスを MIDI シーケンスに変換します。 */
    public static javax.sound.midi.Sequence toMidiSequence(Sequence sequence)
        throws InvalidMfiDataException, MfiUnavailableException {

        MidiConverter converter = MfiSystem.getMidiConverter();
//Debug.println(converter);
        return converter.toMidiSequence(sequence);
    }

    /** MFi ファイルフォーマットを取得します。 */
    public static MfiFileFormat getMfiFileFormat(InputStream stream)
        throws InvalidMfiDataException,
               IOException {

//Debug.println("readers: " + readers.length);
        for (int i = 0; i < readers.length; i++) {
            try {
                MfiFileFormat mff = readers[i].getMfiFileFormat(stream);
//Debug.println(StringUtil.paramString(mff));
                return mff;
            } catch (Exception e) {
Debug.println(e);
            }
        }

        throw new InvalidMfiDataException("unsupported stream: " + stream);
    }

    /** MFi ファイルフォーマットを取得します。 */
    public static MfiFileFormat getMfiFileFormat(File file)
        throws InvalidMfiDataException,
               IOException {

        return getMfiFileFormat(new BufferedInputStream(new FileInputStream(file)));
    }

    /** MFi ファイルフォーマットを取得します。 */
    public static MfiFileFormat getMfiFileFormat(URL url)
        throws InvalidMfiDataException,
               IOException {

        return getMfiFileFormat(new BufferedInputStream(url.openStream()));
    }

    /** MFi シーケンスを取得します。 */
    public static Sequence getSequence(InputStream stream)
        throws InvalidMfiDataException,
               IOException {

//Debug.println("readers: " + readers.length);
        for (int i = 0; i < readers.length; i++) {
            try {
                Sequence sequence = readers[i].getSequence(stream);
//Debug.println(StringUtil.paramString(sequence));
                return sequence;
            } catch (InvalidMfiDataException e) {
Debug.println(e);
                continue;
            }
        }

        throw new InvalidMfiDataException("unsupported stream: " + stream);
    }

    /** MFi シーケンスを取得します。 */
    public static Sequence getSequence(File file)
        throws InvalidMfiDataException,
               IOException {

        return getSequence(new BufferedInputStream(new FileInputStream(file)));
    }

    /** MFi シーケンスを取得します。 */
    public static Sequence getSequence(URL url)
        throws InvalidMfiDataException,
               IOException {

        return getSequence(new BufferedInputStream(url.openStream()));
    }

    /** サポートする MFi ファイルタイプを取得します。 */
    public static int[] getMfiFileTypes() {
        List<Integer> types = new ArrayList<>();
        for (int i = 0; i < writers.length; i++) {
            int[] ts = writers[i].getMfiFileTypes();
            for (int j = 0; j < ts.length; j++) {
                types.add(ts[j]);
            }
        }

        int [] result = new int[types.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = types.get(i);
        }
        
        return result;
    }

    /** 指定したシーケンスに対応する MFi ファイルタイプを取得します。 */
    public static int[] getMfiFileTypes(Sequence sequence) {
        List<Integer> types = new ArrayList<>();
        for (int i = 0; i < writers.length; i++) {
            int[] ts = writers[i].getMfiFileTypes(sequence);
            for (int j = 0; j < ts.length; j++) {
                types.add(ts[j]);
            }
        }

        int [] result = new int[types.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = types.get(i);
        }
        
        return result;
    }

    /** ファイルタイプがサポートされるかどうかを返します。 */
    public static boolean isFileTypeSupported(int fileType) {
        for (int i = 0; i < writers.length; i++) {
            if (writers[i].isFileTypeSupported(fileType)) {
                return true;
            }
        }
        return false;
    }

    /** ファイルタイプが指定したシーケンスでサポートされるかどうかを返します。 */
    public static boolean isFileTypeSupported(int fileType, Sequence sequence) {
        for (int i = 0; i < writers.length; i++) {
            if (writers[i].isFileTypeSupported(fileType, sequence)) {
                return true;
            }
        }
        return false;
    }

    /** MFi or MIDI で書き出します。 */
    public static int write(Sequence in, int fileType, OutputStream out)
        throws IOException {

        for (int i = 0; i < writers.length; i++) {
            if (writers[i].isFileTypeSupported(fileType, in)) {
                return writers[i].write(in, fileType, out);
            }
        }
Debug.println(Level.WARNING, "no writer found for: " + fileType);
        return 0;
    }

    /** MFi or MIDI で書き出します。 */
    public static int write(Sequence in, int fileType, File out)
        throws IOException {

        return write(in, fileType, new BufferedOutputStream(new FileOutputStream(out)));
    }

    //-------------------------------------------------------------------------

    /** all プロバイダ */
    private static MfiDeviceProvider[] providers;
    /** all リーダ */
    private static MfiFileReader[] readers;
    /** all ライタ */
    private static MfiFileWriter[] writers;

    /** default プロバイダ */
    private static MfiDeviceProvider provider;

    /**
     * default は MfiSystem.properties で指定します。
     * <li>vavi.sound.mfi.spi.MfiDeviceProvider
     */
    static {
        final String dir = "/META-INF/services/";
        final String providerFile = "vavi.sound.mfi.spi.MfiDeviceProvider";
        final String readerFile = "vavi.sound.mfi.spi.MfiFileReader";
        final String writerFile = "vavi.sound.mfi.spi.MfiFileWriter";

        Properties props = new Properties();
        Properties mfiSystemProps = new Properties();

        try {
            Class<?> clazz = MfiSystem.class;

            mfiSystemProps.load(clazz.getResourceAsStream("MfiSystem.properties"));
            String defaultProvider = mfiSystemProps.getProperty("default.provider");

            props.load(clazz.getResourceAsStream(dir + providerFile));
props.list(System.err);
            Enumeration<?> e = props.propertyNames();
            int i = 0;
            providers = new MfiDeviceProvider[props.size()];
            while (e.hasMoreElements()) {
                @SuppressWarnings("unchecked")
                Class<MfiDeviceProvider> c = (Class<MfiDeviceProvider>) Class.forName((String) e.nextElement());
                providers[i] = c.newInstance();
//Debug.println("COMPARE: " + c.getName() + ", " + defaultProvider);
                if (c.getName().equals(defaultProvider)) {
                    provider = providers[i];
                }
                i++;
            }
Debug.println("default provider: " + provider.getClass().getName());

            props.clear();
            props.load(clazz.getResourceAsStream(dir + readerFile));
props.list(System.err);
            e = props.propertyNames();
            i = 0;
            readers = new MfiFileReader[props.size()];
            while (e.hasMoreElements()) {
                @SuppressWarnings("unchecked")
                Class<MfiFileReader> c = (Class<MfiFileReader>) Class.forName((String) e.nextElement());
                readers[i++] = c.newInstance();
            }

            props.clear();
            props.load(clazz.getResourceAsStream(dir + writerFile));
props.list(System.err);
            e = props.propertyNames();
            i = 0;
            writers = new MfiFileWriter[props.size()];
            while (e.hasMoreElements()) {
                @SuppressWarnings("unchecked")
                Class<MfiFileWriter> c = (Class<MfiFileWriter>) Class.forName((String) e.nextElement());
                writers[i++] = c.newInstance();
            }
        } catch (Exception e) {
Debug.println(Level.SEVERE, e);
            throw new IllegalStateException(e);
        }
    }
}

/* */
