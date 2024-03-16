/*
 * Copyright (c) 2006 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.mfi;

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

import vavi.sound.mfi.InvalidMfiDataException;
import vavi.sound.sampled.FilterChain;
import vavi.sound.sampled.WaveDivider;
import vavi.util.Debug;


/**
 * DividedMfiWithVoiceMaker.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050403 nsano initial version <br>
 */
class DividedMfiWithVoiceMaker extends MfiWithVoiceMaker {

    /** source PCM */
    private AudioInputStream sourceAis;

    /** output base directory */
    private String directory;
    /** output file template (use {@link String#format(String, Object...)}) */
    private String base;

    /** */
    private static int toReal(int base, int percent) {
        return (int) ((float) base * percent / 100);
    }

    /**
     *
     * @param sourceAis source PCM
     * @param directory output base directory
     * @param base output file template
     * @param model output model
     * @param time dividing time in second
     * @param samplingRate ADPCM sampling rate
     * @param bits ADPCM sampling bits
     * @param channels
     * @param masterVolume [%]
     * @param adpcmVolume [%]
     */
    public DividedMfiWithVoiceMaker(AudioInputStream sourceAis, String directory, String base, String model, float time, int samplingRate, int bits, int channels, int masterVolume, int adpcmVolume) {
        super(model, time, samplingRate, bits, channels, toReal(0x7f, masterVolume), toReal(0x3f, adpcmVolume));

        this.sourceAis = sourceAis;

        this.directory = directory + File.separator + model; // TODO dependence on model is not good
        this.base = base;
    }

    /** */
    private class Event implements WaveDivider.Event {
        /** total size written */
        int r = 0;
        @Override
        public void exec(WaveDivider.Chunk chunk) throws IOException {
            try {
                File file = new File(directory, String.format(base, chunk.sequence + 1));
                r += createMFi(chunk.buffer, file);
            } catch (InvalidMfiDataException e) {
                throw (IOException) new IOException(e);
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
    @Override
    public int create() throws IOException, UnsupportedAudioFileException, InvalidMfiDataException {
long t = System.currentTimeMillis();
        // divide
        Event event = new Event();
        WaveDivider waveDivider = WaveDivider.Factory.getWaveDivider(sourceAis);
Debug.println(Level.FINE, "1: " + (System.currentTimeMillis() - t));
t = System.currentTimeMillis();
        waveDivider.divide(time, event);
Debug.println(Level.FINE, "2: " + (System.currentTimeMillis() - t));
t = System.currentTimeMillis();
        return event.r;
    }

    //----

    @Options
    @HelpOption(argName = "help", option = "?", description = "print this help")
    public static class Arguments {
        @Argument(index = 0)
        File file;
        @Option(argName = "output", option = "o", args = 1, required = false, description = "output mld filename base (use java.lang.String#format)")
        String directory = ".";
        @Option(argName = "template", option = "t", required = false, args = 1, description = "output base directory")
        String base = "%s/out_%d.mld";
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
     * Creates .mld w/ voice file.
     *
     * @param args input wave file
     *             -o output base directory
     *             -t output mld filename base (use java.lang.String#format)
     *             -m terminal model, see {@link vavi.sound.sampled.mfi.type} package
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
            DividedMfiWithVoiceMaker mwvm = new DividedMfiWithVoiceMaker(filterChain.doFilter(ais), arguments.directory, arguments.base, arguments.model, arguments.time, arguments.samplingRate, arguments.bits, arguments.channels, arguments.masterVolume, arguments.adpcmVolume);
            mwvm.create();

            // done
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
