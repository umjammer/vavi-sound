/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.sampled.FilterChain;
import vavi.sound.sampled.WaveDivider;
import vavi.util.Debug;


/**
 * DividedMfiWithVoiceMaker.
 * 
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 050403 nsano initial version <br>
 */
class DividedMfiWithVoiceMaker extends MfiWithVoiceMaker {

    /** source PCM */
    private AudioInputStream sourceAis;

    /** output base directory */
    private String directory;
    /** output file template (use {@link String#format(String, Object...)}) */
    private String base;

    /**
     * 
     * @param sourceAis source PCM
     * @param directory output base directory
     * @param base output file template
     * @param model output model
     * @param time dividing time in second
     * @param samplingRate ADPCM sampling rate
     * @param bits ADPCM sampling bits
     */
    public DividedMfiWithVoiceMaker(AudioInputStream sourceAis, String directory, String base, String model, float time, int samplingRate, int bits, int channels, int masterVolume, int adpcmVolume) {
        super(model, time, samplingRate, bits, channels, masterVolume, adpcmVolume);

        this.sourceAis = sourceAis;

        this.directory = directory + File.separator + model; // TODO model ‚ÉˆË‘¶‚Í—Ç‚­‚È‚¢
        this.base = base;
    }

    /** */
    private class Event implements WaveDivider.Event {
        /** total size written */
        int r = 0;
        public void exec(WaveDivider.Chunk chunk) throws IOException {
            try {
                File file = new File(directory, String.format(base, chunk.sequence + 1));
                r += createMFi(chunk.buffer, file);
            } catch (InvalidMfiDataException e) {
                throw (IOException) new IOException().initCause(e);
            }
        }
    }

    /**
     * 
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @throws InvalidMfiDataException
     * @return total size written
     */
    public int create() throws IOException, UnsupportedAudioFileException, InvalidMfiDataException {
long t = System.currentTimeMillis();
        // divide
        Event event = new Event();
        WaveDivider waveDivider = WaveDivider.Factory.getWaveDivider(sourceAis);
System.err.println("1: " + (System.currentTimeMillis() - t));
t = System.currentTimeMillis();
        waveDivider.divide(time, event);
System.err.println("2: " + (System.currentTimeMillis() - t));
t = System.currentTimeMillis();
        return event.r;
    }

    //----

    /**
     * Creates .mld w/ voice file.
     * 
     * @param args input wave file
     *             -o output base directory
     *             -t output mld filename base (use java.lang.String#format)
     *             -m terminal model, see {@link vavi.sound.sampled.mfi.type} package
     *             -s chunk time [second]
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
            options.addOption(OptionBuilder.withArgName("output")
                              .hasArg()
                              .withDescription("output directory")
                              .create("o"));
            options.addOption(OptionBuilder.withArgName("template")
                              .hasArg()
                              .withDescription("filename template")
                              .create("t"));
            options.addOption(OptionBuilder.withArgName("model")
                              .hasArg()
                              .withDescription("terminal model")
                              .create("m"));
            options.addOption(OptionBuilder.withArgName("size")
                              .hasArg()
                              .withDescription("size (second)")
                              .create("s"));
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

            String file = cl.getArgs()[0];
            String directory = ".";
            String base = "out_%d.mld";
            String model = defaultModel;
            float time = 10;
            int samplingRate = 16000;
            int bits = 4;
            int channels = 1;
            int masterVolume = 0x7f;
            int adpcmVolume = 0x3f;

            if (cl.hasOption("o")) {
                directory = cl.getOptionValue("o");
Debug.println("directory: " + directory);
            }
            if (cl.hasOption("t")) {
                base = cl.getOptionValue("t");
Debug.println("template: " + base);
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
Debug.println("stereo out");
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

            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(file));
            FilterChain filterChain = new FilterChain();
            DividedMfiWithVoiceMaker mwvm = new DividedMfiWithVoiceMaker(filterChain.doFilter(ais), directory, base, model, time, samplingRate, bits, channels, masterVolume, adpcmVolume);
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
