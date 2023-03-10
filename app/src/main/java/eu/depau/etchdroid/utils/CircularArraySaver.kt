package eu.depau.etchdroid.utils

import androidx.collection.CircularArray
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope

class CircularArraySaver<T> : Saver<CircularArray<T>, List<T>> {
    override fun restore(value: List<T>): CircularArray<T> {
        val circularArray = CircularArray<T>(value.size)
        for (item in value) {
            circularArray.addLast(item)
        }
        return circularArray
    }

    override fun SaverScope.save(value: CircularArray<T>): List<T>? {
        val list = mutableListOf<T>()
        for (i in 0 until value.size()) {
            list.add(value[i])
        }
        return list
    }
}