/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound;

import java.io.BufferedInputStream;
import java.io.FilterInputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.NoSuchElementException;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.Line.Info;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.getLogger;


/**
 * SoundUtil.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2022/02/13 umjammer initial version <br>
 */
public final class SoundUtil {

    private static final Logger logger = getLogger(SoundUtil.class.getName());

    private SoundUtil() {}

    /**
     * @param gain number between 0 and 1 (loudest)
     * @before {@link DataLine#open()}
     */
    public static void volume(DataLine line, double gain) {
        try {
            FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (float) (Math.log10(gain) * 20.0);
            gainControl.setValue(dB);
        } catch (IllegalArgumentException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
        }
    }

    /**
     * @throws NoSuchElementException if {@code clazz} not found
     */
    @SuppressWarnings("unchecked")
    public static <T extends DataLine> T getLine(String name, Class<T> clazz) {
        Mixer.Info[] mixersInfo = AudioSystem.getMixerInfo();
        for (Mixer.Info mixerInfo : mixersInfo) {
            if (("#" + mixerInfo.getName()).equals(name)) {
                Mixer mixer = AudioSystem.getMixer(mixerInfo);
                if (clazz == SourceDataLine.class) {
                    Line.Info[] sourceLineInfo = mixer.getSourceLineInfo();
logger.log(Level.TRACE, sourceLineInfo.length + ", " + Arrays.stream(sourceLineInfo).map(Info::getLineClass).toList());
                    for (Line.Info info : sourceLineInfo) {
                        try {
                            T t = (T) mixer.getLine(info);
logger.log(DEBUG, t.getClass().getName() + ", " + mixerInfo.getName());
                            return t;
                        } catch (LineUnavailableException e) {
                            logger.log(Level.TRACE, e.toString());
                        }
                    }
                } else if (clazz == TargetDataLine.class) {
                    Line.Info[] targetLineInfo = mixer.getTargetLineInfo();
logger.log(Level.TRACE, targetLineInfo.length + ", " + Arrays.stream(targetLineInfo).map(Info::getLineClass).toList());
                    for (Line.Info info : targetLineInfo) {
                        try {
                            T t = (T) mixer.getLine(info);
logger.log(DEBUG, t.getClass().getName() + ", " + mixerInfo.getName());
                            return t;
                        } catch (LineUnavailableException e) {
                            logger.log(Level.TRACE, e.toString());
                        }
                    }
                } else {
                    throw new IllegalArgumentException("unsupported class: " + clazz.getName());
                }
            }
        }
        throw new NoSuchElementException(name);
    }

    /**
     * Gets a source path from an input stream.
     * <p>
     * java runtime option
     * <ol>
     *  <ul>{@code --add-opens=java.base/java.io=ALL-UNNAMED}</ul>
     *  <ul>{@code --add-opens=java.base/sun.nio.ch=ALL-UNNAMED}</ul>
     * </ol>
     *
     * @param object input stream
     * @return source object as URI, nullable
     */
    public static URI getSource(Object object) {
        Class<?> c = object.getClass();
        try {
            do {
logger.log(DEBUG, "object1: " + c.getName());
                if (object instanceof BufferedInputStream) {
                    Field pathField = FilterInputStream.class.getDeclaredField("in");
                    pathField.setAccessible(true);
                    object = pathField.get(object);
                }
                if (object instanceof java.io.FileInputStream) {
                    Field pathField = object.getClass().getDeclaredField("path");
                    pathField.setAccessible(true);
                    String path = (String) pathField.get(object);
logger.log(DEBUG, "source: java.io.FileInputStream: path : " + path);
                    return path != null ? Path.of(path).toRealPath().toUri() : null;
                }
                if (object.getClass().getName().equals("sun.nio.ch.ChannelInputStream")) { // because it's package private
                    Field pathField = object.getClass().getDeclaredField("ch");
                    pathField.setAccessible(true);
                    object = pathField.get(object);
                }
                if (object.getClass().getName().equals("sun.nio.ch.FileChannelImpl")) { // because it's package private
                    Field pathField = object.getClass().getDeclaredField("path");
                    pathField.setAccessible(true);
                    String path = (String) pathField.get(object);
logger.log(DEBUG, "source: sun.nio.ch.FileChannelImpl: path : " + path);
                    return path != null ? Path.of(path).toRealPath().toUri() : null;
                }
logger.log(DEBUG, "object2: " + object.getClass().getName());
                c = c.getSuperclass();
            } while (c.getSuperclass() != null);
        } catch (Exception e) {
logger.log(Level.WARNING, e.getMessage(), e);
        }
        return null;
    }
}
