#!/bin/sh

mkdir -p proprietary

unzip -j -o ../../../../$1 system/bin/akmd -d proprietary
touch proprietary/*
chmod 755 proprietary/akmd
