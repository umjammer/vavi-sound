/*
 * Copyright (c) 2003 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.adpcm.ccitt;

import javax.sound.sampled.AudioFormat;


/**
 * These routines comprise an implementation of the CCITT G.721 ADPCM
 * coding algorithm.  Essentially, this implementation is identical to
 * the bit level description except for a few deviations which
 * take advantage of work station attributes, such as hardware 2's
 * complement arithmetic and large memory.  Specifically, certain time
 * consuming operations such as multiplications are replaced
 * with lookup tables and software 2's complement operations are
 * replaced with hardware 2's complement.
 *
 * The deviation from the bit level specification (lookup tables)
 * preserves the bit level performance specifications.
 *
 * As outlined in the G.721 Recommendation, the algorithm is broken
 * down into modules.  Each section of code below is preceded by
 * the name of the module which it is implementing.
 *
 * @author <a href="mailto:vavivavi@yahoo.co.jp">Naohide Sano</a> (nsano)
 * @version 0.00 030713 nsano port to java <br>
 *          0.01 030714 nsano fine tune <br>
 */
class G721 extends G711 {

    /** */
    private static final int qtab_721[] = {
        -124, 80, 178, 246, 300, 349, 400
    };

    /**
     * Maps G.721 code word to reconstructed scale factor normalized log
     * magnitude values.
     */
    private static final int _dqlntab[] = {
        -2048, 4, 135, 213, 273, 323, 373, 425,
        425, 373, 323, 273, 213, 135, 4, -2048
    };

    /** Maps G.721 code word to log of scale factor multiplier. */
    private static final int _witab[] = {
        -12, 18, 41, 64, 112, 198, 355, 1122,
        1122, 355, 198, 112, 64, 41, 18, -12
    };

    /**
     * Maps G.721 code words to a set of values whose long and short
     * term averages are computed and then compared to give an indication
     * how stationary (steady state) the signal is.
     */
    private static final int _fitab[] = {
        0, 0, 0, 0x200, 0x200, 0x200, 0x600, 0xe00,
        0xe00, 0x600, 0x200, 0x200, 0x200, 0, 0, 0
    };

    /**
     * Encodes the input vale of linear PCM, A-law or u-law data sl and returns
     * the resulting code. -1 is returned for unknown input coding value.
     */
    public int encode(int sl) {

    	// linearize input sample to 14-bit PCM
    	if (AudioFormat.Encoding.ALAW.equals(encoding)) {
            sl = alaw2linear(sl) >> 2;
        } else if (AudioFormat.Encoding.ULAW.equals(encoding)) {
            sl = ulaw2linear(sl) >> 2;
        } else if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
//System.err.println("---- " + ccc + " ----");
//ccc++;
//System.err.println("sl:B:\t" + sl);
            sl >>= 2;                                       // 14-bit dynamic range
//System.err.println("sl:A:\t" + sl);
        } else {
            throw new IllegalArgumentException(encoding.toString());
        }
        
        // ACCUM
        int sezi = state.getZeroPredictor();
        int sez = sezi >> 1;
        int se = (sezi + state.getPolePredictor()) >> 1;    // estimated signal

//System.err.println("sl:\t" + sl);
//System.err.println("se:\t" + se);
        // SUBTA
        int d = sl - se;                                    // estimation difference
        
        // quantize the prediction difference
        // MIX
        int y = state.getStepSize();                        // quantizer step size
        int i = quantize(d, y, qtab_721, 7);                // i = ADPCM code
        
        // quantized est diff
        int dq = reconstruct((i & 8) != 0, _dqlntab[i], y);
        
        // ADDB
        int sr = (dq < 0) ? se - (dq & 0x3fff) : se + dq;   // reconst. signal
        
        // ADDC
        int dqsez = sr + sez - se;                          // pole prediction diff.
        
        state.update(4, y, _witab[i] << 5, _fitab[i], dq, sr, dqsez);
        
        return i;
    }

/** debug */
//private int ccc = 0;

    /**
     * Decodes a 4-bit code of G.721 encoded data of i and
     * returns the resulting linear PCM, A-law or u-law value.
     *
     * @return -1 for unknown out_coding value.
     */
    public int decode(int i) {

        i &= 0x0f;                                          // mask to get proper bits
        
        // ACCUM
        int sezi = state.getZeroPredictor();
        int sez = sezi >> 1;
        int sei = sezi + state.getPolePredictor();
        int se = sei >> 1;                                  // se = estimated signal

/*
System.err.println("---- (" + (ccc++) + ")");
System.err.println("i:\t" + StringUtil.toHex2(i));
System.err.println("sezi:\t" + sezi);
System.err.println("sez:\t" + sez);
System.err.println("sei:\t" + sei);
System.err.println("se:\t" + se);
*/
        // MIX
        int y = state.getStepSize();                        // dynamic quantizer step size
        
        // quantized diff.
        int dq = reconstruct((i & 0x08) != 0, _dqlntab[i], y);
        
        // ADDB reconst. signal
        int sr = (dq < 0) ? (se - (dq & 0x3fff)) : se + dq;
        
        int dqsez = sr - se + sez;                          // pole prediction diff.
        
        state.update(4, y, _witab[i] << 5, _fitab[i], dq, sr, dqsez);
        
        if (AudioFormat.Encoding.ALAW.equals(encoding)) {
            return adjustAlawTandem(sr, se, y, i, 8, qtab_721);
        } else if (AudioFormat.Encoding.ULAW.equals(encoding)) {
            return adjustUlawTandem(sr, se, y, i, 8, qtab_721);
        } else if (AudioFormat.Encoding.PCM_SIGNED.equals(encoding)) {
            return sr << 2;                                 // sr was 14-bit dynamic range
        } else {
            throw new IllegalArgumentException(encoding.toString());
        }
    }

    /** */
    public int getEncodingBits() {
        return 4;
    }
}

/* */
