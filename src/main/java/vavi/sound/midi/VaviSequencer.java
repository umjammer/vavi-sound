/*
 * Copyright (c) 2009 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

import vavi.util.Debug;


/**
 * VaviSequencer. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 090110 nsano initial version <br>
 */
class VaviSequencer implements Sequencer {

    private static final String version = "1.0.0.";

    /** the device information */
    protected static final MidiDevice.Info info =
        new MidiDevice.Info("Java MIDI(MFi/SMAF) ADPCM Sequencer",
                            "Vavisoft",
                            "Software sequencer using adpcm",
                            "Version " + version) {};

    /** */
    private Sequencer sequencer = MidiUtil.getDefaultSequencer(VaviMidiDeviceProvider.class);

    /** */
    private VaviSequence vaviSequence;

    /* */
    public void start() {
        if (vaviSequence != null) {
            sequencer.addMetaEventListener(vaviSequence.getMetaEventListener()); // TODO more smart way
Debug.println(Level.FINE, "★1 mel: " + vaviSequence);
        }
        sequencer.start();
Debug.println(Level.FINE, "★1 start: " + sequencer.hashCode());
    }

    @Override
    public void stop() {
Debug.println(Level.FINE, "★1 stop: " + sequencer.hashCode());
        sequencer.stop();
        if (vaviSequence != null) {
Debug.println(Level.FINE, "★1 mel: " + vaviSequence);
            sequencer.removeMetaEventListener(vaviSequence.getMetaEventListener()); // TODO more smart way
        }
    }

    @Override
    public void close() {
Debug.println(Level.FINE, "★1 close: " + sequencer.hashCode());
        sequencer.close();
    }

    @Override
    public void open() throws MidiUnavailableException {
Debug.println(Level.FINE, "★1 open: " + sequencer.hashCode());
        sequencer.open();
    }

    /* */
    public Info getDeviceInfo() {
        return info;
    }

    /* */
    public int[] addControllerEventListener(ControllerEventListener listener, int[] controllers) {
        return sequencer.addControllerEventListener(listener, controllers);
    }

    /* */
    public boolean addMetaEventListener(MetaEventListener listener) {
        return sequencer.addMetaEventListener(listener);
    }

    /* */
    public int getLoopCount() {
        return sequencer.getLoopCount();
    }

    /* */
    public long getLoopEndPoint() {
        return sequencer.getLoopEndPoint();
    }

    /* */
    public long getLoopStartPoint() {
        return sequencer.getLoopStartPoint();
    }

    /* */
    public SyncMode getMasterSyncMode() {
        return sequencer.getMasterSyncMode();
    }

    /* */
    public SyncMode[] getMasterSyncModes() {
        return sequencer.getMasterSyncModes();
    }

    /* */
    public long getMicrosecondLength() {
        return sequencer.getMicrosecondLength();
    }

    /* */
    public long getMicrosecondPosition() {
        return sequencer.getMicrosecondPosition();
    }

    /* */
    public Sequence getSequence() {
        return sequencer.getSequence();
    }

    /* */
    public SyncMode getSlaveSyncMode() {
        return sequencer.getSlaveSyncMode();
    }

    /* */
    public SyncMode[] getSlaveSyncModes() {
        return sequencer.getSlaveSyncModes();
    }

    /* */
    public float getTempoFactor() {
        return sequencer.getTempoFactor();
    }

    /* */
    public float getTempoInBPM() {
        return sequencer.getTempoInBPM();
    }

    /* */
    public float getTempoInMPQ() {
        return sequencer.getTempoInMPQ();
    }

    /* */
    public long getTickLength() {
        return sequencer.getTickLength();
    }

    /* */
    public long getTickPosition() {
        return sequencer.getTickPosition();
    }

    /* */
    public boolean getTrackMute(int track) {
        return sequencer.getTrackMute(track);
    }

    /* */
    public boolean getTrackSolo(int track) {
        return sequencer.getTrackSolo(track);
    }

    /* */
    public boolean isRecording() {
        return sequencer.isRecording();
    }

    /* */
    public boolean isRunning() {
        return sequencer.isRunning();
    }

    /* */
    public void recordDisable(Track track) {
        sequencer.recordDisable(track);
    }

    /* */
    public void recordEnable(Track track, int channel) {
        sequencer.recordEnable(track, channel);
    }

    /* */
    public int[] removeControllerEventListener(ControllerEventListener listener, int[] controllers) {
        return sequencer.removeControllerEventListener(listener, controllers);
    }

    /* */
    public void removeMetaEventListener(MetaEventListener listener) {
        sequencer.removeMetaEventListener(listener);
    }

    /* */
    public void setLoopCount(int count) {
        sequencer.setLoopCount(count);
    }

    /* */
    public void setLoopEndPoint(long tick) {
        sequencer.setLoopEndPoint(tick);
    }

    /* */
    public void setLoopStartPoint(long tick) {
        sequencer.setLoopStartPoint(tick);
    }

    /* */
    public void setMasterSyncMode(SyncMode sync) {
        sequencer.setMasterSyncMode(sync);
    }

    /* */
    public void setMicrosecondPosition(long microseconds) {
        sequencer.setMicrosecondPosition(microseconds);
    }

    /* */
    public void setSequence(Sequence sequence) throws InvalidMidiDataException {
        sequencer.setSequence(sequence);
        setVaviSequence();
    }

    /** */
    private void setVaviSequence() {
        if (getSequence() instanceof VaviSequence) {
            vaviSequence = (VaviSequence) getSequence(); 
Debug.println("vaviSequence: " + vaviSequence);
        } else {
            vaviSequence = null; 
        }
    }

    /* TODO check, see package.html */
    public void setSequence(InputStream stream) throws IOException, InvalidMidiDataException {
        sequencer.setSequence(stream);
        setVaviSequence();
    }

    /* */
    public void setSlaveSyncMode(SyncMode sync) {
        sequencer.setSlaveSyncMode(sync);
    }

    /* */
    public void setTempoFactor(float factor) {
        sequencer.setTempoFactor(factor);
    }

    /* */
    public void setTempoInBPM(float bpm) {
        sequencer.setTempoInBPM(bpm);
    }

    /* */
    public void setTempoInMPQ(float mpq) {
        sequencer.setTempoInMPQ(mpq);
    }

    /* */
    public void setTickPosition(long tick) {
        sequencer.setTickPosition(tick);
    }

    /* */
    public void setTrackMute(int track, boolean mute) {
        sequencer.setTrackMute(track, mute);
    }

    /* */
    public void setTrackSolo(int track, boolean solo) {
        sequencer.setTrackSolo(track, solo);
    }

    /* */
    public void startRecording() {
        sequencer.startRecording();
    }

    /* */
    public void stopRecording() {
        sequencer.stopRecording();
    }

    /* */
    public int getMaxReceivers() {
        return sequencer.getMaxReceivers();
    }

    /* */
    public int getMaxTransmitters() {
        return sequencer.getMaxTransmitters();
    }

    /* */
    public Receiver getReceiver() throws MidiUnavailableException {
        return sequencer.getReceiver();
    }

    /* */
    public List<Receiver> getReceivers() {
        return sequencer.getReceivers();
    }

    /* */
    public Transmitter getTransmitter() throws MidiUnavailableException {
        return sequencer.getTransmitter();
    }

    /* */
    public List<Transmitter> getTransmitters() {
        return sequencer.getTransmitters();
    }

    /* */
    public boolean isOpen() {
        return sequencer.isOpen();
    }

    /* */
    public String toString() {
        return "VaviSequencer: wrapped: " + sequencer.getClass().getName();
    }
}

/* */
