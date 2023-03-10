package eu.depau.etchdroid.ui.utils

// SPDX-License-Identifier: Apache-2.0
// This file is part of World Clock Tile.

import android.content.SharedPreferences
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

/**
 * This helper provides a property delegate that can be used to remove the boilerplate of reading
 * and writing values from SharedPreferences.
 * Types directly supported by SharedPreferences are automatically supported, as well as enum
 * classes.
 * For other types, you can provide a marshal and unmarshal function.
 */

interface SettingsChangeNotifier {
    fun notifyListeners()
}

@Suppress("UNCHECKED_CAST")
class SharedSettingsPropertyDelegate<SettingsType : SettingsChangeNotifier, ReturnType, SharedPrefsType>(
    private val sharedPrefsTypeClass: KClass<*>,
    private val sharedPrefs: SharedPreferences,
    private val key: String,
    private val defaultValue: ReturnType,
    val marshal: (ReturnType) -> SharedPrefsType,
    val unmarshal: (SharedPrefsType) -> ReturnType = {
        it as ReturnType
    }
) {
    operator fun getValue(thisRef: SettingsType, property: KProperty<*>) =
        when (sharedPrefsTypeClass) {
            String::class -> unmarshal(
                sharedPrefs.getString(
                    key,
                    marshal(defaultValue) as String?
                ) as SharedPrefsType
            )
            Int::class -> unmarshal(
                sharedPrefs.getInt(
                    key,
                    marshal(defaultValue) as Int
                ) as SharedPrefsType
            )
            Boolean::class -> unmarshal(
                sharedPrefs.getBoolean(
                    key,
                    marshal(defaultValue) as Boolean
                ) as SharedPrefsType
            )
            Float::class -> unmarshal(
                sharedPrefs.getFloat(
                    key,
                    marshal(defaultValue) as Float
                ) as SharedPrefsType
            )
            Long::class -> unmarshal(
                sharedPrefs.getLong(
                    key,
                    marshal(defaultValue) as Long
                ) as SharedPrefsType
            )
            Set::class -> unmarshal(
                sharedPrefs.getStringSet(
                    key,
                    marshal(defaultValue) as Set<String>?
                ) as SharedPrefsType
            )
            else -> throw IllegalArgumentException("Unsupported type")
        }

    operator fun setValue(thisRef: SettingsType, property: KProperty<*>, value: ReturnType) {
        sharedPrefs.edit().apply {
            when (sharedPrefsTypeClass) {
                String::class -> putString(key, marshal(value) as String)
                Int::class -> putInt(key, marshal(value) as Int)
                Boolean::class -> putBoolean(key, marshal(value) as Boolean)
                Float::class -> putFloat(key, marshal(value) as Float)
                Long::class -> putLong(key, marshal(value) as Long)
                Set::class -> putStringSet(key, marshal(value) as Set<String>)
                else -> throw IllegalArgumentException("Unsupported type")
            }
            apply()
            thisRef.notifyListeners()
        }
    }
}

@Suppress("UNCHECKED_CAST")
@JvmName("fallbackDelegate")
inline fun <SettingsType : SettingsChangeNotifier, ReturnType, reified SharedPrefsType> SharedPreferences.delegate(
    key: String,
    defaultValue: ReturnType,
    noinline unmarshal: (SharedPrefsType) -> ReturnType,
    noinline marshal: (ReturnType) -> SharedPrefsType
): SharedSettingsPropertyDelegate<SettingsType, ReturnType, SharedPrefsType> {
    return SharedSettingsPropertyDelegate(
        SharedPrefsType::class,
        this,
        key,
        defaultValue,
        marshal,
        unmarshal
    )
}

@JvmName("sametypeDelegate")
inline fun <SettingsType : SettingsChangeNotifier, reified T> SharedPreferences.delegate(
    key: String,
    defaultValue: T,
    noinline unmarshal: (T) -> T = { it },
    noinline marshal: (T) -> T = { it }
) = delegate<SettingsType, T, T>(key, defaultValue, unmarshal, marshal)

@JvmName("enumDelegate")
inline fun <SettingsType : SettingsChangeNotifier, reified T : Enum<T>> SharedPreferences.delegate(
    key: String,
    defaultValue: T,
    noinline unmarshal: (String) -> T = { enumValueOf(it) },
    noinline marshal: (T) -> String = { it.name }
) = delegate<SettingsType, T, String>(key, defaultValue, unmarshal, marshal)
