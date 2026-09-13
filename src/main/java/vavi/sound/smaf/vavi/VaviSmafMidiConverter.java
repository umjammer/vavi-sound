/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf.vavi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;

import vavi.sound.midi.MidiConstants.MetaEvent;
import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.MetaMessage;
import vavi.sound.smaf.MidiConverter;
import vavi.sound.smaf.SmafDevice;
import vavi.sound.smaf.SmafEvent;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.Track;
import vavi.sound.smaf.vavi.chunk.ChannelStatus;
import vavi.sound.smaf.vavi.chunk.ScoreTrackChunk;
import vavi.sound.smaf.vavi.chunk.TrackChunk.FormatType;
import vavi.sound.smaf.vavi.chunk.TrackChunk.SequenceType;
import vavi.sound.smaf.vavi.message.MidiContext;
import vavi.sound.smaf.vavi.message.MidiConvertible;
import vavi.sound.smaf.vavi.message.BankSelectMessage;
import vavi.sound.smaf.vavi.message.ExpressionMessage;
import vavi.sound.smaf.vavi.message.ModulationMessage;
import vavi.sound.smaf.vavi.message.NoteMessage;
import vavi.sound.smaf.vavi.message.PanMessage;
import vavi.sound.smaf.vavi.message.PitchBendMessage;
import vavi.sound.smaf.vavi.message.ProgramChangeMessage;
import vavi.sound.smaf.vavi.message.SmafContext;
import vavi.sound.smaf.vavi.message.SmafConvertible;
import vavi.sound.smaf.vavi.message.VolumeMessage;
import vavi.sound.smaf.vavi.message.yamaha.YamahaMessage;
import vavi.util.StringUtil;

import static java.lang.System.getLogger;


/**
 * SmafMidiConverter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071012 nsano initial version <br>
 */
class VaviSmafMidiConverter implements MidiConverter {

    private static final Logger logger = getLogger(VaviSmafMidiConverter.class.getName());

    /** the device information */
    static final SmafDevice.Info info =
        new SmafDevice.Info("Java MIDI, SMAF Sequence Converter",
                            "vavi",
                            "Format Converter between MIDI and SMAF",
                            "Version " + VaviSmafDeviceProvider.version) {};

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void open() {
    }

/** debug */
private final Set<Class<? extends SmafMessage>> uc = new HashSet<>();

    @Override
    public Sequence toMidiSequence(vavi.sound.smaf.Sequence smafSequence)
            throws InvalidSmafDataException {

        try {
            return convert(smafSequence);
        } catch (InvalidMidiDataException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw new InvalidSmafDataException(e);
        }
    }

    /** */
    private Sequence convert(vavi.sound.smaf.Sequence smafSequence)
            throws InvalidSmafDataException, InvalidMidiDataException {

        Track[] smafTracks = smafSequence.getTracks();

        MidiContext midiContext = new MidiContext();

        int resolution = midiContext.getResolution(smafTracks);
logger.log(Level.DEBUG, "resolution: " + resolution);
        Sequence midiSequence = new Sequence(Sequence.PPQ, resolution, 1);
        javax.sound.midi.Track midiTrack = midiSequence.getTracks()[0];

        midiTrack.add(midiContext.getTempoEvent());

logger.log(Level.DEBUG, "smafTracks: " + smafTracks.length);
        for (int i = 0; i < smafTracks.length; i++) {

            midiContext.setMidiTrack(midiTrack);

            midiContext.setSmafTrackNumber(i);
            midiContext.setTrack(smafTracks[i]);
            midiContext.setCurrentTick(0);

            Track smafTrack = smafTracks[i];

            for (int j = 0; j < smafTrack.size(); j++) {
                SmafEvent smafEvent = smafTrack.get(j);
                SmafMessage smafMessage = smafEvent.getMessage();

                midiContext.addCurrentTick(midiContext.getTicksOfDuration(smafMessage.getDuration()));
//logger.log(Level.TRACE, "■■■■■(" + i + ":" + j + ") ticks: " + midiContext.getCurrentTick() + "(" + midiContext.getTicksOf(smafMessage.getDuration()) + "," + smafMessage.getDuration() + "), " + smafMessage.getClass().getSimpleName());

                if (smafMessage instanceof MidiConvertible midiConvertible) {
if (!(smafMessage instanceof NoteMessage) &&
    !(smafMessage instanceof ProgramChangeMessage) &&
    !(smafMessage instanceof ModulationMessage) &&
    !(smafMessage instanceof PitchBendMessage) &&
    !(smafMessage instanceof PanMessage) &&
    !(smafMessage instanceof VolumeMessage) &&
    !(smafMessage instanceof BankSelectMessage) &&
    !(smafMessage instanceof YamahaMessage) &&
    !(smafMessage instanceof ExpressionMessage)) {
 logger.log(Level.DEBUG, "special midi convertible(" + i + ":" + j + "): " + smafMessage);
}
//if (smafMessage instanceof vavi.sound.smaf.vavi.message.NoteMessage) {
// int gateTime = ((vavi.sound.smaf.vavi.message.NoteMessage) smafMessage).getGateTime();
// if (gateTime == 0) {
//  logger.log(Level.WARNING, "★★★★★(" + i + ":" + j + ") gateTime == 0: " + smafMessage);
// }
//}
                    MidiEvent[] midiEvents = midiConvertible.getMidiEvents(midiContext);
                    if (midiEvents != null) {
                        for (MidiEvent midiEvent : midiEvents) {
                            midiTrack.add(midiEvent);
//                          addSmafMessage(midiTrack, midiEvent);
                        }
                    }
                } else if (smafMessage instanceof MetaMessage metaMessage) {
logger.log(Level.DEBUG, "meta: " + MetaEvent.valueOf(metaMessage.getType()));
                    if (metaMessage.getMapData() != null) {
                        for (Map.Entry<String, Object> entry : metaMessage.getMapData().entrySet()) {
logger.log(Level.DEBUG, "  " + entry.getKey() + "=" + entry.getValue());
                        }
                    } else {
                        // TODO should be MidiConvertible
logger.log(Level.DEBUG, "  " + StringUtil.getDump(metaMessage.getData()));
                        javax.sound.midi.MetaMessage mididMetaMessage = new javax.sound.midi.MetaMessage();
                        mididMetaMessage.setMessage(metaMessage.getType(), metaMessage.getData(), metaMessage.getLength());
                        midiTrack.add(new MidiEvent(mididMetaMessage, midiContext.getCurrentTick()));
                    }
                // as for sysex message, all sysex messagess are handled as MidiConvertible at above
                } else {
if (!uc.contains(smafMessage.getClass())) {
 logger.log(Level.WARNING, "unhandled message: " + smafMessage + " @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
 uc.add(smafMessage.getClass());
}
                }
            }
        }

        return midiSequence;
    }

    /** Note may be inserted before Control/Program */
    @SuppressWarnings("unused")
    private static void addSmafMessage(javax.sound.midi.Track midiTrack, MidiEvent midiEvent) {
//logger.log(Level.TRACE, "★: " + midiEvent.getMessage());
//logger.log(Level.TRACE, "★: " + (midiTrack.size() > 1 ? midiTrack.get(midiTrack.size() - 2).getMessage() : null));
        if (midiEvent.getTick() == 0 &&
            midiEvent.getMessage() instanceof ShortMessage &&
            ((ShortMessage) midiEvent.getMessage()).getCommand() == ShortMessage.PROGRAM_CHANGE &&
            midiTrack.size() > 1 &&
            midiTrack.get(midiTrack.size() - 2).getMessage() instanceof ShortMessage &&
            (((ShortMessage) midiTrack.get(midiTrack.size() - 2).getMessage()).getCommand() == ShortMessage.NOTE_ON ||
             ((ShortMessage) midiTrack.get(midiTrack.size() - 2).getMessage()).getCommand() == ShortMessage.NOTE_OFF)) {
            MidiEvent removedMidiEvent = midiTrack.get(midiTrack.size() - 2);
            midiTrack.remove(removedMidiEvent);
            midiTrack.add(midiEvent);
            midiTrack.add(removedMidiEvent);
logger.log(Level.INFO, "★★★★★ : " + MidiUtil.paramString(midiEvent.getMessage()) + ", " + MidiUtil.paramString(removedMidiEvent.getMessage()));
        } else {
            midiTrack.add(midiEvent);
        }
    }

    @Override
    @Deprecated
    public vavi.sound.smaf.Sequence toSmafSequence(Sequence midiSequence)
            throws InvalidMidiDataException {

        return toSmafSequence(midiSequence, 0);
    }

    @Override
    public vavi.sound.smaf.Sequence toSmafSequence(Sequence midiSequence, int fileType)
        throws InvalidMidiDataException {

        try {
            return convert(midiSequence, fileType);
        } catch (InvalidSmafDataException e) {
logger.log(Level.ERROR, e.getMessage(), e);
            throw (InvalidMidiDataException) new InvalidMidiDataException().initCause(e);
        }
    }

/** debug */
private final Set<String> nc = new HashSet<>();

    /**
     * Converts a MIDI sequence into a {@link FormatType#HandyPhoneStandard} SMAF sequence.
     * <p>
     * The MIDI channel decides where a message goes,
     * {@link SmafContext#retrieveSmafTrack(int) channel / 4} is the SMAF track and
     * {@link SmafContext#retrieveVoice(int) channel % 4} the SMAF channel of it, so a MIDI
     * channel above 15 has nowhere to go. Each track is headed by the
     * {@link MetaEvent#META_MACHINE_DEPEND} message {@link VaviSmafFileFormat} needs to write
     * its Score Track Chunk header, and closed by an End of Sequence.
     * </p>
     *
     * @param fileType {@link javax.sound.midi.MidiFileFormat#getType()}
     */
    private vavi.sound.smaf.Sequence convert(Sequence midiSequence, int fileType)
        throws InvalidSmafDataException {

logger.log(Level.DEBUG, "divisionType: " + midiSequence.getDivisionType());
logger.log(Level.DEBUG, "resolution: " + midiSequence.getResolution());
logger.log(Level.DEBUG, "tickLength: " + midiSequence.getTickLength());
logger.log(Level.DEBUG, "microsecondLength: " + midiSequence.getMicrosecondLength());

        SmafContext smafContext = new SmafContext();
        smafContext.setType(fileType);
        smafContext.setMidiSequence(midiSequence);

        // the sequence name and the marker of the MIDI sequence become the contents info
        javax.sound.midi.MetaMessage songTitle = null;
        javax.sound.midi.MetaMessage songWriter = null;

        vavi.sound.smaf.Sequence smafSequence = new vavi.sound.smaf.Sequence();

        for (int i = 0; i < smafContext.getSequenceSize(); i++) {
            MidiEvent midiEvent = smafContext.getMidiEvent(i);
            MidiMessage midiMessage = midiEvent.getMessage();

            int smafTrackNumber;
            String key;

            if (midiMessage instanceof ShortMessage shortMessage) {
                int channel = shortMessage.getChannel();
                int command = shortMessage.getCommand();

                smafTrackNumber = smafContext.retrieveSmafTrack(channel);
                if (smafTrackNumber >= SmafContext.MAX_SMAF_TRACKS) {
if (nc.add("channel." + channel)) {
 logger.log(Level.WARNING, "no SMAF track for the MIDI channel: " + channel);
}
                    continue;
                }

                // a control change is told by its control number, the others by the command
                key = command == ShortMessage.CONTROL_CHANGE ?
                        "short." + command + "." + shortMessage.getData1() :
                        "short." + command;
            } else if (midiMessage instanceof javax.sound.midi.MetaMessage metaMessage) {
                if (metaMessage.getType() == MetaEvent.META_END_OF_TRACK.number()) {
                    // a MIDI track may end before the others, the End of Sequence comes below
                    continue;
                } else if (metaMessage.getType() == MetaEvent.META_TEMPO.number()) {
                    // HandyPhoneStandard has no tempo, SmafContext has baked it into the Δs
                    continue;
                } else if (metaMessage.getType() == MetaEvent.META_NAME.number() && songTitle == null) {
                    // the song title of the contents info chunk
                    songTitle = metaMessage;
                    continue;
                } else if (metaMessage.getType() == MetaEvent.META_MARKER.number() && songWriter == null) {
                    // the song writer of the contents info chunk
                    songWriter = metaMessage;
                    continue;
                }
                smafTrackNumber = 0;
                key = "meta." + metaMessage.getType();
            } else {
if (nc.add("sysex")) {
 logger.log(Level.WARNING, "sysex is not converted: " + MidiUtil.paramString(midiMessage));
}
                continue;
            }

            SmafConvertible convertible = SmafConvertible.getConvertible(key);
            if (convertible == null) {
if (nc.add(key)) {
 logger.log(Level.WARNING, "no convertible for: [" + key + "]");
}
                continue;
            }

            createTracks(smafSequence, smafContext, smafTrackNumber);
            Track smafTrack = smafSequence.getTracks()[smafTrackNumber];

            // the Δ which does not fit in one message
            add(smafTrack, smafContext.getIntervalSmafEvents(smafTrackNumber));
            add(smafTrack, convertible.getSmafEvents(midiEvent, smafContext));
        }

        if (smafSequence.getTracks().length == 0) {
            throw new InvalidSmafDataException("no tracks");
        }

        // the first track carries the contents info of the file
        insertContentsInfo(smafSequence.getTracks()[0], songWriter);
        insertContentsInfo(smafSequence.getTracks()[0], songTitle);

        // the End of Sequence closes every track at the end of the MIDI sequence
        long endTick = midiSequence.getTickLength();
        SmafConvertible convertible = SmafConvertible.getConvertible("meta." + MetaEvent.META_END_OF_TRACK.number());
        Track[] smafTracks = smafSequence.getTracks();
        for (int t = 0; t < smafTracks.length; t++) {
            add(smafTracks[t], smafContext.getIntervalSmafEvents(t, endTick));
        }
        SmafEvent[] smafEvents = convertible.getSmafEvents(new MidiEvent(endOfTrack(), endTick), smafContext);
        for (int t = 0; t < smafTracks.length && t < smafEvents.length; t++) {
            if (smafEvents[t] != null) {
                smafTracks[t].add(smafEvents[t]);
            }
        }

        return smafSequence;
    }

    /**
     * Puts a MIDI text meta message at the head of the track, where
     * {@link VaviSmafFileFormat} looks for the Option of the Contents Info Chunk.
     *
     * @param midiMetaMessage nullable, then nothing is inserted
     */
    private static void insertContentsInfo(Track smafTrack, javax.sound.midi.MetaMessage midiMetaMessage)
        throws InvalidSmafDataException {

        if (midiMetaMessage == null) {
            return;
        }
        byte[] data = midiMetaMessage.getData();
        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(midiMetaMessage.getType(), data, data.length);
        smafTrack.insert(new SmafEvent(metaMessage, 0L), 0);
logger.log(Level.DEBUG, "contents info: " + MetaEvent.valueOf(midiMetaMessage.getType()) + ": " + new String(data));
    }

    /**
     * Creates the SMAF tracks up to {@code smafTrackNumber}, the number is the index into
     * {@link vavi.sound.smaf.Sequence#getTracks()} so the ones before it have to be there too.
     */
    private static void createTracks(vavi.sound.smaf.Sequence smafSequence, SmafContext smafContext, int smafTrackNumber)
        throws InvalidSmafDataException {

        for (int t = smafSequence.getTracks().length; t <= smafTrackNumber; t++) {
            Track smafTrack = smafSequence.createTrack();
            smafContext.setTrackUsed(t, true);
            smafTrack.add(new SmafEvent(machineDependentMessage(), 0L));
logger.log(Level.DEBUG, "create SMAF track: " + t);
        }
    }

    /**
     * The Score Track Chunk header of a track to be written, the counterpart of what
     * {@link ScoreTrackChunk#getSmafEvents()} reads out of a file.
     */
    private static MetaMessage machineDependentMessage() throws InvalidSmafDataException {
        Map<String, Object> props = new HashMap<>();
        props.put("localType", ScoreTrackChunk.class);
        props.put("formatType", FormatType.HandyPhoneStandard);
        props.put("sequenceType", SequenceType.StreamSequence);
        props.put("channelStatuses", channelStatuses());
        props.put("durationTimeBase", SmafContext.TIME_BASE);
        props.put("gateTimeTimeBase", SmafContext.TIME_BASE);

        MetaMessage metaMessage = new MetaMessage();
        metaMessage.setMessage(MetaEvent.META_MACHINE_DEPEND.number(), props);
        return metaMessage;
    }

    /**
     * The channel status of the 4 SMAF channels of a track. Which MIDI channel plays rhythm is
     * not known before the whole sequence has been read, so every channel is "no care", which
     * makes the MIDI drum channel the rhythm one when it is read back.
     *
     * @see MidiContext#CHANNEL_DRUM
     */
    private static ChannelStatus[] channelStatuses() {
        ChannelStatus[] channelStatuses = new ChannelStatus[SmafContext.MAX_SMAF_CHANNELS];
        for (int c = 0; c < channelStatuses.length; c++) {
            channelStatuses[c] = new ChannelStatus(c, (byte) ChannelStatus.Type.NoCare.ordinal());
        }
        return channelStatuses;
    }

    /** */
    private static javax.sound.midi.MetaMessage endOfTrack() throws InvalidSmafDataException {
        try {
            javax.sound.midi.MetaMessage metaMessage = new javax.sound.midi.MetaMessage();
            metaMessage.setMessage(MetaEvent.META_END_OF_TRACK.number(), new byte[0], 0);
            return metaMessage;
        } catch (InvalidMidiDataException e) {
            throw new InvalidSmafDataException(e);
        }
    }

    /** @param smafEvents nullable */
    private static void add(Track smafTrack, SmafEvent[] smafEvents) {
        if (smafEvents != null) {
            for (SmafEvent smafEvent : smafEvents) {
                if (smafEvent == null) {
logger.log(Level.WARNING, "event is null[" + smafTrack.size() + "]");
                    continue;
                }
                smafTrack.add(smafEvent);
            }
        }
    }
}
