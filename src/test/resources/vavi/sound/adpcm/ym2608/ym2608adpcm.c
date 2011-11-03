/*
 * YM2608 ADPCM Codec
 *
 * code : Masashi Wada ( DEARNA )
 * http://www.memb.jp/~dearna/
 */

#include <stdio.h>
#include <stdlib.h>
#include <math.h>

static int ccc = 0;

static long stepsizeTable[ 16 ] =
    {
        57, 57, 57, 57, 77, 102, 128, 153,
        57, 57, 57, 57, 77, 102, 128, 153
    };

int YM2608ADPCM_Encode( short *src , unsigned char *dest , int len ) {
    int lpc , flag;
    long i , dn , xn , stepSize;
    unsigned char adpcm;
    unsigned char adpcmPack;

    /* 初期値の設定 */
    xn	= 0;
    stepSize	= 127;
    flag	= 0;

    for ( lpc = 0 ; lpc < len ; lpc++ ) {
        /* エンコード処理2 */
        dn = *src - xn;
//fprintf(stderr, "%05d: %ld, %d, %ld\n", ccc, dn, *src, xn); // OK
        src++;

        /*
         * エンコード処理3、4
         * I = | dn | / Sn からAnを求める。
         * 乗数を使用して整数位で演算する。
         */
        i = (((unsigned long) abs( dn )) << 16 ) / (((unsigned long) stepSize) << 14 );
//fprintf(stderr, "%05d: %ld\n", ccc, i); // OK
        if ( i > 7 )
            i = 7;
        adpcm = ( unsigned char )i;

        /*
         * エンコード処理5
         * L3+L2/2+L1/4+1/8 * stepSize を8倍して整数演算
         */
        i = ( adpcm * 2 + 1 ) * stepSize / 8;
//fprintf(stderr, "%05d: %ld, %ld, %ld\n", ccc, i, adpcm, stepSize); // OK

        /* 1-2*L4 -> L4が1の場合は-1をかけるのと同じ */
        if ( dn < 0 ) {
            /*
             * -の場合符号ビットを付ける。
             * エンコード処理5でADPCM符号が邪魔になるので、予測値更新時まで保留した。
             */
            adpcm |= 0x8;
            xn -= i;
        } else {
            xn += i;
        }
//fprintf(stderr, "%05d: %ld, %ld\n", ccc, xn, i);

        /*
         * エンコード処理6
         * ステップサイズの更新
         */
        stepSize = ( stepsizeTable[ adpcm ] * stepSize ) / 64;
//fprintf(stderr, "%05d: %ld, %d, %ld\n", ccc, i, adpcm, stepSize); // OK

        /* エンコード処理7 */
        if ( stepSize < 127 )
            stepSize = 127;
        else if ( stepSize > 24576 )
            stepSize = 24576;

        /* ADPCMで保存する */
        if ( flag == 0 ) {
            adpcmPack = ( adpcm << 4 ) ;
            flag = 1;
        } else {
            adpcmPack |= adpcm;
            *dest = adpcmPack;
            dest++;
            flag = 0;
        }
ccc++;
    }

    return 0;
}

int YM2608ADPCM_Decode( unsigned char *src , short *dest , int len ) {
    int lpc , flag , shift , step;
    long i , xn , stepSize;
    long adpcm;

    /* 初期値の設定 */
    xn	= 0;
    stepSize	= 127;
    flag	= 0;
    shift	= 4;
    step	= 0;

    for ( lpc = 0 ; lpc < len ; lpc++ ) {
        adpcm = ( *src >> shift ) & 0xf;
//fprintf(stderr, "0: %02X\n", *src); //

        /*
         * デコード処理2、3
         * L3+L2/2+L1/4+1/8 * stepSize を8倍して整数演算
         */
        i = ( ( adpcm & 7 ) * 2 + 1 ) * stepSize / 8;
        if ( adpcm & 8 )
            xn -= i;
        else
            xn += i;
//fprintf(stderr, "%05d: %ld, %ld, %ld\n", ccc, xn, stepSize, adpcm); // OK

        /* デコード処理4 */
        if ( xn > 32767 )
            xn = 32767;
        else if ( xn < -32768 )
            xn = -32768;

        /* デコード処理5 */
        stepSize = stepSize * stepsizeTable[ adpcm ] / 64;

        /* デコード処理6 */
        if ( stepSize < 127 )
            stepSize = 127;
        else if ( stepSize > 24576 )
            stepSize = 24576;
//fprintf(stderr, "%05d: %ld, %ld, %ld\n", ccc, xn, stepSize, adpcm); // OK

        /* PCMで保存する */
        *dest = ( short )xn;
fprintf(stderr, "%05d: %d\n", ccc, (short) xn); // OK
        dest++;

        src += step;
        step = step ^ 1;
        shift = shift ^ 4;
ccc++;
    }

    return 0;
}

/* */

#ifdef encoder

#define NSAMPLES 1024

char	abuf[NSAMPLES/2];
short	sbuf[NSAMPLES];

/**
 * encoder < input > output
 */
main() {
    int n;

//fprintf(stderr, "%5d\n", sizeof(unsigned long));
    while(1) {
	n = read(0, sbuf, NSAMPLES*2);
	if ( n < 0 ) {
//	    perror("input file");
	    exit(1);
	}
	if ( n == 0 ) break;
	YM2608ADPCM_Encode(sbuf, abuf, n/2);
	write(1, abuf, n/4);
    }
//    fprintf(stderr, "Final valprev=%d, index=%d\n",
//	    state.valprev, state.index);
    exit(0);
}

#else

#define NSAMPLES 1024

char	abuf[NSAMPLES/2];
short	sbuf[NSAMPLES];

/**
 * decoder < input > output
 */
main() {
    int n;

    while(1) {
	n = read(0, abuf, NSAMPLES/2);
	if ( n < 0 ) {
	    perror("input file");
	    exit(1);
	}
	if ( n == 0 ) break;
	YM2608ADPCM_Decode(abuf, sbuf, n*2);
	write(1, sbuf, n*4);
    }
//    fprintf(stderr, "Final valprev=%d, index=%d\n",
//	    state.valprev, state.index);
    exit(0);
}

#endif
