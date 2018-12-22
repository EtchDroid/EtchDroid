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
        return when(this) {
            FilesystemType.FAT12        -> context.getString(R.string.fs_fat12)
            FilesystemType.FAT16        -> context.getString(R.string.fs_fat16)
            FilesystemType.FAT32        -> context.getString(R.string.fs_fat32)
            FilesystemType.EXFAT        -> context.getString(R.string.fs_exfat)
            FilesystemType.NTFS         -> context.getString(R.string.fs_ntfs)
            FilesystemType.REFS         -> context.getString(R.string.fs_refs)
            FilesystemType.HFS          -> context.getString(R.string.fs_hfs)
            FilesystemType.HFSPLUS      -> context.getString(R.string.fs_hfsplus)
            FilesystemType.APFS         -> context.getString(R.string.fs_apfs)
            FilesystemType.APT_DATA     -> context.getString(R.string.fs_apt_data)
            FilesystemType.ISO9660      -> context.getString(R.string.fs_iso9660)
            FilesystemType.EXT2         -> context.getString(R.string.fs_ext2)
            FilesystemType.EXT3         -> context.getString(R.string.fs_ext3)
            FilesystemType.EXT4         -> context.getString(R.string.fs_ext4)
            FilesystemType.BTRFS        -> context.getString(R.string.fs_btrfs)
            FilesystemType.F2FS         -> context.getString(R.string.fs_f2fs)
            FilesystemType.LUKS         -> context.getString(R.string.fs_luks)
            FilesystemType.LINUX_SWAP   -> context.getString(R.string.fs_linux_swap)
            FilesystemType.LINUX_LVM_PV -> context.getString(R.string.fs_linux_lvm_pv)
            FilesystemType.UFS          -> context.getString(R.string.fs_ufs)
            FilesystemType.XFS          -> context.getString(R.string.fs_xfs)
            FilesystemType.ZFS          -> context.getString(R.string.fs_zfs)
            FilesystemType.FREE         -> context.getString(R.string.fs_free)
            FilesystemType.UNFORMATTED  -> context.getString(R.string.fs_unformatted)
            FilesystemType.UNKNOWN      -> context.getString(R.string.fs_unknown)
        }
    }
}