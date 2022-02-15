/*
 * Copyright (c) 2008 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.smaf;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.klab.commons.cli.Argument;
import org.klab.commons.cli.HelpOption;
import org.klab.commons.cli.Option;
import org.klab.commons.cli.Options;

import vavi.sound.sampled.FilterChain;
import vavi.sound.sampled.WaveDivider;
import vavi.sound.smaf.InvalidSmafDataException;
import vavi.util.Debug;


/**
 * DividedSmafWithVoiceMaker.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 080415 nsano initial version <br>
 */
class DividedSmafWithVoiceMaker extends SmafWithVoiceMaker {

    /** source PCM */
    private AudioInputStream sourceAis;

    /** output base directory */
    private String directory;
    /** output file template (use {@link String#format(String, Object...)}) */
    private String base;

    /**
     *
     * @param sourceAis source PCM
     * @param directory output base directory, directory "mmf" will be added
     * @param base output file template
     * @param time dividing time in second
     * @param samplingRate ADPCM sampling rate [Hz]
     * @param bits ADPCM sampling bits
     * @param channels
     * @param masterVolume [%]
     * @param adpcmVolume [%]
     */
    public DividedSmafWithVoiceMaker(AudioInputStream sourceAis, String directory, String base, float time, int samplingRate, int bits, int channels, int masterVolume, int adpcmVolume) {
        super(time, samplingRate, bits, channels, masterVolume, adpcmVolume);

        this.sourceAis = sourceAis;

        this.directory = directory + File.separator + "mmf"; // TODO mmf に依存は良くない
        this.base = base;
    }

    /** */
    private class Event implements WaveDivider.Event {
        /** total size written */
        int r = 0;
        public void exec(WaveDivider.Chunk chunk) throws IOException {
            try {
                File file = new File(directory, String.format(base, chunk.sequence + 1));
Debug.println(Level.FINE, "file: " + file + ", " + directory + ", " + base + ", " + (chunk.sequence + 1));
                r += createSMAF(chunk.buffer, file);
            } catch (InvalidSmafDataException e) {
                throw (IOException) new IOException().initCause(e);
            }
        }
    }

    /**
     *
     * @throws IOException
     * @throws UnsupportedAudioFileException
     * @throws InvalidSmafDataException
     * @return total size written
     */
    public int create() throws IOException, UnsupportedAudioFileException, InvalidSmafDataException {
long t = System.currentTimeMillis();
        // divide
        Event event = new Event();
        WaveDivider waveDivider = WaveDivider.Factory.getWaveDivider(sourceAis);
Debug.println("1: " + (System.currentTimeMillis() - t));
t = System.currentTimeMillis();
        waveDivider.divide(time, event);
Debug.println("2: " + (System.currentTimeMillis() - t));
t = System.currentTimeMillis();
        return event.r;
    }

    //----

    @Options
    @HelpOption(argName = "help", option = "?", description = "print this help")
    public static class Arguments {
        @Argument(index = 0)
        File file;
        @Option(argName = "output", option = "o", args = 1, required = false, description = "output mmf filename base (use java.lang.String#format)")
        String directory = ".";
        @Option(argName = "template", option = "t", required = false, args = 1, description = "output base directory")
        String base = "%s/out_%d.mmf";
        @Option(argName = "model", option = "m", required = false, args = 1, description = "terminal model")
        String model = defaultModel;
        @Option(argName = "size", option = "s", required = false, args = 1, description = "chunk time in [sec]")
        float time = 10;
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
     *             -o output base directory
     *             -t output mmf filename base (use java.lang.String#format)
     *             -s chunk time [second]
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
            DividedSmafWithVoiceMaker swvm = new DividedSmafWithVoiceMaker(filterChain.doFilter(ais), arguments.directory, arguments.base, arguments.time, arguments.samplingRate, arguments.bits, arguments.channels, arguments.masterVolume, arguments.adpcmVolume);
            swvm.create();

            // done
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}

/* */
