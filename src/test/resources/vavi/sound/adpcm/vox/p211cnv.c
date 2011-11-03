/*
    P211i convert sample
    Input Linear PCM WAV must be 8000Hz, 16bit, mono.
       --> without 'fact' chunk.

    Create by Furuhon 18-Apl-2002   Rev.1.00
*/

#include <stdio.h>

#define MAXMLDSZ    100000

struct status {
    short index;
    short last;
};

short decode(char code, struct status *stat);
short adjust(char code);

short table[49] = {
    16, 17, 19, 21, 23, 25, 28, 31, 34, 37,
    41, 45, 50, 55, 60, 66, 73, 80, 88, 97,
    107, 118, 130, 143, 157, 173, 190, 209, 230, 253,
    279, 307, 337, 371, 408, 449, 494, 544, 598, 658,
    724, 796, 876, 963, 1060, 1166, 1282, 1408, 1552
};

char encode(short samp, struct status *stat)
{
    short code = 0;
    short SS = table[stat->index];
    short diff = samp - stat->last;
    short E = (diff < 0) ? -diff : diff;

    if (diff < 0){
        code |= 8;
    }
    if (E >= SS){
        code |= 4;
        E -= SS;
    }
    if (E >= SS/2){
        code |= 2;
        E -= SS/2;
    }
    if (E >= SS/4){
        code |= 1;
    }

    stat->last = decode(code, stat);
//fprintf(stderr, "%04lX -> %02X\n", (unsigned int) samp, code);
    return (code);
}

short decode(char code, struct status *stat)
{
    short diff, samp;
    short SS = table[stat->index];
    short E = SS/8;
    if (code & 1)  E += SS/4;
    if (code & 2)  E += SS/2;
    if (code & 4)  E += SS;
    if (code & 8) diff = -E;
    else          diff = E;
    samp = stat->last + diff;

    if (samp > 2048)  samp = 2048;
    if (samp < -2048) samp = -2048;

    stat->last = samp;
    stat->index += adjust(code);
    if (stat->index < 0)  stat->index = 0;
    if (stat->index > 48) stat->index = 48;

    return (samp);
}

short adjust(char code)
{
    char c = code & 0x07;
    if (c < 4) return (-1);
    return ((c-3)*2);
}

#ifdef encoder

/** */
int main(int argc, char **argv) {
    struct status stat;
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

    stat.last = 0;
    stat.index = 0;

    for (i = 0, j = 0; i < sz && j < MAXMLDSZ; i += 4, j++) {
        short t = *(short*) (&readBuff[i]) / 16;
        outBuff[j] = encode(t, &stat) << 4;
        t = *(short*) &readBuff[i + 2] / 16;
        outBuff[j] |= encode(t, &stat);
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
int main(int argc, char **argv) {
    struct status stat;
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

    stat.last = 0;
    stat.index = 0;

    for (i = 0, j = 0; i < sz && j < MAXMLDSZ; i++, j += 4) {
	short t = (readBuff[i] & 0xf0) >> 4;
        *(short*) &outBuff[j] = decode(t, &stat) * 16;
        t = readBuff[i] & 0x0f;
        *(short*) &outBuff[j + 2] = decode(t, &stat) * 16;
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
