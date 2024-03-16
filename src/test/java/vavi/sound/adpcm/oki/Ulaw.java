/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.oki;


/**
 * μ-law codec.
 * <p>
 * TODO since this class is ccitt G.711, it will be integrated.
 * </p>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 030817 nsano initial version <br>
 */
public class Ulaw extends Xlaw {

    /**
     * This table converts a (8 bit) mulaw value to a 16 bit value.
     * The 16 bits are represented as an array of two bytes for easier access
     * to the individual bytes.
     */
    private static final int[][] ulawtolin16 = {
        {0x02, 0x84}, {0x06, 0x84}, {0x0a, 0x84}, {0x0e, 0x84},
        {0x12, 0x84}, {0x16, 0x84}, {0x1a, 0x84}, {0x1e, 0x84},
        {0x22, 0x84}, {0x26, 0x84}, {0x2a, 0x84}, {0x2e, 0x84},
        {0x32, 0x84}, {0x36, 0x84}, {0x3a, 0x84}, {0x3e, 0x84},
        {0x41, 0x84}, {0x43, 0x84}, {0x45, 0x84}, {0x47, 0x84},
        {0x49, 0x84}, {0x4b, 0x84}, {0x4d, 0x84}, {0x4f, 0x84},
        {0x51, 0x84}, {0x53, 0x84}, {0x55, 0x84}, {0x57, 0x84},
        {0x59, 0x84}, {0x5b, 0x84}, {0x5d, 0x84}, {0x5f, 0x84},
        {0x61, 0x04}, {0x62, 0x04}, {0x63, 0x04}, {0x64, 0x04},
        {0x65, 0x04}, {0x66, 0x04}, {0x67, 0x04}, {0x68, 0x04},
        {0x69, 0x04}, {0x6a, 0x04}, {0x6b, 0x04}, {0x6c, 0x04},
        {0x6d, 0x04}, {0x6e, 0x04}, {0x6f, 0x04}, {0x70, 0x04},
        {0x70, 0xc4}, {0x71, 0x44}, {0x71, 0xc4}, {0x72, 0x44},
        {0x72, 0xc4}, {0x73, 0x44}, {0x73, 0xc4}, {0x74, 0x44},
        {0x74, 0xc4}, {0x75, 0x44}, {0x75, 0xc4}, {0x76, 0x44},
        {0x76, 0xc4}, {0x77, 0x44}, {0x77, 0xc4}, {0x78, 0x44},
        {0x78, 0xa4}, {0x78, 0xe4}, {0x79, 0x24}, {0x79, 0x64},
        {0x79, 0xa4}, {0x79, 0xe4}, {0x7a, 0x24}, {0x7a, 0x64},
        {0x7a, 0xa4}, {0x7a, 0xe4}, {0x7b, 0x24}, {0x7b, 0x64},
        {0x7b, 0xa4}, {0x7b, 0xe4}, {0x7c, 0x24}, {0x7c, 0x64},
        {0x7c, 0x94}, {0x7c, 0xb4}, {0x7c, 0xd4}, {0x7c, 0xf4},
        {0x7d, 0x14}, {0x7d, 0x34}, {0x7d, 0x54}, {0x7d, 0x74},
        {0x7d, 0x94}, {0x7d, 0xb4}, {0x7d, 0xd4}, {0x7d, 0xf4},
        {0x7e, 0x14}, {0x7e, 0x34}, {0x7e, 0x54}, {0x7e, 0x74},
        {0x7e, 0x8c}, {0x7e, 0x9c}, {0x7e, 0xac}, {0x7e, 0xbc},
        {0x7e, 0xcc}, {0x7e, 0xdc}, {0x7e, 0xec}, {0x7e, 0xfc},
        {0x7f, 0x0c}, {0x7f, 0x1c}, {0x7f, 0x2c}, {0x7f, 0x3c},
        {0x7f, 0x4c}, {0x7f, 0x5c}, {0x7f, 0x6c}, {0x7f, 0x7c},
        {0x7f, 0x88}, {0x7f, 0x90}, {0x7f, 0x98}, {0x7f, 0xa0},
        {0x7f, 0xa8}, {0x7f, 0xb0}, {0x7f, 0xb8}, {0x7f, 0xc0},
        {0x7f, 0xc8}, {0x7f, 0xd0}, {0x7f, 0xd8}, {0x7f, 0xe0},
        {0x7f, 0xe8}, {0x7f, 0xf0}, {0x7f, 0xf8}, {0x80, 0x00},
        {0xfd, 0x7c}, {0xf9, 0x7c}, {0xf5, 0x7c}, {0xf1, 0x7c},
        {0xed, 0x7c}, {0xe9, 0x7c}, {0xe5, 0x7c}, {0xe1, 0x7c},
        {0xdd, 0x7c}, {0xd9, 0x7c}, {0xd5, 0x7c}, {0xd1, 0x7c},
        {0xcd, 0x7c}, {0xc9, 0x7c}, {0xc5, 0x7c}, {0xc1, 0x7c},
        {0xbe, 0x7c}, {0xbc, 0x7c}, {0xba, 0x7c}, {0xb8, 0x7c},
        {0xb6, 0x7c}, {0xb4, 0x7c}, {0xb2, 0x7c}, {0xb0, 0x7c},
        {0xae, 0x7c}, {0xac, 0x7c}, {0xaa, 0x7c}, {0xa8, 0x7c},
        {0xa6, 0x7c}, {0xa4, 0x7c}, {0xa2, 0x7c}, {0xa0, 0x7c},
        {0x9e, 0xfc}, {0x9d, 0xfc}, {0x9c, 0xfc}, {0x9b, 0xfc},
        {0x9a, 0xfc}, {0x99, 0xfc}, {0x98, 0xfc}, {0x97, 0xfc},
        {0x96, 0xfc}, {0x95, 0xfc}, {0x94, 0xfc}, {0x93, 0xfc},
        {0x92, 0xfc}, {0x91, 0xfc}, {0x90, 0xfc}, {0x8f, 0xfc},
        {0x8f, 0x3c}, {0x8e, 0xbc}, {0x8e, 0x3c}, {0x8d, 0xbc},
        {0x8d, 0x3c}, {0x8c, 0xbc}, {0x8c, 0x3c}, {0x8b, 0xbc},
        {0x8b, 0x3c}, {0x8a, 0xbc}, {0x8a, 0x3c}, {0x89, 0xbc},
        {0x89, 0x3c}, {0x88, 0xbc}, {0x88, 0x3c}, {0x87, 0xbc},
        {0x87, 0x5c}, {0x87, 0x1c}, {0x86, 0xdc}, {0x86, 0x9c},
        {0x86, 0x5c}, {0x86, 0x1c}, {0x85, 0xdc}, {0x85, 0x9c},
        {0x85, 0x5c}, {0x85, 0x1c}, {0x84, 0xdc}, {0x84, 0x9c},
        {0x84, 0x5c}, {0x84, 0x1c}, {0x83, 0xdc}, {0x83, 0x9c},
        {0x83, 0x6c}, {0x83, 0x4c}, {0x83, 0x2c}, {0x83, 0x0c},
        {0x82, 0xec}, {0x82, 0xcc}, {0x82, 0xac}, {0x82, 0x8c},
        {0x82, 0x6c}, {0x82, 0x4c}, {0x82, 0x2c}, {0x82, 0x0c},
        {0x81, 0xec}, {0x81, 0xcc}, {0x81, 0xac}, {0x81, 0x8c},
        {0x81, 0x74}, {0x81, 0x64}, {0x81, 0x54}, {0x81, 0x44},
        {0x81, 0x34}, {0x81, 0x24}, {0x81, 0x14}, {0x81, 0x04},
        {0x80, 0xf4}, {0x80, 0xe4}, {0x80, 0xd4}, {0x80, 0xc4},
        {0x80, 0xb4}, {0x80, 0xa4}, {0x80, 0x94}, {0x80, 0x84},
        {0x80, 0x78}, {0x80, 0x70}, {0x80, 0x68}, {0x80, 0x60},
        {0x80, 0x58}, {0x80, 0x50}, {0x80, 0x48}, {0x80, 0x40},
        {0x80, 0x38}, {0x80, 0x30}, {0x80, 0x28}, {0x80, 0x20},
        {0x80, 0x18}, {0x80, 0x10}, {0x80, 0x08}, {0x80, 0x00},
    };

    /** */
    private static final int[] lintoulaw = {
        0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01, 0x01,
        0x01, 0x02, 0x02, 0x02, 0x02, 0x03, 0x03, 0x03,
        0x03, 0x04, 0x04, 0x04, 0x04, 0x05, 0x05, 0x05,
        0x05, 0x06, 0x06, 0x06, 0x06, 0x07, 0x07, 0x07,
        0x07, 0x08, 0x08, 0x08, 0x08, 0x09, 0x09, 0x09,
        0x09, 0x0a, 0x0a, 0x0a, 0x0a, 0x0b, 0x0b, 0x0b,
        0x0b, 0x0c, 0x0c, 0x0c, 0x0c, 0x0d, 0x0d, 0x0d,
        0x0d, 0x0e, 0x0e, 0x0e, 0x0e, 0x0f, 0x0f, 0x0f,
        0x0f, 0x10, 0x10, 0x11, 0x11, 0x12, 0x12, 0x13,
        0x13, 0x14, 0x14, 0x15, 0x15, 0x16, 0x16, 0x17,
        0x17, 0x18, 0x18, 0x19, 0x19, 0x1a, 0x1a, 0x1b,
        0x1b, 0x1c, 0x1c, 0x1d, 0x1d, 0x1e, 0x1e, 0x1f,
        0x1f, 0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26,
        0x27, 0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e,
        0x2f, 0x30, 0x32, 0x34, 0x36, 0x38, 0x3a, 0x3c,
        0x3e, 0x41, 0x45, 0x49, 0x4d, 0x53, 0x5b, 0x67,
        0xff, 0xe7, 0xdb, 0xd3, 0xcd, 0xc9, 0xc5, 0xc1,
        0xbe, 0xbc, 0xba, 0xb8, 0xb6, 0xb4, 0xb2, 0xb0,
        0xaf, 0xae, 0xad, 0xac, 0xab, 0xaa, 0xa9, 0xa8,
        0xa7, 0xa6, 0xa5, 0xa4, 0xa3, 0xa2, 0xa1, 0xa0,
        0x9f, 0x9f, 0x9e, 0x9e, 0x9d, 0x9d, 0x9c, 0x9c,
        0x9b, 0x9b, 0x9a, 0x9a, 0x99, 0x99, 0x98, 0x98,
        0x97, 0x97, 0x96, 0x96, 0x95, 0x95, 0x94, 0x94,
        0x93, 0x93, 0x92, 0x92, 0x91, 0x91, 0x90, 0x90,
        0x8f, 0x8f, 0x8f, 0x8f, 0x8e, 0x8e, 0x8e, 0x8e,
        0x8d, 0x8d, 0x8d, 0x8d, 0x8c, 0x8c, 0x8c, 0x8c,
        0x8b, 0x8b, 0x8b, 0x8b, 0x8a, 0x8a, 0x8a, 0x8a,
        0x89, 0x89, 0x89, 0x89, 0x88, 0x88, 0x88, 0x88,
        0x87, 0x87, 0x87, 0x87, 0x86, 0x86, 0x86, 0x86,
        0x85, 0x85, 0x85, 0x85, 0x84, 0x84, 0x84, 0x84,
        0x83, 0x83, 0x83, 0x83, 0x82, 0x82, 0x82, 0x82,
        0x81, 0x81, 0x81, 0x81, 0x80, 0x80, 0x80, 0x80,
    };

    /** */
    public Ulaw() {
        this.decodeTable = ulawtolin16;
        this.encodeTable = lintoulaw;
    }
}
