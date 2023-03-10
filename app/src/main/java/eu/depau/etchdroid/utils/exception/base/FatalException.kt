package eu.depau.etchdroid.utils.exception.base

abstract class FatalException(message: String, cause: Throwable? = null) :
    EtchDroidException(message, cause)