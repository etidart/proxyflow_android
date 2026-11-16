#!/bin/bash
set -euo pipefail
mkdir build_artifacts
cp -r hev-socks5-tunnel/ build_artifacts/jni/
cd build_artifacts/
NDK_PROJECT_PATH="." ndk-build
mkdir -p ../proxyflow_android/app/src/main/jniLibs/
cp -r libs/* ../proxyflow_android/app/src/main/jniLibs/
cd ..
rm -rf build_artifacts
