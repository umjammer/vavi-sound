/*
 * Copyright (c) 2005 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled;

import java.io.EOFException;
import java.io.IOException;
import java.util.logging.Level;

import javax.sound.sampled.AudioInputStream;

import vavi.util.Debug;


/**
 * Pcm16BitMonauralWaveDivider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 050401 nsano initial version <br>
 */
class Pcm16BitMonauralWaveDivider implements WaveDivider {

    /** PCM_SIGNED 8k,16k Hz, 16 bit, mono, 2 bytes/frame, little-endian */
    protected AudioInputStream targetAis;

    /**
     * @param sourceAis PCM_SIGNED any Hz, 16 bit, mono, 2 bytes/frame, little-endian
     */
    Pcm16BitMonauralWaveDivider(AudioInputStream sourceAis) {
        if (sourceAis.getFormat().getChannels() == 1 &&
            sourceAis.getFormat().getSampleSizeInBits() == 16) {
            this.targetAis = sourceAis;
        } else {
            throw new IllegalArgumentException("unsupported format: " + sourceAis.getFormat());
        }
    }

    /**
     * Divides PCM data in WAVE file.
     * only monaural 16 bit PCM is supported.
     * @param seconds time for divide
     * @param event event for each chunks
     */
    @Override
    public void divide(float seconds, Event event) throws IOException {

        int numberChannels = targetAis.getFormat().getChannels();
Debug.println(Level.FINE, "numberChannels: " + numberChannels);

        int samplingRate = (int) targetAis.getFormat().getSampleRate();
Debug.println(Level.FINE, "samplingRate: " + samplingRate);
        int samplingBytes = targetAis.getFormat().getSampleSizeInBits() / 8;
Debug.println(Level.FINE, "samplingBytes: " + samplingBytes);
        int bytesPerSecond = samplingRate * samplingBytes;
Debug.println(Level.FINE, "bytesPerSecond: " + bytesPerSecond);
Debug.println(Level.FINE, "header.bytesPerSecond: " + targetAis.getFormat().getFrameSize() * targetAis.getFormat().getFrameRate());
        long totalTime = (long) (targetAis.available() / (targetAis.getFormat().getFrameSize() * targetAis.getFormat().getFrameRate()) * 1000);
Debug.println(Level.FINE, "totalTime= " + (totalTime / (60 * 1000)) + ":" + ((totalTime % (60 * 1000)) / 1000) + "." + ((totalTime % (60 * 1000)) % 1000));

        // all channel, per second
        int blockSize = samplingRate * numberChannels * samplingBytes;
Debug.println(Level.FINE, "blockSize: " + blockSize);

        int numberOfChunks = targetAis.available() / (int) (blockSize * seconds);
        int moduloOfChunks = targetAis.available() % (int) (blockSize * seconds);
Debug.println(Level.FINE, "numberOfChunks: " + numberOfChunks + ", moduloOfChunks: " + moduloOfChunks);

        for (int i = 0; i < numberOfChunks; i++) {
            byte[] buffer = new byte[(int) (blockSize * seconds)];
            int l = 0;
            while (l < buffer.length) {
                int r = targetAis.read(buffer, l, buffer.length - l);
                if (r == -1) {
                    throw new EOFException("illegal eof");
                }
                l += r;
            }
Debug.println(Level.FINE, "CHUNK[" + i + "] " + buffer.length + " bytes");
            event.exec(new Chunk(i, buffer, samplingRate, samplingBytes * 8, numberChannels));
        }
        if (moduloOfChunks >= 0) {
            byte[] buffer = new byte[moduloOfChunks];
            int l = 0;
            while (l < buffer.length) {
                int r = targetAis.read(buffer, l, buffer.length - l);
                if (r == -1) {
                    throw new EOFException("illegal eof");
                }
                l += r;
            }
            event.exec(new Chunk(numberOfChunks, buffer, samplingRate, samplingBytes * 8, numberChannels));
Debug.println(Level.FINE, "modulo bytes: " + buffer.length + ", " + (((totalTime / 1000) % seconds) + 1) + " [s]");
        }
    }
}
