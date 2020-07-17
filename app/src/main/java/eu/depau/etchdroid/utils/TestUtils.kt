package eu.depau.etchdroid.utils

import eu.depau.etchdroid.BuildConfig

fun assertDebug(condition: Boolean) = assertDebug(condition) { "Assertion failed" }

inline fun assertDebug(condition: Boolean, lazyMessage: () -> Any) {
    if (BuildConfig.DEBUG && !condition) {
        throw AssertionError(lazyMessage())
    }
}