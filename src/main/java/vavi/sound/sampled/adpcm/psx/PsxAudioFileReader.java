/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.adpcm.psx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.spi.AudioFileReader;

import vavi.sound.SoundUtil;
import vavi.sound.adpcm.psx.PsHeaderless;
import vavi.sound.adpcm.psx.Psx.VgmStream;


/**
 * Provider for headerless PSX (PS-ADPCM) audio file reading services.
 * <p>
 * PS-ADPCM streams have no header, so detection is done by file extension
 * ({@code .mib}: 44100 Hz, {@code .mi4}: 48000 Hz) plus a content check, and
 * channels/interleave are guessed by scanning the data (see {@link PsHeaderless}).
 * The {@link InputStream} overloads therefore only work for file backed streams
 * whose path can be recovered via {@link SoundUtil#getSource}, which needs the
 * java runtime options
 * <ol>
 *  <ul>{@code --add-opens=java.base/java.io=ALL-UNNAMED}</ul>
 *  <ul>{@code --add-opens=java.base/sun.nio.ch=ALL-UNNAMED}</ul>
 * </ol>
 * <p>
 * The resulting {@link AudioFormat} carries the properties {@code interleave}
 * (bytes per channel per interleave block set) used by
 * {@link PsxFormatConversionProvider} and {@code numSamples} (samples per channel).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-03 nsano initial version <br>
 */
public class PsxAudioFileReader extends AudioFileReader {

    @Override
    public AudioFileFormat getAudioFileFormat(File file) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormat(file.toPath());
    }

    @Override
    public AudioFileFormat getAudioFileFormat(URL url) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormat(toPath(url));
    }

    @Override
    public AudioFileFormat getAudioFileFormat(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return getAudioFileFormat(toPath(stream));
    }

    @Override
    public AudioInputStream getAudioInputStream(File file) throws UnsupportedAudioFileException, IOException {
        Path path = file.toPath();
        AudioFileFormat fileFormat = getAudioFileFormat(path);
        InputStream stream = Files.newInputStream(path);
        return new AudioInputStream(stream, fileFormat.getFormat(), AudioSystem.NOT_SPECIFIED);
    }

    @Override
    public AudioInputStream getAudioInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        return getAudioInputStream(toPath(url).toFile());
    }

    @Override
    public AudioInputStream getAudioInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        // the analysis reads from its own channel, the given stream is untouched (still at position 0)
        AudioFileFormat fileFormat = getAudioFileFormat(toPath(stream));
        return new AudioInputStream(stream, fileFormat.getFormat(), AudioSystem.NOT_SPECIFIED);
    }

    /** recovers the backing file of a stream reflectively */
    private static Path toPath(InputStream stream) throws UnsupportedAudioFileException {
        URI uri = SoundUtil.getSource(stream);
        if (uri == null) {
            throw new UnsupportedAudioFileException("not a file backed stream, cannot detect headerless psx adpcm");
        }
        return Path.of(uri);
    }

    /** */
    private static Path toPath(URL url) throws UnsupportedAudioFileException {
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException | IllegalArgumentException | FileSystemNotFoundException e) {
            throw (UnsupportedAudioFileException) new UnsupportedAudioFileException("not a local file: " + url).initCause(e);
        }
    }

    /** detection and analysis are delegated to {@link PsHeaderless} */
    private static AudioFileFormat getAudioFileFormat(Path path) throws UnsupportedAudioFileException, IOException {
        VgmStream vgmstream;
        try {
            vgmstream = PsHeaderless.initVgmstreamPsHeaderless(path);
        } catch (IllegalArgumentException e) {
            throw (UnsupportedAudioFileException) new UnsupportedAudioFileException(path.toString()).initCause(e);
        }
        try {
            vgmstream.ch[0].streamFile.close(); // all channels share one underlying channel
        } catch (IOException ignored) {
        }

        Map<String, Object> properties = new HashMap<>();
        properties.put("interleave", vgmstream.interleaveBlockSize);
        properties.put("numSamples", vgmstream.numSamples);

        AudioFormat format = new AudioFormat(PsxEncoding.PSX,
                vgmstream.sampleRate,
                AudioSystem.NOT_SPECIFIED,
                vgmstream.channels,
                AudioSystem.NOT_SPECIFIED,
                AudioSystem.NOT_SPECIFIED,
                false,
                properties);
        return new AudioFileFormat(PsxFileFormatType.PSX, format, AudioSystem.NOT_SPECIFIED);
    }
}
