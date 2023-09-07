# vavi.sound.adpcm.ms

Provices MS ADPCM codec related classes.

## Status

we assume completed because result is the same as sox.

## Tech-know

 * result is different from ACM M$ ADPCM</li>

## TODO

 * ~~111016 stop reading at once~~
 * ~~111016 ↑ or use EngineeringIO~~
 * ~~060124 sound is dirty~~
 * ~~end of the sound is cut~~ → #drain()

## License

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