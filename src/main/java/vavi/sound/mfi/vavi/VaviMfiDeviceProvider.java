/*
 * Copyright (c) 2002 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.mfi.vavi;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import vavi.sound.mfi.MfiDevice;
import vavi.sound.mfi.spi.MfiDeviceProvider;

import static java.lang.System.getLogger;


/**
 * {@link MfiDeviceProvider} implemented by vavi.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 020629 nsano initial version <br>
 *          0.10 020703 nsano complete <br>
 *          0.11 030819 nsano add {@link vavi.sound.mfi.MidiConverter} <br>
 */
public class VaviMfiDeviceProvider extends MfiDeviceProvider {

    private static final Logger logger = getLogger(VaviMfiDeviceProvider.class.getName());

    static {
        try {
            try (InputStream is = VaviMfiDeviceProvider.class.getResourceAsStream("/META-INF/maven/vavi/vavi-sound/pom.properties")) {
                if (is != null) {
                    Properties props = new Properties();
                    props.load(is);
                    version = props.getProperty("version", "undefined in pom.properties");
                } else {
                    version = System.getProperty("vavi.test.version", "undefined");
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** */
    public static final String version;

    /** */
    private final MfiDevice.Info[] mfiDeviceInfos;

    /** */
    public VaviMfiDeviceProvider() {
        this.mfiDeviceInfos = Factory.getMfiDeviceInfos();
    }

    @Override
    public boolean isDeviceSupported(MfiDevice.Info info) {
        for (MfiDevice.Info mfiDeviceInfo : mfiDeviceInfos) {
            if (mfiDeviceInfo.equals(info)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public MfiDevice.Info[] getDeviceInfo() {
        return mfiDeviceInfos;
    }

    @Override
    public MfiDevice getDevice(MfiDevice.Info info)
        throws IllegalArgumentException {

        return Factory.getMfiDevice(info);
    }

    /** */
    private static class Factory {

        /** */
        public static MfiDevice.Info[] getMfiDeviceInfos() {

            List<MfiDevice.Info> tmp = new ArrayList<>(deviceMap.keySet());

            return tmp.toArray(new MfiDevice.Info[0]);
        }

        /** */
        public static MfiDevice getMfiDevice(MfiDevice.Info mfiDeviceInfo) {
            if (deviceMap.containsKey(mfiDeviceInfo)) {
                try {
                    return deviceMap.get(mfiDeviceInfo).getDeclaredConstructor().newInstance();
                } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                }
            }

            throw new IllegalArgumentException(mfiDeviceInfo + " is not supported");
        }

        /** */
        private static final Map<MfiDevice.Info, Class<MfiDevice>> deviceMap = new HashMap<>();

        /* */
        static {
            try {
                // props
                Properties props = new Properties();
                final String path = "vavi.properties";
                props.load(Factory.class.getResourceAsStream(path));

                // midi
                for (Object o : props.keySet()) {
                    String key = (String) o;
                    if (key.startsWith("mfi.device.")) {
                        @SuppressWarnings("unchecked")
                        Class<MfiDevice> deviceClass = (Class<MfiDevice>) Class.forName(props.getProperty(key));
//logger.log(Level.TRACE, "mfi device class: " + StringUtil.getClassName(deviceClass));
                        MfiDevice.Info mfiDeviceInfo = deviceClass.getDeclaredConstructor().newInstance().getDeviceInfo();

                        deviceMap.put(mfiDeviceInfo, deviceClass);
                    }
                }
            } catch (Exception e) {
logger.log(Level.ERROR, e.getMessage(), e);
                throw new IllegalStateException(e);
            }
        }
    }
}
