#!/bin/bash

set -e

HEADER_DIR=native-worker/include
SRC_DIR=native-worker/src
OUTPUT_DIR=quarkus-service/src/main/java
LIB_NAME=sync
PACKAGE_NAME=com.nativecloud
LIB_OUTPUT_DIR=quarkus-service

echo "Compiling C code to lib${LIB_NAME}.so..."
clang -Iinclude -I/usr/include/libxml2 -shared -fPIC \
  -o quarkus-service/libsync.so native-worker/src/sync.c -lcurl -lxml2

./jextract-22/bin/jextract \
  --library sync \
  -I ${HEADER_DIR} \
  --use-system-load-library \
  --output ${OUTPUT_DIR} \
  --target-package com.nativecloud.sync \
  ${HEADER_DIR}/sync.h

echo "Done: lib${LIB_NAME}.so built and Java bindings generated in ${OUTPUT_DIR}"
