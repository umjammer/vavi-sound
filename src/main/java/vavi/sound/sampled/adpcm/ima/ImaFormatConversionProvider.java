/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.ima;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;


/**
 * ImaFormatConversionProvider.
 *
 * TODO read from wav
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class ImaFormatConversionProvider extends FormatConversionProvider {

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] { ImaEncoding.IMA, AudioFormat.Encoding.PCM_SIGNED };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] { ImaEncoding.IMA, AudioFormat.Encoding.PCM_SIGNED };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return new AudioFormat.Encoding[] { ImaEncoding.IMA };
        } else if (sourceFormat.getEncoding() instanceof ImaEncoding) {
            return new AudioFormat.Encoding[] { AudioFormat.Encoding.PCM_SIGNED };
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
            targetEncoding instanceof ImaEncoding) {
            if (sourceFormat.getChannels() > 2 ||
                sourceFormat.getChannels() <= 0 ||
                sourceFormat.isBigEndian()) {
                return new AudioFormat[0];
            } else {
                return new AudioFormat[] {
                    new AudioFormat(targetEncoding,
                                    sourceFormat.getSampleRate(),
                                    -1,     // sample size in bits
                                    sourceFormat.getChannels(),
                                    -1,     // frame size
                                    -1,     // frame rate
                                    false)  // little endian
                };
            }
        } else if (sourceFormat.getEncoding() instanceof ImaEncoding && targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return new AudioFormat[] {
                new AudioFormat(sourceFormat.getSampleRate(),
                                16,         // sample size in bits
                                sourceFormat.getChannels(),
                                true,       // signed
                                false)      // little endian (for PCM wav)
            };
        } else {
            return new AudioFormat[0];
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {
        if (isConversionSupported(targetEncoding, sourceStream.getFormat())) {
            AudioFormat[] formats = getTargetFormats(targetEncoding, sourceStream.getFormat());
            if (formats != null && formats.length > 0) {
                AudioFormat sourceFormat = sourceStream.getFormat();
                AudioFormat targetFormat = formats[0];
                if (sourceFormat.equals(targetFormat)) {
                    return sourceStream;
                } else if (sourceFormat.getEncoding() instanceof ImaEncoding && targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    try {
                        int samplesPerBlock = (int) sourceFormat.getProperty("samplesPerBlock");
                        int blockSize = (int) sourceFormat.getProperty("blockSize");
                        return new Ima2PcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED, samplesPerBlock, blockSize);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof ImaEncoding) {
                    throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
                } else {
                    throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat.toString());
                }
            } else {
                throw new IllegalArgumentException("target format not found");
            }
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        if (isConversionSupported(targetFormat, sourceStream.getFormat())) {
            AudioFormat[] formats = getTargetFormats(targetFormat.getEncoding(), sourceStream.getFormat());
            if (formats != null && formats.length > 0) {
                AudioFormat sourceFormat = sourceStream.getFormat();
                if (sourceFormat.equals(targetFormat)) {
                    return sourceStream;
                } else if (sourceFormat.getEncoding() instanceof ImaEncoding &&
                           targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    try {
                        int samplesPerBlock = (int) sourceFormat.getProperty("samplesPerBlock");
                        int blockSize = (int) sourceFormat.getProperty("blockSize");
                        return new Ima2PcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED, samplesPerBlock, blockSize);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof ImaEncoding) {
                    throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
                } else {
                    throw new IllegalArgumentException("unable to convert " + sourceFormat + " to " + targetFormat);
                }
            } else {
                throw new IllegalArgumentException("target format not found");
            }
        } else {
            throw new IllegalArgumentException("conversion not supported");
        }
    }
}
