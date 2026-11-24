/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.util;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import static java.lang.System.getLogger;


/**
 * Fast convolution by a fixed filter, the fft, multiplying the spectrum, then the inverse fft.
 * <p>
 * the result is the one of the Ooura's {@link SplitRadixFft#rdft} way,
 * {@code rdft(x), x *= rdft(h), rdft^-1(x)}, so it is n / 2 times the circular convolution.
 * an instance keeps a filter, it is not thread safe.
 * </p>
 * <p>
 * the implementation is selected by the system property {@value #PROPERTY},
 * the value is a short name ("scalar", "vector") or a class name of
 * an implementation registered in {@code META-INF/services/vavi.util.Convolver}.
 * the default is "scalar", it is bit exact to the rdft way with the {@link Fft} selected.
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-23 nsano initial version <br>
 */
public interface Convolver {

    /** the system property name to select an implementation */
    String PROPERTY = "vavi.util.Convolver";

    /**
     * sets the filter.
     *
     * @param n the length, a power of 2
     * @param h the filter in the time domain, n data are used, not modified
     */
    void init(int n, double[] h);

    /**
     * convolves in place.
     *
     * @param x channels, n data of each x[0 ... nch - 1] are replaced by n / 2 times the circular convolution with the filter
     * @param nch the number of channels
     */
    void convolve(double[][] x, int nch);

    /**
     * @return true when the result is bit exact to the rdft way by {@link SplitRadixFft}.
     *         when false, a caller like ssrc may change the fft length for efficiency, as the result differs anyway
     */
    default boolean isExact() {
        return true;
    }

    /** @return a new instance selected by {@value #PROPERTY}, fall back to the scalar one */
    static Convolver getInstance() {
        return getInstance(System.getProperty(PROPERTY, "scalar"));
    }

    /**
     * @param name a short name (the simple class name without "Convolver", ignoring case) or a class name
     * @return a new instance, the scalar one when not found or not available (e.g. no jdk.incubator.vector)
     */
    static Convolver getInstance(String name) {
        Logger logger = getLogger(Convolver.class.getName());
        try {
            for (ServiceLoader.Provider<Convolver> provider : ServiceLoader.load(Convolver.class).stream().toList()) {
                Class<? extends Convolver> type = provider.type();
                String shortName = type.getSimpleName().replaceFirst("Convolver$", "");
                if (type.getName().equals(name) || shortName.equalsIgnoreCase(name)) {
                    Convolver convolver = provider.get();
                    logger.log(Level.DEBUG, "convolver: " + type.getName());
                    return convolver;
                }
            }
            logger.log(Level.WARNING, "convolver: no such implementation: " + name + ", use scalar");
        } catch (ServiceConfigurationError | LinkageError e) {
            logger.log(Level.WARNING, "convolver: " + name + " is not available, use scalar: " + e);
        }
        return new ScalarConvolver();
    }
}
