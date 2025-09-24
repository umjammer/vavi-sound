/*
 * https://github.com/vgmstream/vgmstream/blob/master/src/coding/psx_decoder.c
 */

package vavi.sound.adpcm.psx;

import java.io.DataInputStream;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import vavi.io.SeekableDataInputStream;

import static java.lang.System.getLogger;


/**
 * PlayStation ADPCM decoder implementation in Java
 * Ported from C implementation
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @see "https://claude.ai/chat/fb5a6f35-d25f-4b91-a93b-f105bae0d6e5"
 */
public class Psx {

    private static final Logger logger = getLogger(Psx.class.getName());

    /** PS-ADPCM table, defined as rational numbers (as in the spec) */
    private static final float[][] PS_ADPCM_COEFS_F = {
            {0.0f, 0.0f},               // {   0.0        ,   0.0        },
            {0.9375f, 0.0f},            // {  60.0 / 64.0 ,   0.0        },
            {1.796875f, -0.8125f},      // { 115.0 / 64.0 , -52.0 / 64.0 },
            {1.53125f, -0.859375f},     // {  98.0 / 64.0 , -55.0 / 64.0 },
            {1.90625f, -0.9375f},       // { 122.0 / 64.0 , -60.0 / 64.0 },
            // extended table used in few PS3 games, found in ELFs
            {0.46875f, -0.0f},          // {  30.0 / 64.0 ,  -0.0 / 64.0 },
            {0.8984375f, -0.40625f},    // {  57.5 / 64.0 , -26.0 / 64.0 },
            {0.765625f, -0.4296875f},   // {  49.0 / 64.0 , -27.5 / 64.0 },
            {0.953125f, -0.46875f},     // {  61.0 / 64.0 , -30.0 / 64.0 },
            {0.234375f, -0.0f},         // {  15.0 / 64.0 ,  -0.0 / 64.0 },
            {0.44921875f, -0.203125f},  // {  28.75/ 64.0 , -13.0 / 64.0 },
            {0.3828125f, -0.21484375f}, // {  24.5 / 64.0 , -13.75/ 64.0 },
            {0.4765625f, -0.234375f},   // {  30.5 / 64.0 , -15.0 / 64.0 },
            {0.5f, -0.9375f},           // {  32.0 / 64.0 , -60.0 / 64.0 },
            {0.234375f, -0.9375f},      // {  15.0 / 64.0 , -60.0 / 64.0 },
            {0.109375f, -0.9375f},      // {   7.0 / 64.0 , -60.0 / 64.0 },
    };

    /** PS-ADPCM table, defined as spec_coef*64 (for int implementations) */
    private static final int[][] PS_ADPCM_COEFS_I = {
            {0, 0},
            {60, 0},
            {115, -52},
            {98, -55},
            {122, -60},
    };

    public enum LayoutType {
        NONE,
        INTERLEAVE
        // Other layout types would be defined here
    }

    static final int VGMSTREAM_MAX_CHANNELS = 64;

    // Classes that would be defined elsewhere
    public static class VgmStream {
        public int sampleRate;
        public int numSamples;
//        public CodingType codingType;
        public LayoutType layoutType;
        public int interleaveBlockSize;
        public String metaType;
        public int channels;
        public int loopStartSample;
        public int loopEndSample;
        public boolean allowDualStereo;
        public int samplesIntoBlock;
        public int codecConfig;
        public VGMStreamChannel[] ch;
        public int currentSample;
        public boolean hitLoop;
        public boolean loopFlag;
        public int interleave_block_size;
        public int interleave_first_skip;
        public int interleave_first_block_size;
        public int interleave_last_block_size;
        public boolean codec_internal_updates;
        public VGMStreamChannel[] loop_ch;
        public int loop_count;
        public int loop_target;
        public int loop_current_sample;
        public int loop_samples_into_block;
        public int loop_block_size;
        public int loop_block_samples;
        public int loop_block_offset;
        public int loop_next_block_offset;
        public int loop_full_block_size;
        public int current_block_size;
        public int current_block_samples;
        public int current_block_offset;
        public int next_block_offset;
        public int full_block_size;

        public VgmStream(int channels, boolean loop_flag) {
            // up to ~16-24 aren't too rare for multilayered files, 50+ is probably a bug
            if (channels <= 0 || channels > VGMSTREAM_MAX_CHANNELS) {
                throw new IllegalArgumentException("error allocating %d channels".formatted(channels));
            }

            // VGMSTREAM's alloc'ed internals work as follows:
            // - vgmstream: main struct (config+state) modified by metas, layouts and codings as needed
            // - ch: config+state per channel, also modified by those
            // - start_vgmstream: vgmstream clone copied on init_vgmstream and restored on reset_vgmstream
            // - start_ch: ch clone copied on init_vgmstream and restored on reset_vgmstream
            // - loop_ch: ch clone copied on loop start and restored on loop end (decode_do_loop)
            // - codec/layout_data: custom state for complex codecs or layouts, handled externally
            //
            // Here we only create the basic structs to be filled, and only after init_vgmstream it
            // can be considered ready. Clones are shallow copies, in that they share alloc'ed struts
            // (like, vgmstream.ch and start_vgmstream.ch will be the same after init_vgmstream, or
            // start_vgmstream.start_vgmstream will end up pointing to itself)
            //
            // This is all a bit too brittle, so code alloc'ing or changing anything sensitive should
            // take care clones are properly synced.

//            this.start_vgmstream = calloc(1, sizeof(VGMSTREAM));

            this.ch = new VGMStreamChannel[channels];

//            this.start_ch = new VGMStreamChannel[channels];

            if (loop_flag) {
                this.loop_ch = new VGMStreamChannel[channels];
            }

            this.channels = channels;
            this.loopFlag = loop_flag;

//            this.mixer = mixer_init(this.channels); // pre-init

//            this.decode_state = decode_init();

            // TODO improve/init later to minimize memory
            //  garbage buffer for seeking/discarding (local bufs may cause stack overflows with segments/layers)
            //  in theory the bigger the better but in practice there isn't much difference.
//            this.tmpbuf_size = 1024 * 2 * channels * sizeof( float);
//            this.tmpbuf = malloc(this.tmpbuf_size);

            // BEWARE: merge_vgmstream does some free'ing too

            //this.stream_name_size = STREAM_NAME_SIZE;
        }
    }

    /**
     * Calculate number of consecutive samples we can decode. Takes into account hitting
     * a loop start or end, or going past a single frame.
     * @see "https://github.com/vgmstream/vgmstream/blob/master/src/base/decode.c#L1411"
     */
    static int decode_get_samples_to_do(int samples_this_block, int samples_per_frame, VgmStream vgmstream) {
        int samples_to_do;
        int samples_left_this_block;

        samples_left_this_block = samples_this_block - vgmstream.samplesIntoBlock;
        samples_to_do = samples_left_this_block; // by default decodes all samples left

        // fun loopy crap, why did I think this would be any simpler?
        if (vgmstream.loopFlag) {
            int samples_after_decode = vgmstream.currentSample + samples_left_this_block;

            // are we going to hit the loop end during this block?
            if (samples_after_decode > vgmstream.loopEndSample) {
                // only do samples up to loop end
                samples_to_do = vgmstream.loopEndSample - vgmstream.currentSample;
            }

            // are we going to hit the loop start during this block? (first time only)
            if (samples_after_decode > vgmstream.loopStartSample && !vgmstream.hitLoop) {
                // only do samples up to loop start
                samples_to_do = vgmstream.loopStartSample - vgmstream.currentSample;
            }
        }

        // if it's a framed encoding don't do more than one frame
        if (samples_per_frame > 1 && (vgmstream.samplesIntoBlock % samples_per_frame) + samples_to_do > samples_per_frame)
            samples_to_do = samples_per_frame - (vgmstream.samplesIntoBlock % samples_per_frame);

        return samples_to_do;
    }

    static void update_loop_values(layout_config_t layout, VgmStream vgmstream, int[] p_samples_per_frame, int[] p_samples_this_block) {
        if (layout.has_interleave_first &&
                vgmstream.currentSample < layout.samples_this_block_f) {
            // use first interleave
            p_samples_per_frame[0] = layout.samples_per_frame_f;
            p_samples_this_block[0] = layout.samples_this_block_f;
            if (p_samples_this_block[0] == 0 && vgmstream.channels == 1)
                p_samples_this_block[0] = vgmstream.numSamples;
        } else if (layout.has_interleave_last) { // assumes that won't loop back into a interleave_last
            p_samples_per_frame[0] = layout.samples_per_frame_d;
            p_samples_this_block[0] = layout.samples_this_block_d;
            if (p_samples_this_block[0] == 0 && vgmstream.channels == 1)
                p_samples_this_block[0] = vgmstream.numSamples;
        }
    }

    static void update_offsets(layout_config_t layout, VgmStream vgmstream, int[] p_samples_per_frame, int[] p_samples_this_block) {
        int channels = vgmstream.channels;

        if (layout.has_interleave_first &&
                vgmstream.currentSample == layout.samples_this_block_f) {
            // interleave during first interleave: restore standard frame size after going past first interleave
            p_samples_per_frame[0] = layout.samples_per_frame_d;
            p_samples_this_block[0] = layout.samples_this_block_d;
            if (p_samples_this_block[0] == 0 && channels == 1)
                p_samples_this_block[0] = vgmstream.numSamples;

            for (int ch = 0; ch < channels; ch++) {
                int skip = vgmstream.interleave_first_skip * (channels - 1 - ch) +
                        vgmstream.interleave_first_block_size * (channels - ch) +
                        vgmstream.interleave_block_size * ch;
                vgmstream.ch[ch].offset += skip;
            }
        } else if (layout.has_interleave_last &&
                vgmstream.currentSample + p_samples_this_block[0] > vgmstream.numSamples) {
            // interleave during last interleave: adjust values again if inside last interleave
            p_samples_per_frame[0] = layout.samples_per_frame_l;
            p_samples_this_block[0] = layout.samples_this_block_l;
            if (p_samples_this_block[0] == 0 && channels == 1)
                p_samples_this_block[0] = vgmstream.numSamples;

            for (int ch = 0; ch < channels; ch++) {
                int skip = vgmstream.interleave_block_size * (channels - ch) +
                        vgmstream.interleave_last_block_size * ch;
                vgmstream.ch[ch].offset += skip;
            }
        } else if (layout.has_interleave_internal_updates) {
            // interleave for some decoders that have already moved offsets over their data, so skip other channels' data
            for (int ch = 0; ch < channels; ch++) {
                int skip = vgmstream.interleave_block_size * (channels - 1);
                vgmstream.ch[ch].offset += skip;
            }
        } else {
            // regular interleave
            for (int ch = 0; ch < channels; ch++) {
                int skip = vgmstream.interleave_block_size * channels;
                vgmstream.ch[ch].offset += skip;
            }
        }

        vgmstream.samplesIntoBlock = 0;
    }

    static void update_default_values(layout_config_t layout, VgmStream vgmstream, int[] p_samples_per_frame, int[] p_samples_this_block) {
        if (layout.has_interleave_first && vgmstream.currentSample < layout.samples_this_block_f) {
            p_samples_per_frame[0] = layout.samples_per_frame_f;
            p_samples_this_block[0] = layout.samples_this_block_f;
        }
        else if (layout.has_interleave_last &&
                vgmstream.currentSample - vgmstream.samplesIntoBlock + layout.samples_this_block_d > vgmstream.numSamples) {
            p_samples_per_frame[0] = layout.samples_per_frame_l;
            p_samples_this_block[0] = layout.samples_this_block_l;
        }
        else {
            p_samples_per_frame[0] = layout.samples_per_frame_d;
            p_samples_this_block[0] = layout.samples_this_block_d;
        }
    }

    static class layout_config_t {
        /* default */
        int samples_per_frame_d;
        int samples_this_block_d;
        /* first */
        int samples_per_frame_f;
        int samples_this_block_f;
        /* last */
        int samples_per_frame_l;
        int samples_this_block_l;

        boolean has_interleave_first;
        boolean has_interleave_last;
        boolean has_interleave_internal_updates;
    }

    static boolean setup_helper(layout_config_t layout, VgmStream vgmstream) {
        //TO-DO: this could be pre-calc'd after main init
        layout.has_interleave_first = vgmstream.interleave_first_block_size != 0 && vgmstream.channels > 1;
        layout.has_interleave_last = vgmstream.interleave_last_block_size != 0 && vgmstream.channels > 1;
        layout.has_interleave_internal_updates = vgmstream.codec_internal_updates;

        {
            int frame_size_d = decode_get_frame_size(vgmstream);
            layout.samples_per_frame_d = decode_get_samples_per_frame(vgmstream);
            if (frame_size_d == 0 || layout.samples_per_frame_d == 0)
                return false;
            layout.samples_this_block_d = vgmstream.interleaveBlockSize / frame_size_d * layout.samples_per_frame_d;
        }

        if (layout.has_interleave_first) {
            int frame_size_f = decode_get_frame_size(vgmstream);
            layout.samples_per_frame_f = decode_get_samples_per_frame(vgmstream); // TODO samples per shortframe
            if (frame_size_f == 0 || layout.samples_per_frame_f == 0)
                return false;
            layout.samples_this_block_f = vgmstream.interleaveBlockSize / frame_size_f * layout.samples_per_frame_f;
        }

        if (layout.has_interleave_last) {
            int frame_size_l = decode_get_shortframe_size(vgmstream);
            layout.samples_per_frame_l = decode_get_samples_per_shortframe(vgmstream);
            if (frame_size_l == 0 || layout.samples_per_frame_l == 0)
                return false;
            layout.samples_this_block_l = vgmstream.interleaveBlockSize / frame_size_l * layout.samples_per_frame_l;
        }

        return true;
    }

    private static int decode_get_samples_per_shortframe(VgmStream vgmstream) {
        return decode_get_samples_per_frame(vgmstream);
    }

    private static int decode_get_shortframe_size(VgmStream vgmstream) {
        return decode_get_frame_size(vgmstream);
    }

    static int decode_get_samples_per_frame(VgmStream vgmstream) {
        return 28;
    }

    static int decode_get_frame_size(VgmStream vgmstream) {
        return 0x10;
    }

    /**
     * Detect loop start and save values, or detect loop end and restore (loop back).
     * @return true if loop was done.
     */
    static boolean decode_do_loop(VgmStream vgmstream) {
        //if (!vgmstream.loop_flag) return false;

        // is this the loop end? = new loop, continue from loop_start_sample
        if (vgmstream.currentSample == vgmstream.loopEndSample) {

            // disable looping if target count reached and continue normally
            // (only needed with the "play stream end after looping N times" option enabled)
            vgmstream.loop_count++;
            if (vgmstream.loop_target != 0 && vgmstream.loop_target == vgmstream.loop_count) {
                vgmstream.loopFlag = false; // could be improved but works ok, will be restored on resets
                return false;
            }

            // against everything I hold sacred, preserve adpcm history before looping for certain types
//            if (vgmstream.metaType == meta_DSP_STD ||
//                    vgmstream.metaType == meta_DSP_RS03 ||
//                            vgmstream.metaType == meta_DSP_CSTR ||
//                                    vgmstream.metaType == coding_PSX ||
//                                            vgmstream.metaType == coding_PSX_badflags) {
            for (int ch = 0; ch < vgmstream.channels; ch++) {
                vgmstream.loop_ch[ch].adpcmHistory1_16 = vgmstream.ch[ch].adpcmHistory1_16;
                vgmstream.loop_ch[ch].adpcmHistory2_16 = vgmstream.ch[ch].adpcmHistory2_16;
                vgmstream.loop_ch[ch].adpcmHistory1_32 = vgmstream.ch[ch].adpcmHistory1_32;
                vgmstream.loop_ch[ch].adpcmHistory2_32 = vgmstream.ch[ch].adpcmHistory2_32;
            }
//            }

            // TODO: improve
            // codecs with codec_data that decode_loop need special handling, usually:
            // - during decode, codec uses vgmstream.ch[].offset to handle current offset
            // - on hit_loop, current offset is auto-copied to vgmstream.loop_ch[].offset
            // - decode_seek codecs may overwrite vgmstream.loop_ch[].offset with a custom value (such as start_offset)
            // - vgmstream.loop_ch[] is copied below to vgmstream.ch[] (with the newly assigned custom value)
            // - then codec will use vgmstream.ch[].offset during decode
            // regular codecs will use copied vgmstream.loop_ch[].offset without issue
            decode_loop(vgmstream);

            /* restore! */
            System.arraycopy(vgmstream.loop_ch, 0, vgmstream.ch, 0, vgmstream.channels);
            vgmstream.currentSample = vgmstream.loop_current_sample;
            vgmstream.samplesIntoBlock = vgmstream.loop_samples_into_block;
            vgmstream.current_block_size = vgmstream.loop_block_size;
            vgmstream.current_block_samples = vgmstream.loop_block_samples;
            vgmstream.current_block_offset = vgmstream.loop_block_offset;
            vgmstream.next_block_offset = vgmstream.loop_next_block_offset;
            vgmstream.full_block_size = vgmstream.loop_full_block_size;

            // loop layouts (after restore, in case layout needs state manipulations)
//            switch(vgmstream.layoutType) {
//                case layout_segmented:
//                    loop_layout_segmented(vgmstream, vgmstream.loop_current_sample);
//                    break;
//                case layout_layered:
//                    loop_layout_layered(vgmstream, vgmstream.loop_current_sample);
//                    break;
//                default:
//                    break;
//            }

            // play state is applied over loops and stream decoding, so it's not restored on loops
//            vgmstream.pstate = vgmstream.lstate;

            return true; // has looped
        }

        // is this the loop start? save if we haven't saved yet (right when first loop starts)
        if (!vgmstream.hitLoop && vgmstream.currentSample == vgmstream.loopStartSample) {
            // save!
            System.arraycopy(vgmstream.ch, 0, vgmstream.loop_ch, 0, vgmstream.channels);
            vgmstream.loop_current_sample = vgmstream.currentSample;
            vgmstream.loop_samples_into_block = vgmstream.samplesIntoBlock;
            vgmstream.loop_block_size = vgmstream.current_block_size;
            vgmstream.loop_block_samples = vgmstream.current_block_samples;
            vgmstream.loop_block_offset = vgmstream.current_block_offset;
            vgmstream.loop_next_block_offset = vgmstream.next_block_offset;
            vgmstream.loop_full_block_size = vgmstream.full_block_size;

            /* play state is applied over loops and stream decoding, so it's not saved on loops */
            //vgmstream.lstate = vgmstream.pstate;

            vgmstream.hitLoop = true; /* info that loop is now ready to use */
        }

        return false; /* has not looped */
    }

    static void decode_loop(VgmStream vgmstream) {
        decode_seek(vgmstream, vgmstream.loop_current_sample);
    }

    // TODO
    static void decode_state_reset(VgmStream vgmstream) {
//        if (vgmstream.decode_state == null)
//            return;
//        memset(vgmstream.decode_state, 0, sizeof(decode_state_t));
    }

    // TODO
    static void decode_seek(VgmStream vgmstream, int sample) {
        decode_state_reset(vgmstream);

//        if (vgmstream.codec_data == null)
//            return;
//
//        codec_info_t codec_info = codec_get_info(vgmstream);
//        if (codec_info != null) {
//            codec_info.seek(vgmstream, sample);
//            return;
//        }
//
//        if (vgmstream.coding_type == coding_CIRCUS_VQ) {
//            seek_circus_vq(vgmstream.codec_data, sample);
//        }
//
//        if (vgmstream.coding_type == coding_ICE_RANGE ||
//                vgmstream.coding_type == coding_ICE_DCT) {
//            seek_ice(vgmstream.codec_data, sample);
//        }
//
//        if (vgmstream.coding_type == coding_UBI_ADPCM) {
//            seek_ubi_adpcm(vgmstream.codec_data, sample);
//        }
//
//        if (vgmstream.coding_type == coding_ONGAKUKAN_ADPCM) {
//            seek_ongakukan_adp(vgmstream.codec_data, sample);
//        }
//
//        if (vgmstream.coding_type == coding_EA_MT) {
//            seek_ea_mt(vgmstream, sample);
//        }
//
//#if defined(VGM_USE_MP4V2) && defined(VGM_USE_FDKAAC)
//        if (vgmstream.coding_type == coding_MP4_AAC) {
//            seek_mp4_aac(vgmstream, sample);
//        }
//#endif
//
//        if (vgmstream.coding_type == coding_NWA) {
//            seek_nwa(vgmstream.codec_data, sample);
//        }
    }

    /**
     * Represents a channel for ADPCM streaming
     */
    public static class VGMStreamChannel {

        public int offset;
        public int adpcmHistory1_16;
        public int adpcmHistory2_16;
        public int adpcmHistory1_32;
        public int adpcmHistory2_32;
        public SeekableDataInputStream streamFile;
        public int channel;

        public VGMStreamChannel(SeekableDataInputStream streamFile, int offset, int channel) throws IOException {
            this.streamFile = new SeekableDataInputStream(streamFile.origin()) {
                @Override
                public void position(long pos) throws IOException {
System.out.printf("PS-ADPCM: frame_offset=%x\n", pos);
                    super.position(offset + pos * 2 + channel);
                }

                @Override
                public int readNBytes(byte[] b, int off, int len) throws IOException {
                    byte[] buf = new byte[(len - off) * 2];
                    int r = super.readNBytes(buf, 0, buf.length);
                    for (int i = 0; i < r / 2; i ++) {
                        b[i] = buf[i * 2];
                    }
                    return r / 2;
                }
            };
            this.offset = offset;
            this.adpcmHistory1_32 = 0;
            this.adpcmHistory2_32 = 0;
            this.channel = channel;
        }
    }

    /**
     * Clamps a 32-bit value to 16-bit signed range
     */
    private static short clamp16(int sample) {
        if (sample > 32767) return 32767;
        if (sample < -32768) return -32768;
        return (short) sample;
    }

    /**
     * Get the signed value from high nibble
     */
    private static int getHighNibbleSigned(int value) {
        int nibble = (value >> 4) & 0x0F;
        return (nibble >= 8) ? nibble - 16 : nibble;
    }

    /**
     * Get the signed value from low nibble
     */
    private static int getLowNibbleSigned(int value) {
        int nibble = value & 0x0F;
        return (nibble >= 8) ? nibble - 16 : nibble;
    }

    /**
     * Decodes Sony's PS-ADPCM (sometimes called SPU-ADPCM or VAG, just "ADPCM" in the SDK docs).
     * Very similar to XA ADPCM.
     *
     * @param stream           The stream channel to decode from
     * @param outbuf           The output buffer to write the decoded samples to
     * @param channelSpacing   The spacing between channels in the output buffer
     * @param firstSample      The first sample to decode
     * @param samplesToProcess The number of samples to decode
     * @param isBadFlags       Whether to ignore flags
     * @param config           Configuration option (1 for extended mode)
     * @throws IOException If reading from the stream file fails
     */
    public static void decodePSX(VGMStreamChannel stream, short[] outbuf, int channelSpacing,
                                 int firstSample, int samplesToProcess, boolean isBadFlags, int config) throws IOException {
        byte[] frame = new byte[0x10];
        int frameOffset;
        int i, framesIn, sampleCount = 0;
        int bytesPerFrame, samplesPerFrame;
        int coefIndex, shiftFactor, flag;
        int hist1 = stream.adpcmHistory1_32;
        int hist2 = stream.adpcmHistory2_32;
        boolean extendedMode = (config == 1);

        // external interleave (fixed size), mono
        bytesPerFrame = 0x10;
        samplesPerFrame = (bytesPerFrame - 0x02) * 2; // always 28
        framesIn = firstSample / samplesPerFrame;
        firstSample = firstSample % samplesPerFrame;

        // parse frame header
        frameOffset = stream.offset + bytesPerFrame * framesIn;
        stream.streamFile.position(frameOffset);
//logger.log(Level.DEBUG, "frameOffset: %d, bytesPerFrame: %d, samplesToProcess: %d, buf: %d".formatted(frameOffset, bytesPerFrame, samplesToProcess, outbuf.length));
        int r = stream.streamFile.readNBytes(frame, 0, bytesPerFrame); // ignore EOF errors
        if (r != bytesPerFrame) {
logger.log(Level.WARNING, "offset: %d, read underflow: %d / %d".formatted(frameOffset, r, bytesPerFrame));
        }
        coefIndex = (frame[0] >> 4) & 0x0F;
        shiftFactor = frame[0] & 0x0F;
        flag = frame[1] & 0xFF; // only lower nibble needed

        // upper filters only used in few PS3 games, normally 0
        if (!extendedMode) {
            // assert coefIndex > 5 || shiftFactor > 12 : "PS-ADPCM: incorrect coefs/shift at %x".formatted(frameOffset);
//if (!(coefIndex > 5 || shiftFactor > 12)) { logger.log(Level.WARNING, "PS-ADPCM: incorrect coefs/shift at %x".formatted(frameOffset)); }
            if (coefIndex > 5)
                coefIndex = 0;
            if (shiftFactor > 12)
                shiftFactor = 9; // supposedly, from Nocash PSX docs
        }

        if (isBadFlags) // some games store garbage or extra internal logic in the flags, must be ignored
            flag = 0;
        // assert flag > 7 : "PS-ADPCM: unknown flag at %x".formatted(frameOffset);
//if (!(flag > 7)) { logger.log(Level.WARNING, "PS-ADPCM: unknown flag at %x".formatted(frameOffset)); }

        shiftFactor = 20 - shiftFactor;
        // decode nibbles
        for (i = firstSample; i < firstSample + samplesToProcess; i++) {
            int sample = 0;

            if (flag < 0x07) { // with flag 0x07 decoded sample must be 0
                int nibbles = frame[0x02 + i / 2] & 0xff;

                if ((i & 1) != 0) { // high nibble first
                    sample = getHighNibbleSigned(nibbles) << shiftFactor; // scale
                } else {
                    sample = getLowNibbleSigned(nibbles) << shiftFactor; // scale
                }
                sample = sample + (int) ((PS_ADPCM_COEFS_F[coefIndex][0] * hist1 + PS_ADPCM_COEFS_F[coefIndex][1] * hist2) * 256.0f);
                sample >>= 8;
            }

            outbuf[sampleCount] = clamp16(sample); // clamping
            sampleCount += channelSpacing;

            hist2 = hist1;
            hist1 = sample;
        }

        stream.adpcmHistory1_32 = hist1;
        stream.adpcmHistory2_32 = hist2;
    }

    /**
     * PS-ADPCM with configurable frame size and no flag (int math version).
     * Found in some PC/PS3 games.
     *
     * @param stream           The stream channel to decode from
     * @param outbuf           The output buffer to write the decoded samples to
     * @param channelSpacing   The spacing between channels in the output buffer
     * @param firstSample      The first sample to decode
     * @param samplesToProcess The number of samples to decode
     * @param frameSize        The size of each frame
     * @param config           Configuration option (1 for extended/float mode)
     * @throws IOException If reading from the stream file fails
     */
    public static void decodePSXConfigurable(VGMStreamChannel stream, short[] outbuf, int channelSpacing,
                                             int firstSample, int samplesToProcess, int frameSize, int config) throws IOException {
        byte[] frame = new byte[0x50];
        int frameOffset;
        int i, framesIn, sampleCount = 0;
        int bytesPerFrame, samplesPerFrame;
        int coefIndex, shiftFactor;
        int hist1 = stream.adpcmHistory1_32;
        int hist2 = stream.adpcmHistory2_32;
        boolean extendedMode = (config == 1);
        boolean floatMode = (config == 1);

        // external interleave (variable size), mono
        bytesPerFrame = frameSize;
        samplesPerFrame = (bytesPerFrame - 0x01) * 2;
        framesIn = firstSample / samplesPerFrame;
        firstSample = firstSample % samplesPerFrame;

        // parse frame header
        frameOffset = stream.offset + bytesPerFrame * framesIn;
        stream.streamFile.read(frame, frameOffset, bytesPerFrame); // ignore EOF errors
        coefIndex = (frame[0] >> 4) & 0x0F;
        shiftFactor = frame[0] & 0x0F;

        // upper filters only used in few PS3 games, normally 0
        if (!extendedMode) {
            // VGM_ASSERT_ONCE(coefIndex > 5 || shiftFactor > 12, "PS-ADPCM: incorrect coefs/shift at %x\n", frameOffset);
            if (coefIndex > 5)
                coefIndex = 0;
            if (shiftFactor > 12)
                shiftFactor = 9; // supposedly, from Nocash PSX docs
        }

        // decode nibbles
        for (i = firstSample; i < firstSample + samplesToProcess; i++) {
            int sample = 0;
            int nibbles = frame[0x01 + i / 2] & 0xFF;

            if ((i & 1) != 0) { // high nibble
                sample = (nibbles >> 4) & 0x0F;
            } else { // low nibble
                sample = nibbles & 0x0F;
            }
            sample = (short) (((sample << 12) & 0xF000) >> shiftFactor); // 16b sign extend + scale

            if (floatMode) {
                sample = (int) (sample + PS_ADPCM_COEFS_F[coefIndex][0] * hist1 + PS_ADPCM_COEFS_F[coefIndex][1] * hist2);
            } else {
                sample = sample + ((PS_ADPCM_COEFS_I[coefIndex][0] * hist1 + PS_ADPCM_COEFS_I[coefIndex][1] * hist2) >> 6);
            }
            sample = clamp16(sample);

            outbuf[sampleCount] = (short) sample;
            sampleCount += channelSpacing;

            hist2 = hist1;
            hist1 = sample;
        }

        stream.adpcmHistory1_32 = hist1;
        stream.adpcmHistory2_32 = hist2;
    }

    /**
     * PS-ADPCM from Pivotal games, exactly like psx_cfg but with float math
     *
     * @param stream           The stream channel to decode from
     * @param outbuf           The output buffer to write the decoded samples to
     * @param channelSpacing   The spacing between channels in the output buffer
     * @param firstSample      The first sample to decode
     * @param samplesToProcess The number of samples to decode
     * @param frameSize        The size of each frame
     * @throws IOException If reading from the stream file fails
     */
    public static void decodePSXPivotal(VGMStreamChannel stream, short[] outbuf, int channelSpacing,
                                        int firstSample, int samplesToProcess, int frameSize) throws IOException {
        byte[] frame = new byte[0x50];
        int frameOffset;
        int i, framesIn, sampleCount = 0;
        int bytesPerFrame, samplesPerFrame;
        int coefIndex, shiftFactor;
        int hist1 = stream.adpcmHistory1_32;
        int hist2 = stream.adpcmHistory2_32;

        // external interleave (variable size), mono
        bytesPerFrame = frameSize;
        samplesPerFrame = (bytesPerFrame - 0x01) * 2;
        framesIn = firstSample / samplesPerFrame;
        firstSample = firstSample % samplesPerFrame;

        // parse frame header
        frameOffset = stream.offset + bytesPerFrame * framesIn;
        stream.streamFile.read(frame, frameOffset, bytesPerFrame); // ignore EOF errors
        coefIndex = (frame[0] >> 4) & 0x0F;
        shiftFactor = frame[0] & 0x0F;

        assert coefIndex > 5 || shiftFactor > 12 : "PS-ADPCM-piv: incorrect coefs/shift";
        if (coefIndex > 5) // just in case
            coefIndex = 5;
        if (shiftFactor > 12) // same
            shiftFactor = 12;

        shiftFactor = 20 - shiftFactor;
        // decode nibbles
        for (i = firstSample; i < firstSample + samplesToProcess; i++) {
            int sample = 0;
            int nibbles = frame[0x01 + i / 2] & 0xFF;

            if ((i & 1) != 0) { // high nibble
                sample = getHighNibbleSigned(nibbles) << shiftFactor; // scale
            } else {
                sample = getLowNibbleSigned(nibbles) << shiftFactor; // scale
            }
            sample = sample + (int) ((PS_ADPCM_COEFS_F[coefIndex][0] * hist1 + PS_ADPCM_COEFS_F[coefIndex][1] * hist2) * 256.0f);
            sample >>= 8;

            outbuf[sampleCount] = clamp16(sample); // clamping
            sampleCount += channelSpacing;

            hist2 = hist1;
            hist1 = sample;
        }

        stream.adpcmHistory1_32 = hist1;
        stream.adpcmHistory2_32 = hist2;
    }

    /**
     * Find loop samples in PS-ADPCM data and return if the file loops.
     * <p>
     * PS-ADPCM/VAG has optional bit flags that control looping in the SPU:
     * - 0x0 (0000): Normal decode
     * - 0x1 (0001): End marker (last frame)
     * - 0x2 (0010): Loop region (marks files that *may* have loop flags somewhere)
     * - 0x3 (0011): Loop end (jump to loop address)
     * - 0x4 (0100): Start marker
     * - 0x5 (0101): Same as 0x07? Extremely rare
     * - 0x6 (0110): Loop start (save loop address)
     * - 0x7 (0111): End marker and don't decode
     * - 0x8+(1NNN): Not valid
     */
    private static boolean findStreamInfoInternal(DataInputStream sf, int startOffset, int dataSize,
                                                  int channels, int interleave, int[] loopStart, int[] loopEnd, int[] streamSize, int config) throws IOException {
        int numSamples = 0, loopStartSample = 0, loopEndSample = 0;
        boolean loopStartFound = false, loopEndFound = false;
        int offset = startOffset;
        int maxOffset = startOffset + dataSize;
        int interleaveConsumed = 0;
        boolean detectFullLoops = (config & 1) != 0;
        boolean stopOnNull = (config & 2) != 0;
        int frames = 0;

        if (dataSize == 0 || channels == 0 || (channels > 1 && interleave == 0))
            return false;

        while (offset < maxOffset) {
            sf.skipBytes(offset);
            int header = sf.readUnsignedShort();
            int flag = header & 0x0f; // lower nibble only (for HEVAG)
            frames++;

            if (flag == 0x06 && !loopStartFound) {
                loopStartSample = numSamples; // loop start before this frame
                loopStartFound = true;
            }

            if (flag == 0x03 && loopEndSample == 0) {
                loopEndSample = numSamples + 28; // loop end after this frame
                loopEndFound = true;

                // ignore strange case in Commandos (PS2), has many loop starts and ends
                sf.skipBytes(0x11 - Short.BYTES);
                if (channels == 1
                        && offset + 0x10 < maxOffset
                        && (sf.readUnsignedByte() & 0x0f) == 0x06) {
                    loopEndSample = 0;
                    loopEndFound = false;
                }

                if (loopStartFound && loopEndFound)
                    break;
            }

            // hack for some games that don't have loop points but do full loops
            if (flag == 0x01 && detectFullLoops) {
                byte[] eof = {(byte) 0xff, (byte) 0x07, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
                byte[] buf = new byte[0x10];
                int hdr = (header >> 8) & 0xFF;

                int read = sf.read(buf, offset + 0x10, buf.length);
                if (read > 0
                        && buf[0] != 0x00 // ignore blank frame
                        && buf[0] != 0x0c // ignore silent frame
                        && buf[0] != 0x3c // ignore some L-R tracks with different end flags
                        && buf[0] != 0x1c // ignore some L-R tracks with different end flags
                ) {

                    // assume full loop with repeated frame header and null frame
                    boolean matches = true;
                    if (hdr == (buf[0] & 0xFF)) {
                        for (int i = 1; i < buf.length; i++) {
                            if (buf[i] != eof[i]) {
                                matches = false;
                                break;
                            }
                        }

                        if (matches) {
                            loopStartSample = 28; // skip first frame as it's null in PS-ADPCM
                            loopEndSample = numSamples + 28; // loop end after this frame
                            loopStartFound = true;
                            loopEndFound = true;
                            break;
                        }
                    }
                }
            }

            numSamples += 28;
            offset += 0x10;

            // skip other channels
            interleaveConsumed += 0x10;
            if (interleaveConsumed == interleave) {
                interleaveConsumed = 0;
                offset += interleave * (channels - 1);
            }

            // stream done flag
            if (stopOnNull && offset > startOffset && (flag & 0x01) != 0) {
                frames++;
                break;
            }
        }

        if (streamSize != null) {
            // uses frames rather than offsets to take interleave into account
            streamSize[0] = frames * 0x10 * channels;
        }

        // From Sony's docs: if only loop_end is set loop back to "phoneme region start", but in practice doesn't
        if (loopStartFound && loopEndFound) {
            loopStart[0] = loopStartSample;
            loopEnd[0] = loopEndSample;
            return true;
        }

        return false; // no loop
    }

    /**
     * Find loop samples in PS-ADPCM data (basic version)
     */
    public static boolean findLoopOffsets(DataInputStream sf, int startOffset, int dataSize,
                                          int channels, int interleave, int[] loopStart, int[] loopEnd) throws IOException {
        return findStreamInfoInternal(sf, startOffset, dataSize, channels, interleave, loopStart, loopEnd, null, 0x00);
    }

    /**
     * Find loop samples in PS-ADPCM data (with full loops detection)
     */
    public static boolean findLoopOffsetsFull(DataInputStream sf, int startOffset, int dataSize,
                                              int channels, int interleave, int[] loopStart, int[] loopEnd) throws IOException {
        return findStreamInfoInternal(sf, startOffset, dataSize, channels, interleave, loopStart, loopEnd, null, 0x01);
    }

    /**
     * Find stream info including loop points and stream size
     */
    public static boolean findStreamInfo(DataInputStream sf, int startOffset, int dataSize,
                                         int channels, int interleave, int[] loopStart, int[] loopEnd, int[] streamSize) throws IOException {
        return findStreamInfoInternal(sf, startOffset, dataSize, channels, interleave, loopStart, loopEnd, streamSize, 0x02);
    }

    /**
     * Find padding in PS-ADPCM stream
     */
    public static int findPadding(DataInputStream sf, int startOffset, int dataSize,
                                  int channels, int interleave, boolean discardEmpty) throws IOException {
        int minOffset, offset, readOffset;
        int frameSize = 0x10;
        int paddingSize = 0;
        int interleaveConsumed = 0;
        byte[] buf = new byte[0x8000];
        int bufPos = 0;
        int bytes;

        if (dataSize == 0 || channels == 0 || (channels > 1 && interleave == 0))
            return 0;

        offset = startOffset + dataSize;

        // in rare cases (ex. Gitaroo Man) channels have inconsistent empty padding, use first as guide
        offset = offset - interleave * (channels - 1);

        // some files have padding spanning multiple interleave blocks
        minOffset = startOffset; // offset - interleave;
        readOffset = 0;

        while (offset > minOffset) {
            int f1, f2, f3, f4;
            int flag;
            boolean isEmpty = false;

            // read in chunks to optimize (less SF rebuffering since we go in reverse)
            if (offset < readOffset || bufPos <= 0) {
                readOffset = offset - buf.length;
                if (readOffset < 0)
                    readOffset = 0; // ?
                bytes = sf.read(buf, readOffset, buf.length);
                bufPos = (bytes / (int) frameSize * (int) frameSize);
            }

            bufPos -= (int) frameSize;
            offset -= frameSize;

            ByteBuffer byteBuffer = ByteBuffer.wrap(buf, bufPos, (int) frameSize).order(ByteOrder.BIG_ENDIAN);
            f1 = byteBuffer.getInt();
            f2 = byteBuffer.getInt();
            f3 = byteBuffer.getInt();
            f4 = byteBuffer.getInt();
            flag = (f1 >> 16) & 0xff;

            if (f1 == 0 && f2 == 0 && f3 == 0 && f4 == 0)
                isEmpty = true;

            if (!isEmpty && discardEmpty) {
                if (flag == 0x07 || flag == 0x77)
                    isEmpty = true; // 'discard frame' flag
                else if ((f1 & 0xff00_ffff) == 0 && f2 == 0 && f3 == 0 && f4 == 0)
                    isEmpty = true; // silent with flags (typical for looping files)
                else if ((f1 & 0xff00_ffff) == 0x0c00_0000 && f2 == 0 && f3 == 0 && f4 == 0)
                    isEmpty = true; // silent (maybe shouldn't ignore flag 0x03?)
                else if ((f1 & 0x0000_ffff) == 0x0000_7777 && f2 == 0x7777_7777 && f3 == 0x7777_7777 && f4 == 0x7777_7777)
                    isEmpty = true; // silent-ish
            }

            if (!isEmpty)
                break;

            paddingSize += frameSize * channels;

            // skip other channels
            interleaveConsumed += 0x10;
            if (interleaveConsumed == interleave) {
                interleaveConsumed = 0;
                offset -= interleave * (channels - 1);
                bufPos -= interleave * (channels - 1);
            }
        }

        return paddingSize;
    }

    /**
     * Convert bytes to samples
     */
    public static int bytesToSamples(int bytes, int channels) {
        if (channels <= 0) return 0;
        return bytes / channels / 0x10 * 28;
    }

    /**
     * Convert bytes to samples for configurable frame size
     */
    public static int cfgBytesToSamples(int bytes, int frameSize, int channels) {
        int samplesPerFrame = (frameSize - 0x01) * 2;
        return bytes / channels / frameSize * samplesPerFrame;
    }

    /**
     * Test PS-ADPCM frames for correctness
     */
    public static boolean checkFormat(SeekableDataInputStream sf, int sfSize, int offset, int max) throws IOException {
        int maxOffset = offset + max;
        if (maxOffset > sfSize)
            maxOffset = sfSize;

        sf.position(offset);
        while (offset < maxOffset) {
            int predictor = (sf.readUnsignedByte() >> 4) & 0x0f;
            int flags = sf.readUnsignedByte();

            if (predictor > 5 || flags > 7) {
logger.log(Level.DEBUG, "Predictor: %d, flags: %d".formatted(predictor, flags));
                return false;
            }
            offset += 0x10;
            sf.position(offset);
        }

        return true;
    }
}