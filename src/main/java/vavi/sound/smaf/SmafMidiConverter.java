/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;

import vavi.sound.midi.MidiUtil;
import vavi.sound.midi.smaf.SmafVaviSequence;
import vavi.sound.smaf.message.MidiContext;
import vavi.sound.smaf.message.MidiConvertible;
import vavi.util.Debug;


/**
 * SmafMidiConverter.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 071012 nsano initial version <br>
 */
class SmafMidiConverter implements SmafDevice {

    /** the device information */
    private static final SmafDevice.Info info =
        new SmafDevice.Info("Java MIDI, SMAF Sequence Converter",
                            "Vavisoft",
                            "Format Converter between MIDI and SMAF",
                            "Version " + SmafDeviceProvider.version) {};

    /* */
    public Info getDeviceInfo() {
        return info;
    }

    /* */
    public void close() {
    }

    /* */
    public boolean isOpen() {
        return true;
    }

    /* */
    public void open() {
    }

/** debug */
private Set<Class<? extends SmafMessage>> uc = new HashSet<>();

    /** Converts smaf sequence to midi sequence */
    Sequence convert(vavi.sound.smaf.Sequence smafSequence)
        throws InvalidMidiDataException,
               IOException,
               InvalidSmafDataException {

        Track[] smafTracks = smafSequence.getTracks();

        MidiContext midiContext = new MidiContext();

        int resolution = midiContext.getResolution(smafTracks);
Debug.println("resolution: " + resolution);
        Sequence midiSequence = new SmafVaviSequence(Sequence.PPQ, resolution, 1);
        javax.sound.midi.Track midiTrack = midiSequence.getTracks()[0];

        midiTrack.add(midiContext.getTempoEvent());

        for (int i = 0; i < smafTracks.length; i++) {

            midiContext.setMidiTrack(midiTrack);

            midiContext.setSmafTrackNumber(i);
            midiContext.setTrack(smafTracks[i]);
            midiContext.setCurrentTick(0);

            Track smafTrack = smafTracks[i];

            for (int j = 0; j < smafTrack.size(); j++) {
                SmafEvent smafEvent = smafTrack.get(j);
                SmafMessage smafMessage = smafEvent.getMessage();

                midiContext.addCurrentTick(midiContext.getTicksOf(smafMessage.getDuration()));
//Debug.println("■■■■■(" + i + ":" + j + ") ticks: " + midiContext.getCurrentTick() + "(" + midiContext.getTicksOf(smafMessage.getDuration()) + "," + smafMessage.getDuration() + "), " + smafMessage.getClass().getSimpleName());

                if (smafMessage instanceof MidiConvertible) {
if (!(smafMessage instanceof vavi.sound.smaf.message.NoteMessage) &&
    !(smafMessage instanceof vavi.sound.smaf.message.ModulationMessage) &&
    !(smafMessage instanceof vavi.sound.smaf.message.PitchBendMessage) &&
    !(smafMessage instanceof vavi.sound.smaf.message.PanMessage) &&
    !(smafMessage instanceof vavi.sound.smaf.message.ExpressionMessage)) {
 Debug.println("midi convertible(" + i + ":" + j + "): " + smafMessage);
}
//if (smafMessage instanceof vavi.sound.smaf.message.NoteMessage) {
// int gateTime = ((vavi.sound.smaf.message.NoteMessage) smafMessage).getGateTime();
// if (gateTime == 0) {
//  Debug.println(Level.WARNING, "★★★★★(" + i + ":" + j + ") gateTime == 0: " + smafMessage);
// }
//}
                    MidiEvent[] midiEvents = ((MidiConvertible) smafMessage).getMidiEvents(midiContext);
                    if (midiEvents != null) {
                        for (int k = 0; k < midiEvents.length; k++) {
                            midiTrack.add(midiEvents[k]);
//                          addSmafMessage(midiTrack, midiEvents[k]);
                        }
                    }
                } else if (smafMessage instanceof MetaMessage) {
                    Debug.println("meta: " + MetaMessage.class.cast(smafMessage).getType());
                    for (Map.Entry<String, Object> entry : MetaMessage.class.cast(smafMessage).data.entrySet()) {
                        Debug.println(entry.getKey() + "=" + entry.getValue());
                    }
                } else {
if (!uc.contains(smafMessage.getClass())) {
 Debug.println(Level.WARNING, "unhandled message: " + smafMessage);
 uc.add(smafMessage.getClass());
}
                }
            }
        }

        return midiSequence;
    }

    /** Note が Control/Program より先に入ることがある */
    @SuppressWarnings("unused")
    private void addSmafMessage(javax.sound.midi.Track midiTrack, MidiEvent midiEvent) {
//Debug.println("★: " + midiEvent.getMessage());
//Debug.println("★: " + (midiTrack.size() > 1 ? midiTrack.get(midiTrack.size() - 2).getMessage() : null));
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
Debug.println("★★★★★ : " + MidiUtil.paramString(midiEvent.getMessage()) + ", " + MidiUtil.paramString(removedMidiEvent.getMessage()));
        } else {
            midiTrack.add(midiEvent);
        }
    }

    /** Converts midi sequence to smaf sequence */
    vavi.sound.smaf.Sequence convert(Sequence midiSequence, int fileType)
        throws InvalidMidiDataException,
               IOException,
               InvalidSmafDataException {

        throw new UnsupportedOperationException("not implemented yet");
    }
}

/* */
