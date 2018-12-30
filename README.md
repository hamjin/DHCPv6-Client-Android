# DHCPv6 Client

[![CircleCI](https://circleci.com/gh/Mygod/DHCPv6-Client-Android.svg?style=svg)](https://circleci.com/gh/Mygod/DHCPv6-Client-Android)
[![API](https://img.shields.io/badge/API-21%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=21)
[![Releases](https://img.shields.io/github/downloads/Mygod/DHCPv6-Client-Android/total.svg)](https://github.com/Mygod/DHCPv6-Client-Android/releases)
[![Language: Kotlin](https://img.shields.io/github/languages/top/Mygod/DHCPv6-Client-Android.svg)](https://github.com/Mygod/DHCPv6-Client-Android/search?l=kotlin)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/665184d6cb6d446680c5ec56680c59ce)](https://www.codacy.com/app/Mygod/DHCPv6-Client-Android?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Mygod/DHCPv6-Client-Android&amp;utm_campaign=Badge_Grade)
[![License](https://img.shields.io/github/license/Mygod/DHCPv6-Client-Android.svg)](https://github.com/Mygod/DHCPv6-Client-Android/blob/master/LICENSE)

Requires root, obviously.  
<a href="https://play.google.com/store/apps/details?id=be.mygod.dhcpv6client" target="_blank"><img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" height="60"></a>
or <a href="https://labs.xda-developers.com/store/app/be.mygod.dhcpv6client" target="_blank">XDA Labs</a>

This is [another](https://github.com/realmar/DHCPv6-Client-Android) wrapper of wide-dhcpv6 for Android that's designed
 to work with the latest Android releases.


## Q & A

### Not working?

Try turning off and on service; turning off and on Wi-Fi; wiping app data (which will clean the old DUID) can help too.

### Why do I need to disable background restriction on Android 8.0+?

Google doesn't want background services draining your battery. However, background services are crucial to this app so that you won't
 get an annoying SU notification every time you connect to a network. This app shouldn't have an observable effect on your battery life
 even with battery optimizations off. If it does, please open an issue.

### How is this different from [the other app](https://github.com/realmar/DHCPv6-Client-Android)?

* More stable on devices with kernel 3.9+ (`wide-dhcpv6` is able to use `SO_REUSEPORT`);
* Supports Android 5.0+ while the other app supports Android 4.1 up to [6.0](https://github.com/realmar/DHCPv6-Client-Android/issues/8);
* Completely systemless and doesn't require Busybox; (no extra steps for install/uninstall)
* No closed source components and licensed in Apache 2.0;
* Fewer settings. (open an issue/PR if you think you need some other options, I'm very lazy)


## Open Source Licenses

* [wide-dhcpv6](https://github.com/Mygod/wide-dhcpv6): [BSD](https://sourceforge.net/projects/wide-dhcpv6/)
* AOSP ([platform/bionic](https://android.googlesource.com/platform/bionic/+/68d0150221eb505a576f6ad5ca1f367b4ce547a0)): [Apache 2.0](https://source.android.com/setup/start/licenses#android-open-source-project-license)
