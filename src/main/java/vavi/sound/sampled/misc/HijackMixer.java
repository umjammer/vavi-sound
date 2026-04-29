/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.misc;

import javax.sound.sampled.Control;
import javax.sound.sampled.Control.Type;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import static vavi.sound.sampled.misc.MiscMixerProvider.version;


/**
 * HijackMixer.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-04-27 nsano initial version <br>
 */
public class HijackMixer implements Mixer {

    public static final Info mixerInfo = new Info(
            "Hijack Mixer",
            "vavi",
            "Mixer for Hijack",
            version) {};

    private final HijackSourceDataLine line = new HijackSourceDataLine();

    @Override
    public Info getMixerInfo() {
        return mixerInfo;
    }

    @Override
    public Line.Info[] getSourceLineInfo() {
        return new Line.Info[] {line.getLineInfo()};
    }

    @Override
    public Line.Info[] getTargetLineInfo() {
        return new Line.Info[0];
    }

    @Override
    public Line.Info[] getSourceLineInfo(Line.Info info) {
        return getSourceLineInfo();
    }

    @Override
    public Line.Info[] getTargetLineInfo(Line.Info info) {
        return new Line.Info[0];
    }

    @Override
    public boolean isLineSupported(Line.Info info) {
        return true;
    }

    @Override
    public Line getLine(Line.Info info) throws LineUnavailableException {
        return line;
    }

    @Override
    public int getMaxLines(Line.Info info) {
        return 1;
    }

    @Override
    public Line[] getSourceLines() {
        if (isOpen()) {
            return new Line[] {line};
        } else {
            return new Line[0];
        }
    }

    @Override
    public Line[] getTargetLines() {
        return new Line[0];
    }

    @Override
    public void synchronize(Line[] lines, boolean maintainSync) {
    }

    @Override
    public void unsynchronize(Line[] lines) {
    }

    @Override
    public boolean isSynchronizationSupported(Line[] lines, boolean maintainSync) {
        return false;
    }

    @Override
    public Line.Info getLineInfo() {
        return null;
    }

    @Override
    public void open() throws LineUnavailableException {
        line.open();
    }

    @Override
    public void close() {
        line.close();
    }

    @Override
    public boolean isOpen() {
        return line.isOpen();
    }

    @Override
    public Control[] getControls() {
        return line.getControls();
    }

    @Override
    public boolean isControlSupported(Type control) {
        return line.isControlSupported(control);
    }

    @Override
    public Control getControl(Type control) {
        return line.getControl(control);
    }

    @Override
    public void addLineListener(LineListener listener) {
    }

    @Override
    public void removeLineListener(LineListener listener) {
    }
}
