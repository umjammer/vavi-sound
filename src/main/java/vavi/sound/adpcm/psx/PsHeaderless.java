/*
 * https://github.com/A-SunsetMkt-Forks/vgmstream/blob/master/src/meta/ps_headerless.c
 */

package vavi.sound.adpcm.psx;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import vavi.io.SeekableDataInputStream;
import vavi.sound.adpcm.psx.Psx.VGMStreamChannel;
import vavi.sound.adpcm.psx.Psx.VgmStream;

import static java.lang.System.getLogger;


/**
 * PsHeaderless.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see "https://claude.ai/chat/741eb151-c8c9-4db7-87ec-028804885eca"
 */
public class PsHeaderless {

    private static final Logger logger = getLogger(PsHeaderless.class.getName());

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
        int r = dis.readNBytes(mibBuffer, 0, 0x10);
        readOffset += r;
        mibBuffer[0] = 0;

        boolean doChannelUpdate = true;
        boolean bDoUpdateInterleave = true;

        readOffset = 0;
        do {
            dis.position(readOffset);
            r = dis.readNBytes(testBuffer, 0, 0x10);
            if (r <= 0) {
logger.log(Level.DEBUG, "EOF");
                break;
            }
            readOffset += r;

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
                dis.position(readOffset);
                dis.readNBytes(testBuffer, 0, 0x10);
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

//        codingType = CodingType.PSX;
//        layoutType = (channelCount == 1) ? LayoutType.NONE : LayoutType.INTERLEAVE;
        logger.log(Level.DEBUG, "LayoutType: " + (channelCount == 1 ? "NONE" : "INTERLEAVE")); // TODO interleave

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
                vgmstream.loopStartSample = loopStart / 16 * 18; // TODO 18 instead of 28 probably a bug
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

        logger.log(Level.DEBUG, "loopToEnd: " + loopToEnd);
        if (loopToEnd) {
            // try to find if there's no empty line ...
            int emptySamples = 0;

            for (i = 0; i < 16; i++) {
                mibBuffer[i] = 0; // memset
            }

            readOffset = fileLength - 0x10;
            do {
                dis.position(readOffset);
                dis.readNBytes(testBuffer, 0, 0x10);
                if (Arrays.equals(mibBuffer, 0, 16, testBuffer, 0, 16)) {
                    emptySamples += 28;
                }
                readOffset -= 0x10;
            } while (Arrays.equals(testBuffer, 0, 16, mibBuffer, 0, 16));

            vgmstream.loopEndSample -= (emptySamples * channelCount);
        }

        vgmstream.metaType = "PS_HEADERLESS";
        vgmstream.allowDualStereo = true;

        logger.log(Level.DEBUG, "startOffset: " + startOffset + ", readOffset: " + readOffset);
        for (int ch = 0; ch < vgmstream.channels; ch++) {
            vgmstream.ch[ch] = new VGMStreamChannel(dis, startOffset, ch);
        }

        return vgmstream;
    }
}
