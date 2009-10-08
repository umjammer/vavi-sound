/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import junit.framework.TestCase;

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
import vavi.util.Debug;
import vavi.util.win32.WAVE;


/**
 * VaviMfiFileWriterTest. 
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 2007/01/25 nsano initial version <br>
 */
public class VaviMfiFileWriterTest extends TestCase {

    /* @see junit.framework.TestCase#setUp() */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /**
     * Creates .mld w/ voice file. wav �t�@�C���̓��m�����݂̂ł�
     * �T���v�����O���[�g�͕ύX����܂���
     * <p>
     * TODO 65535 bytes �𒴂����Ƃ��̏���
     * </p>
     * input wav(PCM mono) file
     * output mld file
     */
    public void test1() throws Exception {

        InputStream in = new BufferedInputStream(new FileInputStream("in.wav"));
        WAVE wave = (WAVE) WAVE.readFrom(in);
        in.close();

        WAVE.fmt format = (WAVE.fmt) wave.findChildOf(WAVE.fmt.class);
        if (format.getFormatId() != 0x0001) {
            throw new IllegalArgumentException("not PCM");
        }
        WAVE.data data = (WAVE.data) wave.findChildOf(WAVE.data.class);
Debug.println("wave: " + data.getWave().length);

        int samplingRate = format.getSamplingRate();
        int samplingBits = format.getSamplingBits();
        int numberChannels = format.getNumberChannels();
        if (numberChannels != 1) {
            throw new IllegalArgumentException("multi channel not supported");
        }

        int bytesPerSecond = samplingRate * samplingBits / 8;
Debug.println("bytesPerSecond: " + bytesPerSecond);
Debug.println("fmt.bytesPerSecond: " + format.getBytesPerSecond());
        double time = (double) in.available() / bytesPerSecond * 1000;
Debug.println("time: " + time + " ms");

        //----

        Sequence sequence = new Sequence();
        Track track = sequence.createTrack();
        MfiMessage message;

        message = new CuePointMessage(0x00, 0x00);
        track.add(new MfiEvent(message, 0l));

        double aDelta = (60d / 120d) / 120d * 1000;
        int delta = (int) Math.round(time / aDelta);
Debug.println("delta: " + delta);

        message = new TempoMessage(0x00, 0xff, 0xcb, 0x78);
        track.add(new MfiEvent(message, 0l));

        d505i(track, data.getWave(), samplingRate, delta);

        message = new EndOfTrackMessage(0, 0);
        track.add(new MfiEvent(message, 0l));

        int r = MfiSystem.write(sequence,
                                VaviMfiFileFormat.FILE_TYPE,
                                new File("out.mid"));
Debug.println("write: " + r);

        System.exit(r != 0 ? 0 : 1);
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

    /**
     * ���� channel �Ŏ��� {@link ShortMessage#NOTE_OFF}, {@link ShortMessage#CONTROL_CHANGE}
     * �ł��� MIDI �C�x���g���擾���܂��B
     *
     * @throws NoSuchElementException ���̃C�x���g���Ȃ�
     * @throws IllegalStateException ���݂̃C�x���g�� {@link ShortMessage} �ł͂Ȃ�
     */
/*
    public MidiEvent getNoteOffOrControllChangeMidiEvent() throws NoSuchElementException {

        ShortMessage shortMessage = null;

        MidiEvent midiEvent = midiEvents.get(midiEventIndex);
        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
            shortMessage = (ShortMessage) midiMessage;
        } else {
            throw new IllegalStateException("current is not ShortMessage");
        }

        int channel = shortMessage.getChannel();
        int data1 = shortMessage.getData1();

        for (int i = midiEventIndex + 1; i < midiEvents.size(); i++) {
            midiEvent = midiEvents.get(i);
            midiMessage = midiEvent.getMessage();
            if (midiMessage instanceof ShortMessage) {
                shortMessage = (ShortMessage) midiMessage;
                if (shortMessage.getChannel() == channel &&
                    shortMessage.getData1() == data1) {
                    // next note off
                    noteOffEventUsed.set(i);    // ����t���O on
                    return midiEvent;
                } else if (shortMessage.getChannel() == channel &&
                           shortMessage.getCommand() == ShortMessage.CONTROL_CHANGE) {
                    // next control change
                    String key = "midi.short." + shortMessage.getCommand() + "." + shortMessage.getData1();
                    MfiConvertible convertible = MfiConvertibleFactory.getConverter(key);
                    // TODO Converter �����邩�ǂ���
                    if (convertible == null) {
                        continue;
                    }
Debug.println("(NEXT): " + MidiUtil.paramString(midiMessage) + ", " + convertible.toString());

                    // next note off
                    for (int j = i + 1; j < midiEvents.size(); j++) {
                        MidiEvent farMidiEvent = midiEvents.get(j);
                        MidiMessage farMidiMessage = farMidiEvent.getMessage();
                        if (farMidiMessage instanceof ShortMessage) {
                            ShortMessage farShortMessage = (ShortMessage) farMidiMessage;
                            if (farShortMessage.getChannel() == channel &&
                                farShortMessage.getData1() == data1) {
                                // far next note off
                                noteOffEventUsed.set(i);    // ����t���O on
                            }
                        }
                    }

                    //
                    return midiEvent;
                }
            }
        }

        throw new NoSuchElementException(channel + "ch, " + data1);
    }
*/

    /**
     * ���� channel �Ŏ��� {@link ShortMessage} �ł��� MIDI �C�x���g���擾���܂��B
     * (not used)
     * @throws NoSuchElementException ���� MIDI �C�x���g���Ȃ�
     * @throws IllegalStateException ���݂̃C�x���g�� {@link ShortMessage} �ł͂Ȃ�
     */
/*
    public MidiEvent getNextMidiEvent() throws NoSuchElementException {

        ShortMessage shortMessage = null;

        MidiEvent midiEvent = midiEvents.get(midiEventIndex);
        MidiMessage midiMessage = midiEvent.getMessage();
        if (midiMessage instanceof ShortMessage) {
            shortMessage = (ShortMessage) midiMessage;
        } else {
            throw new IllegalStateException("current is not ShortMessage");
        }

        int channel = shortMessage.getChannel();
        int data1 = shortMessage.getData1();

        for (int i = midiEventIndex + 1; i < midiEvents.size(); i++) {
            midiEvent = midiEvents.get(i);
            midiMessage = midiEvent.getMessage();
            if (midiMessage instanceof ShortMessage) {
                shortMessage = (ShortMessage) midiMessage;
                if (shortMessage.getChannel() == channel &&
                    shortMessage.getCommand() == ShortMessage.NOTE_ON &&
                    shortMessage.getData1() != data1) {
Debug.println("next: " + shortMessage.getChannel() + "ch, " + shortMessage.getData1());
                    return midiEvent;
                }
            }
        }

        throw new NoSuchElementException("no next event of channel: " + channel);
    }
*/
    /**
     * @return �␳�Ȃ� ���^�C��
     * TODO ���ł���ł��܂������́H
     */
/*
    private int retrieveDelta(int mfiTrackNumber, long currentTick) {
        return (int) Math.round((currentTick - previousTicks[mfiTrackNumber]) / scale);
    }

*/
}

/* */
