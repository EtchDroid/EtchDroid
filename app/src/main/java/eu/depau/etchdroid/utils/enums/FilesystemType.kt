package eu.depau.etchdroid.utils.enums

import android.content.Context
import eu.depau.etchdroid.R

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
    UNKNOWN;

    fun getString(context: Context): String {
        return when (this) {
            FAT12 -> context.getString(R.string.fs_fat12)
            FAT16 -> context.getString(R.string.fs_fat16)
            FAT32 -> context.getString(R.string.fs_fat32)
            EXFAT -> context.getString(R.string.fs_exfat)
            NTFS -> context.getString(R.string.fs_ntfs)
            REFS -> context.getString(R.string.fs_refs)
            HFS -> context.getString(R.string.fs_hfs)
            HFSPLUS -> context.getString(R.string.fs_hfsplus)
            APFS -> context.getString(R.string.fs_apfs)
            APT_DATA -> context.getString(R.string.fs_apt_data)
            ISO9660 -> context.getString(R.string.fs_iso9660)
            EXT2 -> context.getString(R.string.fs_ext2)
            EXT3 -> context.getString(R.string.fs_ext3)
            EXT4 -> context.getString(R.string.fs_ext4)
            BTRFS -> context.getString(R.string.fs_btrfs)
            F2FS -> context.getString(R.string.fs_f2fs)
            LUKS -> context.getString(R.string.fs_luks)
            LINUX_SWAP -> context.getString(R.string.fs_linux_swap)
            LINUX_LVM_PV -> context.getString(R.string.fs_linux_lvm_pv)
            UFS -> context.getString(R.string.fs_ufs)
            XFS -> context.getString(R.string.fs_xfs)
            ZFS -> context.getString(R.string.fs_zfs)
            FREE -> context.getString(R.string.fs_free)
            UNFORMATTED -> context.getString(R.string.fs_unformatted)
            UNKNOWN -> context.getString(R.string.fs_unknown)
        }
    }
}