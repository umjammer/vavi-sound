/*
 * This program(except FFT and Bessel function part) is distributed under
 * LGPL. See LGPL.txt for details. But, if you make a new program with derived
 * code from this program,I strongly wish that my name and derived code are
 * indicated explicitly.
 */

package vavi.sound.pcm.resampling.ssrc;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

import vavi.util.I0Bessel;

import static vavi.util.SplitRadixFft.rdft;


/**
 * Shibatch Sampling Rate Converter.
 *
 * TODO 2pass 1st phase use pipe
 *
 * @author <a href="mailto:shibatch@users.sourceforge.net">Naoki Shibata</a>
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 1.30 060127 nsano port to java version <br>
 */
public class SSRC {

    /** */
    private static final Logger logger = Logger.getLogger(SSRC.class.getName());

    /** */
    private static final ByteOrder byteOrder = ByteOrder.LITTLE_ENDIAN;

    /** */
    private static final String VERSION = "1.30";

    /** */
    private double AA = 170;

    /** */
    private double DF = 100;

    /** */
    private int FFTFIRLEN = 65536;

    /** */
//  private static final int M = 15;

    /** */
    private static int round(double x) {
        return x >= 0 ? (int) (x + 0.5) : (int) (x - 0.5);
    }

    /** */
    private boolean quiet = false;

    /** */
    private int lastShowed2;

    /** */
    private long startTime, lastShowed;

    /** */
    private static class Shaper {

        /** */
        private static final int RANDBUFLEN = 65536;

        /** */
        private static final int[] sCoefFreq = {
                0, 48000, 44100, 37800, 32000, 22050, 48000, 44100
        };

        /** */
        private static final int[] sCoefLen = {
                1, 16, 20, 16, 16, 15, 16, 15
        };

        /** */
        private static final int[] samp = {
                8, 18, 27, 8, 8, 8, 10, 9
        };

        /** */
        private static final double[][] shaperCoefs = {
                { -1 }, // triangular dither

                {   -2.8720729351043701172,   5.0413231849670410156,  -6.2442994117736816406,   5.8483986854553222656,
                        -3.7067542076110839844,   1.0495119094848632812,   1.1830236911773681641,  -2.1126792430877685547,
                        1.9094531536102294922,  -0.99913084506988525391,  0.17090806365013122559,  0.32615602016448974609,
                        -0.39127644896507263184,  0.26876461505889892578, -0.097676105797290802002, 0.023473845794796943665,
                }, // 48k, N=16, amp=18

                {   -2.6773197650909423828,   4.8308925628662109375,  -6.570110321044921875,    7.4572014808654785156,
                        -6.7263274192810058594,   4.8481650352478027344,  -2.0412089824676513672,  -0.7006359100341796875,
                        2.9537565708160400391,  -4.0800385475158691406,   4.1845216751098632812,  -3.3311812877655029297,
                        2.1179926395416259766,  -0.879302978515625,       0.031759146600961685181, 0.42382788658142089844,
                        -0.47882103919982910156,  0.35490813851356506348, -0.17496839165687561035,  0.060908168554306030273,
                }, // 44.1k, N=20, amp=27

                {   -1.6335992813110351562,   2.2615492343902587891,  -2.4077029228210449219,   2.6341717243194580078,
                        -2.1440362930297851562,   1.8153258562088012695,  -1.0816224813461303711,   0.70302653312683105469,
                        -0.15991993248462677002, -0.041549518704414367676, 0.29416576027870178223, -0.2518316805362701416,
                        0.27766478061676025391, -0.15785403549671173096,  0.10165894031524658203, -0.016833892092108726501,
                }, // 37.8k, N=16

                {   -0.82901298999786376953,  0.98922657966613769531, -0.59825712442398071289,  1.0028809309005737305,
                        -0.59938216209411621094,  0.79502451419830322266, -0.42723315954208374023,  0.54492527246475219727,
                        -0.30792605876922607422,  0.36871799826622009277, -0.18792048096656799316,  0.2261127084493637085,
                        -0.10573341697454452515,  0.11435490846633911133, -0.038800679147243499756, 0.040842197835445404053,
                }, // 32k, N=16

                {   -0.065229974687099456787, 0.54981261491775512695,  0.40278548002243041992,  0.31783768534660339355,
                        0.28201797604560852051,  0.16985194385051727295,  0.15433363616466522217,  0.12507140636444091797,
                        0.08903945237398147583,  0.064410120248794555664, 0.047146003693342208862, 0.032805237919092178345,
                        0.028495194390416145325, 0.011695005930960178375, 0.011831838637590408325,
                }, // 22.05k, N=15

                {   -2.3925774097442626953,   3.4350297451019287109,  -3.1853709220886230469,   1.8117271661758422852,
                        0.20124770700931549072, -1.4759907722473144531,   1.7210904359817504883,  -0.97746700048446655273,
                        0.13790138065814971924,  0.38185903429985046387, -0.27421241998672485352, -0.066584214568138122559,
                        0.35223302245140075684, -0.37672343850135803223,  0.23964276909828186035, -0.068674825131893157959,
                }, // 48k, N=16, amp=10

                {   -2.0833916664123535156,   3.0418450832366943359,  -3.2047898769378662109,   2.7571926116943359375,
                        -1.4978630542755126953,   0.3427594602108001709,   0.71733748912811279297, -1.0737057924270629883,
                        1.0225815773010253906,  -0.56649994850158691406,  0.20968692004680633545,  0.065378531813621520996,
                        -0.10322438180446624756,  0.067442022264003753662, 0.00495197344571352005,
                }, // 44.1k, N=15, amp=9
        };

        /** */
        private double[][] shapeBuf;

        /** */
        private int shaperType, shaperLen, shaperClipMin, shaperClipMax;

        /** */
        private double[] randBuf;

        /** */
        private int randPtr;

        /**  */
        private static final int POOLSIZE = 97;

        /**  */
        private int initShaper(int freq, int nch, int min, int max, int dType, int pdf, double noiseAmp) {
            int i;
            int[] pool = new int[POOLSIZE];

            for (i = 1; i < 6; i++) {
                if (freq == sCoefFreq[i]) {
                    break;
                }
            }
            if ((dType == 3 || dType == 4) && i == 6) {
                logger.warning(String.format("ATH based noise shaping for destination frequency %dHz is not available, using triangular dither\n", freq));
            }
            if (dType == 2 || i == 6) {
                i = 0;
            }
            if (dType == 4 && (i == 1 || i == 2)) {
                i += 5;
            }

            shaperType = i;

            shapeBuf = new double[nch][];
            shaperLen = sCoefLen[shaperType];

            for (i = 0; i < nch; i++) {
                shapeBuf[i] = new double[shaperLen];
            }

            shaperClipMin = min;
            shaperClipMax = max;

            randBuf = new double[RANDBUFLEN];

            Random random = new Random(System.currentTimeMillis()); // TODO seed should be controlled for test
            for (i = 0; i < POOLSIZE; i++) {
                pool[i] = random.nextInt();
            }

            switch (pdf) {
            case 0: // rectangular
                for (i = 0; i < RANDBUFLEN; i++) {
                    int r, p;

                    p = random.nextInt() % POOLSIZE;
                    r = pool[p];
                    pool[p] = random.nextInt();
                    randBuf[i] = noiseAmp * (((double) r) / Integer.MAX_VALUE - 0.5);
                }
                break;

            case 1: // triangular
                for (i = 0; i < RANDBUFLEN; i++) {
                    int r1, r2, p;

                    p = random.nextInt() % POOLSIZE;
                    r1 = pool[p];
                    pool[p] = random.nextInt();
                    p = random.nextInt() % POOLSIZE;
                    r2 = pool[p];
                    pool[p] = random.nextInt();
                    randBuf[i] = noiseAmp * ((((double) r1) / Integer.MAX_VALUE) - (((double) r2) / Integer.MAX_VALUE));
                }
                break;

            case 2: // gaussian
            {
                int sw = 0;
                double t = 0, u = 0;

                for (i = 0; i < RANDBUFLEN; i++) {
                    double r;
                    int p;

                    if (sw == 0) {
                        sw = 1;

                        p = random.nextInt() % POOLSIZE;
                        r = ((double) pool[p]) / Integer.MAX_VALUE;
                        pool[p] = random.nextInt();
                        if (r == 1.0) {
                            r = 0.0;
                        }

                        t = Math.sqrt(-2 * Math.log(1 - r));

                        p = random.nextInt() % POOLSIZE;
                        r = ((double) pool[p]) / Integer.MAX_VALUE;
                        pool[p] = random.nextInt();

                        u = 2 * Math.PI * r;

                        randBuf[i] = noiseAmp * t * Math.cos(u);
                    } else {
                        sw = 0;

                        randBuf[i] = noiseAmp * t * Math.sin(u);
                    }
                }
            }
            break;
            }

            randPtr = 0;

            if (dType == 0 || dType == 1) {
                return 1;
            }
            return samp[shaperType];
        }

        /**  */
        private int doShaping(double s, double[] peak, int dtype, int ch) {
            double u, h;
            int i;

            if (dtype == 1) {
                s += randBuf[randPtr++ & (RANDBUFLEN - 1)];

                if (s < shaperClipMin) {
                    double d = s / shaperClipMin;
                    peak[0] = Math.max(peak[0], d);
                    s = shaperClipMin;
                }
                if (s > shaperClipMax) {
                    double d = s / shaperClipMax;
                    peak[0] = Math.max(peak[0], d);
                    s = shaperClipMax;
                }

                return round(s);
            }

            h = 0;
            for (i = 0; i < shaperLen; i++)
                h += shaperCoefs[shaperType][i] * shapeBuf[ch][i];
            s += h;
            u = s;
            s += randBuf[randPtr++ & (RANDBUFLEN - 1)];

            for (i = shaperLen - 2; i >= 0; i--)
                shapeBuf[ch][i + 1] = shapeBuf[ch][i];

            if (s < shaperClipMin) {
                double d = s / shaperClipMin;
                peak[0] = Math.max(peak[0], d);
                s = shaperClipMin;
                shapeBuf[ch][0] = s - u;

                if (shapeBuf[ch][0] > 1)
                    shapeBuf[ch][0] = 1;
                if (shapeBuf[ch][0] < -1)
                    shapeBuf[ch][0] = -1;
            } else if (s > shaperClipMax) {
                double d = s / shaperClipMax;
                peak[0] = Math.max(peak[0], d);
                s = shaperClipMax;
                shapeBuf[ch][0] = s - u;

                if (shapeBuf[ch][0] > 1)
                    shapeBuf[ch][0] = 1;
                if (shapeBuf[ch][0] < -1)
                    shapeBuf[ch][0] = -1;
            } else {
                s = round(s);
                shapeBuf[ch][0] = s - u;
            }

            return (int) s;
        }

        /**  */
        private void quitShaper(int nch) {
        }
    }

    /** */
    private Shaper shaper = new Shaper();

    /** */
    private static double alpha(double a) {
        if (a <= 21) {
            return 0;
        }
        if (a <= 50) {
            return 0.5842 * Math.pow(a - 21, 0.4) + 0.07886 * (a - 21);
        }
        return 0.1102 * (a - 8.7);
    }

    /** */
    private static double win(double n, int len, double alp, double iza) {
        return I0Bessel.value(alp * Math.sqrt(1 - 4 * n * n / (((double) len - 1) * ((double) len - 1)))) / iza;
    }

    /** */
    private static double sinc(double x) {
        return x == 0 ? 1 : Math.sin(x) / x;
    }

    /** */
    private static double hn_lpf(int n, double lpf, double fs) {
        double t = 1 / fs;
        double omega = 2 * Math.PI * lpf;
        return 2 * lpf * t * sinc(n * omega * t);
    }

    /** */
    private static void usage() {
        System.err.print("http://shibatch.sourceforge.net/\n\n");
        System.err.print("usage: ssrc [<options>] <source wav file> <destination wav file>\n");
        System.err.print("options : --rate <sampling rate>     output sample rate\n");
        System.err.print("          --att <attenuation(dB)>    attenuate signal\n");
        System.err.print("          --bits <number of bits>    output quantization bit length\n");
        System.err.print("          --tmpfile <file name>      specify temporal file\n");
        System.err.print("          --twopass                  two pass processing to avoid clipping\n");
        System.err.print("          --normalize                normalize the wave file\n");
        System.err.print("          --quiet                    nothing displayed except error\n");
        System.err.print("          --dither [<type>]          dithering\n");
        System.err.print("                                       0 : no dither\n");
        System.err.print("                                       1 : no noise shaping\n");
        System.err.print("                                       2 : triangular spectral shape\n");
        System.err.print("                                       3 : ATH based noise shaping\n");
        System.err.print("                                       4 : less dither amplitude than type 3\n");
        System.err.print("          --pdf <type> [<amp>]       select p.d.f. of noise\n");
        System.err.print("                                       0 : rectangular\n");
        System.err.print("                                       1 : triangular\n");
        System.err.print("                                       2 : Gaussian\n");
        System.err.print("          --profile <type>           specify profile\n");
        System.err.print("                                       standard : the default quality\n");
        System.err.print("                                       fast     : fast, not so bad quality\n");
    }

    /** */
    private static void error(int x) {
        System.err.printf("unknown error %d\n", x);
        System.exit(-1);
    }

    /** */
    private void setStartTime() {
        startTime = System.currentTimeMillis();
        lastShowed = 0;
        lastShowed2 = -1;
    }

    /** */
    private void showProgress(double p) {
        int eta, pc;
        long t;
        if (quiet) {
            return;
        }

        t = System.currentTimeMillis() - startTime;
        if (p == 0) {
            eta = 0;
        } else {
            eta = (int) (t * (1 - p) / p);
        }

        pc = (int) (p * 100);

        if (pc != lastShowed2 || t != lastShowed) {
            System.err.printf(" %3d%% processed", pc);
            lastShowed2 = pc;
        }
        if (t != lastShowed) {
            System.err.printf(", ETA =%4dmsec", eta);
            lastShowed = t;
        }
        System.err.println();
        System.err.flush();
    }

    /** */
    private static int gcd(int x, int y) {
        int t;

        while (y != 0) {
            t = x % y;
            x = y;
            y = t;
        }
        return x;
    }

    /** */
    abstract static class Resampler {
        int nch;
        int bps;
        int dbps;
        int sfrq;
        int dfrq;

        double gain;
        int chanklen;
        boolean twopass;
        int dither;

        void init(int nch, int bps, int dbps, int sfrq, int dfrq, double gain, int chanklen, boolean twopass, int dither) {
            this.nch = nch;
            this.bps = bps;
            this.dbps = dbps;
            this.sfrq = sfrq;
            this.dfrq = dfrq;

            this.gain = gain;
            this.chanklen = chanklen;
            this.twopass = twopass;
            this.dither = dither;
        }

        /**
         * @return bytes written
         */
        abstract int resample(ReadableByteChannel in, WritableByteChannel out) throws IOException;

        double peak;
    }

    /** */
    class Upsampler extends Resampler {

        /* */
        int resample(ReadableByteChannel fpi, WritableByteChannel fpo) throws IOException {
            int frqgcd, osf, fs1, fs2;
            double[][] stage1;
            double[] stage2;
            int n1, n1x, n1y, n2, n2b;
            int filter2len;
            int[] f1order, f1inc;
            int[] fft_ip;
            double[] fft_w;
            ByteBuffer rawinbuf, rawoutbuf;
            double[] inbuf, outbuf;
            double[][] buf1, buf2;
            double[] peak = new double[] { 0 };
            int spcount = 0;
            int i, j;
            int sumWritten = 0;

System.err.println("upsample");

            filter2len = FFTFIRLEN; // stage 2 filter length

            // Make stage 1 filter

            {
                double aa = AA; // stop band attenuation(dB)
                double lpf, d, df, alp, iza;
//              double delta;
                double guard = 2;

                frqgcd = gcd(sfrq, dfrq);

                fs1 = sfrq / frqgcd * dfrq;

                if (fs1 / dfrq == 1) {
                    osf = 1;
                } else if (fs1 / dfrq % 2 == 0) {
                    osf = 2;
                } else if (fs1 / dfrq % 3 == 0) {
                    osf = 3;
                } else {
                    throw new IllegalArgumentException(
                        String.format("Resampling from %dHz to %dHz is not supported.\n" +
                                      "%d/gcd(%d,%d)=%d must be divided by 2 or 3.\n",
                                      sfrq, dfrq, sfrq, sfrq, dfrq, fs1 / dfrq));
                }

                df = (dfrq * osf / 2f - sfrq / 2f) * 2 / guard;
                lpf = sfrq / 2f + (dfrq * osf / 2f - sfrq / 2f) / guard;

//              delta = Math.pow(10, -aa / 20);
                if (aa <= 21) {
                    d = 0.9222;
                } else {
                    d = (aa - 7.95) / 14.36;
                }

                n1 = (int) (fs1 / df * d + 1);
                if (n1 % 2 == 0) {
                    n1++;
                }

                alp = alpha(aa);
                iza = I0Bessel.value(alp);
//System.err.printf("iza = %g\n",iza);

                n1y = fs1 / sfrq;
                n1x = n1 / n1y + 1;

                f1order = new int[n1y * osf];
                for (i = 0; i < n1y * osf; i++) {
                    f1order[i] = fs1 / sfrq - (i * (fs1 / (dfrq * osf))) % (fs1 / sfrq);
                    if (f1order[i] == fs1 / sfrq) {
                        f1order[i] = 0;
                    }
                }

                f1inc = new int[n1y * osf];
                for (i = 0; i < n1y * osf; i++) {
                    f1inc[i] = f1order[i] < fs1 / (dfrq * osf) ? nch : 0;
                    if (f1order[i] == fs1 / sfrq) {
                        f1order[i] = 0;
                    }
                }

                stage1 = new double[n1y][n1x];

                for (i = -(n1 / 2); i <= n1 / 2; i++) {
                    stage1[(i + n1 / 2) % n1y][(i + n1 / 2) / n1y] = win(i, n1, alp, iza) * hn_lpf(i, lpf, fs1) * fs1 / sfrq;
                }
            }

            // Make stage 2 filter

            {
                double aa = AA; // stop band attenuation(dB)
                double lpf, d, df, alp, iza;
//              double delta;
                int ipsize, wsize;

//              delta = Math.pow(10, -aa / 20);
                if (aa <= 21) {
                    d = 0.9222;
                } else {
                    d = (aa - 7.95) / 14.36;
                }

                fs2 = dfrq * osf;

                for (i = 1;; i = i * 2) {
                    n2 = filter2len * i;
                    if (n2 % 2 == 0) {
                        n2--;
                    }
                    df = (fs2 * d) / (n2 - 1);
                    lpf = sfrq / 2f;
                    if (df < DF) {
                        break;
                    }
                }

                alp = alpha(aa);

                iza = I0Bessel.value(alp);

                for (n2b = 1; n2b < n2; n2b *= 2) {
                }
                n2b *= 2;

                stage2 = new double[n2b];

                for (i = -(n2 / 2); i <= n2 / 2; i++) {
                    stage2[i + n2 / 2] = win(i, n2, alp, iza) * hn_lpf(i, lpf, fs2) / n2b * 2;
                }

                ipsize = (int) (2 + Math.sqrt(n2b));
                fft_ip = new int[ipsize];
                fft_ip[0] = 0;
                wsize = n2b / 2;
                fft_w = new double[wsize];

                rdft(n2b, 1, stage2, fft_ip, fft_w);
            }

            // Apply filters

            setStartTime();

            {
                int n2b2 = n2b / 2;
                // inbufのfs1での次に読むサンプルの場所を保持
                int rp;
                // 次にdisposeするsfrqでのサンプル数
                int ds;
                // 実際にファイルからinbufに読み込まれた値から計算した stage2 filterに渡されるサンプル数
                int nsmplwrt1;
                // 実際にファイルからinbufに読み込まれた値から計算した stage2 filterに渡されるサンプル数
                int nsmplwrt2 = 0;
                // stage1 filterから出力されたサンプルの数をn1y*osfで割った余り
                int s1p;
                boolean init;
                boolean ending;
                int sumread, sumwrite;
                int osc;
                int ip, ip_backup;
                int s1p_backup, osc_backup;
                int ch, p;
                int inbuflen;
                int delay;

                buf1 = new double[nch][n2b2 / osf + 1];

                buf2 = new double[nch][n2b];

                rawinbuf = ByteBuffer.allocate(nch * (n2b2 + n1x) * bps); // ,bps
                rawoutbuf = ByteBuffer.allocate(nch * (n2b2 / osf + 1) * dbps); // ,dbps

                inbuf = new double[nch * (n2b2 + n1x)];
                outbuf = new double[nch * (n2b2 / osf + 1)];

                s1p = 0;
                rp = 0;
                ds = 0;
                osc = 0;

                init = true;
                ending = false;
                inbuflen = n1 / 2 / (fs1 / sfrq) + 1;
                delay = (int) ((double) n2 / 2 / (fs2 / dfrq));

                sumread = sumwrite = 0;

                while (true) {
                    int nsmplread, toberead, toberead2;

                    toberead2 = toberead = (int) (Math.floor((double) n2b2 * sfrq / (dfrq * osf)) + 1 + n1x - inbuflen);
                    if (toberead + sumread > chanklen) {
                        toberead = chanklen - sumread;
                    }

                    rawinbuf.position(0);
                    rawinbuf.limit(bps * nch * toberead);
                    nsmplread = fpi.read(rawinbuf);
                    rawinbuf.flip();
                    nsmplread /= bps * nch;

                    switch (bps) {
                    case 1:
                        for (i = 0; i < nsmplread * nch; i++)
                            inbuf[nch * inbuflen + i] = (1 / (double) 0x7f) * ((double) rawinbuf.get(i) - 128);
                        break;

                    case 2:
                        for (i = 0; i < nsmplread * nch; i++) {
                            int v = rawinbuf.order(byteOrder).asShortBuffer().get(i);
                            inbuf[nch * inbuflen + i] = (1 / (double) 0x7fff) * v;
                        }
                        break;

                    case 3:
                        for (i = 0; i < nsmplread * nch; i++) {
                            inbuf[nch * inbuflen + i] = (1 / (double) 0x7fffff) *
                            (((rawinbuf.get(i * 3    ) & 0xff) <<  0) |
                             ((rawinbuf.get(i * 3 + 1) & 0xff) <<  8) |
                             ((rawinbuf.get(i * 3 + 2) & 0xff) << 16));
                        }
                        break;

                    case 4:
                        for (i = 0; i < nsmplread * nch; i++) {
                            int v = rawinbuf.order(byteOrder).asIntBuffer().get(i);
                            inbuf[nch * inbuflen + i] = (1 / (double) 0x7fffffff) * v;
                        }
                        break;
                    }

                    for (; i < nch * toberead2; i++) {
                        inbuf[nch * inbuflen + i] = 0;
                    }

                    inbuflen += toberead2;

                    sumread += nsmplread;

                    ending = nsmplread <= 0 || sumread >= chanklen;

//                  nsmplwrt1 = ((rp - 1) * sfrq / fs1 + inbuflen - n1x) * dfrq * osf / sfrq;
//                  if (nsmplwrt1 > n2b2) { nsmplwrt1 = n2b2; }
                    nsmplwrt1 = n2b2;

                    // apply stage 1 filter

                    ip = ((sfrq * (rp - 1) + fs1) / fs1) * nch; // inbuf

                    s1p_backup = s1p;
                    ip_backup = ip;
                    osc_backup = osc;

                    for (ch = 0; ch < nch; ch++) {
                        int op = ch; // outbuf
//                      int fdo = fs1 / (dfrq * osf);
                        int no = n1y * osf;

                        s1p = s1p_backup;
                        ip = ip_backup + ch;

                        switch (n1x) {
                        case 7:
                            for (p = 0; p < nsmplwrt1; p++) {
                                int s1o = f1order[s1p];

                                buf2[ch][p] =
                                    stage1[s1o][0] * inbuf[ip + 0 * nch] +
                                    stage1[s1o][1] * inbuf[ip + 1 * nch] +
                                    stage1[s1o][2] * inbuf[ip + 2 * nch] +
                                    stage1[s1o][3] * inbuf[ip + 3 * nch] +
                                    stage1[s1o][4] * inbuf[ip + 4 * nch] +
                                    stage1[s1o][5] * inbuf[ip + 5 * nch]+
                                    stage1[s1o][6] * inbuf[ip + 6 * nch];

                                ip += f1inc[s1p];

                                s1p++;
                                if (s1p == no) {
                                    s1p = 0;
                                }
                            }
                            break;

                        case 9:
                            for (p = 0; p < nsmplwrt1; p++) {
                                int s1o = f1order[s1p];

                                buf2[ch][p] =
                                    stage1[s1o][0] * inbuf[ip + 0 * nch] +
                                    stage1[s1o][1] * inbuf[ip + 1 * nch] +
                                      stage1[s1o][2] * inbuf[ip + 2 * nch] +
                                      stage1[s1o][3] * inbuf[ip + 3 * nch] +
                                      stage1[s1o][4] * inbuf[ip + 4 * nch] +
                                      stage1[s1o][5] * inbuf[ip + 5 * nch] +
                                      stage1[s1o][6] * inbuf[ip + 6 * nch] +
                                      stage1[s1o][7] * inbuf[ip + 7 * nch] +
                                      stage1[s1o][8] * inbuf[ip + 8 * nch];

                                ip += f1inc[s1p];

                                s1p++;
                                if (s1p == no) {
                                    s1p = 0;
                                }
                            }
                            break;

                        default:
                            for (p = 0; p < nsmplwrt1; p++) {
                                double tmp = 0;
                                int ip2 = ip;

                                int s1o = f1order[s1p];

                                for (i = 0; i < n1x; i++) {
                                    tmp += stage1[s1o][i] * inbuf[ip2];
                                    ip2 += nch;
                                }
                                buf2[ch][p] = tmp;

                                ip += f1inc[s1p];

                                s1p++;
                                if (s1p == no) {
                                    s1p = 0;
                                }
                            }
                            break;
                        }

                        osc = osc_backup;

                        // apply stage 2 filter

                        for (p = nsmplwrt1; p < n2b; p++) {
                            buf2[ch][p] = 0;
                        }

//for (i = 0; i < n2b2; i++) { System.err.printf("%d:%g",i,buf2[ch][i]); }

                        rdft(n2b, 1, buf2[ch], fft_ip, fft_w);

                        buf2[ch][0] = stage2[0] * buf2[ch][0];
                        buf2[ch][1] = stage2[1] * buf2[ch][1];

                        for (i = 1; i < n2b / 2; i++) {
                            double re, im;

                            re = stage2[i * 2] * buf2[ch][i * 2] - stage2[i * 2 + 1] * buf2[ch][i * 2 + 1];
                            im = stage2[i * 2 + 1] * buf2[ch][i * 2] + stage2[i * 2] * buf2[ch][i * 2 + 1];

//System.err.printf("%d : %g %g %g %g %g %g\n", i, stage2[i * 2],stage2[i * 2 + 1],buf2[ch][i * 2],buf2[ch][i * 2 + 1], re, im);

                            buf2[ch][i * 2] = re;
                            buf2[ch][i * 2 + 1] = im;
                        }

                        rdft(n2b, -1, buf2[ch], fft_ip, fft_w);

                        for (i = osc, j = 0; i < n2b2; i += osf, j++) {
                            double f = (buf1[ch][j] + buf2[ch][i]);
                            outbuf[op + j * nch] = f;
                        }

                        nsmplwrt2 = j;

                        osc = i - n2b2;

                        for (j = 0; i < n2b; i += osf, j++) {
                            buf1[ch][j] = buf2[ch][i];
                        }
                    }

                    rp += nsmplwrt1 * (sfrq / frqgcd) / osf;

                    rawoutbuf.clear();
                    if (twopass) {
                        for (i = 0; i < nsmplwrt2 * nch; i++) {
                            double f = outbuf[i] > 0 ? outbuf[i] : -outbuf[i];
                            peak[0] = Math.max(peak[0], f);
                            rawoutbuf.asDoubleBuffer().put(i, outbuf[i]);
                        }
                    } else {
                        switch (dbps) {
                        case 1: {
                            double gain2 = gain * 0x7f;
                            ch = 0;

                            for (i = 0; i < nsmplwrt2 * nch; i++) {
                                int s;

                                if (dither != 0) {
                                    s = shaper.doShaping(outbuf[i] * gain2, peak, dither, ch);
                                } else {
                                    s = round(outbuf[i] * gain2);

                                    if (s < -0x80) {
                                        double d = (double) s / -0x80;
                                        peak[0] = Math.max(peak[0], d);
                                        s = -0x80;
                                    }
                                    if (0x7f < s) {
                                        double d = (double) s / 0x7f;
                                        peak[0] = Math.max(peak[0], d);
                                        s = 0x7f;
                                    }
                                }

                                rawoutbuf.put(i, (byte) (s + 0x80));

                                ch++;
                                if (ch == nch) {
                                    ch = 0;
                                }
                            }
                        }
                            break;

                        case 2: {
                            double gain2 = gain * 0x7fff;
                            ch = 0;

                            for (i = 0; i < nsmplwrt2 * nch; i++) {
                                int s;

                                if (dither != 0) {
                                    s = shaper.doShaping(outbuf[i] * gain2, peak, dither, ch);
                                } else {
                                    s = round(outbuf[i] * gain2);

                                    if (s < -0x8000) {
                                        double d = (double) s / -0x8000;
                                        peak[0] = Math.max(peak[0], d);
                                        s = -0x8000;
                                    }
                                    if (0x7fff < s) {
                                        double d = (double) s / 0x7fff;
                                        peak[0] = Math.max(peak[0], d);
                                        s = 0x7fff;
                                    }
                                }

                                rawoutbuf.order(byteOrder).asShortBuffer().put(i, (short) s);

                                ch++;
                                if (ch == nch) {
                                    ch = 0;
                                }
                            }
                        }
                            break;

                        case 3: {
                            double gain2 = gain * 0x7fffff;
                            ch = 0;

                            for (i = 0; i < nsmplwrt2 * nch; i++) {
                                int s;

                                if (dither != 0) {
                                    s = shaper.doShaping(outbuf[i] * gain2, peak, dither, ch);
                                } else {
                                    s = round(outbuf[i] * gain2);

                                    if (s < -0x800000) {
                                        double d = (double) s / -0x800000;
                                        peak[0] = Math.max(peak[0], d);
                                        s = -0x800000;
                                    }
                                    if (0x7fffff < s) {
                                        double d = (double) s / 0x7fffff;
                                        peak[0] = Math.max(peak[0], d);
                                        s = 0x7fffff;
                                    }
                                }

                                rawoutbuf.put(i * 3, (byte) (s & 255));
                                s >>= 8;
                                rawoutbuf.put(i * 3 + 1, (byte) (s & 255));
                                s >>= 8;
                                rawoutbuf.put(i * 3 + 2, (byte) (s & 255));

                                ch++;
                                if (ch == nch) {
                                    ch = 0;
                                }
                            }
                        }
                            break;

                        }
                    }

                    if (!init) {
                        if (ending) {
                            if ((double) sumread * dfrq / sfrq + 2 > sumwrite + nsmplwrt2) {
                                rawoutbuf.position(0);
                                rawoutbuf.limit(dbps * nch * nsmplwrt2);
                                sumWritten += fpo.write(rawoutbuf);
                                sumwrite += nsmplwrt2;
                            } else {
                                rawoutbuf.position(0);
                                rawoutbuf.limit((int) (dbps * nch * (Math.floor((double) sumread * dfrq / sfrq) + 2 - sumwrite)));
                                sumWritten += fpo.write(rawoutbuf);
                                break;
                            }
                        } else {
                            rawoutbuf.position(0);
                            rawoutbuf.limit(dbps * nch * nsmplwrt2);
                            sumWritten +=  fpo.write(rawoutbuf);
                            sumwrite += nsmplwrt2;
                        }
                    } else {

                        if (nsmplwrt2 < delay) {
                            delay -= nsmplwrt2;
                        } else {
                            if (ending) {
                                if ((double) sumread * dfrq / sfrq + 2 > sumwrite + nsmplwrt2 - delay) {
                                    rawoutbuf.position(dbps * nch * delay);
                                    rawoutbuf.limit(dbps * nch * nsmplwrt2);
                                    sumWritten += fpo.write(rawoutbuf);
                                    sumwrite += nsmplwrt2 - delay;
                                } else {
                                    rawoutbuf.position(dbps * nch * delay);
                                    rawoutbuf.limit((int) (dbps * nch * (Math.floor((double) sumread * dfrq / sfrq) + 2 - sumwrite - delay)));
                                    sumWritten += fpo.write(rawoutbuf);
                                    break;
                                }
                            } else {
                                rawoutbuf.position(dbps * nch * delay);
                                rawoutbuf.limit(dbps * nch * (nsmplwrt2 - delay));
                                sumWritten += fpo.write(rawoutbuf);
                                sumwrite += nsmplwrt2 - delay;
                                init = false;
                            }
                        }
                    }

                    {
                        ds = (rp - 1) / (fs1 / sfrq);

                        assert (inbuflen >= ds);

                        System.arraycopy(inbuf, nch * ds, inbuf, 0, nch * (inbuflen - ds)); // memmove TODO overlap
                        inbuflen -= ds;
                        rp -= ds * (fs1 / sfrq);
                    }

                    if ((spcount++ & 7) == 7) {
                        showProgress((double) sumread / chanklen);
                    }
                }
            }

            showProgress(1);

            this.peak = peak[0];

            return sumWritten;
        }
    }

    /** */
    class Downsampler extends Resampler {

        /* */
        int resample(ReadableByteChannel fpi, WritableByteChannel fpo) throws IOException {
            int frqgcd, osf, fs1, fs2;
            double[] stage1;
            double[][] stage2;
            int n2, n2x, n2y, n1, n1b;
            int filter1len;
            int[] f2order, f2inc;
            int[] fft_ip;
            double[] fft_w;
            ByteBuffer rawinbuf, rawoutbuf;
            double[] inbuf, outbuf;
            double[][] buf1, buf2;
            int i, j;
            int spcount = 0;
            double[] peak = new double[] { 0 };
            int sumWritten = 0;

System.err.println("downsample");

            filter1len = FFTFIRLEN; // stage 1 filter length

            // Make stage 1 filter

            {
                double aa = AA; // stop band attenuation(dB)
                double lpf, d, df, alp, iza;
//                double delta;
                int ipsize, wsize;

                frqgcd = gcd(sfrq, dfrq);

                if (dfrq / frqgcd == 1) {
                    osf = 1;
                } else if (dfrq / frqgcd % 2 == 0) {
                    osf = 2;
                } else if (dfrq / frqgcd % 3 == 0) {
                    osf = 3;
                } else {
                    throw new IllegalArgumentException(
                        String.format("Resampling from %dHz to %dHz is not supported.\n" +
                                      "%d/gcd(%d,%d)=%d must be divided by 2 or 3.",
                                      sfrq, dfrq, dfrq, sfrq, dfrq, dfrq / frqgcd));
                }

                fs1 = sfrq * osf;

//                delta = Math.pow(10, -aa / 20);
                if (aa <= 21) {
                    d = 0.9222;
                } else {
                    d = (aa - 7.95) / 14.36;
                }

                n1 = filter1len;
                for (i = 1;; i = i * 2) {
                    n1 = filter1len * i;
                    if (n1 % 2 == 0) {
                        n1--;
                    }
                    df = (fs1 * d) / (n1 - 1);
                    lpf = (dfrq - df) / 2;
                    if (df < DF) {
                        break;
                    }
                }

                alp = alpha(aa);

                iza = I0Bessel.value(alp);
//System.err.printf("iza %f, alp: %f\n", iza, alp); // OK

                for (n1b = 1; n1b < n1; n1b *= 2) {
                }
                n1b *= 2;

                stage1 = new double[n1b];

                for (i = -(n1 / 2); i <= n1 / 2; i++) {
                    stage1[i + n1 / 2] = win(i, n1, alp, iza) * hn_lpf(i, lpf, fs1) * fs1 / sfrq / n1b * 2;
//System.err.printf("1: %06d: %e\n", i + n1 / 2, stage1[i + n1 / 2]); // OK
                }

                ipsize = (int) (2 + Math.sqrt(n1b));
                fft_ip = new int[ipsize];
                fft_ip[0] = 0;
                wsize = n1b / 2;
                fft_w = new double[wsize];

                rdft(n1b, 1, stage1, fft_ip, fft_w);
//for (i = -(n1 / 2); i <= n1 / 2; i++) {
// System.err.printf("1': %06d: %e\n", i + n1 / 2, stage1[i + n1 / 2]);
//}
//for (i = 0; i < ipsize; i++) {
// System.err.printf("ip: %06d: %d\n", i, fft_ip[i]); // OK
//}
//for (i = 0; i < wsize; i++) {
// System.err.printf("w: %06d: %e\n", i, fft_w[i]); // OK
//}
            }

            // Make stage 2 filter

            if (osf == 1) {
                fs2 = sfrq / frqgcd * dfrq;
                n2 = 1;
                n2y = n2x = 1;
                f2order = new int[n2y];
                f2order[0] = 0;
                f2inc = new int[n2y];
                f2inc[0] = sfrq / dfrq;
                stage2 = new double[n2y][n2x];
                stage2[0][0] = 1;
            } else {
                double aa = AA; // stop band attenuation(dB)
                double lpf, d, df, alp, iza;
//                double delta;
                double guard = 2;

                fs2 = sfrq / frqgcd * dfrq;

                df = (fs1 / 2f - sfrq / 2f) * 2 / guard;
                lpf = sfrq / 2f + (fs1 / 2f - sfrq / 2f) / guard;

//                delta = Math.pow(10, -aa / 20);
                if (aa <= 21) {
                    d = 0.9222;
                } else {
                    d = (aa - 7.95) / 14.36;
                }

                n2 = (int) (fs2 / df * d + 1);
                if (n2 % 2 == 0) {
                    n2++;
                }

                alp = alpha(aa);
                iza = I0Bessel.value(alp);
//System.err.printf("iza %f, alp: %f\n", iza, alp); // OK

                n2y = fs2 / fs1; // 0でないサンプルがfs2で何サンプルおきにあるか？
                n2x = n2 / n2y + 1;

                f2order = new int[n2y];
                for (i = 0; i < n2y; i++) {
                    f2order[i] = fs2 / fs1 - (i * (fs2 / dfrq)) % (fs2 / fs1);
                    if (f2order[i] == fs2 / fs1) {
                        f2order[i] = 0;
                    }
                }

                f2inc = new int[n2y];
                for (i = 0; i < n2y; i++) {
                    f2inc[i] = (fs2 / dfrq - f2order[i]) / (fs2 / fs1) + 1;
                    if (f2order[i + 1 == n2y ? 0 : i + 1] == 0) {
                        f2inc[i]--;
                    }
                }

                stage2 = new double[n2y][n2x];

//System.err.printf("n2y: %d, n2: %d\n", n2y, n2);
                for (i = -(n2 / 2); i <= n2 / 2; i++) {
                    stage2[(i + n2 / 2) % n2y][(i + n2 / 2) / n2y] = win(i, n2, alp, iza) * hn_lpf(i, lpf, fs2) * fs2 / fs1;
//System.err.printf(" stage2[%02d][%02d]: %f\n", (i + n2 / 2) % n2y, (i + n2 / 2) / n2y, win(i, n2, alp, iza) * hn_lpf(i, lpf, fs2) * fs2 / fs1); // OK
                }
            }

            // Apply filters

            setStartTime();

            {
                int n1b2 = n1b / 2;
//                int rp; // inbufのfs1での次に読むサンプルの場所を保持
                int rps; // rpを(fs1/sfrq=osf)で割った余り
                int rp2; // buf2のfs2での次に読むサンプルの場所を保持
                int ds; // 次にdisposeするsfrqでのサンプル数
                // 実際にファイルからinbufに読み込まれた値から計算した stage2 filterに渡されるサンプル数
//                int nsmplwrt1;
                // 実際にファイルからinbufに読み込まれた値から計算した stage2 filterに渡されるサンプル数
                int nsmplwrt2 = 0;
                int s2p; // stage1 filterから出力されたサンプルの数をn1y*osfで割った余り
                boolean init, ending;
//                int osc;
                int bp; // rp2から計算される．buf2の次に読むサンプルの位置
                int rps_backup, s2p_backup;
                int k, ch, p;
                int inbuflen = 0;
                int sumread, sumwrite;
                int delay;
                int op;

                // |....B....|....C....| buf1 n1b2+n1b2
                // |.A.|....D....| buf2 n2x+n1b2
                //
                // まずinbufからBにosf倍サンプリングしながらコピー
                // Cはクリア
                // BCにstage 1 filterをかける
                // DにBを足す
                // ADにstage 2 filterをかける
                // Dの後ろをAに移動
                // CをDにコピー

                buf1 = new double[nch][n1b];

                buf2 = new double[nch][n2x + 1 + n1b2];

                rawinbuf = ByteBuffer.allocate((nch * (n1b2 / osf + osf + 1)) * bps);
//System.err.println((double) n1b2 * sfrq / dfrq + 1);
                rawoutbuf = ByteBuffer.allocate((int) (((double) n1b2 * sfrq / dfrq + 1) * (dbps * nch)));
                inbuf = new double[nch * (n1b2 / osf + osf + 1)];
                outbuf = new double[(int) (nch * ((double) n1b2 * sfrq / dfrq + 1))];

                op = 0; // outbuf

                s2p = 0;
//                rp = 0;
                rps = 0;
                ds = 0;
//                osc = 0;
                rp2 = 0;

                init = true;
                ending = false;
                delay = (int) ((double) n1 / 2 / ((double) fs1 / dfrq) + (double) n2 / 2 / ((double) fs2 / dfrq));

                sumread = sumwrite = 0;

                while (true) {
                    int nsmplread;
                    int toberead;

                    toberead = (n1b2 - rps - 1) / osf + 1;
                    if (toberead + sumread > chanklen) {
                        toberead = chanklen - sumread;
                    }

                    rawinbuf.position(0);
                    rawinbuf.limit(bps * nch * toberead);
                    nsmplread = fpi.read(rawinbuf);
                    rawinbuf.flip();
                    nsmplread /= bps * nch;

                    switch (bps) {
                    case 1:
                        for (i = 0; i < nsmplread * nch; i++) {
                            inbuf[nch * inbuflen + i] = (1 / (double) 0x7f) * ((rawinbuf.get(i) & 0xff) - 128);
                        }
                        break;

                    case 2:
                        for (i = 0; i < nsmplread * nch; i++) {
                            int v = rawinbuf.order(byteOrder).asShortBuffer().get(i);
                            inbuf[nch * inbuflen + i] = (1 / (double) 0x7fff) * v;
    //System.err.printf("I: %f\n", inbuf[nch * inbuflen + i]);
                        }
                        break;

                    case 3:
                        for (i = 0; i < nsmplread * nch; i++) {
                            inbuf[nch * inbuflen + i] = (1 / (double) 0x7fffff) *
                                (((rawinbuf.get(i * 3    ) & 0xff) <<  0) |
                                 ((rawinbuf.get(i * 3 + 1) & 0xff) <<  8) |
                                 ((rawinbuf.get(i * 3 + 2) & 0xff) << 16));
                        }
                        break;

                    case 4:
                        for (i = 0; i < nsmplread * nch; i++) {
                            int v = rawinbuf.order(byteOrder).getInt(i);
                            inbuf[nch * inbuflen + i] = (1 / (double) 0x7fffffff) * v;
                        }
                        break;
                    }

                    for (; i < nch * toberead; i++) {
                        inbuf[i] = 0;
                    }

                    sumread += nsmplread;

                    ending = nsmplread <= 0 || sumread >= chanklen;

                    rps_backup = rps;
                    s2p_backup = s2p;

                    for (ch = 0; ch < nch; ch++) {
                        rps = rps_backup;

                        for (k = 0; k < rps; k++) {
                            buf1[ch][k] = 0;
                        }

                        for (i = rps, j = 0; i < n1b2; i += osf, j++) {
                            assert (j < ((n1b2 - rps - 1) / osf + 1));

                            buf1[ch][i] = inbuf[j * nch + ch];

                            for (k = i + 1; k < i + osf; k++) {
                                buf1[ch][k] = 0;
                            }
                        }

                        assert (j == ((n1b2 - rps - 1) / osf + 1));

                        for (k = n1b2; k < n1b; k++) {
                            buf1[ch][k] = 0;
                        }

                        rps = i - n1b2;
//                        rp += j;

                        rdft(n1b, 1, buf1[ch], fft_ip, fft_w);

                        buf1[ch][0] = stage1[0] * buf1[ch][0];
                        buf1[ch][1] = stage1[1] * buf1[ch][1];

                        for (i = 1; i < n1b2; i++) {
                            double re, im;

                            re = stage1[i * 2] * buf1[ch][i * 2] - stage1[i * 2 + 1] * buf1[ch][i * 2 + 1];
                            im = stage1[i * 2 + 1] * buf1[ch][i * 2] + stage1[i * 2] * buf1[ch][i * 2 + 1];

                            buf1[ch][i * 2] = re;
                            buf1[ch][i * 2 + 1] = im;
                        }

                        rdft(n1b, -1, buf1[ch], fft_ip, fft_w);

                        for (i = 0; i < n1b2; i++) {
                            buf2[ch][n2x + 1 + i] += buf1[ch][i];
                        }

                        {
                            int t1 = rp2 / (fs2 / fs1);
                            if (rp2 % (fs2 / fs1) != 0) {
                                t1++;
                            }

                            bp = buf2[0].length * ch + t1; // &(buf2[ch][t1]);
                        }

                        s2p = s2p_backup;

                        for (p = 0; bp - (buf2[0].length * ch) < n1b2 + 1; p++) { // buf2[ch]
                            double tmp = 0;
                            int bp2;
                            int s2o;

                            bp2 = bp;
                            s2o = f2order[s2p];
                            bp += f2inc[s2p];
                            s2p++;

                            if (s2p == n2y) {
                                s2p = 0;
                            }

                            assert ((bp2 - (buf2[0].length * ch)) * (fs2 / fs1) - (rp2 + p * (fs2 / dfrq)) == s2o); // &(buf2[ch][0])
                            for (i = 0; i < n2x; i++) {
//System.err.printf("%d (%d, %d)\n", i, bp2 / buf2[0].length, bp2 % buf2[0].length);
                                tmp += stage2[s2o][i] * buf2[bp2 / buf2[0].length][bp2 % buf2[0].length]; // *bp2++
                                bp2++;
                            }

                            outbuf[op + p * nch + ch] = tmp;
//System.err.printf("O: %06d: %f\n", op + p * nch + ch, tmp);
                        }

                        nsmplwrt2 = p;
                    }

                    rp2 += nsmplwrt2 * (fs2 / dfrq);

                    rawoutbuf.clear();
                    if (twopass) {
                        for (i = 0; i < nsmplwrt2 * nch; i++) {
                            double f = outbuf[i] > 0 ? outbuf[i] : -outbuf[i];
                            peak[0] = Math.max(peak[0], f);
//System.err.println("p: " + rawoutbuf.position() + ", l: " + rawoutbuf.limit());
                            rawoutbuf.asDoubleBuffer().put(i, outbuf[i]);
//if (i < 100) {
// System.err.printf("1: %06d: %f\n", i, outbuf[i]);
//}
//System.err.print(StringUtil.getDump(rawoutbuf, i, 8));
                        }
                    } else {
                        switch (dbps) {
                        case 1: {
                            double gain2 = gain * 0x7f;
                            ch = 0;

                            for (i = 0; i < nsmplwrt2 * nch; i++) {
                                int s;

                                if (dither != 0) {
                                    s = shaper.doShaping(outbuf[i] * gain2, peak, dither, ch);
                                } else {
                                    s = round(outbuf[i] * gain2);

                                    if (s < -0x80) {
                                        double d = (double) s / -0x80;
                                        peak[0] = Math.max(peak[0], d);
                                        s = -0x80;
                                    }
                                    if (0x7f < s) {
                                        double d = (double) s / 0x7f;
                                        peak[0] = Math.max(peak[0], d);
                                        s = 0x7f;
                                    }
                                }

                                rawoutbuf.put(i, (byte) (s + 0x80));

                                ch++;
                                if (ch == nch) {
                                    ch = 0;
                                }
                            }
                        }
                            break;

                        case 2: {
                            double gain2 = gain * 0x7fff;
                            ch = 0;

                            for (i = 0; i < nsmplwrt2 * nch; i++) {
                                int s;

                                if (dither != 0) {
                                    s = shaper.doShaping(outbuf[i] * gain2, peak, dither, ch);
                                } else {
                                    s = round(outbuf[i] * gain2);

                                    if (s < -0x8000) {
                                        double d = (double) s / -0x8000;
                                        peak[0] = Math.max(peak[0], d);
                                        s = -0x8000;
                                    }
                                    if (0x7fff < s) {
                                        double d = (double) s / 0x7fff;
                                        peak[0] = Math.max(peak[0], d);
                                        s = 0x7fff;
                                    }
                                }

                                rawoutbuf.order(byteOrder).asShortBuffer().put(i, (short) s);

                                ch++;
                                if (ch == nch) {
                                    ch = 0;
                                }
                            }
                        }
                            break;

                        case 3: {
                            double gain2 = gain * 0x7fffff;
                            ch = 0;

                            for (i = 0; i < nsmplwrt2 * nch; i++) {
                                int s;

                                if (dither != 0) {
                                    s = shaper.doShaping(outbuf[i] * gain2, peak, dither, ch);
                                } else {
                                    s = round(outbuf[i] * gain2);

                                    if (s < -0x800000) {
                                        double d = (double) s / -0x800000;
                                        peak[0] = Math.max(peak[0], d);
                                        s = -0x800000;
                                    }
                                    if (0x7fffff < s) {
                                        double d = (double) s / 0x7fffff;
                                        peak[0] = Math.max(peak[0], d);
                                        s = 0x7fffff;
                                    }
                                }

                                rawoutbuf.put(i * 3, (byte) (s & 255));
                                s >>= 8;
                                rawoutbuf.put(i * 3 + 1, (byte) (s & 255));
                                s >>= 8;
                                rawoutbuf.put(i * 3 + 2, (byte) (s & 255));

                                ch++;
                                if (ch == nch) {
                                    ch = 0;
                                }
                            }
                        }
                            break;

                        }
                    }

                    if (!init) {
                        if (ending) {
                            if ((double) sumread * dfrq / sfrq + 2 > sumwrite + nsmplwrt2) {
                                rawoutbuf.position(0);
                                rawoutbuf.limit(dbps * nch * nsmplwrt2);
                                sumWritten += fpo.write(rawoutbuf);
                                sumwrite += nsmplwrt2;
                            } else {
                                rawoutbuf.position(0);
                                rawoutbuf.limit((int) (dbps * nch * (Math.floor((double) sumread * dfrq / sfrq) + 2 - sumwrite)));
                                sumWritten += fpo.write(rawoutbuf);
                                break;
                            }
                        } else {
                            rawoutbuf.position(0);
                            rawoutbuf.limit(dbps * nch * nsmplwrt2);
                            sumWritten += fpo.write(rawoutbuf);
                            sumwrite += nsmplwrt2;
                        }
                    } else {
                        if (nsmplwrt2 < delay) {
                            delay -= nsmplwrt2;
                        } else {
                            if (ending) {
                                if ((double) sumread * dfrq / sfrq + 2 > sumwrite + nsmplwrt2 - delay) {
                                    rawoutbuf.position(dbps * nch * delay);
                                    rawoutbuf.limit(dbps * nch * nsmplwrt2);
                                    sumWritten += fpo.write(rawoutbuf);
                                    sumwrite += nsmplwrt2 - delay;
                                } else {
                                    rawoutbuf.position(dbps * nch * delay);
System.err.printf("%d, %d, %d, %d\n",
  (int) (dbps * nch * (Math.floor((double) sumread * dfrq / sfrq) + 2 - sumwrite - delay)),
  (int) Math.floor((double) sumread * dfrq / sfrq), sumwrite, delay);
                                    rawoutbuf.limit((int) (dbps * nch * (Math.floor((double) sumread * dfrq / sfrq) + 2 - sumwrite - delay)));
                                    sumWritten += fpo.write(rawoutbuf);
                                    break;
                                }
                            } else {
                                rawoutbuf.position(dbps * nch * delay);
                                rawoutbuf.limit(dbps * nch * (nsmplwrt2 - delay));
                                sumWritten += fpo.write(rawoutbuf);
                                sumwrite += nsmplwrt2 - delay;
                                init = false;
                            }
                        }
                    }

                    {
                        ds = (rp2 - 1) / (fs2 / fs1);

                        if (ds > n1b2) {
                            ds = n1b2;
                        }

                        for (ch = 0; ch < nch; ch++) {
                            System.arraycopy(buf2[ch], ds, buf2[ch], 0, n2x + 1 + n1b2 - ds); // memmove TODO overlap
                        }

                        rp2 -= ds * (fs2 / fs1);
                    }

                    for (ch = 0; ch < nch; ch++) {
                        System.arraycopy(buf1[ch], n1b2, buf2[ch], n2x + 1, n1b2);
                    }

                    if ((spcount++ & 7) == 7) {
                        showProgress((double) sumread / chanklen);
                    }
                }
            }

            showProgress(1);

            this.peak = peak[0];

            return sumWritten;
        }
    }

    /** */
    class NoSrc extends Resampler {

        /* */
        int resample(ReadableByteChannel fpi, WritableByteChannel fpo) throws IOException {
            double[] peak = new double[] {
                0
            };
            int ch = 0, sumread = 0;
            int sumWritten = 0;

            setStartTime();

            ByteBuffer bb = null;
            if (twopass) {
                bb = ByteBuffer.allocate(8);
            }

            int r = 0;
            ByteBuffer buf = ByteBuffer.allocate(4);
            while (sumread < chanklen * nch) {
                double f = 0;
                int s;

                switch (bps) {
                case 1:
                    buf.position(0);
                    buf.limit(1);
                    r = fpi.read(buf);
                    buf.flip();
                    f = (1 / (double) 0x7f) * (buf.get(0) - 128);
                    break;
                case 2:
                    buf.position(0);
                    buf.limit(2);
                    r = fpi.read(buf);
                    buf.flip();
                    s = buf.order(byteOrder).asShortBuffer().get(0);
                    f = (1 / (double) 0x7fff) * s;
                    break;
                case 3:
                    buf.position(0);
                    buf.limit(3);
                    r = fpi.read(buf);
                    buf.flip();
                    f = (1 / (double) 0x7fffff) *
                          (((buf.get(0) & 0xff) <<  0) |
                           ((buf.get(1) & 0xff) <<  8) |
                           ((buf.get(2) & 0xff) << 16));
                    break;
                case 4:
                    buf.position(0);
                    buf.limit(4);
                    r = fpi.read(buf);
                    buf.flip();
                    s = buf.order(byteOrder).asIntBuffer().get(0);
                    f = (1 / (double) 0x7fffffff) * s;
                    break;
                }

                if (r <= 0) {
                    break;
                }
                f *= gain;

                if (!twopass) {
                    switch (dbps) {
                    case 1:
                        f *= 0x7f;
                        s = dither != 0 ? shaper.doShaping(f, peak, dither, ch) : round(f);
                        buf.position(0);
                        buf.limit(1);
                        buf.put(0, (byte) (s + 128));
                        buf.flip();
                        sumWritten += fpo.write(buf);
                        break;
                    case 2:
                        f *= 0x7fff;
                        s = dither != 0 ? shaper.doShaping(f, peak, dither, ch) : round(f);
                        buf.position(0);
                        buf.limit(2);
                        buf.asShortBuffer().put(0, (short) s);
                        buf.flip();
                        sumWritten += fpo.write(buf);
                        break;
                    case 3:
                        f *= 0x7fffff;
                        s = dither != 0 ? shaper.doShaping(f, peak, dither, ch) : round(f);
                        buf.position(0);
                        buf.limit(3);
                        buf.put(0, (byte) (s & 255));
                        s >>= 8;
                        buf.put(1, (byte) (s & 255));
                        s >>= 8;
                        buf.put(2, (byte) (s & 255));
                        buf.flip();
                        sumWritten += fpo.write(buf);
                        break;
                    }
                } else {
                    double p = f > 0 ? f : -f;
                    peak[0] = Math.max(peak[0], p);
                    bb.position(0);
                    bb.putDouble(f);
                    bb.flip();
                    sumWritten += fpo.write(bb);
                }

                ch++;
                if (ch == nch) {
                    ch = 0;
                }
                sumread++;

                if ((sumread & 0x3ffff) == 0) {
                    showProgress((double) sumread / (chanklen * nch));
                }
            }

            showProgress(1);

            this.peak = peak[0];

            return sumWritten;
        }
    }

    public static void main(String[] args) throws Exception {
        SSRC app = new SSRC();
        app.exec(args);
    }

    /** */
    private static final double[] presets = {
        0.7, 0.9, 0.18
    };

    /** as a command line program */
    public void exec(String[] argv) throws IOException {
        String sfn, dfn, tmpfn = null;
        File fo;
        FileChannel fpo;
        File ft;
        FileChannel fpto;
        boolean twopass, normalize;
        int dither, pdf, samp = 0;
        int nch, bps;
        int length;
        int sfrq, dfrq, dbps;
        double att, noiseamp;
        double[] peak = new double[] { 0 };
        int i;

        // parse command line options

        dfrq = -1;
        att = 0;
        dbps = -1;
        twopass = false;
        normalize = false;
        dither = 0;
        pdf = 0;
        noiseamp = 0.18;

        for (i = 0; i < argv.length; i++) {
            if (argv[i].charAt(0) != '-') {
                break;
            }

            if (argv[i].equals("--rate")) {
                dfrq = Integer.parseInt(argv[++i]);
//System.err.printf("dfrq: %d\n", dfrq);
                continue;
            }

            if (argv[i].equals("--att")) {
                att = Float.parseFloat(argv[++i]);
                continue;
            }

            if (argv[i].equals("--bits")) {
                dbps = Integer.parseInt(argv[++i]);
                if (dbps != 8 && dbps != 16 && dbps != 24) {
                    throw new IllegalArgumentException("Error: Only 8bit, 16bit and 24bit PCM are supported.");
                }
                dbps /= 8;
                continue;
            }

            if (argv[i].equals("--twopass")) {
                twopass = true;
                continue;
            }

            if (argv[i].equals("--normalize")) {
                twopass = true;
                normalize = true;
                continue;
            }

            if (argv[i].equals("--dither")) {
                try {
                    dither = Integer.parseInt(argv[i + 1]);
                    if (dither < 0 || dither > 4) {
                        throw new IllegalArgumentException("unrecognized dither type : " + argv[i + 1]);
                    }
                    i++;
                } catch (NumberFormatException e) {
                    dither = -1;
                }
                continue;
            }

            if (argv[i].equals("--pdf")) {
                try {
                    pdf = Integer.parseInt(argv[i + 1]);
                    if (pdf < 0 || pdf > 2) {
                        throw new IllegalArgumentException("unrecognized p.d.f. type : " + argv[i + 1]);
                    }
                    i++;
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("unrecognized p.d.f. type : " + argv[i + 1]);
                }

                try {
                    noiseamp = Double.parseDouble(argv[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    noiseamp = presets[pdf];
                }

                continue;
            }

            if (argv[i].equals("--quiet")) {
                quiet = true;
                continue;
            }

            if (argv[i].equals("--tmpfile")) {
                tmpfn = argv[++i];
                continue;
            }

            if (argv[i].equals("--profile")) {
                if (argv[i + 1].equals("fast")) {
                    AA = 96;
                    DF = 8000;
                    FFTFIRLEN = 1024;
                } else if (argv[i + 1].equals("standard")) {
                    /* nothing to do */
                } else {
                    throw new IllegalArgumentException("unrecognized profile : " + argv[i + 1]);
                }
                i++;
                continue;
            }

            throw new IllegalArgumentException("unrecognized option : " + argv[i]);
        }

        if (!quiet) {
            System.err.printf("Shibatch sampling rate converter version " + VERSION + "(high precision/nio)\n\n");
        }

        if (argv.length - i != 2) {
            usage();
            throw new IllegalStateException("too few arguments");
        }

        sfn = argv[i];
        dfn = argv[i + 1];

        fo = new File(dfn);

        try (FileInputStream fis = new FileInputStream(sfn);
             FileOutputStream fos = new FileOutputStream(fo)) {

            FileChannel fpi = fis.getChannel();

            // read wav header

            short word;
            int dword;

            ByteBuffer bb = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
            bb.limit(36);
            fpi.read(bb);
            bb.flip();
System.err.println("p: " + bb.position() + ", l: " + bb.limit());
            if (bb.get() != 'R') error(1);
            if (bb.get() != 'I') error(1);
            if (bb.get() != 'F') error(1);
            if (bb.get() != 'F') error(1);

            dword = bb.getInt();

            if (bb.get() != 'W') error(2);
            if (bb.get() != 'A') error(2);
            if (bb.get() != 'V') error(2);
            if (bb.get() != 'E') error(2);
            if (bb.get() != 'f') error(2);
            if (bb.get() != 'm') error(2);
            if (bb.get() != 't') error(2);
            if (bb.get() != ' ') error(2);

            int sizeOfFmt = bb.getInt();

            if (bb.getShort() != 1) {
                throw new IllegalStateException("Error: Only PCM is supported.");
            }
            nch = bb.getShort();
            sfrq = bb.getInt();
            bps = bb.getInt();
            if (bps % sfrq * nch != 0) {
                error(4);
            }

            word = bb.getShort();
            word = bb.getShort();

            bps /= sfrq * nch;

            if (sizeOfFmt > 16) {
                bb.position(0);
                bb.limit(2);
                fpi.read(bb);
                bb.flip();
                int sizeofExtended = bb.getShort();
                fpi.position(fpi.position() + sizeofExtended);
            }

            while (true) {
                bb.position(0);
                bb.limit(8);
                fpi.read(bb);
                bb.flip();
                int c0 = bb.get();
                int c1 = bb.get();
                int c2 = bb.get();
                int c3 = bb.get();
                length = bb.getInt();
System.err.printf("chunk: %c%c%c%c\n", c0, c1, c2, c3);
                if (c0 == 'd' && c1 == 'a' && c2 == 't' && c3 == 'a') {
                    break;
                }
                if (fpi.position() == fpi.size()) {
                    break;
                }
                fpi.position(fpi.position() + length);
            }
            if (fpi.position() == fpi.size()) {
                throw new IllegalStateException("Couldn't find data chank");
            }

            if (bps != 1 && bps != 2 && bps != 3 && bps != 4) {
                throw new IllegalStateException("Error : Only 8bit, 16bit, 24bit and 32bit PCM are supported.");
            }

            if (dbps == -1) {
                if (bps != 1) {
                    dbps = bps;
                } else {
                    dbps = 2;
                }
                if (dbps == 4) {
                    dbps = 3;
                }
            }

            if (dfrq == -1) {
                dfrq = sfrq;
            }

            if (dither == -1) {
                if (dbps < bps) {
                    if (dbps == 1) {
                        dither = 4;
                    } else {
                        dither = 3;
                    }
                } else {
                    dither = 1;
                }
            }

            if (!quiet) {
                String[] dtype = {
                    "none", "no noise shaping", "triangular spectral shape", "ATH based noise shaping", "ATH based noise shaping(less amplitude)"
                };
                String[] ptype = {
                    "rectangular", "triangular", "gaussian"
                };
                System.err.printf("frequency : %d -> %d\n", sfrq, dfrq);
                System.err.printf("attenuation : %gdB\n", att);
                System.err.printf("bits per sample : %d -> %d\n", bps * 8, dbps * 8);
                System.err.printf("nchannels : %d\n", nch);
                System.err.printf("length : %d bytes, %g secs\n", length, (double) length / bps / nch / sfrq);
                if (dither == 0) {
                    System.err.print("dither type : none\n");
                } else {
                    System.err.printf("dither type : %s, %s p.d.f, amp = %g\n", dtype[dither], ptype[pdf], noiseamp);
                }
                System.err.print("\n");
            }

            if (twopass) {
            }

            fpo = fos.getChannel();

            // generate wav header

            bb = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);

            bb.put("RIFF".getBytes());
            dword = 0;
            bb.putInt(dword);

            bb.put("WAVEfmt ".getBytes());
            dword = 16;
            bb.putInt(dword);
            word = 1;
            bb.putShort(word); // format category, PCM
            word = (short) nch;
            bb.putShort(word); // channels
            dword = dfrq;
            bb.putInt(dword); // sampling rate
            dword = dfrq * nch * dbps;
            bb.putInt(dword); // bytes per sec
            word = (short) (dbps * nch);
            bb.putShort(word); // block alignment
            word = (short) (dbps * 8);
            bb.putShort(word); // bits per sample

            bb.put("data".getBytes());
            dword = 0;
            bb.putInt(dword);

            bb.flip();
            fpo.write(bb);

            if (dither != 0) {
                int min = 0, max = 0;
                if (dbps == 1) {
                    min = -0x80;
                    max = 0x7f;
                }
                if (dbps == 2) {
                    min = -0x8000;
                    max = 0x7fff;
                }
                if (dbps == 3) {
                    min = -0x800000;
                    max = 0x7fffff;
                }
                if (dbps == 4) {
                    min = -0x80000000;
                    max = 0x7fffffff;
                }

                samp = shaper.initShaper(dfrq, nch, min, max, dither, pdf, noiseamp);
            }

            if (twopass) {
                double gain = 0;
                int ch = 0;
                int fptlen, sumread;

                if (!quiet) {
                    System.err.print("Pass 1\n");
                }

                if (tmpfn != null) {
                    ft = new File(tmpfn);
                } else {
                    ft = File.createTempFile("ssrc_", ".tmp");
                }
                try (FileOutputStream tfos= new FileOutputStream(ft)) {
                    fpto = tfos.getChannel();

//System.err.printf("nch: %d, bps: %d, size: %d, sfrq: %d, dfrq: %d, ???: %d, ???: %d, twopass: %b, dither: %d\n", nch, bps, 8, sfrq, dfrq, 1, length / bps / nch, twopass, dither);
                    Resampler resampler;
                    if (sfrq < dfrq) {
                        resampler = new Upsampler();
                    } else if (sfrq > dfrq) {
                        resampler = new Downsampler();
                    } else {
                        resampler = new NoSrc();
                    }
                    if (normalize) {
                        resampler.init(nch, bps, 8, sfrq, dfrq, 1, length / bps / nch, twopass, dither);
                    } else {
                        resampler.init(nch, bps, 8, sfrq, dfrq, Math.pow(10, -att / 20), length / bps / nch, twopass, dither);
                    }
                    resampler.resample(fpi, fpto);
                    peak[0] = resampler.peak;

                    fpto.close();
                }

                if (!quiet) {
                    System.err.printf("\npeak : %gdB\n", 20 * Math.log10(peak[0]));
                }

                if (!normalize) {
                    if (peak[0] < Math.pow(10, -att / 20)) {
                        peak[0] = 1;
                    } else {
                        peak[0] *= Math.pow(10, att / 20);
                    }
                } else {
                    peak[0] *= Math.pow(10, att / 20);
                }

                if (!quiet) {
                    System.err.print("\nPass 2\n");
                }

                if (dither != 0) {
                    switch (dbps) {
                    case 1:
                        gain = (normalize || peak[0] >= (0x7f - samp) / (double) 0x7f) ? 1 / peak[0] * (0x7f - samp) : 1 / peak[0] * 0x7f;
                        break;
                    case 2:
                        gain = (normalize || peak[0] >= (0x7fff - samp) / (double) 0x7fff) ? 1 / peak[0] * (0x7fff - samp) : 1 / peak[0] * 0x7fff;
                        break;
                    case 3:
                        gain = (normalize || peak[0] >= (0x7fffff - samp) / (double) 0x7fffff) ? 1 / peak[0] * (0x7fffff - samp) : 1 / peak[0] * 0x7fffff;
                        break;
                    }
                } else {
                    switch (dbps) {
                    case 1:
                        gain = 1 / peak[0] * 0x7f;
                        break;
                    case 2:
                        gain = 1 / peak[0] * 0x7fff;
                        break;
                    case 3:
                        gain = 1 / peak[0] * 0x7fffff;
                        break;
                    }
                }
                shaper.randPtr = 0;

                setStartTime();

                fptlen = (int) (ft.length() / 8);
//System.err.println("tmp: " + fpt.getFilePointer());

                try (FileInputStream fisf = new FileInputStream(ft)) {
                    FileChannel fpti = fisf.getChannel();
                    bb = ByteBuffer.allocate(8);
                    for (sumread = 0; sumread < fptlen;) {
                        double f;
                        int s;

                        bb.clear();
                        fpti.read(bb);
                        bb.flip();
                        f = bb.getDouble();
//if (sumread < 100) {
// System.err.printf("2: %06d: %f\n", sumread, f);
//}
                        f *= gain;
                        sumread++;

                        switch (dbps) {
                        case 1: {
                            s = dither != 0 ? shaper.doShaping(f, peak, dither, ch) : round(f);

                            ByteBuffer buf = ByteBuffer.allocate(1);
                            buf.put((byte) (s + 128));
                            buf.flip();

                            fpo.write(buf);
                        }
                            break;
                        case 2: {
                            s = dither != 0 ? shaper.doShaping(f, peak, dither, ch) : round(f);

                            ByteBuffer buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
                            buf.putShort((short) s);
                            buf.flip();

                            fpo.write(buf);
                        }
                            break;
                        case 3: {
                            s = dither != 0 ? shaper.doShaping(f, peak, dither, ch) : round(f);

                            ByteBuffer buf = ByteBuffer.allocate(3);
                            buf.put((byte) (s & 255));
                            s >>= 8;
                            buf.put((byte) (s & 255));
                            s >>= 8;
                            buf.put((byte) (s & 255));
                            buf.flip();

                            fpo.write(buf);
                        }
                            break;
                        }

                        ch++;
                        if (ch == nch) {
                            ch = 0;
                        }

                        if ((sumread & 0x3ffff) == 0) {
                            showProgress((double) sumread / fptlen);
                        }
                    }
                    showProgress(1);
                    if (!quiet) {
                        System.err.print("\n");
                    }
                    fpti.close();
                }
                //System.err.println("ft: " + ft);
                if (!ft.delete()) {
                    System.err.printf("Failed to remove %s\n", ft);
                }
            } else {
                Resampler resampler;
                if (sfrq < dfrq) {
                    resampler = new Upsampler();
                } else if (sfrq > dfrq) {
                    resampler = new Downsampler();
                } else {
                    resampler = new NoSrc();
                }
                resampler.init(nch, bps, dbps, sfrq, dfrq, Math.pow(10, -att / 20), length / bps / nch, twopass, dither);
                resampler.resample(fpi, fpo);
                peak[0] = resampler.peak;
                if (!quiet) {
                    System.err.print("\n");
                }
            }

            if (dither != 0) {
                shaper.quitShaper(nch);
            }

            if (!twopass && peak[0] > 1) {
                if (!quiet) {
                    System.err.printf("clipping detected : %gdB\n", 20 * Math.log10(peak[0]));
                }
            }

            int len;

            fpo.close();

            fo = new File(dfn);

            len =  (int) fo.length();
            try (RandomAccessFile raf = new RandomAccessFile(fo, "rw")) {
                fpo = raf.getChannel();
                bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);

                dword = len - 8;
                bb.position(0);
                bb.limit(4);
                bb.putInt(dword);
                bb.flip();
                fpo.write(bb, 4);

                dword = len - 44;
                bb.position(0);
                bb.limit(4);
                bb.putInt(dword);
                bb.flip();
                fpo.write(bb, 40);

                fpo.close();
            }
        }
    }

    /**
     * as a filter
     *
     * @param fpi input stream
     * @param fpo output stream
     * @param length input length
     * @param nch number of channels
     * @param sfrq source frequency
     * @param bps source bytes per channel
     * @param dfrq destination frequency
     * @param dbps destination bytes per channel
     * @param props properties
     */
    void io(ReadableByteChannel fpi, WritableByteChannel fpo, int length, int nch, int sfrq, int bps, int dfrq, int dbps, Map<String, Object> props) throws IOException {
        boolean twopass = (boolean) props.getOrDefault("twopass", true);
        boolean normalize = (boolean) props.getOrDefault("normalize", true);
        int dither = (int) props.getOrDefault("dither", 0); // 0 ~ 3
        int pdf = (int) props.getOrDefault("pdf", 0); // 0 ~ 1
        String profile = (String) props.getOrDefault("profile", "standard");
        int samp = 0;
        double att, noiseamp;
        double[] peak = new double[] { 0 };

        // TODO options
        att = 0;

        // presets[pdf]
        noiseamp = 0.18;

        switch (profile) {
        case "fast":
            AA = 96;
            DF = 8000;
            FFTFIRLEN = 1024;
            break;
        case "standard":
            /* nothing to do */
            break;
        }

logger.fine(String.format("nch: %d, sfrq: %d, bps: %d, sfrq: %d, bps: %d\n", nch, sfrq, bps, dfrq, dbps));

        if (bps != 1 && bps != 2 && bps != 3 && bps != 4) {
            throw new IllegalArgumentException("Only 8bit, 16bit, 24bit and 32bit PCM are supported.");
        }

        if (dither == -1) {
            if (dbps < bps) {
                if (dbps == 1) {
                    dither = 4;
                } else {
                    dither = 3;
                }
            } else {
                dither = 1;
            }
        }

        if (!quiet) {
            String[] dtype = {
                "none", "no noise shaping", "triangular spectral shape", "ATH based noise shaping", "ATH based noise shaping(less amplitude)"
            };
            String[] ptype = {
                "rectangular", "triangular", "gaussian"
            };
            System.err.printf("frequency : %d -> %d\n", sfrq, dfrq);
            System.err.printf("attenuation : %gdB\n", att);
            System.err.printf("bits per sample : %d -> %d\n", bps * 8, dbps * 8);
            System.err.printf("nchannels : %d\n", nch);
            System.err.printf("length : %d bytes, %g secs\n", length, (double) length / bps / nch / sfrq);
            if (dither == 0) {
                System.err.print("dither type : none\n");
            } else {
                System.err.printf("dither type : %s, %s p.d.f, amp = %g\n", dtype[dither], ptype[pdf], noiseamp);
            }
        }

        if (dither != 0) {
            int min = 0, max = 0;
            if (dbps == 1) {
                min = -0x80;
                max = 0x7f;
            }
            if (dbps == 2) {
                min = -0x8000;
                max = 0x7fff;
            }
            if (dbps == 3) {
                min = -0x800000;
                max = 0x7fffff;
            }
            if (dbps == 4) {
                min = -0x80000000;
                max = 0x7fffffff;
            }

            shaper.initShaper(dfrq, nch, min, max, dither, pdf, noiseamp);
        }

        if (twopass) {
            double gain = 0;
            int ch = 0;
            int fptlen, sumread;
            File file = File.createTempFile("ssrc", ".tmp");
            file.deleteOnExit();
            try (FileInputStream fis = new FileInputStream(file);
                 FileOutputStream fos = new FileOutputStream(file)) {
                FileChannel pipeIn = fis.getChannel();
                FileChannel pipeOut = fos.getChannel();

                if (!quiet) {
                    System.err.print("Pass 1\n");
                }

logger.fine(String.format("nch: %d, bps: %d, size: %d, sfrq: %d, dfrq: %d, ???: %d, ???: %d, twopass: %b, dither: %d\n", nch, bps, 8, sfrq, dfrq, 1, length / bps / nch, twopass, dither));
                Resampler resampler;
                if (sfrq < dfrq) {
                    resampler = new Upsampler();
                } else if (sfrq > dfrq) {
                    resampler = new Downsampler();
                } else {
                    resampler = new NoSrc();
                }
                if (normalize) {
                    resampler.init(nch, bps, 8, sfrq, dfrq, 1, length / bps / nch, twopass, dither);
                } else {
                    resampler.init(nch, bps, 8, sfrq, dfrq, Math.pow(10, -att / 20), length / bps / nch, twopass, dither);
                }
                fptlen = resampler.resample(fpi, pipeOut);
                peak[0] = resampler.peak;

                pipeOut.close();

                if (!quiet) {
                    System.err.printf("\npeak : %gdB\n", 20 * Math.log10(peak[0]));
                }

                if (!normalize) {
                    if (peak[0] < Math.pow(10, -att / 20)) {
                        peak[0] = 1;
                    } else {
                        peak[0] *= Math.pow(10, att / 20);
                    }
                } else {
                    peak[0] *= Math.pow(10, att / 20);
                }

                if (!quiet) {
                    System.err.print("\nPass 2\n");
                }

                if (dither != 0) {
                    switch (dbps) {
                    case 1:
                        gain = (normalize || peak[0] >= (0x7f - samp) / (double) 0x7f) ? 1 / peak[0] * (0x7f - samp) : 1 / peak[0] * 0x7f;
                        break;
                    case 2:
                        gain = (normalize || peak[0] >= (0x7fff - samp) / (double) 0x7fff) ? 1 / peak[0] * (0x7fff - samp) : 1 / peak[0] * 0x7fff;
                        break;
                    case 3:
                        gain = (normalize || peak[0] >= (0x7fffff - samp) / (double) 0x7fffff) ? 1 / peak[0] * (0x7fffff - samp) : 1 / peak[0] * 0x7fffff;
                        break;
                    }
                } else {
                    switch (dbps) {
                    case 1:
                        gain = 1 / peak[0] * 0x7f;
                        break;
                    case 2:
                        gain = 1 / peak[0] * 0x7fff;
                        break;
                    case 3:
                        gain = 1 / peak[0] * 0x7fffff;
                        break;
                    }
                }
                shaper.randPtr = 0;

                setStartTime();

                fptlen /= 8;

                ByteBuffer bb = ByteBuffer.allocate(8);
                for (sumread = 0; sumread < fptlen;) {
                    double f;
                    int s;

                    bb.clear();
                    pipeIn.read(bb);
                    bb.flip();
                    f = bb.getDouble();
//if (sumread < 100) {
// System.err.printf("2: %06d: %f\n", sumread, f);
//}
                    f *= gain;
                    sumread++;

                    switch (dbps) {
                    case 1: {
                        s = dither != 0 ? shaper.doShaping(f, peak, dither, ch) : round(f);

                        ByteBuffer buf = ByteBuffer.allocate(1);
                        buf.put((byte) (s + 128));
                        buf.flip();

                        fpo.write(buf);
                    }
                        break;
                    case 2: {
                        s = dither != 0 ? shaper.doShaping(f, peak, dither, ch) : round(f);

                        ByteBuffer buf = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
                        buf.putShort((short) s);
                        buf.flip();

                        fpo.write(buf);
                    }
                        break;
                    case 3: {
                        s = dither != 0 ? shaper.doShaping(f, peak, dither, ch) : round(f);

                        ByteBuffer buf = ByteBuffer.allocate(3);
                        buf.put((byte) (s & 255));
                        s >>= 8;
                        buf.put((byte) (s & 255));
                        s >>= 8;
                        buf.put((byte) (s & 255));
                        buf.flip();

                        fpo.write(buf);
                    }
                        break;
                    }

                    ch++;
                    if (ch == nch) {
                        ch = 0;
                    }

                    if ((sumread & 0x3ffff) == 0) {
                        showProgress((double) sumread / fptlen);
                    }
                }
                showProgress(1);
                if (!quiet) {
                    System.err.print("\n");
                }
                pipeIn.close();
            }
        } else {
            Resampler resampler;
            if (sfrq < dfrq) {
                resampler = new Upsampler();
            } else if (sfrq > dfrq) {
                resampler = new Downsampler();
            } else {
                resampler = new NoSrc();
            }
            resampler.init(nch, bps, dbps, sfrq, dfrq, Math.pow(10, -att / 20), length / bps / nch, twopass, dither);
            resampler.resample(fpi, fpo);
            peak[0] = resampler.peak;
            if (!quiet) {
                System.err.print("\n");
            }
        }

        if (dither != 0) {
            shaper.quitShaper(nch);
        }

        if (!twopass && peak[0] > 1) {
            if (!quiet) {
                System.err.printf("clipping detected : %gdB\n", 20 * Math.log10(peak[0]));
            }
        }
    }
}

/* */
