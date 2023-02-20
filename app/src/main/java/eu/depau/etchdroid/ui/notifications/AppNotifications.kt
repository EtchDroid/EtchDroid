package eu.depau.etchdroid.ui.notifications

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import eu.depau.etchdroid.R
import eu.depau.kotlet.android.extensions.notification.NotificationImportanceCompat
import eu.depau.kotlet.android.extensions.notification.registerNotificationChannel

object AppNotifications {
    private const val asyncProgressChannelId = "eu.depau.etchdroid.notifications.ASYNC)WORKER_PROGRESS"
    private const val asyncResultChannelId = "eu.depau.etchdroid.notifications.ASYNC_WORKER_RESULT"

    private var channelsRegistered = false

    @TargetApi(Build.VERSION_CODES.O)
    fun registerChannels(context: Context) {
        if (channelsRegistered || Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return

        val notificationManager = context.getSystemService(NotificationManager::class.java)!!

        notificationManager.registerNotificationChannel(
                asyncProgressChannelId,
                context.getString(R.string.notchan_writestatus_title),
                context.getString(R.string.notchan_writestatus_desc),
                NotificationImportanceCompat.IMPORTANCE_LOW
        )

        notificationManager.registerNotificationChannel(
                asyncResultChannelId,
                context.getString(R.string.result_channel_name),
                context.getString(R.string.result_channel_desc),
                NotificationImportanceCompat.IMPORTANCE_DEFAULT
        )

        channelsRegistered = true
    }

    private fun getNotificationBuilder(context: Context, channel: String): NotificationCompat.Builder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationCompat.Builder(context, channel)
        } else {
            NotificationCompat.Builder(context)
        }
    }

    fun buildAsyncWorkerProgressNotification(
            context: Context,
            contentTitle: String? = null,
            contentText: String? = null,
            subText: String? = null,
            ongoing: Boolean = true,
            progressPercent: Int = 0,
            indeterminate: Boolean = false,
            smallIcon: Int = R.drawable.ic_usb
    ): Notification {

        val builder = getNotificationBuilder(context, asyncProgressChannelId)

        if (contentTitle != null)
            builder.setContentTitle(contentTitle)
        if (subText != null)
            builder.setSubText(subText)
        if (contentText != null)
            builder.setContentText(contentText)
        else
            builder.setContentText(context.getString(R.string.notif_initializing))
        builder.setSmallIcon(smallIcon)

        builder.setOngoing(ongoing)
        builder.setProgress(100, progressPercent, indeterminate)

        return builder.build()
    }

}