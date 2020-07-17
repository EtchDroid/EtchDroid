#!/bin/bash

set +e

source ./ci-scripts/vars.sh

export SDK_FILENAME=sdk-tools-linux-4333796.zip
export ANDROID_API_LEVEL=29
export ANDROID_BUILD_TOOLS_VERSION=29.0.3
export NDK_VERSION=21.0.6113669
export CMAKE_VERSION=3.10.2.4988404

mkdir -p "$ANDROID_HOME"

curl --silent --show-error --location --fail --retry 3 --output "/tmp/${SDK_FILENAME}" "https://dl.google.com/android/repository/${SDK_FILENAME}"
unzip -q "/tmp/${SDK_FILENAME}" -d "${ANDROID_HOME}"
rm "/tmp/${SDK_FILENAME}"

mkdir ~/.android
echo '### User Sources for Android SDK Manager' > ~/.android/repositories.cfg
yes | sdkmanager --licenses >/dev/null 2>&1
sdkmanager --update

yes | sdkmanager \
  'tools' \
  'platform-tools' \
  "build-tools;$ANDROID_BUILD_TOOLS_VERSION" \
  "platforms;android-$ANDROID_API_LEVEL" \
  'extra-android-m2repository' \
  'extra-google-m2repository' \
  'ndk-bundle' \
  "ndk;$NDK_VERSION" \
  "cmake;$CMAKE_VERSION"

