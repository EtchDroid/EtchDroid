package eu.depau.etchdroid.utils.ktexts

import android.app.Notification
import android.app.Service

fun <T> Service.runForeground(id: Int, notification: Notification, removeNotification: Boolean, body: () -> T): T {
    this.startForeground(id, notification)
    try {
        return body()
    } finally {
        stopForeground(removeNotification)
    }
}