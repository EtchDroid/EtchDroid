package eu.depau.etchdroid.utils

class DeferredInit<T>(val mutable: Boolean = false) {
    private var _value: T? = null

    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): T {
        return _value ?: throw IllegalStateException(
            "Property ${property.name} has not been initialized"
        )
    }

    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) {
        if (this._value != null && !mutable)
            throw IllegalStateException("Property ${property.name} has already been initialized")
        this._value = value
    }

    val isInitialized: Boolean
        get() = _value != null

    val value: T
        get() = _value!!
}

inline fun <reified T> lateInit(mutable: Boolean = false): DeferredInit<T> = DeferredInit(mutable)