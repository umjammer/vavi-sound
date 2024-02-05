/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.klab.commons.cli.Argument;
import org.klab.commons.cli.HelpOption;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.mfi.MfiEvent;
import vavi.sound.mfi.MfiMessage;
import vavi.sound.mfi.MfiSystem;
import vavi.sound.mfi.Sequence;
import vavi.sound.mfi.Track;
import vavi.sound.mfi.vavi.VaviMfiFileFormat;
import vavi.sound.mfi.vavi.header.ProtMessage;
import vavi.sound.mfi.vavi.header.SorcMessage;
import vavi.sound.mfi.vavi.header.TitlMessage;
import vavi.sound.mfi.vavi.header.VersMessage;
import vavi.sound.mfi.vavi.track.EndOfTrackMessage;
import vavi.sound.sampled.FilterChain;
import vavi.util.Debug;


/**
 * MfiWithVoiceMaker.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050403 nsano initial version <br>
 */
class MfiWithVoiceMaker {

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

    /** {@link MachineDependentMfiWithVoiceMaker} object for model */
    protected MachineDependentMfiWithVoiceMaker mdvm;

    /** */
    private static int toReal(int base, int percent) {
        return (int) ((float) base * percent / 100);
    }

    /**
     *
     * @param sourceAis source PCM
     * @param filename output file
     */
    public MfiWithVoiceMaker(AudioInputStream sourceAis, String filename, String model, float time, int samplingRate, int bits, int channels, int masterVolume, int adpcmVolume) {
        this(model, time, samplingRate, bits, channels, masterVolume, adpcmVolume);

        this.sourceAis = sourceAis;

        this.filename = filename;
    }

    /**
     *
     * @param model output model
     * @param time time in second
     * @param bits ADPCM sampling bits
     * @param masterVolume [real number]
     * @param adpcmVolume [real number]
     * @throws IllegalArgumentException when MachineDependentMfiWithVoiceMaker not found
     */
    protected MfiWithVoiceMaker(String model, float time, int samplingRate, int bits, int channels, int masterVolume, int adpcmVolume) {
        this.time = time;

        this.samplingRate = samplingRate;
        this.bits = bits;
        this.channels = channels;
        this.masterVolume = toReal(0x7f, masterVolume);
        this.adpcmVolume = toReal(0x3f, adpcmVolume);

        this.mdvm = MachineDependentMfiWithVoiceMaker.factory.get(model);
    }

    /**
     * Creates a MFi.
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @throws InvalidMfiDataException
     * @return total size written
     */
    public int create() throws IOException, UnsupportedAudioFileException, InvalidMfiDataException {
long t = System.currentTimeMillis();
        // divide
System.err.println("1: " + (System.currentTimeMillis() - t));
t = System.currentTimeMillis();
        byte[] buffer = new byte[sourceAis.available()];
        int l = 0;
        while (l < buffer.length) {
            int r = sourceAis.read(buffer, l, buffer.length - l);
            l += r;
        }
        int result = createMFi(buffer, new File(filename));
System.err.println("2: " + (System.currentTimeMillis() - t));
t = System.currentTimeMillis();
        return result;
    }

    /**
     * Creates a MFi.
     * @param data PCM data
     * @param file output file
     * @return size written
     */
    protected int createMFi(byte[] data, File file) throws InvalidMfiDataException, IOException {
        //
        Sequence sequence = new Sequence();
        Track track = sequence.createTrack();
        MfiMessage message;

        // copyright
        message = new SorcMessage(sorc);
        track.add(new MfiEvent(message, 0L));

        // title
        String title = file.getName();
        title = title.substring(0, title.lastIndexOf('.'));
        message = new TitlMessage(title);
        track.add(new MfiEvent(message, 0L));

        // version
        message = new VersMessage(vers);
        track.add(new MfiEvent(message, 0L));

        // maker
        message = new ProtMessage(prot);
        track.add(new MfiEvent(message, 0L));

        // machine depend, do every thing!
        for (MfiEvent event: mdvm.getEvents(data, time, samplingRate, bits, channels, masterVolume, adpcmVolume)) {
            track.add(event);
        }

        // eot
        message = new EndOfTrackMessage(0, 0);
        track.add(new MfiEvent(message, 0L));

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        int r = MfiSystem.write(sequence, VaviMfiFileFormat.FILE_TYPE, file);
Debug.println(Level.FINE, "write: " + r);
        return r;
    }

    /** maker */
    protected static String prot;

    /** version */
    protected static String vers;

    /** copyright */
    protected static int sorc;

    /** */
    protected static String defaultModel;

    /* */
    static {
        try {
            Properties props = new Properties();
            props.load(MfiWithVoiceMaker.class.getResourceAsStream("/vavi/sound/sampled/mfi/MfiWithVoiceMaker.properties"));

            prot = props.getProperty("prot");
            vers = props.getProperty("vers");
            sorc = Integer.parseInt(props.getProperty("sorc"));
            defaultModel = props.getProperty("defaultModel");
        } catch (Exception e) {
            Debug.printStackTrace(e);
        }
    }

    //----

    @Options
    @HelpOption(argName = "help", option = "?", description = "print this help")
    public static class Arguments {
        @Argument(index = 0)
        File file;
        @Option(argName = "filename", option = "f", args = 1, required = true, description = "output mld filename")
        String outFilename = "out.mld";
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
     * Creates .mld w/ voice file.
     *
     * @param args input wave file
     *             -f output mld filename
     *             -m terminal model, see {@link vavi.sound.sampled.mfi.type} package
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
            MfiWithVoiceMaker mwvm = new MfiWithVoiceMaker(filterChain.doFilter(ais), arguments.outFilename, arguments.model, 0 /* TODO */, arguments.samplingRate, arguments.bits, arguments.channels, arguments.masterVolume, arguments.adpcmVolume);
            mwvm.create();

            // done
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
