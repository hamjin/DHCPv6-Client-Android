# DHCPv6 Client

[![Build Status](https://api.travis-ci.org/Mygod/DHCPv6-Client-Android.svg)](https://travis-ci.org/Mygod/DHCPv6-Client-Android)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Releases](https://img.shields.io/github/downloads/Mygod/DHCPv6-Client-Android/total.svg)](https://github.com/Mygod/DHCPv6-Client-Android/releases)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/665184d6cb6d446680c5ec56680c59ce)](https://www.codacy.com/app/Mygod/DHCPv6-Client-Android?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Mygod/DHCPv6-Client-Android&amp;utm_campaign=Badge_Grade)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Requires root, obviously.  
<a href="https://play.google.com/store/apps/details?id=be.mygod.dhcpv6client" target="_blank"><img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="60"></a>
or <a href="https://labs.xda-developers.com/store/app/be.mygod.dhcpv6client" target="_blank">XDA Labs</a>

This is [another](https://github.com/realmar/DHCPv6-Client-Android) wrapper of wide-dhcpv6 for Android that's designed
 to work with the latest Android releases.


## Q & A

### Not working?

Try turning off and on service; turning off and on Wi-Fi; wiping app data (which will clean the old DUID) can help too.

### How is this different from the other app?

* Supports Android 5.0+ while the other app supports Android 4.1 up to [6.0](https://github.com/realmar/DHCPv6-Client-Android/issues/8);
* Completely systemless and doesn't require Busybox; (no extra steps for install/uninstall)
* Fewer settings. (open an issue/PR if you think you need some other options, I'm very lazy)


## Open Source Licenses

* [wide-dhcpv6](https://github.com/Mygod/wide-dhcpv6): [BSD](https://sourceforge.net/projects/wide-dhcpv6/)
* AOSP ([platform/bionic](https://android.googlesource.com/platform/bionic/+/68d0150221eb505a576f6ad5ca1f367b4ce547a0)): [Apache 2.0](https://source.android.com/setup/start/licenses#android-open-source-project-license)
