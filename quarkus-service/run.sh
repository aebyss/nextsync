#!/bin/bash

set -e

#Adjust if libaudio.so is in a different folder
export LD_LIBRARY_PATH=$(pwd)

JAVA_OPTS="--enable-preview --enable-native-access=ALL-UNNAMED -Djava.library.path=$(pwd)" \
  ./mvnw quarkus:dev
