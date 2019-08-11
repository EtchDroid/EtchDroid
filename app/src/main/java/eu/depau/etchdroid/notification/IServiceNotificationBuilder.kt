package eu.depau.etchdroid.notification

import android.app.Notification

interface IServiceNotificationBuilder {
    fun build(): Notification
}