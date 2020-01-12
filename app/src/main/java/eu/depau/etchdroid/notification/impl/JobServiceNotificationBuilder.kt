package eu.depau.etchdroid.notification.impl

import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.Build
import androidx.core.app.NotificationCompat
import eu.depau.etchdroid.R
import eu.depau.etchdroid.notification.IServiceNotificationBuilder
import eu.depau.kotlet.android.extensions.notification.NotificationImportanceCompat
import eu.depau.kotlet.android.extensions.notification.registerNotificationChannel

class JobServiceNotificationBuilder(val context: Context) : IServiceNotificationBuilder {
    private val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
    private val notificationChannelProgress = notificationManager
            .registerNotificationChannel(
                    "eu.depau.etchdroid.notifications.JOB_PROGRESS",
                    context.getString(R.string.notif_channel_job_progress_name),
                    context.getString(R.string.notif_channel_job_progress_desc),
                    NotificationImportanceCompat.IMPORTANCE_LOW
            )

    private val notificationChannelResult = notificationManager
            .registerNotificationChannel(
                    "eu.depau.etchdroid.notifications.JOB_RESULT",
                    context.getString(R.string.notif_channel_job_result_name),
                    context.getString(R.string.notif_channel_job_result_desc),
                    NotificationImportanceCompat.IMPORTANCE_DEFAULT
            )

    private fun getNotificationBuilder(channel: String): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, channel)
        } else {
            NotificationCompat.Builder(context)
        }
    }

    init {
        // Delete old notification channels
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.apply {
                deleteNotificationChannel("eu.depau.etchdroid.notifications.USB_WRITE_PROGRESS")
                deleteNotificationChannel("eu.depau.etchdroid.notifications.USB_WRITE_RESULT")
            }
        }
    }

    override fun getBuilder(): NotificationCompat.Builder {
        val builder = getNotificationBuilder(notificationChannelProgress)
        return builder.apply {
            setSmallIcon(R.drawable.ic_usb_white_24dp)
            setContentTitle(context.getString(R.string.notif_starting_job))
            setProgress(100, 0, true)
        }
    }

    override fun getBuilderDone(): NotificationCompat.Builder {
        val builder = getNotificationBuilder(notificationChannelResult)
        return builder.apply {
            setSmallIcon(R.drawable.ic_usb_white_24dp)
        }
    }
}