# vavi.sound.adpcm.ym2608

Provides YAMAHA (YM2608) ADPCM codec related classes.

## Status

completed (maybe)

## TODO

 * ~~somehow the sound is small~~
 * ~~OutputStream~~
 * the C reference is strange. around `i =` at `encode` may be < 0.

## License

none

## Basic knowledge of ADPCM and the ADPCM specifications of YM2608

[source](https://web.archive.org/web/20111013151432/http://hackipedia.org/Platform/Sega/Genesis/hardware,%20FM%20synthesis,%20YM2608/html/adpcm.html)

### ADPCM

ADPCM differs from MPEG in that it compresses the amplitude information of one sample of linear PCM data. The basic
principle of ADPCM is that data compression is performed using differences between samples, but there are many types of
codecs, and the YM2608 ADPCM specification is one of these codecs. Many codecs convert one sample of data into 2 to 4
bits, and this ADPCM data does not completely return to its original waveform, and the performance of the codec is
reflected in its "reproducibility of the original waveform."

In the case of 16-bit linear PCM, a maximum of 16 bits of information is required to express the difference from the
previous sample, but if this is recorded as is (DPCM format), the data cannot be reduced. Therefore, ADPCM compresses
data by sacrificing the accuracy of the difference. Specifically, this process dynamically changes the step size
according to the encoding calculation rules of the codec, and converts each difference value to 2 to 4 bits of
precision.

For example, YM2608 ADPCM has 4 bits of information, so there are 16 types of predicted amplitude values divided by
step size. Since the difference is divided into halves depending on whether the difference is positive or negative,
there are eight types of predicted values that are actually applied, and the encoder selects the location closest to
these eight types of predicted values. Furthermore, the predicted values for two of these points are treated as
meaning outside the predicted value range, which is either lower or higher, so there are six types of predicted values
with the smallest error. ADPCM encoding performs conversion processing into a 4-bit value while comparing the actual
difference value and the predicted value in this way.

The step size is the resolution width of one sample, and 16-bit PCM with a step size of 1 is expressed in 65536 steps
from 0 to 65535. 16-bit PCM with a step size of 4 is expressed in 16384 steps from 0x0000 to 0xFFFC, where the lower two
bits are 0. If the step size is 8192, it is expressed in 16 steps from 0x0000 to 0xF000 with the lower 12 bits set to 0.
In brute force, 8-bit PCM can also be expressed as 16-bit PCM with a step size of 256 (although the recording range is
the upper 8 bits). The step size is also called the quantization width.

For this reason, when converting the step size from a small value to a large value (ADPCM encoding), the waveform
accuracy deteriorates as the error between the difference value and the boundary value of the step size increases.
Furthermore, even when conversion (ADPCM decoding) is performed to restore the step size to its original value, the
information corresponding to the lost precision cannot be reproduced. This means
sacrificing "the reproducibility of the original waveform" and "the accuracy of the difference."

Dynamic changes in step size are updated by codec encoding operations for each sample. This is because if the step size
is recorded as data, data compression will not be efficient. If an appropriate step size for the original waveform is
used during encoding, the conversion accuracy can be kept as high as possible. This means that the better the encoder
and its waveform prediction rules, the higher the recovery rate of ADPCM data.

### Encoding

YM2608's ADPCM algorithm is relatively simple. YM2608's ADPCM converts 16-bit PCM to 4-bit ADPCM data. Also, the step
size is bounded by a linear range of 1/4 unit. This encoding procedure is described below.

1. Initialize the predicted value and step size along with obtaining the first sample X1. The initial value of the
   predicted value x0=0, and the initial value of the step size S0=127.

2. To find the ADPCM data An, calculate the difference between Xn and the predicted value xn, and find the difference
   ⊿n.

3. Divide the absolute value of ⊿n by Sn (I=|⊿n|/Sn) and determine the lower 3 bits from the operation result of I as
   shown in the table below.

| An | L3 | L2 | L1 | I= ⊿n&#124;/Sn       |   f    |
|:--:|:--:|:--:|:--:|:---------------------|:------:|
| 0  | 0  | 0  | 0  | I &lt; 1/4           | 57/64  |
| 1  | 0  | 0  | 1  | 1/4 &lt;= I &lt; 2/4 | 57/64  |
| 2  | 0  | 1  | 0  | 2/4 &lt;= I &lt; 1/4 | 57/64  |
| 3  | 0  | 1  | 1  | 3/4 &lt;= I &lt; 4/4 | 57/64  |
| 4  | 1  | 0  | 0  | 4/4 &lt;= I &lt; 5/4 | 77/64  |
| 5  | 1  | 0  | 1  | 5/4 &lt;= I &lt; 6/4 | 102/64 |
| 6  | 1  | 1  | 0  | 6/4 &lt;= I &lt; 7/4 | 128/64 |
| 7  | 1  | 1  | 1  | 7/4 &lt;= I          | 153/64 |

If the division value I between ⊿n and Sn is 1/4 or more and less than 2/4, An is determined to be 1. In other words, if
the actual difference exceeds the predicted step size (less than 1/4 or more than 7/4), it may deviate significantly
from the predicted value, and this will begin to affect data reproducibility. represents.

4. If ⊿n is positive, set the most significant bit (bit 4) of An to 0; if ⊿n is negative, set it to 1.

5. Find the predicted value x(n+1). The predicted value is updated using each bit value of An and the following formula.

```
x(n+1) = (1 - 2 x L4) x (L3 + L2 / 2 + L1 / 4 + 1 / 8) x ⊿n + xn
```

6. Find the step size S(n+1). The step size is updated using the relationship between the lower 3 bits of An and f in
   the table above.

```
S(n+1) = f(An) x Sn
```

7. Saturate the step size Sn. The ADPCM step size of YM2608 is determined to be a minimum of 127 and a maximum of 24576.

8. Repeat steps 2 to 7 until the end of the data.

### Decoding

Encoding 5 and 6 are the formulas for calculating the predicted value and step size, and the predicted value xn becomes
the restored value Xn.

1. Initialize the predicted value and step size while acquiring the first ADPCM sample A1. The initial value of the
   predicted value x0=0, and the initial value of the step size S0=127.

2. Find the predicted value x(n+1). To update the predicted value, use each bit value of An and the table as in
   encoding.

3. Let the predicted value x(n+1) be the restored value X(n).

4. Clip the restored value X(n) to the range of -32768 to 32767 as appropriate.

5. Find the step size S(n+1). The step size is updated using the relationship between the lower 3 bits of An and f in
   the table above.

6.Saturate the step size S(n+1). The ADPCM step size of YM2608 is determined to be a minimum of 127 and a maximum of
24576.

7.Repeat steps 2 to 6 until the end of ADPCM data.
