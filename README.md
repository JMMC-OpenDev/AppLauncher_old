AppLauncher
===========

Java tool to ease astronomers work flow:
- ensure VO interoperability by managing the SAMP hub;
- discover and automatically start VO applications when needed.

Details available [here](http://www.jmmc.fr/applauncher).

License
=======

Distributed under GPLv3 license.

Goodies are also greatly appreciated if you feel like rewarding us for the job :)

Build
=====

AppLauncher uses `maven` to build from sources.

Its sole dependency is [jMCS](https://github.com/JMMC-OpenDev/jMCS).

You must build in this order : `jmcs` , `smptest`, `smprsc` , `smprun` .
