/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashSet;
import java.util.Set;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.SysexMessage;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.MidiConverter;
import vavi.sound.mfi.Track;
import vavi.sound.mfi.vavi.track.EndOfTrackMessage;
import vavi.sound.mfi.vavi.track.NopMessage;
import vavi.sound.midi.MidiUtil;
import vavi.sound.midi.mfi.MfiVaviSequence;

import static java.lang.System.getLogger;


/**
 * The format converter between MIDI and MFi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020627 nsano initial version <br>
 *          0.10 020703 nsano complete <br>
 *          0.11 030618 nsano add vibrato related <br>
 *          0.20 030819 nsano refactoring <br>
 *          0.21 030903 nsano add midi to mfi <br>
 */
class VaviMidiConverter implements MidiConverter {

    private static final Logger logger = getLogger(VaviMidiConverter.class.getName());

    /** the device information */
    private static final MfiDevice.Info info = new MfiDevice.Info(
        "Java MIDI, MFi Sequence Converter",
        "Vavisoft",
        "Format Converter between MIDI and MFi",
        "Version " + VaviMfiDeviceProvider.version) {};

    /* */
    @Override
    public MfiDevice.Info getDeviceInfo() {
        return info;
    }

    /* */
    @Override
    public void close() {
    }

    /* */
    @Override
    public boolean isOpen() {
        return true;
    }

    /* */
    @Override
    public void open() {
    }

    // ----

    /**
     * Converts midi sequence to mfi sequence.
     */
    @Override
    @Deprecated
    public vavi.sound.mfi.Sequence toMfiSequence(Sequence midiSequence)
        throws InvalidMidiDataException {

        return toMfiSequence(midiSequence, 0);
    }

    /** Converts midi sequence to mfi sequence. */
    @Override
    public vavi.sound.mfi.Sequence toMfiSequence(Sequence midiSequence, int fileType)
        throws InvalidMidiDataException {

        try {
            return convert(midiSequence, fileType);
        } catch (IOException | InvalidMfiDataException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        }
    }

/* debug */
private static final Set<String> uc = new HashSet<>();

    /** Converts midi sequence to mfi sequence */
    protected vavi.sound.mfi.Sequence convert(Sequence midiSequence, int fileType)
        throws InvalidMfiDataException, IOException {

logger.log(Level.DEBUG, "divisionType: " + midiSequence.getDivisionType());
logger.log(Level.DEBUG, "microsecondLength: " + midiSequence.getMicrosecondLength());
logger.log(Level.DEBUG, "resolution: " + midiSequence.getResolution());
logger.log(Level.DEBUG, "tickLength: " + midiSequence.getTickLength());

        //
        vavi.sound.mfi.Sequence mfiSequence = new vavi.sound.mfi.Sequence();

        MfiContext mfiContext = new MfiContext();
        mfiContext.setType(fileType);
        mfiContext.setMidiSequence(midiSequence);

        for (int i = 0; i < mfiContext.getSequenceSize(); i++) {
            MidiEvent midiEvent = mfiContext.getMidiEvent(i);

            MidiMessage midiMessage = midiEvent.getMessage();

            Track[] mfiTracks = mfiSequence.getTracks();
            int maxTracks = mfiTracks.length;

            String key = null;
            int mfiTrackNumber = 0;

            //
            if (midiMessage instanceof ShortMessage shortMessage) {

                int channel = shortMessage.getChannel();
                int command = shortMessage.getCommand();
                int data1 = shortMessage.getData1();
//              int data2 = shortMessage.getData2();

                mfiTrackNumber = mfiContext.retrieveMfiTrack(channel);

                if (!mfiContext.isTrackUsed(mfiTrackNumber) &&
                        maxTracks <= mfiTrackNumber) {

                    for (int j = maxTracks; j <= mfiTrackNumber; j++) {
                        mfiSequence.createTrack();
logger.log(Level.DEBUG, ">>>> create MFi track: " + j);
                        mfiTracks = mfiSequence.getTracks();
                        maxTracks = mfiTracks.length;
                    }
                }
                mfiContext.setTrackUsed(mfiTrackNumber, true);
                // 0xc_ program change
                // 0xb_ 1: modulation depth
                // 0xb_ 7: volume
                // 0xb0 100, 101, 6: rpn H, rpn L, data entry
                // 0xe_ pitch bend
                // 0xb0 10: panpot

                if ((command & 0xf0) == 0xb0) {
                    key = "short." + (command & 0xf0) + "." + data1;
                } else {
                    key = "short." + (command & 0xf0);
                }
            } else if (midiMessage instanceof SysexMessage sysexMessage) {

                byte[] data = sysexMessage.getData();

                // GM system on
                // master volume
                if (maxTracks == 0) {
                    mfiSequence.createTrack();
logger.log(Level.DEBUG, "create MFi track: 0");
                    mfiTracks = mfiSequence.getTracks();
                    maxTracks = mfiTracks.length;
                }
                mfiContext.setTrackUsed(0, true);

                mfiTrackNumber = 0;
                key = "sysex." + data[0];
            } else if (midiMessage instanceof MetaMessage metaMessage) {
                // 1 -> ProtInfo
                // 2 -> CopyInfo
                // 3 -> TitlInfo
                // 0x51 tempo

                int meta = metaMessage.getType();
//                byte[] data = metaMessage.getData();

                if (maxTracks == 0) {
                    mfiSequence.createTrack();
logger.log(Level.DEBUG, "create MFi track: 0");
                    mfiTracks = mfiSequence.getTracks();
                    maxTracks = mfiTracks.length;
                }
                mfiContext.setTrackUsed(0, true);

                mfiTrackNumber = 0;
                key = "meta." + meta;
            }

            // convert
try {
            MfiConvertible converter = MfiConvertible.factory.get(key);
            if (converter == null) {
if (!uc.contains(key)) {
 logger.log(Level.WARNING, "no converter for: [" + key + "]");
 uc.add(key);
}
            } else if (converter instanceof EndOfTrackMessage) { // TODO ???
                // converted
                MfiEvent[] mfiEvents = converter.getMfiEvents(midiEvent, mfiContext);
                for (int t = 0; t < mfiEvents.length && t < maxTracks; t++) {
                    if (mfiEvents[t] != null) {
                        if (!mfiContext.isEofSet(t)) {

                            MfiEvent[] nops = mfiContext.getIntervalMfiEvents(t);
                            if (nops != null) {
                                for (MfiEvent nop : nops) {
                                    mfiTracks[t].add(nop);
                                }
                            }

                            mfiTracks[t].add(mfiEvents[t]);
                            mfiContext.setEofSet(t, true);
                        } else {
//logger.log(Level.TRACE, "EOF already set[" +  t + "]");
                        }
                    } else {
logger.log(Level.DEBUG, "message is null[" +  mfiTracks[t].size() + "]: " + midiMessage);
                    }
                }
            } else {
                // interval
                MfiEvent[] mfiEvents = mfiContext.getIntervalMfiEvents(mfiTrackNumber);
                if (mfiEvents != null) {
                    for (MfiEvent mfiEvent : mfiEvents) {
                        if (mfiEvent == null) {
                            logger.log(Level.WARNING, "NOP is null[" + mfiTracks[mfiTrackNumber].size() + "]: " + MidiUtil.paramString(midiMessage));
                        }
                        addEventToTrack(mfiContext, midiEvent.getTick(), mfiTracks[mfiTrackNumber], mfiTrackNumber, mfiEvent);
                    }
                }

                // converted
                mfiEvents = converter.getMfiEvents(midiEvent, mfiContext);
                if (mfiEvents != null) {
                    for (MfiEvent mfiEvent : mfiEvents) {
                        if (mfiEvent == null) {
                            logger.log(Level.WARNING, "event is null[" + mfiTracks[mfiTrackNumber].size() + ", " + mfiEvents.length + "]: " + converter.getClass() + ", " + MidiUtil.paramString(midiMessage));
                        }
                        addEventToTrack(mfiContext, midiEvent.getTick(), mfiTracks[mfiTrackNumber], mfiTrackNumber, mfiEvent);
                    }
                }
            }
} catch (IllegalArgumentException e) {
 logger.log(Level.WARNING, e);
}
        }

        return mfiSequence;
    }

private final int[] deltas = new int[4];

    private void addEventToTrack(MfiContext mfiContext, long tick, Track mfiTrack, int mfiTrackNumber, MfiEvent mfiEvent) {
        MfiMessage mfiMessage = mfiEvent.getMessage();
        deltas[mfiTrackNumber] += mfiMessage.getDelta();
        double tickDash = deltas[mfiTrackNumber] * mfiContext.getScale();
if ((tickDash / tick) * 100 < 95 && (tickDash / tick) * 100 != 0 && !(mfiMessage instanceof NopMessage)) {
 logger.log(Level.ERROR, "XXXXX track: %d, tick: %d, tick': %.2f (%.2f), %d, %s",
  mfiTrackNumber,
  tick,
  tickDash,
  tick != 0 ? (tickDash / tick) * 100 : 0,
  mfiContext.getPreviousTick(mfiTrackNumber),
  mfiMessage);
}
        mfiTrack.add(mfiEvent);
    }

    // ----

    /** Converts mfi sequence to midi sequence  */
    @Override
    public Sequence toMidiSequence(vavi.sound.mfi.Sequence mfiSequence)
        throws InvalidMfiDataException {

        try {
            return convert(mfiSequence);
        } catch (IOException | InvalidMidiDataException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new InvalidMfiDataException(e);
        }
    }

    /** Converts mfi sequence to midi sequence */
    protected Sequence convert(vavi.sound.mfi.Sequence mfiSequence)
        throws InvalidMidiDataException,
               IOException,
               InvalidMfiDataException {

        Track[] mfiTracks = mfiSequence.getTracks();

        MidiContext midiContext = new MidiContext();

        int resolution = midiContext.getResolution(mfiTracks);
logger.log(Level.DEBUG, "resolution: " + resolution);
        Sequence midiSequence = new MfiVaviSequence(Sequence.PPQ, resolution, 1);
        javax.sound.midi.Track midiTrack = midiSequence.getTracks()[0];

        for (int i = 0; i < mfiTracks.length; i++) {

            midiContext.setMfiTrackNumber(i);
            midiContext.setCurrent(0);

            Track mfiTrack = mfiTracks[i];

            for (int j = 0; j < mfiTrack.size(); j++) {
                MfiEvent mfiEvent = mfiTrack.get(j);
                MfiMessage mfiMessage = mfiEvent.getMessage();

                midiContext.addCurrent(mfiMessage.getDelta());

                if (mfiMessage instanceof MidiConvertible) {
//logger.log(Level.TRACE, "midi convertible: " + message);
                    MidiEvent[] midiEvents = ((MidiConvertible) mfiMessage).getMidiEvents(midiContext);
                    if (midiEvents != null) {
                        for (MidiEvent midiEvent : midiEvents) {
                            midiTrack.add(midiEvent);
                        }
                    }
                } else if (mfiMessage instanceof SubMessage) {
logger.log(Level.WARNING, "ignore sequence: " + mfiMessage);
                } else {
logger.log(Level.WARNING, "unknown sequence: " + mfiMessage);
                }
            }
        }

        return midiSequence;
    }
}
