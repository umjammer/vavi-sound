# SSRC

Shibatch Sampling Rate Converter

## Usage

### use vector, giving up bit-exactness

```
-Dvavi.util.Convolver=vector -Dvavi.sound.pcm.resampling.ssrc.fir=vector -Dvavi.util.Fft=vectorradix4
```

| 44.1k source, 2 min stereo |    scalar (default)    | all vector  |  speedup  |
|---|---|---|---|
| →48k, warm JVM             | ~895 ms                | ~430–455 ms | ~2.0x     |
| →8k, warm JVM              | 492 ms (original code) | ~255–265 ms | ~1.8–1.9x |
| →48k, fresh JVM, one run   | 1.15 s                 | 1.04 s      | ~1.1x     |
| →8k, fresh JVM, one run    | 0.75 s                 | 0.81 s      | 0.93x     |

## Reference

 * [Original](https://github.com/shibatch/SSRC)

### License

* [LGPL](http://www.gnu.org/copyleft/lesser.html)
* modification
   * ported to java

### Original License

* [LGPL](http://www.gnu.org/copyleft/lesser.html)

## TODO

 * ~~javax.sound spi~~
 * https://github.com/wendykierp/JTransforms (fft)
