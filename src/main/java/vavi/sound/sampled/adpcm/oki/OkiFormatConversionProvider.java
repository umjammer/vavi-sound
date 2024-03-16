/*
 * Copyright (c) 2020 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.oki;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;


/**
 * OkiFormatConversionProvider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 201020 nsano initial version <br>
 */
public class OkiFormatConversionProvider extends FormatConversionProvider {

    @Override
    public AudioFormat.Encoding[] getSourceEncodings() {
        return new AudioFormat.Encoding[] { OkiEncoding.OKI, AudioFormat.Encoding.PCM_SIGNED };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings() {
        return new AudioFormat.Encoding[] { OkiEncoding.OKI, AudioFormat.Encoding.PCM_SIGNED };
    }

    @Override
    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
            return new AudioFormat.Encoding[] { OkiEncoding.OKI };
        } else if (sourceFormat.getEncoding() instanceof OkiEncoding) {
            return new AudioFormat.Encoding[] { AudioFormat.Encoding.PCM_SIGNED };
        } else {
            return new AudioFormat.Encoding[0];
        }
    }

    @Override
    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) &&
            targetEncoding instanceof OkiEncoding) {
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
        } else if (sourceFormat.getEncoding() instanceof OkiEncoding && targetEncoding.equals(AudioFormat.Encoding.PCM_SIGNED)) {
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
                } else if (sourceFormat.getEncoding() instanceof OkiEncoding && targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    return new Oki2PcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED);
                } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof OkiEncoding) {
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
                } else if (sourceFormat.getEncoding() instanceof OkiEncoding &&
                           targetFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {
                    return new Oki2PcmAudioInputStream(sourceStream, targetFormat, AudioSystem.NOT_SPECIFIED);
                } else if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED) && targetFormat.getEncoding() instanceof OkiEncoding) {
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
