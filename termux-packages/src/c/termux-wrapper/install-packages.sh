#!/usr/bin/env bash

DEBDIR="$1"
DESTDIR="$2"

echo "Creating output tarball"

cd "$DEBDIR"

(
	rm -f pkg_info.txt
	rm -Rf root
	rm -Rf tmp
) > /dev/null 2>&1

mkdir root

# Extract packages
ls | sort | grep \\.deb | grep -v -- '-dev_' | while read pkg; do
	mkdir tmp
	cd tmp
	ar -x ../$pkg
	tar -xJf control.tar.xz
	cat control >> ../pkg_info.txt
	echo >> ../pkg_info.txt
	cd ../root
	tar -xJf ../tmp/data.tar.xz
	cd ..
	rm -Rf tmp
done

mkdir -p $DESTDIR

# Create info files
sha256sum pkg_info.txt > pkg_fprint.txt
cp pkg_info.txt pkg_fprint.txt root/data/data/*/files/termux
cp pkg_info.txt pkg_fprint.txt $DESTDIR

# Cleanup unneeded files
pushd root/data/data/*/files/termux/usr > /dev/null
rm -Rf include
rm -Rf share/doc share/info share/man

# Create output tar
cd ../..
tar -cf $DESTDIR/packages.tar termux

# Cleanup
popd > /dev/null
rm -Rf root pkg_info.txt pkg_fprint.txt