# vavi.sound.mfi

Provides MFi sound related classes.

## Abstract

It has almost the same structure as the {@link javax.sound.midi} package.
Please refer to {@link javax.sound.midi} for usage.

When playing with {@link vavi.sound.mfi.MfiSystem.getSequencer()}
"#Real Time Sequencer" in system property javax.sound.midi.Sequencer
Please make it clear. {@link vavi.sound.midi.VaviSequencer}
If it becomes the default sequencer {@link vavi.sound.mfi.MfiSystem#getMetaEventListener()}
will be registered twice.

## TODO

 * Implementing special instructions
 * ~~030825 Handling of MfiFileFormat header information is not implemented, should it be made into a message? Should it be a file format?~~ →
   * ~~030825 There are scales that don't appear~~
 * The method of obtaining MfiSystem converter is different from others
 * ~~090110 MfiSystem#main does not sound...~~

## License

Because the original source is not found
Although it seems that there is no need to worry about the primary license,
I'll note this down just in case.

 * [open i-Melody Project](http://www.xucker.jpn.org/ood/java/imelody/")
 * [Academic Free License (AFL)](http://opensource.org/licenses/academic.php")
 * [GNU Library or Lesser General Public License (LGPL)](http://www.gnu.org/licenses/lgpl.html)
