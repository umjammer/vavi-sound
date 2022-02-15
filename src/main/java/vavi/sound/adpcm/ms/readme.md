<title>vavi.sound.adpcm.ms</title>

MS ADPCM フォーマット関連のクラスを提供します．

## Status

sox と同じ結果になったので完成とみなす。

## Tech-know

 * ACM の M$ ADPCM とは結果が違う</li>

## TODO

 * ~~datetime="111016">一括読み込みを止める~~
 * ~~datetime="111016">↑もしくは EngineeringIO を使う~~
 * ~~datetime="060124">音が汚い~~
 * ~~最後のフラグメントが切れている~~ → #drain()

## TODO
```
/*
 * adpcm.c  codex functions for MS_ADPCM data
 *          (hopefully) provides interoperability with
 *          Microsoft's ADPCM format, but, as usual,
 *          see LACK-OF-WARRANTY information below.
 *
 *      Copyright (C) 1999 Stanley J. Brooks &lt;stabro@megsinet.net&gt;
 *
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Lesser General Public
 *   License as published by the Free Software Foundation; either
 *   version 2 of the License, or (at your option) any later version.
 *
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *   Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
```