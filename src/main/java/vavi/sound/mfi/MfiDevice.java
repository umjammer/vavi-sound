/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi;



/**
 * MfiDevice.
 * 
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020629 nsano initial version <br>
 */
public interface MfiDevice {

    /** */
    static class Info {

        /** */
        String name;
        /** */
        String vendor;
        /** */
        String description;
        /** */
        String version;

        /** */
        protected Info(String name,
                       String vendor,
                       String description,
                       String version) {
            this.name = name;
            this.vendor = vendor;
            this.description = description;
            this.version = version;
        }

        /** */
        public final boolean equals(Object obj) {
            return super.equals(obj);
        }

        /** */
        public final int hashCode() {
            return super.hashCode();
        }

        /** */
        public final String getName() {
            return name;
        }

        /** */
        public final String getVendor() {
            return vendor;
        }

        /** */
        public final String getDescription() {
            return description;
        }

        /** */
        public final String getVersion() {
            return version;
        }

        /** */
        public final String toString() {
            return name;
        }
    }

    /** */
    void close();

    /** */
    Info getDeviceInfo();

    /** */
    boolean isOpen();

    /** */
    void open() throws MfiUnavailableException;
}

/* */
