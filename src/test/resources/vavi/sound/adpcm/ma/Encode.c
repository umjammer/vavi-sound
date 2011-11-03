
#include <stdio.h>
#include "Encode.h"

// ============================================================================

static long calc_predict( unsigned char* p, long predict, long quantwidth)
{
	long temp;
	
	temp = (
				(long)(1L - 2L * p[4]) *
				(
					(long)p[3] * quantwidth +
					(long)p[2] * (long)(quantwidth / 2L) +
					(long)p[1] * (long)(quantwidth / 4L) +
								 (long)(quantwidth / 8L)
				)
			);
	
	temp += predict;
	
	if ( temp < -32768L )
		temp = -32768L;
	
	if ( temp > 32767L )
		temp = 32767L;
	
	return temp;
}


// ============================================================================

static long calc_quantwidth( unsigned char* p, long width )
{
	long temp = 0;

	switch ( (p[3]<<2) | (p[2]<<1) | p[1] )
	{
	case 0:
	case 1:
	case 2:
	case 3:
		temp = width * 0x0e6;
		temp = temp >> 8;
		break;
	case 4:
		temp = width * 0x133;
		temp = temp >> 8;
		break;
	case 5:
		temp = width * 0x199;
		temp = temp >> 8;
		break;
	case 6:
		temp = width * 0x200;
		temp = temp >> 8;
		break;
	case 7:
		temp = width * 0x266;
		temp = temp >> 8;
		break;
	}

	if ( temp < 127L )
		temp = 127L;

	if ( temp > 24576L )
		temp = 24576L;
	
	return temp;
}

// ============================================================================


int Decode(unsigned char* adpbuf, unsigned long adpsize, unsigned char* pcmbuf, unsigned long* pcmsize )
{
	long adpcm, delta, predict, quantwidth;
	unsigned char L[5];
	unsigned long i;
	short* pspcm;
	
	predict=0L;
	quantwidth=127L;
	pspcm=(short*)pcmbuf;
	
	L[0] = 0;
	
	for ( i=0; i<adpsize ; i++ ) {
		
		adpcm = (i % 2) == 1 ? (adpbuf[i/2] >> 4) & 0x0f : adpbuf[i/2] & 0x0f;

		L[4] = ( adpcm & 0x08 ) ? 1 : 0;
		L[3] = ( adpcm & 0x04 ) ? 1 : 0;
		L[2] = ( adpcm & 0x02 ) ? 1 : 0;
		L[1] = ( adpcm & 0x01 ) ? 1 : 0;
		
		predict    = calc_predict( L, predict, quantwidth );
		quantwidth = calc_quantwidth( L, quantwidth );
		
		pspcm[i] = predict;
	}
	
	*pcmsize = i;
	
	return 1;
}

// ============================================================================

static unsigned long put_data( unsigned char* pdes, unsigned char* p, unsigned long size )
{
	unsigned char odd, bit;
	unsigned char * pp;
	
	bit = ( (p[4]<<3) | (p[3]<<2) | (p[2]<<1) | p[1] ) & 0x0f;
	
	odd = (unsigned char)( size & 1 );
	
	pp = pdes + ( size >> 1 );
	
	if ( odd )
		*pp =  ( bit << 4 ) + *pp;
	else
		*pp =  bit & 0x0f;
	
	return (size >> 1);
}

// ============================================================================

static int check_size(unsigned long pcm_size, unsigned long adpcm_size)
{
	unsigned long pcm_samples = pcm_size / sizeof(short);
	unsigned long valid_size = (pcm_samples & 1) ? ((pcm_samples+1)>>1) : (pcm_samples>>1);
	
	if ( adpcm_size >= valid_size )
		return 1;
	
	return 0;
}


// ============================================================================

int Encode(unsigned char* pcmbuf, unsigned long pcmsize, unsigned char* adpbuf, unsigned long* adpsize )
{
	long pcm, delta, predict, quantwidth;
	unsigned char L[5];
	unsigned long i, j, wsamples, asamples;
	short* pspcm;
	
	predict=0L;
	quantwidth=127L;
	wsamples=pcmsize / sizeof(short);
	asamples=0;
	pspcm=(short*)pcmbuf;
	
	if ( !check_size(pcmsize, *adpsize) )
		return 0;
	
	L[0] = 0;
	
	for ( i=0; i<wsamples ; i++ ) {
		
		pcm = pspcm[i];
		delta = pcm - predict;
//fprintf(stderr, "%06ld: delta: %-6d, pcm: %-6d, preDict: %-6d\n", i, delta, pcm, predict);
		
		L[4] = ( delta < 0 ) ? 1 : 0;
		
		delta = L[4] ? (-delta) : (delta);
		
		for ( j=0; j<7; j++ ) {
			if ( delta  < ((long)(quantwidth/4L)*((long)j+1)) )
				break;
		}
		
		L[3] = (unsigned char)((j>>2) & 1);
		L[2] = (unsigned char)((j>>1) & 1);
		L[1] = (unsigned char)((j>>0) & 1);
		
		predict    = calc_predict( L, predict, quantwidth );
		quantwidth = calc_quantwidth( L, quantwidth );
		
		put_data( adpbuf, L, i );

		if ( (i & 1) == 0 ) 
			asamples++;
	}
	
	*adpsize = asamples;
	
	return 1;
}


//----

#define MAXMLDSZ    100000

#ifdef encoder

/** */
int main(int argc, char **argv) {
    unsigned char readBuff[MAXMLDSZ * 4];
    unsigned char outBuff[MAXMLDSZ];
    int i, j, sz;
    FILE* fp;

    if ((fp = fopen(argv[1], "rb"))==0){
        printf("error: (%s)pcm file not found.\n", argv[1]);
        return 1;
    }
    sz = fread(readBuff, 1, sizeof(readBuff), fp);
    fclose(fp);

    long l = MAXMLDSZ * 2;
    Encode(readBuff, sz, outBuff, &l);

    if ((fp = fopen(argv[2], "wb")) == 0) {
        printf("error: (%s)adpcm file cannot open.\n", argv[2]);
        return 1;
    }
    fwrite(outBuff, 1, l, fp);
    fclose(fp);

    return 0;
}

#else

/** */
int main(int argc, char **argv) {
    unsigned char readBuff[MAXMLDSZ];
    unsigned char outBuff[MAXMLDSZ * 4];
    int i, j, sz;
    FILE* fp;

    if ((fp = fopen(argv[1], "rb"))==0){
        printf("error: (%s)adpcm file not found.\n", argv[1]);
        return 1;
    }
    sz = fread(readBuff, 1, sizeof(readBuff), fp);
    fclose(fp);

    long l;
    Decode(readBuff, sz * 2, outBuff, &l);

    if ((fp = fopen(argv[2], "wb")) == 0) {
        printf("error: (%s)pcm file cannot open.\n", argv[2]);
        return 1;
    }
    fwrite(outBuff, 1, l * 2, fp);
    fclose(fp);

    return 0;
}

#endif
