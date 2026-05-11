# vavi.sound.mfi

♪ Provides MFi sound related classes.

### Abstract

It has almost the same structure as the {@link javax.sound.midi} package.
Please refer to {@link javax.sound.midi} for usage.

## References

 * https://web.archive.org/web/20070614134617/http://www.ne.jp/asahi/phs/phs/ezpmd.html
 * https://web.archive.org/web/20030326000315/http://www.ki.rim.or.jp/~breeze/cgi-bin/502imld.cgi
 * https://archive.org/details/ungoodmld (samples)
 * https://www.awm.jp/~yoya/cache/homepage2.nifty.com/cstation/mfi_1.html

### License

Because the original source is not found
Although it seems that there is no need to worry about the primary license,
I'll note this down just in case.

 * [open i-Melody Project](https://sourceforge.net/projects/imelo/")
 * [Academic Free License (AFL)](http://opensource.org/licenses/academic.php")
 * [GNU Library or Lesser General Public License (LGPL)](http://www.gnu.org/licenses/lgpl.html)

## TODO

* Implementing special instructions
* ~~030825 Handling of MfiFileFormat header information is not implemented, should it be made into a message? Should it be a file format?~~ →
   * ~~030825 There are scales that don't appear~~
* The method of obtaining MfiSystem converter is different from others
* ~~090110 MfiSystem#main does not sound...~~
