package eu.depau.etchdroid.enums

enum class FilesystemType {
    // Microsoft
    FAT12,
    FAT16,
    FAT32,
    EXFAT,
    NTFS,
    REFS,

    // Apple
    HFS,
    HFSPLUS,
    APFS,
    APT_DATA, // Apple Partition Table stuff

    // ISO 9660
    ISO9660,

    // Linux
    EXT2,
    EXT3,
    EXT4,
    BTRFS,
    F2FS,
    LUKS,
    LINUX_SWAP,
    LINUX_LVM_PV,

    // BSD
    UFS,
    XFS,
    ZFS,

    FREE,
    UNFORMATTED,
    UNKNOWN
}