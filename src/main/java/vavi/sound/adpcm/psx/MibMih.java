/*
 * https://github.com/A-SunsetMkt-Forks/vgmstream/blob/master/src/meta/mib_mih.c
 */

package vavi.sound.adpcm.psx;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import vavi.io.LittleEndianDataInputStream;


/**
 * MibMih.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see "https://claude.ai/chat/741eb151-c8c9-4db7-87ec-028804885eca"
 */
public class MibMih {
    
    // MIH+MIB - SCEE MultiStream interleaved bank (header+data) [namCollection: Ace Combat 2 (PS2), Rampage: Total Destruction (PS2)]
    public static DataInputStream initVgmstreamMibMih(Path sfBody) throws IOException {
        DataInputStream vgmstream = null;
        Path sfHead = null;
        int headerOffset, startOffset;

        // check extension
        if (!sfBody.getFileName().toString().endsWith(".mib"))
            return null;

        sfHead = sfBody.getParent().resolve(sfBody.getFileName().toString().replaceFirst("\\.mib$", ".mih"));
        if (!Files.exists(sfHead)) return null;

        headerOffset = 0x00;
        startOffset = 0x00;

        LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(Files.newInputStream(sfHead));
        if (ledis.readInt() != 0x40) { // header size
            // Marc Ecko's Getting Up (PS2) has a name at the start (hack, not standard .mib+mih)
            int nameSize = ledis.readInt();
            ledis.skipBytes(nameSize);
            if (ledis.readInt() == 0x40 && ledis.readInt() == 0x40) {
                headerOffset = 0x04 + nameSize + 0x04;
            } else {
                ledis.close();
                return null;
            }
        }

        vgmstream = initVgmstreamMultistream(sfHead, sfBody, headerOffset, startOffset);

        ledis.close();
        return vgmstream;
    }

    // MIC - SCEE MultiStream interleaved bank (merged MIH+MIB) [Rogue Trooper (PS2), The Sims 2 (PS2)]
    public static DataInputStream initVgmstreamMic(Path sf) throws IOException {
        DataInputStream vgmstream = null;
        int headerOffset, startOffset;

        // check extension
        // .mic: official extension
        // (extensionless): The Urbz (PS2), The Sims 2 series (PS2)
        if (!sf.getFileName().toString().endsWith(".mic"))
            return null;
        LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(Files.newInputStream(sf));
        if (ledis.readInt() != 0x40) // header size
            return null;

        headerOffset = 0x00;
        startOffset = 0x40;

        vgmstream = initVgmstreamMultistream(sf, sf, headerOffset, startOffset);

        return vgmstream;
    }

    private static DataInputStream initVgmstreamMultistream(Path sfHead, Path sfBody, int headerOffset, int startOffset) throws IOException {
        DataInputStream vgmstream = null;
        int dataSize, frameSize, frameLast, frameCount;
        int channels, sampleRate;
        boolean loopFlag = false; // MIB+MIH/MIC don't loop (nor use PS-ADPCM flags) per spec

        // 0x04: padding size (always 0x20, MIH header must be multiple of 0x40)
        //if (read_u8(header_offset + 0x04, sf_head) != 0x20) goto fail;
        LittleEndianDataInputStream ledis = new LittleEndianDataInputStream(Files.newInputStream(sfHead));
        ledis.skipBytes(headerOffset);
        frameLast = ledis.readInt() >> 8; // 24b
        channels = ledis.readInt();
        sampleRate = ledis.readInt();
        frameSize = ledis.readInt();
        frameCount = ledis.readInt();
        if (frameCount == 0) { // rarely [Gladius (PS2)]
            frameCount = ((int) Files.size(sfBody) - startOffset) / (frameSize * channels);
        }

        dataSize = frameCount * frameSize;
        if (frameLast != 0)
            dataSize -= frameSize - frameLast; // last frame has less usable data
        dataSize *= channels;

        // build the VGMSTREAM
        vgmstream = new DataInputStream(Files.newInputStream(sfBody));
        // meta: channels, loopFlag

        // meta: sampleRate;
        Psx.bytesToSamples(dataSize, channels);

        // layoutType = LayoutType.INTERLEAVE;
        // interleaveBlockSize = frameSize;

        // startOffset

        return vgmstream;
    }
}
