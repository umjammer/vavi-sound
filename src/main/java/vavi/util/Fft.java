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
 * Fast Fourier Transform, the Ooura fft package interface.
 * <p>
 * the work areas {@code ip} and {@code w} are compatible with
 * {@link SplitRadixFft}, every implementation must give the same result.
 * </p>
 * <p>
 * the implementation is selected by the system property {@value #PROPERTY},
 * the value is a short name ("scalar", "vector") or a class name of
 * an implementation registered in {@code META-INF/services/vavi.util.Fft}.
 * the default is "scalar".
 * </p>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-11-08 nsano initial version <br>
 */
public interface Fft {

    /** the system property name to select an implementation */
    String PROPERTY = "vavi.util.Fft";

    /**
     * Real Discrete Fourier Transform.
     *
     * @see SplitRadixFft#rdft(int, int, double[], int[], double[])
     */
    void rdft(int n, int isgn, double[] a, int[] ip, double[] w);

    /** @return the implementation selected by {@value #PROPERTY}, fall back to the scalar one */
    static Fft getInstance() {
        return getInstance(System.getProperty(PROPERTY, "scalar"));
    }

    /**
     * @param name a short name (the simple class name without "Fft", ignoring case) or a class name
     * @return the implementation, the scalar one when not found or not available (e.g. no jdk.incubator.vector)
     */
    static Fft getInstance(String name) {
        Logger logger = getLogger(Fft.class.getName());
        try {
            for (ServiceLoader.Provider<Fft> provider : ServiceLoader.load(Fft.class).stream().toList()) {
                Class<? extends Fft> type = provider.type();
                String shortName = type.getSimpleName().replaceFirst("Fft$", "");
                if (type.getName().equals(name) || shortName.equalsIgnoreCase(name)) {
                    Fft fft = provider.get();
                    logger.log(Level.DEBUG, "fft: " + type.getName());
                    return fft;
                }
            }
            logger.log(Level.WARNING, "fft: no such implementation: " + name + ", use scalar");
        } catch (ServiceConfigurationError | LinkageError e) {
            logger.log(Level.WARNING, "fft: " + name + " is not available, use scalar: " + e);
        }
        return new ScalarFft();
    }
}
