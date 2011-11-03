/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.smaf;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.klab.commons.cli.Argument;
import org.klab.commons.cli.HelpOption;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import vavi.sound.mobile.AudioEngine;
import vavi.sound.sampled.FilterChain;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.sound.smaf.SmafMessage;
import vavi.sound.smaf.chunk.AudioSequenceDataChunk;
import vavi.sound.smaf.chunk.ContentsInfoChunk;
import vavi.sound.smaf.chunk.FileChunk;
import vavi.sound.smaf.chunk.PcmAudioTrackChunk;
import vavi.sound.smaf.chunk.SeekAndPhraseInfoChunk;
import vavi.sound.smaf.chunk.WaveDataChunk;
import vavi.sound.smaf.chunk.WaveType;
import vavi.sound.smaf.chunk.TrackChunk.FormatType;
import vavi.sound.smaf.chunk.TrackChunk.SequenceType;
import vavi.sound.smaf.message.EndOfSequenceMessage;
import vavi.sound.smaf.message.NopMessage;
import vavi.sound.smaf.message.VolumeMessage;
import vavi.sound.smaf.message.WaveMessage;
import vavi.sound.smaf.sequencer.WaveSequencer;
import vavi.util.Debug;


/**
 * SmafWithVoiceMaker.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 080415 nsano initial version <br>
 */
class SmafWithVoiceMaker {

    /** source PCM */
    private AudioInputStream sourceAis;

    /** output file */
    private String filename;

    /** time in second */
    protected float time;

    /** ADPCM sampling rate */
    protected int samplingRate;
    /** ADPCM sampling bits */
    protected int bits;
    /** ADPCM number of channels */
    protected int channels;
    /** master volume */
    protected int masterVolume;
    /** ADPCM volume */
    protected int adpcmVolume;

    /** */
    private static int toReal(int base, int percent) {
        return (int) ((float) base * percent / 100); 
    }
    
    /**
     * 
     * @param sourceAis source PCM
     * @param filename output file
     */
    public SmafWithVoiceMaker(AudioInputStream sourceAis, String filename, float time, int samplingRate, int bits, int channels, int masterVolume, int adpcmVolume) {
        this(time, samplingRate, bits, channels, masterVolume, adpcmVolume);

        this.sourceAis = sourceAis;

        this.filename = filename;
    }

    /**
     * 
     * @param time time in second
     * @param bits ADPCM sampling bits
     */
    protected SmafWithVoiceMaker(float time, int samplingRate, int bits, int channels, int masterVolume, int adpcmVolume) {
        this.time = time;

        this.samplingRate = samplingRate;
        this.bits = bits;
        this.channels = channels;
        this.masterVolume = toReal(0x7f, masterVolume);
        this.adpcmVolume = toReal(0x3f, adpcmVolume);
    }

    /**
     * Creates a MFi.
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @throws InvalidSmafDataException
     * @return total size written
     */
    public int create() throws IOException, UnsupportedAudioFileException, InvalidSmafDataException {
long t = System.currentTimeMillis();
        // divide
Debug.println("1: " + (System.currentTimeMillis() - t));
t = System.currentTimeMillis();
        byte[] buffer = new byte[sourceAis.available()];
        int l = 0;
        while (l < buffer.length) {
            int r = sourceAis.read(buffer, l, buffer.length - l);
            l += r;
        }
        int result = createSMAF(buffer, new File(filename));
Debug.println("2: " + (System.currentTimeMillis() - t));
t = System.currentTimeMillis();
        return result;
    }

    /** */
    private static final String df = "yyyyMMdd";

    /** */
    private static final int ADPCM = 1;

    /**
     * Creates a SMAF.
     * TODO channels
     * @param data PCM data
     * @param file output file
     * @return size written
     */
    protected int createSMAF(byte[] data, File file) throws InvalidSmafDataException, IOException {

        ContentsInfoChunk contentsInfoChunk = new ContentsInfoChunk(); 
        contentsInfoChunk.setContentsClass(ContentsInfoChunk.CONTENT_CLASS_YAMAHA);
        contentsInfoChunk.setContentsType(1);
        contentsInfoChunk.setContentsCodeType(0);
        contentsInfoChunk.setCopyStatus(0);
        contentsInfoChunk.setCopyCounts(0);
        contentsInfoChunk.addSubData("M2", "\0");       // ???
        contentsInfoChunk.addSubData("ST", file.getName().substring(0, file.getName().lastIndexOf('.')));
        contentsInfoChunk.addSubData("CD", new SimpleDateFormat(df).format(new Date()));
        contentsInfoChunk.addSubData("A0", "YW21CE");   // ???
        contentsInfoChunk.addSubData("A2", "YW21BA");   // ???
        contentsInfoChunk.addSubData("VN", vn);
        contentsInfoChunk.addSubData("CR", cr);

        int timeBase = 4; // [ms]
        
Debug.println("time: " + time + ", " + data.length);
        int numberOfChunks = (int) ((time * 1000) / (NopMessage.maxSteps * timeBase));
Debug.println("numberOfChunks: " + numberOfChunks);
        int moduloOfChunks = (int) ((time * 1000) % (NopMessage.maxSteps * timeBase) / timeBase);
Debug.println("moduloOfChunks: " + moduloOfChunks);

        int messageBytes = 0;
        int streamNumber = 1;

        AudioSequenceDataChunk audioSequenceDataChunk = new AudioSequenceDataChunk();
        SmafMessage message = new VolumeMessage(0, 0, 127);
        audioSequenceDataChunk.addSmafMessage(message);
        messageBytes += message.getLength();
//Debug.println("messageBytes: volume: " + messageBytes);
        for (int i = 0; i < numberOfChunks; i++) {
            message = new WaveMessage(0, 0, streamNumber++, NopMessage.maxSteps);
            audioSequenceDataChunk.addSmafMessage(message);
            messageBytes += message.getLength();
//Debug.println("messageBytes: wave: " + messageBytes);

            message = new NopMessage(NopMessage.maxSteps);
            audioSequenceDataChunk.addSmafMessage(message);
            messageBytes += message.getLength();
//Debug.println("messageBytes: nop: " + messageBytes);
        }
        if (moduloOfChunks != 0) {
            message = new WaveMessage(0, 0, streamNumber++, moduloOfChunks);
            audioSequenceDataChunk.addSmafMessage(message);
            messageBytes += message.getLength();
//Debug.println("messageBytes: wave: " + messageBytes);

            message = new NopMessage(moduloOfChunks);
            audioSequenceDataChunk.addSmafMessage(message);
            messageBytes += message.getLength();
//Debug.println("messageBytes: nop: " + messageBytes);
        }
        audioSequenceDataChunk.addSmafMessage(new EndOfSequenceMessage(0));

        SeekAndPhraseInfoChunk seekAndPhraseInfoChunk = new SeekAndPhraseInfoChunk();
        seekAndPhraseInfoChunk.setStartPoint(0);
        seekAndPhraseInfoChunk.setStopPoint(messageBytes);
Debug.println("sp: " + messageBytes);

        AudioEngine audioEngine = WaveSequencer.Factory.getAudioEngine(ADPCM);
        int chunkSize = numberOfChunks == 0 ? 0 : data.length / numberOfChunks;
Debug.println("chunkSize: " + chunkSize);
        int moduloChunkSize = numberOfChunks == 0 ? data.length : data.length % chunkSize;
Debug.println("moduloChunkSize: " + moduloChunkSize);
        streamNumber = 1;

        PcmAudioTrackChunk pcmAudioTrackChunk = new PcmAudioTrackChunk();
        pcmAudioTrackChunk.setFormatType(FormatType.HandyPhoneStandard);
        pcmAudioTrackChunk.setSequenceType(SequenceType.StreamSequence);
        pcmAudioTrackChunk.setWaveType(new WaveType(channels, ADPCM, samplingRate, bits));
        pcmAudioTrackChunk.setDurationTimeBase(timeBase);
        pcmAudioTrackChunk.setGateTimeTimeBase(timeBase);
        pcmAudioTrackChunk.setSeekAndPhraseInfoChunk(seekAndPhraseInfoChunk);
        pcmAudioTrackChunk.setSequenceDataChunk(audioSequenceDataChunk);
        for (int i = 0; i < numberOfChunks; i++) {
            byte[] temp = new byte[chunkSize];
            System.arraycopy(data, chunkSize * i, temp, 0, chunkSize);
            byte[] adpcm = audioEngine.encode(bits, channels, temp);
            WaveDataChunk waveDataChunk = new WaveDataChunk();
            waveDataChunk.setWaveNumber(streamNumber++);
            waveDataChunk.setWaveData(adpcm);
            pcmAudioTrackChunk.addWaveDataChunk(waveDataChunk);
        }
        if (moduloOfChunks != 0) {
            byte[] temp = new byte[moduloChunkSize];
            System.arraycopy(data, chunkSize * numberOfChunks, temp, 0, moduloChunkSize);
            byte[] adpcm = audioEngine.encode(bits, channels, temp);
            WaveDataChunk waveDataChunk = new WaveDataChunk();
            waveDataChunk.setWaveNumber(streamNumber++);
            waveDataChunk.setWaveData(adpcm);
            pcmAudioTrackChunk.addWaveDataChunk(waveDataChunk);
        }

        FileChunk fileChunk = new FileChunk();
        fileChunk.setContentsInfoChunk(contentsInfoChunk);
        fileChunk.addPcmAudioTrackChunk(pcmAudioTrackChunk);

        fileChunk.writeTo(new FileOutputStream(file));
        int r = fileChunk.getSize();
Debug.println("write: " + r);
        return r;
    }

    /** vendor */
    protected static String vn;

    /** copyright */
    protected static String cr;

    /** */
    protected static String defaultModel;

    /** */
    static {
        try {
            Properties props = new Properties();
            props.load(SmafWithVoiceMaker.class.getResourceAsStream("/vavi/sound/sampled/smaf/SmafWithVoiceMaker.properties"));

            vn = props.getProperty("contentsInfo.subDatum.vn");
            cr = props.getProperty("contentsInfo.subDatum.cr");
            defaultModel = props.getProperty("defaultModel");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    //----

    @Options
    @HelpOption(argName = "help", option = "?", description = "print this help")
    public static class Arguments {
        @Argument(index = 0)
        File file;
        @Option(argName = "filename", option = "f", args = 1, required = false, description = "output mmf filename")
        String outFilename = "out.mmf";
        @Option(argName = "model", option = "m", required = false, args = 1, description = "terminal model")
        String model = defaultModel;
        @Option(argName = "rate", option = "r", required = false, args = 1, description = "adpcm sampling rate [Hz]")
        int samplingRate = 16000;
        @Option(argName = "bits", option = "b", required = false, args = 1, description = "adpcm sampling bits")
        int bits = 4;
        @Option(argName = "channels", option = "c", required = false, args = 1, description = "adpcm channels")
        int channels = 1;
        @Option(argName = "masterVolume", option = "v", required = false, args = 1, description = "master volume in [%]")
        int masterVolume = 100;
        @Option(argName = "adpcmVolume", option = "a", required = false, args = 1, description = "adpcm volume in [%]")
        int adpcmVolume = 100;
    }

    /**
     * Creates .mmf w/ voice file.
     * 
     * @param args input wave file
     *             -f output mmf filename
     *             -r adpcm sampling rate [Hz]
     *             -b adpcm sampling bits
     *             -c adpcm channels
     *             -v master volume [%]
     *             -a adpcm volume [%]
     */
    public static void main(String[] args) {
        try {
            Arguments arguments = new Arguments();
            Options.Util.bind(args, arguments);

            // create
            AudioInputStream ais = AudioSystem.getAudioInputStream(arguments.file);
            FilterChain filterChain = new FilterChain();
            SmafWithVoiceMaker mwvm = new SmafWithVoiceMaker(filterChain.doFilter(ais), arguments.outFilename, 0 /* TODO */, arguments.samplingRate, arguments.bits, arguments.channels, arguments.masterVolume, arguments.adpcmVolume);
            mwvm.create();

            // done
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}

/* */
