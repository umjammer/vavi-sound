/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.ShortMessage;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.Track;
import vavi.sound.mfi.vavi.mitsubishi.MitsubishiMessage;
import vavi.sound.mfi.vavi.track.CuePointMessage;
import vavi.sound.mfi.vavi.track.EndOfTrackMessage;
import vavi.sound.mfi.vavi.track.TempoMessage;
import vavi.sound.midi.MidiUtil;
import vavi.util.Debug;
import vavi.util.win32.WAVE;


/**
 * VaviMfiFileWriterTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2007/01/25 nsano initial version <br>
 */
public class VaviMfiFileWriterTest {

    @BeforeAll
    static void setup() throws IOException {
        Files.createDirectories(Paths.get("tmp"));
    }

    /**
     * Creates .mld w/ voice file. wav files are mono only
     * sampling rate remains unchanged
     * <p>
     * TODO processing when exceeding 65535 bytes
     * </p>
     * input wav(PCM mono) file
     * output mld file
     */
    @Test
    public void test1() throws Exception {

        InputStream in = new BufferedInputStream(VaviMfiFileWriterTest.class.getResourceAsStream("/ooo_m.wav"));
        WAVE wave = WAVE.readFrom(in, WAVE.class);
        in.close();

        WAVE.fmt format = wave.findChildOf(WAVE.fmt.class);
        if (format.getFormatId() != 0x0001) {
            throw new IllegalArgumentException("not PCM");
        }
        WAVE.data data = wave.findChildOf(WAVE.data.class);
Debug.println(Level.FINE, "wave: " + data.getWave().length);

        int samplingRate = format.getSamplingRate();
        int samplingBits = format.getSamplingBits();
        int numberChannels = format.getNumberChannels();
        if (numberChannels != 1) {
            throw new IllegalArgumentException("multi channel not supported");
        }

        int bytesPerSecond = samplingRate * samplingBits / 8;
Debug.println(Level.FINE, "bytesPerSecond: " + bytesPerSecond);
Debug.println(Level.FINE, "fmt.bytesPerSecond: " + format.getBytesPerSecond());
        double time = (double) data.getWave().length / bytesPerSecond * 1000;
Debug.println(Level.FINE, "time: " + time + " ms");

        //----

        Sequence sequence = new Sequence();
        Track track = sequence.createTrack();
        MfiMessage message;

        message = new CuePointMessage(0x00, 0x00);
        track.add(new MfiEvent(message, 0L));

        double aDelta = (60d / 120d) / 120d * 1000;
        int delta = (int) Math.round(time / aDelta);
Debug.println(Level.FINE, "delta: " + delta);

        message = new TempoMessage(0x00, 0xff, 0xcb, 0x78);
        track.add(new MfiEvent(message, 0L));

        d505i(track, data.getWave(), samplingRate, delta);

        message = new EndOfTrackMessage(0, 0);
        track.add(new MfiEvent(message, 0L));

        int r = MfiSystem.write(sequence,
                                VaviMfiFileFormat.FILE_TYPE,
                                new File("tmp/out.mid"));
Debug.println(Level.FINE, "write: " + r);
    }

    /**
     * @param pcm wave, any sampling rate, 16bit, mono
     */
    private void d505i(Track track, byte[] pcm, int sampleRate, int delta)
        throws InvalidMfiDataException {

        // adpcm vol
        track.add(MitsubishiMessage.getVolumeEvents(0, 100).get(0));

        // adpcm pan
        track.add(MitsubishiMessage.getPanEvents(0).get(0));

        // adpcm data
        for (MfiEvent event : MitsubishiMessage.getAdpcmEvents(pcm, delta, sampleRate, 4, 1)) {
            track.add(event);
        }
    }

//    /**
//     * Get the following MIDI events on the same channel:
//     * {@link ShortMessage#NOTE_OFF}, {@link ShortMessage#CONTROL_CHANGE}.
//     *
//     * @throws NoSuchElementException no next event
//     * @throws IllegalStateException current event is not {@link ShortMessage}
//     */
//    public MidiEvent getNoteOffOrControllChangeMidiEvent() throws NoSuchElementException {
//
//        ShortMessage shortMessage = null;
//
//        MidiEvent midiEvent = midiEvents.get(midiEventIndex);
//        MidiMessage midiMessage = midiEvent.getMessage();
//        if (midiMessage instanceof ShortMessage) {
//            shortMessage = (ShortMessage) midiMessage;
//        } else {
//            throw new IllegalStateException("current is not ShortMessage");
//        }
//
//        int channel = shortMessage.getChannel();
//        int data1 = shortMessage.getData1();
//
//        for (int i = midiEventIndex + 1; i < midiEvents.size(); i++) {
//            midiEvent = midiEvents.get(i);
//            midiMessage = midiEvent.getMessage();
//            if (midiMessage instanceof ShortMessage) {
//                shortMessage = (ShortMessage) midiMessage;
//                if (shortMessage.getChannel() == channel &&
//                    shortMessage.getData1() == data1) {
//                    // next note off
//                    noteOffEventUsed.set(i);    // consumed flag on
//                    return midiEvent;
//                } else if (shortMessage.getChannel() == channel &&
//                           shortMessage.getCommand() == ShortMessage.CONTROL_CHANGE) {
//                    // next control change
//                    String key = "midi.short." + shortMessage.getCommand() + "." + shortMessage.getData1();
//                    MfiConvertible convertible = MfiConvertibleFactory.getConverter(key);
//                    // TODO Converter whether exists
//                    if (convertible == null) {
//                        continue;
//                    }
//Debug.println("(NEXT): " + MidiUtil.paramString(midiMessage) + ", " + convertible.toString());
//
//                    // next note off
//                    for (int j = i + 1; j < midiEvents.size(); j++) {
//                        MidiEvent farMidiEvent = midiEvents.get(j);
//                        MidiMessage farMidiMessage = farMidiEvent.getMessage();
//                        if (farMidiMessage instanceof ShortMessage) {
//                            ShortMessage farShortMessage = (ShortMessage) farMidiMessage;
//                            if (farShortMessage.getChannel() == channel &&
//                                farShortMessage.getData1() == data1) {
//                                // far next note off
//                                noteOffEventUsed.set(i);    // consumed flag on
//                            }
//                        }
//                    }
//
//                    //
//                    return midiEvent;
//                }
//            }
//        }
//
//        throw new NoSuchElementException(channel + "ch, " + data1);
//    }
//
//    /**
//     * Get the next {@link ShortMessage} MIDI event on the same channel.
//     * (not used)
//     * @throws NoSuchElementException no next MIDI event
//     * @throws IllegalStateException current event is not {@link ShortMessage}
//     */
//    public MidiEvent getNextMidiEvent() throws NoSuchElementException {
//
//        ShortMessage shortMessage = null;
//
//        MidiEvent midiEvent = midiEvents.get(midiEventIndex);
//        MidiMessage midiMessage = midiEvent.getMessage();
//        if (midiMessage instanceof ShortMessage) {
//            shortMessage = (ShortMessage) midiMessage;
//        } else {
//            throw new IllegalStateException("current is not ShortMessage");
//        }
//
//        int channel = shortMessage.getChannel();
//        int data1 = shortMessage.getData1();
//
//        for (int i = midiEventIndex + 1; i < midiEvents.size(); i++) {
//            midiEvent = midiEvents.get(i);
//            midiMessage = midiEvent.getMessage();
//            if (midiMessage instanceof ShortMessage) {
//                shortMessage = (ShortMessage) midiMessage;
//                if (shortMessage.getChannel() == channel &&
//                    shortMessage.getCommand() == ShortMessage.NOTE_ON &&
//                    shortMessage.getData1() != data1) {
//Debug.println("next: " + shortMessage.getChannel() + "ch, " + shortMessage.getData1());
//                    return midiEvent;
//                }
//            }
//        }
//
//        throw new NoSuchElementException("no next event of channel: " + channel);
//    }
//
//    /**
//     * @return no correction Δ time
//     * TODO why does this work?
//     */
//    private int retrieveDelta(int mfiTrackNumber, long currentTick) {
//        return (int) Math.round((currentTick - previousTicks[mfiTrackNumber]) / scale);
//    }
}
