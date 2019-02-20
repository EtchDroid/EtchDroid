---
title: My USB drive is broken! Plz helppppp :(
permalink: /broken_usb/
layout: page
---

![Unsupported USB drive](/assets/img/broken_usb_1.png){:width="49%"} ![Issues with USB drive](/assets/img/broken_usb_2.png){:width="49%"} 

If you just flashed your USB drive and you see one of the notifications above, your USB drive **IS NOT BROKEN**.

There is litterally **no way** EtchDroid can break your USB drive.

# What does that message actually mean

Most images you'll want to flash with EtchDroid will have a somewhat *weird* format. For example, take an Ubuntu image: that image is specifically made to boot both from a USB drive and a DVD.

This means that the resulting USB drive's file system will be marked as read-only.

Android complains about it because it expects a writable file system. Not finding any, one of these errors will show up telling you how to format it.

Simply unplug the USB drive and plug it into a computer. If the image you provided is valid and EtchDroid was able to write it, it should work just fine.

# How to format the USB drive

Once you're done with the image you've written you may want to format it with a regular file system. This will allow you to use the USB drive to store regular files once again.

## On Android

1. Close EtchDroid, plug your USB drive.
1. Tap the "Unsupported USB Drive" or "Issues with USB drive" notification.
    If it does not show up:
    1. Open Settings
    1. Find "Storage"
    1. Tap the name of the USB drive
    1. If the file explorer opens, tap the menu button, "Storage settings", "Format"
1. Tap "Format USB drive"

Remember to eject the USB drive from settings before unplugging it.

![Format USB drive (Android)](/assets/img/format_android.png){:width="300px"}

## On Ubuntu

This should work for regular Ubuntu with GNOME or Unity and with other GNU/Linux distributions with GNOME. If you have a different desktop environment, please refer to your distribution's documentation.

1. Open the Activities dashboard
1. Type "disk" and open "Disks" or "Disk Utility"
1. Select your USB drive from the left sidebar
1. From the overflow menu on the upper right, select "Format Disk..."
1. Use default settings, then confirm
1. Under "Volumes", select "Free space", then click the "+" button
1. Use maximum size, then go to the next page
1. Enter a name for your USB drive, under type select "For use with all systems and device"
1. Click "Create" and confirm

![Format USB drive (Ubuntu)](/assets/img/format_linux.png){:width="100%"}

## On macOS

**Note:** I don't use macOS so instructions below may not be accurate.

1. Open the Disk utility
1. Select your USB drive
1. There should be an "Erase drive" button somewhere
1. You should find an option to format it with a "FAT" file system

### or 

You can use "SD Memory Formatter" (even though you're not using an SD card, it will work on a USB drive too):

https://www.sdcard.org/downloads/formatter_4/

## On Windows

Use "SD Memory Formatter" (even though you're not using an SD card, it will work on a USB drive too):

https://www.sdcard.org/downloads/formatter_4/

![Format USB drive (Windows)](https://www.pendrivelinux.com/wp-content/uploads/SD-Formatter1.png)
