#!/usr/bin/env bash

cd ../termux-packages

for package in "$@"
do
    ./build-package.sh -a $TERMUX_ARCH $package
done

cd ../termux-wrapper

./install-packages.sh $TERMUX_DEBDIR $ASSETS_PATH