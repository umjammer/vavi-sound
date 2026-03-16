/*
 * Copyright (c) 2004 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.midi.smaf;

import java.util.List;
import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;

import vavi.sound.midi.MidiUtil;
import vavi.sound.smaf.SmafSynthesizer.SmafReceiver;

import static vavi.sound.midi.VaviMidiDeviceProvider.version;


/**
 * SmafVaviSequence.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 041222 nsano initial version <br>
 */
public class SmafSynthesizer implements Synthesizer {

    /** the device information */
    public static final MidiDevice.Info info =
            new MidiDevice.Info("Java MIDI(SMAF) Synthesizer",
                    "vavi",
                    "Software synthesizer for SMAF with adpcm",
                    "Version " + version) {};

    private final Synthesizer midiSynthesizer;

    public SmafSynthesizer() {
        this.midiSynthesizer = MidiUtil.getDefaultSynthesizer(vavi.sound.midi.VaviMidiDeviceProvider.class);
    }

    @Override
    public Info getDeviceInfo() {
        return info;
    }

    @Override
    public boolean isOpen() {
        return midiSynthesizer.isOpen();
    }

    @Override
    public long getMicrosecondPosition() {
        return midiSynthesizer.getMicrosecondPosition();
    }

    @Override
    public int getMaxReceivers() {
        return midiSynthesizer.getMaxReceivers();
    }

    @Override
    public int getMaxTransmitters() {
        return midiSynthesizer.getMaxTransmitters();
    }

    @Override
    public void open() throws MidiUnavailableException {
        midiSynthesizer.open();
    }

    @Override
    public void close() {
        midiSynthesizer.close();
    }

    @Override
    public int getMaxPolyphony() {
        return midiSynthesizer.getMaxPolyphony();
    }

    @Override
    public long getLatency() {
        return midiSynthesizer.getLatency();
    }

    @Override
    public MidiChannel[] getChannels() {
        return midiSynthesizer.getChannels();
    }

    @Override
    public VoiceStatus[] getVoiceStatus() {
        return midiSynthesizer.getVoiceStatus();
    }

    @Override
    public boolean isSoundbankSupported(Soundbank soundbank) {
        return midiSynthesizer.isSoundbankSupported(soundbank);
    }

    @Override
    public boolean loadInstrument(Instrument instrument) {
        return midiSynthesizer.loadInstrument(instrument);
    }

    @Override
    public void unloadInstrument(Instrument instrument) {
        midiSynthesizer.unloadInstrument(instrument);
    }

    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {
        return midiSynthesizer.remapInstrument(from, to);
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        return midiSynthesizer.getDefaultSoundbank();
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        return midiSynthesizer.getAvailableInstruments();
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        return midiSynthesizer.getLoadedInstruments();
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        return midiSynthesizer.loadAllInstruments(soundbank);
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        midiSynthesizer.unloadAllInstruments(soundbank);
    }

    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        return midiSynthesizer.loadInstruments(soundbank, patchList);
    }

    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
        midiSynthesizer.unloadInstruments(soundbank, patchList);
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {
        return new SmafReceiver(midiSynthesizer);
    }

    @Override
    public List<Receiver> getReceivers() {
        return midiSynthesizer.getReceivers();
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {
        return midiSynthesizer.getTransmitter();
    }

    @Override
    public List<Transmitter> getTransmitters() {
        return midiSynthesizer.getTransmitters();
    }
}
