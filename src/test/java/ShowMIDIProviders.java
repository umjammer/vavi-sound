/*
 * Copyright (c) 2019 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.util.Arrays;
import java.util.ServiceLoader;


/**
 * list midi device provider.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2019/04/08 umjammer initial version <br>
 */
public class ShowMIDIProviders {

    /**
     *
     * @param args ignored
     */
    public static void main(String[] args) throws Exception {
        ServiceLoader.load(javax.sound.midi.spi.MidiDeviceProvider.class)
                .forEach(provider -> Arrays.asList(provider.getDeviceInfo())
                        .forEach(info -> System.out.println(info.getName())));
    }
}
