#!/bin/sh

mkdir -p proprietary

unzip -j -o ../../../$1 system/app/FieldTest.apk system/bin/debug_tool system/bin/htclogkernel -d proprietary
touch proprietary/*

chmod 755 proprietary/debug_tool
chmod 755 proprietary/htclogkernel
