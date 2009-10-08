package vavi.sound.adpcm.dvi;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class Adpcm {

   public static final int RTP_PT = 5;

   public String codecName() {
      return("DVI ADPCM");
   } 

   public int getSampleRate() {
      return 8000;
   }

   private static final int indexTable[] = {
      -1, -1, -1, -1, 2, 4, 6, 8,
      -1, -1, -1, -1, 2, 4, 6, 8,
   };

   private static final int stepsizeTable[] = {
      7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
      19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
      50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
      130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
      337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
      876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
      2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
      5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
      15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
   };

   public class AdpcmState {    
      int valprev, index;
   }

   public Object createState() {
      return new AdpcmState();
   }

   public void initState(Object state) {
      ((AdpcmState)state).valprev = 0;
      ((AdpcmState)state).index = 0;
   }

   public int code(Object state, short[] input, int inp, int len, byte[] output, int outp) {
      int sign;
      int delta;
      int valprev = ((AdpcmState)state).valprev;
      int vpdiff;
      int index = ((AdpcmState)state).index;
      int step = stepsizeTable[index];
      int outputbuffer = 0;
      int bufferstep = 1;

      Convert.short2byte((short)valprev, output, outp);
      output[outp + 2] = (byte)index;
      output[outp + 3] = (byte)0;
      outp += 4;

      int count = len;
      while (--count >= 0) {

         delta = input[inp++] - valprev;
         sign = (delta < 0) ? 8 : 0;
         if ( 0 != sign ) delta = (-delta);

         int tmp = 0;
         vpdiff = step >> 3;
         if ( delta > step ) {
            tmp = 4;
            delta -= step;
            vpdiff += step;
         }
         step >>= 1;
         if ( delta > step  ) {
            tmp |= 2;
            delta -= step;
            vpdiff += step;
         }
         step >>= 1;
         if ( delta > step ) {
            tmp |= 1;
            vpdiff += step;
         }
         delta = tmp;

         if ( 0 != sign )
            valprev -= vpdiff;
         else
            valprev += vpdiff;

         if ( valprev > 32767 )
            valprev = 32767;
         else if ( valprev < -32768 )
            valprev = -32768;

         delta |= sign;

         index += indexTable[delta];
         if ( index < 0 ) index = 0;
         if ( index > 88 ) index = 88;
         step = stepsizeTable[index];

         if ( 0 != bufferstep ) {
            outputbuffer = (delta << 4) & 0xf0;
         } else {
            output[outp++] = (byte)((delta & 0x0f) | outputbuffer);
         }
         bufferstep = (0 == bufferstep) ? 1 : 0;
      }

      if ( 0 == bufferstep )
         output[outp++] = (byte)outputbuffer;

      ((AdpcmState)state).valprev = valprev;
      ((AdpcmState)state).index = index;
      return (len / 2) + 4;
   }

   public int decode(Object state, byte[] input, int inp, int len, short[] output, int outp) {
      int sign;
      int delta;
      int vpdiff;
      int valprev = Convert.byte2short(input, inp);
      int index = input[inp + 2];
      int inputbuffer = 0;
      int bufferstep = 0;

      if ( index < 0 ) index = 0;
      else if ( index > 88 ) index = 88;

      int step = stepsizeTable[index];

      inp += 4;

      len = (len - 4) * 2;

      int count = len;
      while(count-- > 0) {

         if ( 0 == bufferstep ) {
            inputbuffer = input[inp++];
            delta = (inputbuffer >> 4) & 0xf;
            bufferstep = 1;
         } else {
            delta = inputbuffer & 0xf;
            bufferstep = 0;
         }

         index += indexTable[delta];
         if ( index < 0 ) index = 0;
         else if ( index > 88 ) index = 88;

         sign = delta & 8;
         delta = delta & 7;

         vpdiff = step >> 1;
         if ( (delta & 4) == 4 ) vpdiff += (step << 2);
         if ( (delta & 2) == 2 ) vpdiff += (step << 1);
         if ( (delta & 1) == 1 ) vpdiff += step;
         vpdiff >>= 2;

         if ( 0 != sign )
            valprev -= vpdiff;
         else
            valprev += vpdiff;

         if ( valprev > 32767 )
            valprev = 32767;
         else if ( valprev < -32768 )
            valprev = -32768;

         step = stepsizeTable[index];
         output[outp++] = (short) valprev;
      }

      ((AdpcmState)state).valprev = valprev;
      ((AdpcmState)state).index = index;
      return len;
   }

   /** */
   public static void main(String[] args) throws Exception {
       if ("-e".equals(args[2])) {
           encode(args);
       } else {
           decode(args);
       }
   }

   /** */
   static void encode(String[] args) throws Exception {
       Adpcm adpcm = new Adpcm();
       AdpcmState state = adpcm.new AdpcmState();
       InputStream is = new BufferedInputStream(new FileInputStream(args[0]));
       OutputStream os = new BufferedOutputStream(new FileOutputStream(args[1]));
       byte[] buf = new byte[1024];
       while (is.available() > 0) {
           int l = 0;
           while (l < buf.length) {
               int r = is.read(buf, l, buf.length - l);
               if (r == -1) {
                   break;
               }
               l += r;
           }
           short[] sbuf = new short[l / 2];
           for (int i = 0; i < sbuf.length; i++) {
               sbuf[i]  = buf[i * 2];
               sbuf[i] |= buf[i * 2 + 1] << 8;
           }
           byte[] abuf = new byte[sbuf.length / 2 + 4];
           adpcm.code(state, sbuf, 0, sbuf.length, abuf, 0);
           for (int i = 0; i < sbuf.length / 2; i++) {
               os.write(abuf[i]);
           }
       }
       os.close();
       is.close();
   }

   /** */
   static void decode(String[] args) throws Exception {
       Adpcm adpcm = new Adpcm();
       AdpcmState state = adpcm.new AdpcmState();
       InputStream is = new BufferedInputStream(new FileInputStream(args[0]));
       OutputStream os = new BufferedOutputStream(new FileOutputStream(args[1]));
       byte[] buf = new byte[1024];
       while (is.available() > 0) {
           int l = 0;
           while (l < buf.length) {
               int r = is.read(buf, l, buf.length - l);
               if (r == -1) {
                   throw new EOFException();
               }
               l += r;
           }
           short[] sbuf = new short[l * 2];
           adpcm.decode(state, buf, 0, l, sbuf, 0);
           for (int i = 0; i < sbuf.length; i++) {
               os.write(sbuf[i] & 0x00ff);
               os.write((sbuf[i] & 0xff00) >> 8);
           }
       }
       os.close();
       is.close();
   }
}
