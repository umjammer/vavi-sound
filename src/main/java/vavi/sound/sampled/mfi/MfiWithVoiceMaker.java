/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

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
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
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

    /** MachineDependVoiceMaker object for model */
    protected MachineDependMfiWithVoiceMaker mdvm;

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
     */
    protected MfiWithVoiceMaker(String model, float time, int samplingRate, int bits, int channels, int masterVolume, int adpcmVolume) {
        this.time = time;

        this.samplingRate = samplingRate;
        this.bits = bits;
        this.channels = channels;
        this.masterVolume = masterVolume;
        this.adpcmVolume = adpcmVolume;

        this.mdvm = MachineDependMfiWithVoiceMakerFactory.getMachineDependMfiWithVoiceMaker(model);
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
        track.add(new MfiEvent(message, 0l));

        // title
        String title = file.getName();
        title = title.substring(0, title.lastIndexOf('.'));
        message = new TitlMessage(title);
        track.add(new MfiEvent(message, 0l));

        // version
        message = new VersMessage(vers);
        track.add(new MfiEvent(message, 0l));

        // maker
        message = new ProtMessage(prot);
        track.add(new MfiEvent(message, 0l));

        // machine depend, do every thing!
        for (MfiEvent event: mdvm.getEvents(data, time, samplingRate, bits, channels, masterVolume, adpcmVolume)) {
            track.add(event);
        }

        // eot
        message = new EndOfTrackMessage(0, 0);
        track.add(new MfiEvent(message, 0l));

        int r = MfiSystem.write(sequence, VaviMfiFileFormat.FILE_TYPE, file);
Debug.println("write: " + r);
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

    /** */
    static {
        try {
            Properties props = new Properties();
            props.load(MfiWithVoiceMaker.class.getResourceAsStream("MfiWithVoiceMaker.properties"));

            prot = props.getProperty("prot");
            vers = props.getProperty("vers");
            sorc = Integer.parseInt(props.getProperty("sorc"));
            defaultModel = props.getProperty("defaultModel");
        } catch (Exception e) {
            Debug.printStackTrace(e);
        }
    }

    //----

    /**
     * Creates .mld w/ voice file.
     * 
     * @param args input wave file
     *             -f output mld filename
     *             -s chunk time [second]
     *             -m terminal model, see {@link vavi.sound.sampled.mfi.type} package
     *             -r adpcm sampling rate
     *             -b adpcm sampling bits
     *             -c adpcm channels
     *             -v master volume
     *             -a adpcm volume
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        try {
            Options options = new Options();
            options.addOption(OptionBuilder.withArgName("filename")
                              .hasArg()
                              .withDescription("output mld filename")
                              .create("f"));
            options.addOption(OptionBuilder.withArgName("size")
                              .hasArg()
                              .withDescription("size (second)")
                              .create("s"));
            options.addOption(OptionBuilder.withArgName("model")
                              .hasArg()
                              .withDescription("terminal model")
                              .create("m"));
            options.addOption(OptionBuilder.withArgName("rate")
                              .hasArg()
                              .withDescription("sampling rate")
                              .create("r"));
            options.addOption(OptionBuilder.withArgName("bits")
                              .hasArg()
                              .withDescription("adpcm bits")
                              .create("b"));
            options.addOption(OptionBuilder.withArgName("channels")
                              .hasArg()
                              .withDescription("adpcm channels")
                              .create("c"));
            options.addOption(OptionBuilder.withArgName("masterVoluem")
                              .hasArg()
                              .withDescription("master volume in %")
                              .create("v"));
            options.addOption(OptionBuilder.withArgName("adpcmVoluem")
                              .hasArg()
                              .withDescription("adpcm volume in %")
                              .create("a"));

            CommandLineParser parser = new BasicParser();
            CommandLine cl = parser.parse(options, args);

            String inFilename = cl.getArgs()[0];
            String outFilename = "out.mld";
            String model = defaultModel;
            float time = 10;
            int samplingRate = 16000;
            int bits = 4;
            int channels = 1;
            int masterVolume = 0x7f;
            int adpcmVolume = 0x3f;

            if (cl.hasOption("f")) {
                outFilename = cl.getOptionValue("f");
Debug.println("filename: " + outFilename);
            }
            if (cl.hasOption("m")) {
                model = cl.getOptionValue("m");
Debug.println("model: " + model);
            }
            if (cl.hasOption("s")) {
                time = Float.parseFloat(cl.getOptionValue("s"));
Debug.println("size: " + time);
            }
            if (cl.hasOption("r")) {
                samplingRate = Integer.parseInt(cl.getOptionValue("r"));
Debug.println("rate: " + samplingRate);
            }
            if (cl.hasOption("b")) {
                bits = Integer.parseInt(cl.getOptionValue("b"));
Debug.println("bits: " + bits);
            }
            if (cl.hasOption("c")) {
                channels = Integer.parseInt(cl.getOptionValue("c"));
Debug.println("channels: " + channels);
            }
            if (cl.hasOption("v")) {
                masterVolume = (int) ((float) masterVolume * Integer.parseInt(cl.getOptionValue("v")) / 100);
Debug.println("masterVolume: " + masterVolume + ", " + cl.getOptionValue("v") + "%");
            }
            if (cl.hasOption("a")) {
                adpcmVolume = (int) ((float) adpcmVolume * Integer.parseInt(cl.getOptionValue("a")) / 100);
Debug.println("adpcmVolume: " + adpcmVolume + ", " + cl.getOptionValue("a") + "%");
            }
    
            // create

            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(inFilename));
            FilterChain filterChain = new FilterChain();
            MfiWithVoiceMaker mwvm = new MfiWithVoiceMaker(filterChain.doFilter(ais), outFilename, model, time, samplingRate, bits, channels, masterVolume, adpcmVolume);
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
