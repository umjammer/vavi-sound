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
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;

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
        FloatControl gainControl = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
        float dB = (float) (Math.log10(gain) * 20.0);
        gainControl.setValue(dB);
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
                    return path != null ? URI.create(path) : null;
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
                    return path != null ? URI.create(path) : null;
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
