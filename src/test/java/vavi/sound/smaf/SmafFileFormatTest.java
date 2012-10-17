/*
 * Copyright (c) 2012 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.smaf;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import org.junit.Test;

import static org.junit.Assert.*;


/**
 * SmafFileFormatTest. 
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (umjammer)
 * @version 0.00 2012/10/02 umjammer initial version <br>
 */
public class SmafFileFormatTest {

    @Test
    public void test() {
        fail("Not yet implemented");
    }
    
    //----
    
    /**
     * load only
     * @param args 0: input mmf
     */
    public static void main(String[] args) throws Exception {
        SmafSystem.getSmafFileFormat(new BufferedInputStream(new FileInputStream(args[0])));
        System.exit(0);
    }
}

/* */
