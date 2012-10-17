/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Properties;
import java.util.logging.Level;

import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;
import javax.sound.midi.spi.MidiDeviceProvider;

import vavi.util.Debug;
import vavi.util.StringUtil;


/**
 * MidiUtil.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
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
                sb.append(StringUtil.toHex2(data[i]));
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
                sb.append(StringUtil.toHex2(data[i]));
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
        case 8: // 128
            return "NOTE_OFF";
        case 9: // 144
            return "NOTE_ON";
        case 10:    // 160
            return "POLY_PRESSURE";
        case 11:    // 176
            if (value1 >= 120) {
                return "CHANNEL_MODE_MESSAGE";
            } else {
                return "CONTROL_CHANGE";
            }
        case 12:    // 192
            return "PROGRAM_CHANGE";
        case 13:    // 208
            return "CHANNEL_PRESSURE";
        case 14:    // 224
            return "PITCH_BEND_CHANGE";
        default:
            return String.valueOf(statusByte);
        }
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
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }

    /** */
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
Debug.println("decodingEncoding: " + decodingEncoding);
            }

            value = props.getProperty("encodingEncoding");
            if (value != null) {
                encodingEncoding = value;
Debug.println("encodingEncoding: " + encodingEncoding);
            }

            // defaultSequencer
            value = props.getProperty("defaultSequencer");
            if (value != null) {
                String defaultSequencer = value;
Debug.println("defaultSequencer: " + defaultSequencer);
                if (defaultSequencer.indexOf("#") != -1) {
                    String[] pair = defaultSequencer.split("#");
                    sequencerClassName = pair[0];
                    sequencerDeviceName = pair[1];
                } else {
                    sequencerClassName = defaultSequencer;
                }
            }
Debug.println("sequencerClassName: " + sequencerClassName);
Debug.println("sequencerDeviceName: " + sequencerDeviceName);
            
        } catch (Exception e) {
Debug.printStackTrace(e);
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }

    /**
     * com.sun.media.sound.RealTimeSequencer を返したい。 
     */
    public static Sequencer getDefaultSequencer() {

        for (MidiDeviceProvider provider : providers) {
            for (MidiDevice.Info info : provider.getDeviceInfo()) {
                MidiDevice device = provider.getDevice(info); 
                String name = null;
                try {
                    byte[] bytes = info.getName().getBytes("ISO8859-1");
                    name = new String(bytes /* , "Windows-31J" */);
                } catch (IOException e) {
Debug.println(e);
                }
                if (Sequencer.class.isInstance(device)) {
                    if (sequencerDeviceName != null) {
                        if (sequencerDeviceName.equals(name)) {
Debug.println("default sequencer: " + provider.getClass().getName() + ", " + device.getClass().getName() + ", " + name + ", " + device.hashCode());
                            return (Sequencer) device;
                        }
                    } else {
                        if (device.getClass().getName().equals(sequencerClassName)) {
Debug.println("default sequencer: " + provider.getClass().getName() + ", " + device.getClass().getName() + ", " + name + ", " + device.hashCode());
                            return (Sequencer) device;
                        }
                    }
                }
            }
        }

        throw new IllegalStateException("no default midi sequencer");
    }

    /** */
    public static MidiDeviceProvider[] getMidiDeviceProvider() {
        return providers;
    }

    /** */
    private static MidiDeviceProvider[] providers;

    /**
     * @depends /META-INF/services/javax.sound.midi.spi.MidiDeviceProvider 
     */
    static {
        final String dir = "/META-INF/services/";
        final String readerFile = "javax.sound.midi.spi.MidiDeviceProvider";

        Properties props = new Properties();

        try {
            Class<?> clazz = MidiUtil.class;

            props.load(clazz.getResourceAsStream(dir + readerFile));
Debug.println("midi device providers");
props.list(System.err);
            Enumeration<?> e = props.propertyNames();
            int i = 0;
            providers = new MidiDeviceProvider[props.size()];
            while (e.hasMoreElements()) {
                Class<?> c = Class.forName((String) e.nextElement());
                providers[i++] = (MidiDeviceProvider) c.newInstance();
            }
        } catch (Exception e) {
Debug.printStackTrace(e);
            throw (RuntimeException) new IllegalStateException().initCause(e);
        }
    }
}

/* */
