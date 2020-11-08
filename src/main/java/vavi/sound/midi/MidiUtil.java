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
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.logging.Level;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.spi.MidiDeviceProvider;

import vavi.util.Debug;


/**
 * MidiUtil.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041230 nsano initial version <br>
 */
public final class MidiUtil {

    /** */
    private MidiUtil() {
    }

    /** */
    public static String paramString(MidiMessage midiMessage) {
        String result = null;
        if (midiMessage instanceof ShortMessage) {
            ShortMessage msg = (ShortMessage) midiMessage;
            int channel = msg.getChannel();
            int command = msg.getCommand();
            int data1 = msg.getData1();
            int data2 = msg.getData2();
            result = "channel=" + (channel + 1) +
                ",event=" + getChannelMessage(command, data1) +
                ",data1=" + (command == ShortMessage.PROGRAM_CHANGE ? data1 + " " + MidiConstants.getInstrumentName(data1) : String.valueOf(data1)) +
                ",data2=" + data2;
        } else if (midiMessage instanceof SysexMessage) {
            SysexMessage msg = (SysexMessage) midiMessage;
            byte[] data = msg.getData();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                sb.append(String.format("%02x", data[i]));
                sb.append(" ");
            }
            result = "channel=n/a" +
                ",event=SYSX" +
                ",data1=" + sb +
                ",data2=";
        } else if (midiMessage instanceof MetaMessage) {
            MetaMessage msg = (MetaMessage) midiMessage;
            int type = msg.getType();
            byte[] data = msg.getData();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < data.length; i++) {
                sb.append(String.format("%02x", data[i]));
                sb.append(" ");
            }
            result = "channel=n/a" +
                ",event=meta" +
                ",data1=" + type +
                ",data2=" + sb;
        }

        return result;
    }

    /** */
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

    /** */
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
        if ((value & 0xF0000000) != 0) {
            output.write(0x80 | ((value >> 28) & 0x7F));
        }
        if ((value & 0xFFE00000) != 0) {
            output.write(0x80 | ((value >> 21) & 0x7F));
        }
        if ((value & 0xFFFFC000) != 0) {
            output.write(0x80 | ((value >> 14) & 0x7F));
        }
        if ((value & 0xFFFFFF80) != 0) {
            output.write(0x80 | ((value >> 7) & 0x7F));
        }
        output.write(value & 0x7F);
    }

    /** */
    private static String decodingEncoding = "JISAutoDetect";

    /** MIDI データで先頭が 0xff の場合対応 (エンコードされている) */
    public static String getDecodedMessage(byte[] data) {
        return getDecodedMessage(data, decodingEncoding);
    }

    /** MIDI データで先頭が 0xff の場合対応 (エンコードされている) */
    public static String getDecodedMessage(byte[] data, String encoding) {
        int start = 0;
        int length = data.length;
        if ((data[0] & 0xff) == 0xff) {
            // 00 0xff
            // 01 0x03 ?
            // 02      length
            try {
//Debug.println("META: " + data[1] + ", " + data[2] + ", " + StringUtil.getDump(data, start, length));
                start = 3;
                length -= 3;
                return new String(data, start, length, encoding);
            } catch (UnsupportedEncodingException e) {
Debug.println(Level.WARNING, "unknown cp: " + e.getMessage());
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

    /** */
    public static byte[] getEncodedMessage(String text) {
        return getEncodedMessage(text, encodingEncoding);
    }

    /** */
    public static byte[] getEncodedMessage(String text, String encoding) {
        byte[] textData = null;
        try {
            textData = text.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
Debug.println(Level.WARNING, "unknown cp: " + e.getMessage());
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
Debug.println(Level.FINE, "decodingEncoding: " + decodingEncoding);
            }

            value = props.getProperty("encodingEncoding");
            if (value != null) {
                encodingEncoding = value;
Debug.println(Level.FINE, "encodingEncoding: " + encodingEncoding);
            }

            // defaultSequencer
            value = props.getProperty("defaultSequencer");
            if (value != null) {
                String defaultSequencer = value;
Debug.println(Level.FINE, "defaultSequencer: " + defaultSequencer);
                if (defaultSequencer.indexOf("#") != -1) {
                    String[] pair = defaultSequencer.split("#");
                    sequencerClassName = pair[0];
                    sequencerDeviceName = pair[1];
                } else {
                    sequencerClassName = defaultSequencer;
                }
            }
Debug.println(Level.FINE, "sequencerClassName: " + sequencerClassName);
Debug.println(Level.FINE, "sequencerDeviceName: " + sequencerDeviceName);

            // defaultSynthesizer
            value = props.getProperty("defaultSynthesizer");
            if (value != null) {
                String defaultSynthesizer = value;
Debug.println("defaultSynthesizer: " + defaultSynthesizer);
                if (defaultSynthesizer.indexOf("#") != -1) {
                    String[] pair = defaultSynthesizer.split("#");
                    synthesizerClassName = pair[0];
                    synthesizerDeviceName = pair[1];
                } else {
                    synthesizerClassName = defaultSynthesizer;
                }
            }
Debug.println(Level.FINE, "sequencerClassName: " + sequencerClassName);
Debug.println(Level.FINE, "sequencerDeviceName: " + sequencerDeviceName);
        } catch (Exception e) {
Debug.printStackTrace(e);
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
Debug.println(e);
                }
                if (synthesizerDeviceName != null) {
                    if (synthesizerDeviceName.equals(name)) {
                        MidiDevice device = provider.getDevice(info);
Debug.println(Level.FINE, "default synthesizer: " + provider.getClass().getName() + ", " + device.getClass().getName() + ", " + name + ", " + device.hashCode());
                        return (Synthesizer) device;
                    }
                } else {
                    MidiDevice device = provider.getDevice(info);
                    if (Synthesizer.class.isInstance(device)) {
                        if (device.getClass().getName().equals(synthesizerClassName)) {
Debug.println(Level.FINE, "default synthesizer: " + provider.getClass().getName() + ", " + device.getClass().getName() + ", " + name + ", " + device.hashCode());
                            return (Synthesizer) device;
                        }
                    }
                }
            }
        }

        throw new IllegalStateException("no default midi sequencer");
    }

    /**
     * com.sun.media.sound.RealTimeSequencer を返したい。
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
Debug.println(e);
                }
                if (sequencerDeviceName != null) {
                    if (sequencerDeviceName.equals(name)) {
                        MidiDevice device = provider.getDevice(info);
Debug.println(Level.FINE, "default sequencer: " + provider.getClass().getName() + ", " + device.getClass().getName() + ", " + name + ", " + device.hashCode());
                        return (Sequencer) device;
                    }
                } else {
                    MidiDevice device = provider.getDevice(info);
                    if (Sequencer.class.isInstance(device)) {
                        if (device.getClass().getName().equals(sequencerClassName)) {
Debug.println(Level.FINE, "default sequencer: " + provider.getClass().getName() + ", " + device.getClass().getName() + ", " + name + ", " + device.hashCode());
                            return (Sequencer) device;
                        }
                    }
                }
            }
        }

        throw new IllegalStateException("no default midi sequencer");
    }

    /** */
    private static ServiceLoader<MidiDeviceProvider> providers;

    /**
     * @depends /META-INF/services/javax.sound.midi.spi.MidiDeviceProvider
     */
    static {
        try {
            providers = ServiceLoader.load(javax.sound.midi.spi.MidiDeviceProvider.class);
if (Debug.isLoggable(Level.FINE)) {
 providers.forEach(provider -> System.err.println(provider.getClass()));
}
        } catch (Throwable t) {
Debug.printStackTrace(Level.FINE, t);
        }
    }
}

/* */
