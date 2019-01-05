package eu.depau.etchdroid.worker.enums

enum class ErrorType {
    UNPLUGGED,
    INPUT_FORMAT_ERROR,
    ENOSPC_ON_USB,
    ENOSPC_ON_DEVICE,
    PERMISSION_DENIED_ANDROID,
    PERMISSION_DENIED_FS,
    PERMISSION_DENIED_USB
}