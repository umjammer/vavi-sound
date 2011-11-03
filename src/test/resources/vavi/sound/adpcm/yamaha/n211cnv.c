/*
    Nxxxi convert sample
    Input Linear PCM WAV must be 8000Hz, 16bit, mono.
       --> without 'fact' chunk.

    Create by Furuhon 19-Apl-2004   Rev.1.00
*/

#include <stdio.h>

#define MAXMLDSZ    100000

struct status {
    short index;
    short last;
};

short adjust(char code, int SS)
{
    switch (code & 0x07){
        case 0x00:
        case 0x01:
        case 0x02:
        case 0x03:
            SS = (SS * 115) / 128;
            break;

        case 0x04:
            SS = (SS * 307) / 256;
            break;

        case 0x05:
            SS = (SS * 409) / 256;
            break;

        case 0x06:
            SS = (SS * 2);
            break;

        case 0x07:
            SS = (SS * 307) / 128;
            break;
    }

    if (SS < 127)       SS = 127;
    if (SS > 32768*3/4) SS = 32768*3/4;

    return (short)SS;
}

short decode(char code, struct status *stat)
{
    int diff, E, SS, samp;

    SS = stat->index;
    E = SS/8;
    if (code & 0x01)
        E += SS/4;
    if (code & 0x02)
        E += SS/2;
    if (code & 0x04)
        E += SS;
    diff = (code & 0x08) ? -E : E;
    samp = stat->last + diff;

    if (samp > 32767)  samp = 32767;
    if (samp < -32768) samp = -32768;

    stat->last = (short)samp;
    stat->index = adjust(code, SS);

    return( stat->last );
}

char encode(short samp, struct status *stat)
{
    int code, diff, E, SS;

    SS = stat->index;
    code = 0x00;
    if( (diff = samp - stat->last) < 0 )
        code = 0x08;
    E = diff < 0 ? -diff : diff;
    if( E >= SS ) {
        code = code | 0x04;
        E -= SS;
    }
    if( E >= SS/2 ) {
        code = code | 0x02;
        E -= SS/2;
    }
    if( E >= SS/4 ) {
        code = code | 0x01;
    }

    stat->last = decode((char)code, stat);
//fprintf(stderr, "%04lX -> %02X\n", (unsigned int) samp, code);
    return( (char)code );
}

#ifdef encoder

/**
 * @param argv 1: pcm 16bit signed little endian, 2: out adpcm
 */
int main(int argc, char** argv) {
    struct status stat;
    unsigned char readBuff[MAXMLDSZ * 4];
    unsigned char outBuff[MAXMLDSZ];
    int i, j, sz;
    FILE* fp;

    if ((fp = fopen(argv[1], "rb")) == 0) {
        printf("error: (%s)pcm file not found.\n", argv[1]);
        return 1;
    }
    sz = fread(readBuff, 1, sizeof(readBuff), fp);
    fclose(fp);

    stat.last = 0;
    stat.index = 127;

    for (i = 0, j = 0; i < sz && j < MAXMLDSZ; i += 4, j++) {
        short t = *(short*) &readBuff[i];
        outBuff[j] = encode(t, &stat) & 0xf;
        t = *(short*) &readBuff[i + 2];
        outBuff[j] |= encode(t, &stat) << 4;
    }

    if ((fp = fopen(argv[2], "wb")) == 0) {
        printf("error: (%s)adpcm file cannot open.\n", argv[2]);
        return 1;
    }
    fwrite(outBuff, 1, j, fp);
    fclose(fp);

    return 0;
}

#else

/** */
int main(int argc, char** argv) {
    struct status stat;
    unsigned char readBuff[MAXMLDSZ];
    unsigned char outBuff[MAXMLDSZ * 4];
    int i, j, sz;
    FILE* fp;

    if ((fp = fopen(argv[1], "rb")) == 0) {
        printf("error: (%s)adpcm file not found.\n", argv[1]);
        return 1;
    }
    sz = fread(readBuff, 1, sizeof(readBuff), fp);
    fclose(fp);

    stat.last = 0;
    stat.index = 127; // TODO ???

    for (i = 0, j = 0; i < sz && j < MAXMLDSZ; i++, j += 4) {
        short t = readBuff[i] & 0x0f;
        *(short*) &outBuff[j] = decode(t, &stat);
	t = (readBuff[i] & 0xf0) >> 4;
        *(short*) &outBuff[j + 2] = decode(t, &stat);
    }

    if ((fp = fopen(argv[2], "wb")) == 0) {
        printf("error: (%s)pcm file cannot open.\n", argv[2]);
        return 1;
    }
    fwrite(outBuff, 1, j, fp);
    fclose(fp);

    return 0;
}

#endif
