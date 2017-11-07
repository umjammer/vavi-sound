/*
 * Copyright (c) 2007 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;


/**
 * SmafDevice.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 071010 nsano initial version <br>
 */
public interface SmafDevice {

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
    void open() throws SmafUnavailableException;
}

/* */
