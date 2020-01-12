package eu.depau.etchdroid.notification

import androidx.core.app.NotificationCompat

interface IServiceNotificationBuilder {
    fun getBuilder(): NotificationCompat.Builder
    fun getBuilderDone(): NotificationCompat.Builder
}