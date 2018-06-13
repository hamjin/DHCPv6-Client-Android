cmake_minimum_required(VERSION 3.4.1)

set(ROOT src/main/cpp/wide-dhcpv6)

add_executable( dhcp6c.so
                #src/main/cpp/android-ifaddrs/ifaddrs.c
                #${ROOT}/missing/getifaddrs.c
                #src/main/cpp/ifaddrs-android.cc
                #src/main/cpp/dhcpcd/ifaddrs.c
                src/main/cpp/ifaddrs.cpp
                ${ROOT}/addrconf.c
                ${ROOT}/auth.c
                ${ROOT}/base64.c
                ${ROOT}/cfparse.c
                ${ROOT}/cftoken.c
                ${ROOT}/common.c
                ${ROOT}/config.c
                ${ROOT}/dhcp6_ctl.c
                ${ROOT}/dhcp6c.c
                ${ROOT}/dhcp6c_ia.c
                ${ROOT}/dhcp6c_script.c
                ${ROOT}/if.c
                ${ROOT}/lease.c
                ${ROOT}/prefixconf.c
                ${ROOT}/timer.c
)
add_executable( dhcp6ctl.so
                ${ROOT}/auth.c
                ${ROOT}/base64.c
                ${ROOT}/dhcp6_ctlclient.c
)

target_include_directories(dhcp6c.so PRIVATE ${ROOT})
target_include_directories(dhcp6ctl.so PRIVATE ${ROOT})

add_definitions(-DSYSCONFDIR="./sysconf")
add_definitions(-DLOCALDBDIR="./localdb")
add_definitions(-DLINE_MAX=120)
add_definitions(-DHAVE_GETADDRINFO=1 -DHAVE_GETNAMEINFO=1 -DHAVE_IF_NAMETOINDEX=1 -DHAVE_STRLCPY=1 -DHAVE_STRLCAT=1
                -DHAVE_DAEMON=1 -DHAVE_WARNX=1 -DSTDC_HEADERS=1 -DHAVE_SYS_TYPES_H=1 -DHAVE_SYS_STAT_H=1
                -DHAVE_STDLIB_H=1 -DHAVE_STRING_H=1 -DHAVE_MEMORY_H=1 -DHAVE_STRINGS_H=1 -DHAVE_INTTYPES_H=1
                -DHAVE_STDINT_H=1 -DHAVE_UNISTD_H=1 -DHAVE_FCNTL_H=1 -DHAVE_SYS_IOCTL_H=1 -DHAVE_SYS_TIME_H=1
                -DHAVE_SYSLOG_H=1 -DHAVE_UNISTD_H=1 -DHAVE_IFADDRS_H=1 -DTIME_WITH_SYS_TIME=1 -DHAVE_STRUCT_TM_TM_ZONE=1
                -DHAVE_TM_ZONE=1 -DHAVE_SIG_ATOMIC_T=1 -DGETPGRP_VOID=1 -DSETPGRP_VOID=1 -DRETSIGTYPE=void
                -DHAVE_MKTIME=1 -DHAVE_SELECT=1 -DHAVE_SOCKET=1 -DHAVE_CLOCK_GETTIME=1 -DHAVE_ARC4RANDOM=1
                -DHAVE_ANSI_FUNC=1 -DHAVE_TAILQ_FOREACH_REVERSE_OLD=1 -DHAVE_STDARG_H=1)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wno-format-zero-length")
#set(LOCAL_LDFLAGS "${LOCAL_LDFLAGS} -g -ggdb")
