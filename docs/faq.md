---
title: FAQ
permalink: /faq/
layout: page
---

### My USB drive is broken, please help!

It's not. Calm down and read this: [My USB drive is broken! Plz helppppp :(](/broken_usb/)

### I flashed [insert OS name here] ISO and it doesn't boot

EtchDroid is only able to write installers for OS images that explicitly support being written to USB drives.

This **does not include Windows**. Windows images need to be handled a lot differently and research is being done to support them.

Other OSes such as macOS and old GNU/Linux distros (such as Damn Small Linux) are also not supported for the same reason.

Known working OS images:
- Ubuntu and all the derivatives
- Debian
- Fedora
- Arch Linux
- Raspberry Pi SD card images (you need to extract the zip file)

### I'm trying to flash this Raspberry Pi image but the SD card does not appear on the list

Due to limitations in the Android API, you **cannot** use the internal SD card slot.

You need to purchase a USB SD card adapter and use that instead.

Also, beware that most Raspberry Pi distributions come in a zip file which contains an `.img` file. You need to extract it first using another app of your choice.

### When I tap "Write DMG image", an empty file picker is shown

That's because you don't have any DMG files on your phone. Download one and see if it shows up.

### When I tap a USB drive from the list, the app hangs

You tried to use the app but it crashed, then you ran it again without unplugging the USB drive. It hangs because as it crashed, it didn't get the chance to clean up the USB interface, so now it's in an inconsistent state. Reattach the USB drive and restart the app.

### I'm getting this "Socket operation on non socket" error

That happens for a number of reasons:

- You unplugged your USB drive while it was being written to
- Your USB drive is defective
- Your USB OTG adapter is defective
- You're using a USB hub
- You're on Android 9 and recently changed your phone's language (yep!)

The first 4 can be easily solved by replacing a component or just being careful not to unplug the USB drive while it's being worked on. The last one can be solved by rebooting your phone.

### My issue is not listed here

File it on GitHub: [link](https://github.com/Depau/EtchDroid/issues)
