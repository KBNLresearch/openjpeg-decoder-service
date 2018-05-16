#!/usr/bin/env bash

cd src/main/native

gcc -I /usr/local/include/openjpeg-2.3 \
    -I $JAVA_HOME/include -I $JAVA_HOME/include/linux -fPIC -std=c99 \
    -shared -o ../resources/native/libjp2j.so log.c opj_res.c nl_kb_jp2_Jp2Header.c nl_kb_jp2_Jp2Decode.c \
    -lopenjp2
