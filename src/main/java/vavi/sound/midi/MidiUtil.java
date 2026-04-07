/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiDevice.Info;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.spi.MidiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * MidiUtil.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041230 nsano initial version <br>
 */
public final class MidiUtil {

    private static final Logger logger = getLogger(MidiUtil.class.getName());

    /** this is utility class */
    private MidiUtil() {
    }

    /**
     * should be called before {@link javax.sound.midi.Sequencer#setSequence}
     * @param volume 0 ~ 1.0
     */
    public static void volume(Receiver receiver, float volume) throws InvalidMidiDataException {
        int value = (int) (16383 * volume);
        byte[] data = { (byte) 0xf0, 0x7f, 0x7f, 0x04, 0x01, (byte) (value & 0x7f), (byte) ((value >> 7) & 0x7f), (byte) 0xf7 };
        MidiMessage sysex = new SysexMessage(data, data.length);
        receiver.send(sysex, -1);
    }

    /**
     * for {@link MidiMessage#toString}
     */
    public static String paramString(MidiMessage midiMessage) {
        String result = null;
        if (midiMessage instanceof ShortMessage msg) {
            int channel = msg.getChannel();
            int command = msg.getCommand();
            int data1 = msg.getData1();
            int data2 = msg.getData2();
            result = "channel=" + (channel + 1) +
                ",event=" + getChannelMessage(command, data1) +
                ",data1=" + (command == ShortMessage.PROGRAM_CHANGE ? data1 + " " + MidiConstants.getInstrumentName(data1) : String.valueOf(data1)) +
                ",data2=" + data2;
        } else if (midiMessage instanceof SysexMessage msg) {
            byte[] data = msg.getData();
            StringBuilder sb = new StringBuilder();
            for (byte datum : data) {
                sb.append("%02x".formatted(datum));
                sb.append(" ");
            }
            result = "channel=n/a" +
                ",event=SYSX" +
                ",data1=" + sb +
                ",data2=";
        } else if (midiMessage instanceof MetaMessage msg) {
            int type = msg.getType();
            byte[] data = msg.getData();
            StringBuilder sb = new StringBuilder();
            for (byte datum : data) {
                sb.append("%02x".formatted(datum));
                sb.append(" ");
            }
            result = "channel=n/a" +
                ",event=meta" +
                ",data1=" + MidiConstants.MetaEvent.valueOf(type) +
                ",data2=" + sb;
        }

        return result;
    }

    /** Gets strings stands for status byte. */
    private static String getChannelMessage(int statusByte, int value1) {
        switch (statusByte / 16) {
        case 8: // 128, 0x80
            return "NOTE_OFF";
        case 9: // 144, 0x90
            return "NOTE_ON";
        case 10:    // 160, 0xa0
            return "POLY_PRESSURE";
        case 11:    // 176, 0xb0
            if (value1 >= 120) { // 0x78
                return "CHANNEL_MODE_MESSAGE";
            } else {
                return "CONTROL_CHANGE";
            }
        case 12:    // 192, 0xc0
            return "PROGRAM_CHANGE";
        case 13:    // 208, 0xd0
            return "CHANNEL_PRESSURE";
        case 14:    // 224, 0xe0
            return "PITCH_BEND_CHANGE";
        default:
            return String.valueOf(statusByte);
        }
    }

    /**
     * read 1 ~ 4 bytes
     * @return 0 ~ 268435455 (0x0fff_ffff)
     */
    public static int readVariableLength(DataInput input) throws IOException {
        int b = input.readUnsignedByte();
        int v = b & 0x7f;
        while ((b & 0x80) != 0) {
            b = input.readUnsignedByte();
            v = (v << 7) + (b & 0x7F);
        }
        return v;
    }

    /**
     * Writes a variable-size int to output, as defined by the Midi format.
     * @param output - The stream to write to.
     * @throws IOException on any write error.
     */
    public static void writeVarInt(DataOutput output, int value) throws IOException {
        // The bits are laid out like this:
        // 11112222 22233333 33444444 45555555
        //
        // It's easier to just enumerate them than to make a loop.
        //
        if ((value & 0xf000_0000) != 0) {
            output.write(0x80 | ((value >> 28) & 0x7f));
        }
        if ((value & 0xffe0_0000) != 0) {
            output.write(0x80 | ((value >> 21) & 0x7f));
        }
        if ((value & 0xffff_c000) != 0) {
            output.write(0x80 | ((value >> 14) & 0x7f));
        }
        if ((value & 0xffff_ff80) != 0) {
            output.write(0x80 | ((value >> 7) & 0x7f));
        }
        output.write(value & 0x7f);
    }

    /**
     * to 7bit
     * @see "https://qiita.com/ringorou/items/5e2384f7cf226d9e648a"
     * @see "https://chatgpt.com/c/69bfda03-4618-83a8-b338-ec80f9dc81e6"
     */
    public static int encode87(byte[] src, byte[] dst, int off, int len) {
        int n = off;
        int end = off + len;
        int pt = 0;

        while (n < end) {
            int b8 = 0;

            for (int i = 0; i < 7 && n < end; i++, n++) {
                int v = src[n] & 0xff;
                dst[pt++] = (byte) (v & 0x7f);
                b8 |= ((v >>> 7) & 1) << i;
            }

            dst[pt++] = (byte) b8;
        }

        return pt; // actual encoded size
    }

    /**
     * to 8bit
     * @see "https://qiita.com/ringorou/items/5e2384f7cf226d9e648a"
     * @see "https://chatgpt.com/c/69bfda03-4618-83a8-b338-ec80f9dc81e6"
     */
    public static int decode87(byte[] src, byte[] dst, int off, int len) {
        int n = off;
        int end = off + len;
        int pt = 0;

        while (n < end) {
            int blockStart = pt;
            int i = 0;

            for (; i < 7 && n < end - 1; i++, n++) {
                dst[pt++] = src[n];
            }

            int b8 = src[n++] & 0xff;

            for (int j = 0; j < i; j++) {
                int v = dst[blockStart + j] & 0xff;
                v |= ((b8 >> j) & 1) << 7;
                dst[blockStart + j] = (byte) v;
            }
        }

        return pt;
    }

    /** encoding for midi meta message */
    private static String decodingEncoding = "JISAutoDetect";

    /** compatible with MIDI data starting with 0xff (encoded) */
    public static String getDecodedMessage(byte[] data) {
        return getDecodedMessage(data, decodingEncoding);
    }

    /** compatible with MIDI data starting with 0xff (encoded) */
    public static String getDecodedMessage(byte[] data, String encoding) {
        int start = 0;
        int length = data.length;
        if ((data[0] & 0xff) == 0xff) {
            // 00 0xff
            // 01 0x03 ?
            // 02      length
            try {
//logger.log(Level.TRACE, "META: " + data[1] + ", " + data[2] + ", " + StringUtil.getDump(data, start, length));
                start = 3;
                length -= 3;
                return new String(data, start, length, encoding);
            } catch (UnsupportedEncodingException e) {
logger.log(Level.WARNING, "unknown cp: " + e.getMessage());
            }
        }
        try {
            return new String(data, start, length, encoding);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** TODO auto detection??? */
    private static String encodingEncoding = "Windows-31J";

    /**
     * Gets decoded string.
     * @see #decodingEncoding
     */
    public static byte[] getEncodedMessage(String text) {
        return getEncodedMessage(text, encodingEncoding);
    }

    /**
     * Gets decoded string.
     * @see #decodingEncoding
     */
    public static byte[] getEncodedMessage(String text, String encoding) {
        byte[] textData;
        try {
            textData = text.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
logger.log(Level.WARNING, "unknown cp: " + e.getMessage());
            textData = text.getBytes();
        }
        byte[] data = new byte[textData.length + 3];
        data[0] = (byte) 0xff;
        data[1] = (byte) 0x03; // TODO ???
        data[2] = (byte) textData.length;
        System.arraycopy(textData, 0, data, 3, textData.length);
        return data;
    }

    /** */
    private static String sequencerClassName = null;

    /** */
    private static String sequencerDeviceName = null;

    /** */
    private static String synthesizerClassName = null;

    /** */
    private static String synthesizerDeviceName = null;

    /* */
    static {
        try {
            // props
            Properties props = new Properties();
            final String path = "midi.properties";
            props.load(MidiUtil.class.getResourceAsStream(path));

            // encodings
            String value = props.getProperty("decodingEncoding");
            if (value != null) {
                decodingEncoding = value;
logger.log(Level.DEBUG, "decodingEncoding: " + decodingEncoding);
            }

            value = props.getProperty("encodingEncoding");
            if (value != null) {
                encodingEncoding = value;
logger.log(Level.DEBUG, "encodingEncoding: " + encodingEncoding);
            }

            // defaultSequencer
            value = props.getProperty("defaultSequencer");
            if (value != null) {
                String defaultSequencer = value;
logger.log(Level.DEBUG, "defaultSequencer: " + defaultSequencer);
                if (defaultSequencer.contains("#")) {
                    String[] pair = defaultSequencer.split("#");
                    sequencerClassName = pair[0];
                    sequencerDeviceName = pair[1];
                } else {
                    sequencerClassName = defaultSequencer;
                }
            }
logger.log(Level.DEBUG, "sequencerClassName: " + sequencerClassName);
logger.log(Level.DEBUG, "sequencerDeviceName: " + sequencerDeviceName);

            // defaultSynthesizer
            value = props.getProperty("defaultSynthesizer");
            if (value != null) {
                String defaultSynthesizer = value;
logger.log(Level.DEBUG, "defaultSynthesizer: " + defaultSynthesizer);
                if (defaultSynthesizer.contains("#")) {
                    String[] pair = defaultSynthesizer.split("#");
                    synthesizerClassName = pair[0];
                    synthesizerDeviceName = pair[1];
                } else {
                    synthesizerClassName = defaultSynthesizer;
                }
            }
logger.log(Level.DEBUG, "sequencerClassName: " + sequencerClassName);
logger.log(Level.DEBUG, "sequencerDeviceName: " + sequencerDeviceName);
        } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new IllegalStateException(e);
        }
    }

    /**
     * want to return com.sun.media.sound.SoftSynthesizer.
     */
    public static Synthesizer getDefaultSynthesizer(Class<? extends MidiDeviceProvider> self) {

        for (MidiDeviceProvider provider : providers) {
            if (self.isInstance(provider)) {
                continue;
            }
            for (MidiDevice.Info info : provider.getDeviceInfo()) {
                String name = null;
                try {
                    byte[] bytes = info.getName().getBytes("ISO8859-1");
                    name = new String(bytes /* , "Windows-31J" */);
                } catch (IOException e) {
logger.log(Level.DEBUG, e);
                }
                if (synthesizerDeviceName != null) {
                    if (synthesizerDeviceName.equals(name)) {
                        MidiDevice device = provider.getDevice(info);
logger.log(Level.DEBUG, "default synthesizer: " + provider.getClass().getName() + ", " + device.getClass().getName() + ", " + name + ", " + device.hashCode());
                        return (Synthesizer) device;
                    }
                } else {
                    MidiDevice device = provider.getDevice(info);
                    if (device instanceof Synthesizer) {
                        if (device.getClass().getName().equals(synthesizerClassName)) {
logger.log(Level.DEBUG, "default synthesizer: " + provider.getClass().getName() + ", " + device.getClass().getName() + ", " + name + ", " + device.hashCode());
                            return (Synthesizer) device;
                        }
                    }
                }
            }
        }

        throw new IllegalStateException("no default midi sequencer");
    }

    /**
     * want to return "com.sun.media.sound.RealTimeSequencer".
     */
    public static Sequencer getDefaultSequencer(Class<? extends MidiDeviceProvider> self) {

        for (MidiDeviceProvider provider : providers) {
            if (self.isInstance(provider)) {
                continue;
            }
            for (MidiDevice.Info info : provider.getDeviceInfo()) {
                String name = null;
                try {
                    byte[] bytes = info.getName().getBytes("ISO8859-1");
                    name = new String(bytes /* , "Windows-31J" */);
                } catch (IOException e) {
logger.log(Level.DEBUG, e);
                }
                if (sequencerDeviceName != null) {
                    if (sequencerDeviceName.equals(name)) {
                        MidiDevice device = provider.getDevice(info);
logger.log(Level.DEBUG, "default sequencer: " + provider.getClass().getName() + ", " + device.getClass().getName() + ", " + name + ", " + device.hashCode());
                        return (Sequencer) device;
                    }
                } else {
                    MidiDevice device = provider.getDevice(info);
                    if (device instanceof Sequencer) {
                        if (device.getClass().getName().equals(sequencerClassName)) {
logger.log(Level.DEBUG, "default sequencer: " + provider.getClass().getName() + ", " + device.getClass().getName() + ", " + name + ", " + device.hashCode());
                            return (Sequencer) device;
                        }
                    }
                }
            }
        }

        throw new IllegalStateException("no default midi sequencer");
    }

    /**
     * @see #getMidiDevice
     */
    public static class MidiMatcher {
        String name;
        String vendor;
        String description;
        String version;

        /**
         *
         * @param name nullable
         * @param vendor nullable
         * @param description nullable
         * @param version nullable
         */
        public MidiMatcher(String name, String vendor, String description, String version) {
            this.name = name;
            this.vendor = vendor;
            this.description = description;
            this.version = version;
        }

        static final int NOT_MATCH = 4;
        int matches(Info info) {
            int score = NOT_MATCH;
            if (info.getName() != null && name != null && info.getName().contains(name)) score--;
            if (info.getVendor() != null && vendor != null && info.getVendor().contains(vendor)) score--;
            if (info.getDescription() != null && description != null && info.getDescription().contains(description)) score--;
            if (info.getVersion() != null && version != null && info.getVersion().contains(version)) score--;
            return score;
        }
    }

    /**
     * @param isIn nullable
     */
    public static Info getMidiDevice(MidiMatcher matcher, Boolean isIn) throws MidiUnavailableException {
        List<Info> result = new ArrayList<>();
        Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (Info info : infos) {
            int score = matcher.matches(info);
            if (score == MidiMatcher.NOT_MATCH) continue;
            MidiDevice device = MidiSystem.getMidiDevice(info);
logger.log(Level.DEBUG, device.getClass().getName() + ", " + device.getMaxTransmitters() + ", " + device.getMaxReceivers());
            if (isIn != null) {
                if (isIn) {
                    if (device.getMaxReceivers() == 0) {
                        result.add(info);
                    }
                } else /* if (!isIn) */ {
                    if (device.getMaxTransmitters() == 0) {
                        result.add(info);
                    }
                }
            } else {
                result.add(info);
            }
        }
        result.sort(Comparator.comparingInt(matcher::matches));
        return result.get(0);
    }

    /** spi */
    private static ServiceLoader<MidiDeviceProvider> providers;

    /*
     * @depends /META-INF/services/javax.sound.midi.spi.MidiDeviceProvider
     */
    static {
        try {
            providers = ServiceLoader.load(javax.sound.midi.spi.MidiDeviceProvider.class);
if (logger.isLoggable(Level.TRACE)) {
 providers.forEach(provider -> System.err.println(provider.getClass()));
}
        } catch (Throwable t) {
logger.log(Level.DEBUG, t.getMessage(), t);
        }
    }
}
