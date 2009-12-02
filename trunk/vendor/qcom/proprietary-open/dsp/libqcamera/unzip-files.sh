#!/bin/sh

mkdir -p proprietary

unzip -j -o ../../../../../$1 system/lib/libcamera.so system/lib/libqcamera.so -d proprietary
touch proprietary/*
