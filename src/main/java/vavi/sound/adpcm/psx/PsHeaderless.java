/*
 * https://github.com/A-SunsetMkt-Forks/vgmstream/blob/master/src/meta/ps_headerless.c
 */

package vavi.sound.adpcm.psx;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import vavi.io.SeekableDataInputStream;
import vavi.sound.adpcm.psx.Psx.LayoutType;
import vavi.sound.adpcm.psx.Psx.VGMStreamChannel;
import vavi.sound.adpcm.psx.Psx.VgmStream;

import static java.lang.System.getLogger;


/**
 * PsHeaderless.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see "https://claude.ai/chat/741eb151-c8c9-4db7-87ec-028804885eca"
 * @see "https://gemini.google.com/app/ce7c6594310c8a72"
 */
public class PsHeaderless {

    private static final Logger logger = getLogger(PsHeaderless.class.getName());

    /** bulk read size for the sequential scan below, a 0x10 multiple */
    private static final int SCAN_BUFFER_SIZE = 0x10000;

    /**
     * Reads {@code b.length} bytes at the current channel position.
     * <p>
     * {@link SeekableDataInputStream} doesn't override {@link java.io.InputStream#read(byte[], int, int)},
     * so reading through it costs one channel read (i.e. one syscall) per byte. The scan below touches
     * every 0x10 line of the file, so it goes to the channel directly.
     *
     * @return the byte count actually read, 0 at EOF ({@code b} is left untouched then)
     */
    private static int read(SeekableByteChannel sbc, byte[] b) throws IOException {
        ByteBuffer bb = ByteBuffer.wrap(b);
        while (bb.hasRemaining() && sbc.read(bb) > 0) {
        }
        return bb.position();
    }

    /** {@link #read(SeekableByteChannel, byte[])} at {@code offset} */
    private static int read(SeekableByteChannel sbc, long offset, byte[] b) throws IOException {
        sbc.position(offset);
        return read(sbc, b);
    }

    /**
     * Headerless PS-ADPCM
     * Guesses interleave/channels/loops by testing data and using the file extension for sample rate.
     * This is an ugly crutch for older sets, use TXTH to properly play headerless data instead.
     */
    public static VgmStream initVgmstreamPsHeaderless(Path streamFile) throws IOException {
        VgmStream vgmstream;
        int startOffset = 0x00;
        String filename;

        byte[] mibBuffer = new byte[0x10];
        byte[] testBuffer = new byte[0x10];

        int fileLength;
        int loopStart = 0;
        int loopEnd = 0;
        int interleave = 0;

        int readOffset = 0;

        int[] loopStartPoints = new int[0x10];
        int loopStartPointsCount = 0;
        int[] loopEndPoints = new int[0x10];
        int loopEndPointsCount = 0;
        boolean loopToEnd = false;
        boolean forceNoLoop = false;
        boolean gotEmptyLine = false;

        int i, channelCount = 0;

        // checks
        // .mib: common, but many ext-less files are renamed to this.
        // .mi4: fake .mib to force another sample rate
        filename = streamFile.getFileName().toString();
        String fileExt = filename.substring(filename.lastIndexOf('.') + 1);
logger.log(Level.DEBUG, "ext: " + fileExt);
        if (!fileExt.equalsIgnoreCase("mib") && !fileExt.equalsIgnoreCase("mi4"))
            throw new IllegalArgumentException(fileExt);

        // test if raw PS-ADPCM
        SeekableDataInputStream dis = new SeekableDataInputStream(Files.newByteChannel(streamFile));
        if (!Psx.checkFormat(dis, (int) Files.size(streamFile), 0x00, 0x2000))
            throw new IllegalArgumentException("check format");

        fileLength = (int) Files.size(streamFile);

        // Search for interleave value (checking channel starts) and loop points (using PS-ADPCM flags).
        // Channel start will by 0x0000, 0x0002, 0x0006 followed by 12 zero values.
        // Interleave value is the offset where those repeat, and channels the number of times.
        // Loop flags in second byte are: 0x06 = start, 0x03 = end (per channel).
        // Interleave can be large (up to 0x20000 found so far) and is always a 0x10 multiple value.
        SeekableByteChannel sbc = dis.origin();
        int r = read(sbc, 0x00, mibBuffer);
        readOffset += r;
        mibBuffer[0] = 0;

        boolean doChannelUpdate = true;
        boolean bDoUpdateInterleave = true;

        // the scan walks the whole file line by line, so it reads in big chunks rather than seeking per line
        byte[] scanBuffer = new byte[SCAN_BUFFER_SIZE];
        int scanLimit = 0;
        int scanOffset = 0;

        readOffset = 0;
        sbc.position(0);
        do {
            if (scanOffset == scanLimit) {
                scanLimit = read(sbc, scanBuffer);
                scanOffset = 0;
            }
            if (scanLimit - scanOffset < 0x10) {
logger.log(Level.DEBUG, "EOF");
                break;
            }
            System.arraycopy(scanBuffer, scanOffset, testBuffer, 0, 0x10);
            scanOffset += 0x10;
            readOffset += 0x10;

            // be sure to point to an interleave value
            if (readOffset < (fileLength * 0.5)) {

                if (!Arrays.equals(testBuffer, 2, 2 + 0x0e, mibBuffer, 2, 2 + 0x0e)) {
                    if (doChannelUpdate) {
                        doChannelUpdate = false;
                        channelCount++;
                    }
                    if (channelCount < 2)
                        bDoUpdateInterleave = true;
                }

                testBuffer[0] = 0;
                if (Arrays.equals(testBuffer, 0, 0x10, mibBuffer, 0, 0x10)) {
                    gotEmptyLine = true;

                    if (bDoUpdateInterleave) {
                        bDoUpdateInterleave = false;
                        interleave = readOffset - 0x10;
                    }
                    if (readOffset - 0x10 == channelCount * interleave) {
                        doChannelUpdate = true;
                    }
                }
            }

            // Loop Start ...
            if (testBuffer[0x01] == 0x06) {
                if (loopStartPointsCount < 0x10) {
                    loopStartPoints[loopStartPointsCount] = readOffset - 0x10;
                    loopStartPointsCount++;
                }
            }

            // Loop End ...
            if (testBuffer[0x01] == 0x03 && testBuffer[0x03] != 0x77) {
                if (loopEndPointsCount < 0x10) {
                    loopEndPoints[loopEndPointsCount] = readOffset;
                    loopEndPointsCount++;
                }
            }

            if (testBuffer[0x01] == 0x04) {
                // 0x04 loop points flag can't be with a 0x03 loop points flag
                if (loopStartPointsCount < 0x10) {
                    loopStartPoints[loopStartPointsCount] = readOffset - 0x10;
                    loopStartPointsCount++;

                    // Loop end value is not set by flags ...
                    // go until end of file
                    loopToEnd = true;
                }
            }

        } while (readOffset < fileLength);

        if (testBuffer[0] == 0x0c && testBuffer[1] == 0)
            forceNoLoop = true;

        if (channelCount == 0)
            channelCount = 1;

        // Calc Loop Points & Interleave ...
        if (loopStartPointsCount >= 2) {
            // can't get more then 0x10 loop point !
            if (loopStartPointsCount <= 0x0F) {
                // Always took the first 2 loop points
                interleave = loopStartPoints[1] - loopStartPoints[0];
                loopStart = loopStartPoints[1];

                // Can't be one channel .mib with interleave values
                if (interleave > 0 && channelCount == 1)
                    channelCount = 2;
            } else {
                loopStart = 0;
            }
        }

        if (loopEndPointsCount >= 2) {
            // can't get more then 0x10 loop point !
            if (loopEndPointsCount <= 0x0F) {
                // No need to recalculate interleave value ...
                loopEnd = loopEndPoints[loopEndPointsCount - 1];

                // Can't be one channel .mib with interleave values
                if (channelCount == 1)
                    channelCount = 2;
            } else {
                loopToEnd = false;
                loopEnd = 0;
            }
        }

        if (loopToEnd)
            loopEnd = fileLength;

        if (forceNoLoop)
            loopEnd = 0;

        if (interleave > 0x10 && channelCount == 1)
            channelCount = 2;

        if (interleave == 0)
            interleave = 0x10;

        // further check on channel_count ...
        if (gotEmptyLine) {
            int newChannelCount = 0;

            readOffset = 0;

            // count empty lines at interleave = channels
            do {
                newChannelCount++;
                read(sbc, readOffset, testBuffer);
                readOffset += interleave;
            } while (Arrays.equals(testBuffer, 0, 16, mibBuffer, 0, 16));

            newChannelCount--;
            if (newChannelCount > channelCount)
                channelCount = newChannelCount;
        }

        // build the VGMSTREAM
        vgmstream = new VgmStream(channelCount, loopEnd != 0);
        logger.log(Level.DEBUG, "channelCount: " + channelCount);
        logger.log(Level.DEBUG, "loopEnd: " + loopEnd);

        vgmstream.layoutType = (channelCount == 1) ? LayoutType.NONE : LayoutType.INTERLEAVE;
logger.log(Level.DEBUG, "LayoutType: " + vgmstream.layoutType);

        vgmstream.interleaveBlockSize = interleave;
        logger.log(Level.DEBUG, "interleave: " + interleave);

        if (fileExt.equalsIgnoreCase("mib"))
            vgmstream.sampleRate = 44100;

        //
        if (fileExt.equalsIgnoreCase("mi4"))
            vgmstream.sampleRate = 48000;
        logger.log(Level.DEBUG, "sampleRate: " + vgmstream.sampleRate);

        //
        vgmstream.numSamples = fileLength / 16 / channelCount * 28;
        logger.log(Level.DEBUG, "numSamples: " + vgmstream.numSamples);

        if (loopEnd != 0) {
            if (channelCount == 1) {
                // FIXED: Typo 18 -> 28
                vgmstream.loopStartSample = loopStart / 16 * 28;
                vgmstream.loopEndSample = loopEnd / 16 * 28;
            } else {
                vgmstream.loopStartSample = ((((loopStart / vgmstream.interleaveBlockSize) - 1) * vgmstream.interleaveBlockSize) / 16 * 14 * channelCount) / channelCount;
                if (loopStart % vgmstream.interleaveBlockSize != 0) {
                    vgmstream.loopStartSample += (((loopStart % vgmstream.interleaveBlockSize) - 1) / 16 * 14 * channelCount);
                }

                if (loopEnd == fileLength) {
                    vgmstream.loopEndSample = (loopEnd / 16 * 28) / channelCount;
                } else {
                    vgmstream.loopEndSample = ((((loopEnd / vgmstream.interleaveBlockSize) - 1) * vgmstream.interleaveBlockSize) / 16 * 14 * channelCount) / channelCount;
                    if (loopEnd % vgmstream.interleaveBlockSize != 0) {
                        vgmstream.loopEndSample += (((loopEnd % vgmstream.interleaveBlockSize) - 1) / 16 * 14 * channelCount);
                    }
                }
            }
        }

        if (loopToEnd) {
            // try to find if there's no empty line ...
            int emptySamples = 0;

            for (i = 0; i < 16; i++) {
                mibBuffer[i] = 0; // memset
            }

            readOffset = fileLength - 0x10;
            do {
                read(sbc, readOffset, testBuffer);
                if (Arrays.equals(mibBuffer, 0, 16, testBuffer, 0, 16)) {
                    emptySamples += 28;
                }
                readOffset -= 0x10;
            } while (Arrays.equals(testBuffer, 0, 16, mibBuffer, 0, 16));

            vgmstream.loopEndSample -= (emptySamples * channelCount);
        }

        vgmstream.metaType = "PS_HEADERLESS";
        vgmstream.allowDualStereo = true;

        for (int ch = 0; ch < vgmstream.channels; ch++) {
            // interleaved layout: each channel starts one interleave block after the previous
            // (Psx.update_offsets advances every channel by interleave * channels per block set)
            int channelOffset = startOffset + (channelCount != 1 ? vgmstream.interleaveBlockSize * ch : 0);
            // interleaveBlockSize arg is now ignored by Psx.VGMStreamChannel but passed for code compat
            vgmstream.ch[ch] = new VGMStreamChannel(dis, channelOffset, ch, channelCount != 1 ? vgmstream.interleaveBlockSize : -1);
        }

        return vgmstream;
    }
}
