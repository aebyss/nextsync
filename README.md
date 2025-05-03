# Nextsync – Native Nextcloud Sync Service with jextract

This project implements a Java-based file synchronization service for Nextcloud using native libraries via the Foreign Function & Memory API (FFM API). Native interactions with `libcurl` are exposed in Java through automatically generated bindings using `jextract`.

## Overview

- Java 22 with Foreign Function & Memory API
- Native C library (`libsync.so`) using libcurl and libxml2
- REST API built with Quarkus
- Folder watcher that uploads files on change
- Uses `jextract` for generating Java bindings (no JNI)

## Project Structure

native-worker/ # C source code (sync.c, sync.h)
quarkus-service/ # Quarkus-based Java REST service
src/main/java/ # jextract-generated Java bindings
libsync.so # Compiled native shared library


## Features

- Upload local files to Nextcloud via HTTP PUT
- List remote folders using WebDAV PROPFIND
- REST endpoints to trigger upload or folder listing
- File watcher that observes a local directory
- Fully JNI-free native integration using `jextract` and the FFM API

## Building the Project

### 1. Compile the native library

```bash
cd native-worker
gcc -fPIC -shared -o libsync.so src/sync.c -lcurl -lxml2

Make sure libsync.so is discoverable via the JVM (e.g., by setting LD_LIBRARY_PATH or placing it in the project root).

### Run jextract

jextract --source \
  --target-package com.nativecloud.sync \
  --output src/main/java \
  -I include \
  include/sync.h
  ````

or use the run script run.sh

## Start the quarkus service

cd quarkus-service
./mvnw compile quarkus:dev

