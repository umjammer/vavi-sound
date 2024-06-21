/*
 * Copyright (c) 2009 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Properties;
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

import static java.lang.System.getLogger;


/**
 * VaviSequencer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 090110 nsano initial version <br>
 */
class VaviSequencer implements Sequencer {

    private static final Logger logger = getLogger(VaviSequencer.class.getName());

    static {
        try {
            try (InputStream is = VaviSequencer.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound/pom.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version", "undefined in pom.properties");
                } else {
                    version = System.getProperty("vavi.test.version", "undefined");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static final String version;

    /** the device information */
    protected static final MidiDevice.Info info =
        new MidiDevice.Info("Java MIDI(MFi/SMAF) ADPCM Sequencer",
                            "vavi",
                            "Software sequencer using adpcm",
                            "Version " + version) {};

    /** */
    private final Sequencer sequencer = MidiUtil.getDefaultSequencer(VaviMidiDeviceProvider.class);

    /** */
    private VaviSequence vaviSequence;

    @Override
    public void start() {
        if (vaviSequence != null) {
            sequencer.addMetaEventListener(vaviSequence.getMetaEventListener()); // TODO more smart way
logger.log(Level.DEBUG, "★1 mel: " + vaviSequence);
        }
        sequencer.start();
logger.log(Level.DEBUG, "★1 start: " + sequencer.hashCode());
    }

    @Override
    public void stop() {
logger.log(Level.DEBUG, "★1 stop: " + sequencer.hashCode());
        sequencer.stop();
        if (vaviSequence != null) {
logger.log(Level.DEBUG, "★1 mel: " + vaviSequence);
            sequencer.removeMetaEventListener(vaviSequence.getMetaEventListener()); // TODO more smart way
        }
    }

    @Override
    public void close() {
logger.log(Level.DEBUG, "★1 close: " + sequencer.hashCode());
        sequencer.close();
    }

    @Override
    public void open() throws MidiUnavailableException {
logger.log(Level.DEBUG, "★1 open: " + sequencer.hashCode());
        sequencer.open();
    }

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public int[] addControllerEventListener(ControllerEventListener listener, int[] controllers) {
        return sequencer.addControllerEventListener(listener, controllers);
    }

    @Override
    public boolean addMetaEventListener(MetaEventListener listener) {
        return sequencer.addMetaEventListener(listener);
    }

    @Override
    public int getLoopCount() {
        return sequencer.getLoopCount();
    }

    @Override
    public long getLoopEndPoint() {
        return sequencer.getLoopEndPoint();
    }

    @Override
    public long getLoopStartPoint() {
        return sequencer.getLoopStartPoint();
    }

    @Override
    public SyncMode getMasterSyncMode() {
        return sequencer.getMasterSyncMode();
    }

    @Override
    public SyncMode[] getMasterSyncModes() {
        return sequencer.getMasterSyncModes();
    }

    @Override
    public long getMicrosecondLength() {
        return sequencer.getMicrosecondLength();
    }

    @Override
    public long getMicrosecondPosition() {
        return sequencer.getMicrosecondPosition();
    }

    @Override
    public Sequence getSequence() {
        return sequencer.getSequence();
    }

    @Override
    public SyncMode getSlaveSyncMode() {
        return sequencer.getSlaveSyncMode();
    }

    @Override
    public SyncMode[] getSlaveSyncModes() {
        return sequencer.getSlaveSyncModes();
    }

    @Override
    public float getTempoFactor() {
        return sequencer.getTempoFactor();
    }

    @Override
    public float getTempoInBPM() {
        return sequencer.getTempoInBPM();
    }

    @Override
    public float getTempoInMPQ() {
        return sequencer.getTempoInMPQ();
    }

    @Override
    public long getTickLength() {
        return sequencer.getTickLength();
    }

    @Override
    public long getTickPosition() {
        return sequencer.getTickPosition();
    }

    @Override
    public boolean getTrackMute(int track) {
        return sequencer.getTrackMute(track);
    }

    @Override
    public boolean getTrackSolo(int track) {
        return sequencer.getTrackSolo(track);
    }

    @Override
    public boolean isRecording() {
        return sequencer.isRecording();
    }

    @Override
    public boolean isRunning() {
        return sequencer.isRunning();
    }

    @Override
    public void recordDisable(Track track) {
        sequencer.recordDisable(track);
    }

    @Override
    public void recordEnable(Track track, int channel) {
        sequencer.recordEnable(track, channel);
    }

    @Override
    public int[] removeControllerEventListener(ControllerEventListener listener, int[] controllers) {
        return sequencer.removeControllerEventListener(listener, controllers);
    }

    @Override
    public void removeMetaEventListener(MetaEventListener listener) {
        sequencer.removeMetaEventListener(listener);
    }

    @Override
    public void setLoopCount(int count) {
        sequencer.setLoopCount(count);
    }

    @Override
    public void setLoopEndPoint(long tick) {
        sequencer.setLoopEndPoint(tick);
    }

    @Override
    public void setLoopStartPoint(long tick) {
        sequencer.setLoopStartPoint(tick);
    }

    @Override
    public void setMasterSyncMode(SyncMode sync) {
        sequencer.setMasterSyncMode(sync);
    }

    @Override
    public void setMicrosecondPosition(long microseconds) {
        sequencer.setMicrosecondPosition(microseconds);
    }

    @Override
    public void setSequence(Sequence sequence) throws InvalidMidiDataException {
        sequencer.setSequence(sequence);
        setVaviSequence();
    }

    /** */
    private void setVaviSequence() {
        if (getSequence() instanceof VaviSequence) {
            vaviSequence = (VaviSequence) getSequence();
logger.log(Level.DEBUG, "vaviSequence: " + vaviSequence);
        } else {
            vaviSequence = null;
        }
    }

    /* TODO check, see package.html */
    @Override
    public void setSequence(InputStream stream) throws IOException, InvalidMidiDataException {
        sequencer.setSequence(stream);
        setVaviSequence();
    }

    @Override
    public void setSlaveSyncMode(SyncMode sync) {
        sequencer.setSlaveSyncMode(sync);
    }

    @Override
    public void setTempoFactor(float factor) {
        sequencer.setTempoFactor(factor);
    }

    @Override
    public void setTempoInBPM(float bpm) {
        sequencer.setTempoInBPM(bpm);
    }

    @Override
    public void setTempoInMPQ(float mpq) {
        sequencer.setTempoInMPQ(mpq);
    }

    @Override
    public void setTickPosition(long tick) {
        sequencer.setTickPosition(tick);
    }

    @Override
    public void setTrackMute(int track, boolean mute) {
        sequencer.setTrackMute(track, mute);
    }

    @Override
    public void setTrackSolo(int track, boolean solo) {
        sequencer.setTrackSolo(track, solo);
    }

    @Override
    public void startRecording() {
        sequencer.startRecording();
    }

    @Override
    public void stopRecording() {
        sequencer.stopRecording();
    }

    @Override
    public int getMaxReceivers() {
        return sequencer.getMaxReceivers();
    }

    @Override
    public int getMaxTransmitters() {
        return sequencer.getMaxTransmitters();
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return sequencer.getReceiver();
    }

    @Override
    public List<Receiver> getReceivers() {
        return sequencer.getReceivers();
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        return sequencer.getTransmitter();
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return sequencer.getTransmitters();
    }

    @Override
    public boolean isOpen() {
        return sequencer.isOpen();
    }

    @Override
    public String toString() {
        return "VaviSequencer: wrapped: " + sequencer.getClass().getName();
    }
}
