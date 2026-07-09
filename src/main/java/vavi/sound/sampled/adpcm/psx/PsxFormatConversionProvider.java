/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.psx;

import java.io.IOException;
import java.io.UncheckedIOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;


/**
 * PsxFormatConversionProvider.
 * <p>
 * PSX (PS-ADPCM) to PCM only, there is no encoder.
 * The source format may carry an "interleave" property (bytes per channel per
 * interleave block set, a 0x10 multiple); when absent 0x10 is assumed.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-03 nsano initial version <br>
 */
public class PsxFormatConversionProvider extends FormatConversionProvider {

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] { PsxEncoding.PSX };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] { AudioFormat.Encoding.PCM_SIGNED };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() instanceof PsxEncoding) {
            return new AudioFormat.Encoding[] { AudioFormat.Encoding.PCM_SIGNED };
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding() instanceof PsxEncoding && targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return new AudioFormat[] {
                new AudioFormat(sourceFormat.getSampleRate(),
                                16,         // sample size in bits
                                sourceFormat.getChannels(),
                                true,       // signed
                                false)      // little endian
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
                } else if (sourceFormat.getEncoding() instanceof PsxEncoding && targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    try {
                        return new Psx2PcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED,
                                sourceFormat.getChannels(), interleaveOf(sourceFormat));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
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

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        if (isConversionSupported(targetFormat, sourceStream.getFormat())) {
            AudioFormat[] formats = getTargetFormats(targetFormat.getEncoding(), sourceStream.getFormat());
            if (formats != null && formats.length > 0) {
                AudioFormat sourceFormat = sourceStream.getFormat();
                if (sourceFormat.equals(targetFormat)) {
                    return sourceStream;
                } else if (sourceFormat.getEncoding() instanceof PsxEncoding &&
                           targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    try {
                        return new Psx2PcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED,
                                sourceFormat.getChannels(), interleaveOf(sourceFormat));
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
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

    /** the "interleave" property of the source format, 0x10 when absent */
    private static int interleaveOf(AudioFormat sourceFormat) {
        Object value = sourceFormat.getProperty("interleave");
        return value instanceof Integer ? (int) value : 0x10;
    }
}
