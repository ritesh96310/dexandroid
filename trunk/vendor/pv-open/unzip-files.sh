#!/bin/sh

mkdir -p proprietary

unzip -j -o ../../$1 system/etc/pvasflocal.cfg system/lib/libomx_wmadec_sharedlibrary.so system/lib/libomx_wmvdec_sharedlibrary.so system/lib/libpvasfcommon.so system/lib/libpvasflocalpbreg.so system/lib/libpvasflocalpb.so -d proprietary
touch proprietary/*
