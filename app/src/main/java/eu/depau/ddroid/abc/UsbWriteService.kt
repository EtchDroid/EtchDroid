package eu.depau.ddroid.abc

import android.app.IntentService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.net.Uri
import android.os.Build
import eu.depau.ddroid.R
import eu.depau.ddroid.utils.getFileName
import eu.depau.ddroid.utils.toHRSize


abstract class UsbWriteService(name: String) : IntentService(name) {
    val TAG = name
    val FOREGROUND_ID = 1931
    val WRITE_PROGRESS_CHANNEL_ID = "eu.depau.ddroid.notifications.USB_WRITE_PROGRESS"
    private var prevTime = System.currentTimeMillis()
    private var prevBytes = 0L
    private var notifyChanRegistered = false

    fun getNotificationBuilder(): Notification.Builder {
        if (!notifyChanRegistered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channame = "USB write progress"
                val description = "Displays the status of ongoing USB writes"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(WRITE_PROGRESS_CHANNEL_ID, channame, importance)
                channel.description = description

                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager!!.createNotificationChannel(channel)
            }
            notifyChanRegistered = true
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, WRITE_PROGRESS_CHANNEL_ID)
        else
            Notification.Builder(this)
    }

    fun updateNotification(usbDevice: String, uri: Uri, bytes: Long, total: Long) {
        // Notification rate limiting
        val time = System.currentTimeMillis()
        if (time <= prevTime + 1000)
            return

        val speed = ((bytes - prevBytes).toDouble() / (time - prevTime).toDouble()).toHRSize()
        prevTime = time
        prevBytes = bytes

        val perc: Int = (bytes.toDouble() / total * 100.0).toInt()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FOREGROUND_ID, buildForegroundNotification(usbDevice, uri, bytes, total, "$perc% • $speed/s"))
    }

    fun errorNotification() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val b = getNotificationBuilder()
                .setContentTitle("Write failed")
                .setContentText("The USB drive may have been unplugged while writing.")
                .setOngoing(false)
                .setSmallIcon(R.drawable.ic_usb_white_24dp)

        notificationManager.notify(FOREGROUND_ID, b.build())
    }

    fun buildForegroundNotification(usbDevice: String, uri: Uri, bytes: Long, total: Long, subText: String? = null): Notification {
        val progr: Int
        val indet: Boolean

        if (total < 0) {
            progr = 0
            indet = true
        } else {
            progr = (bytes.toFloat()/total * 100).toInt()
            indet = false
        }

        val b = getNotificationBuilder()

        b.setContentTitle("Writing image")
                .setContentText("${uri.getFileName(this)} to $usbDevice")
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_usb_white_24dp)
                .setProgress(100, progr, indet)

        if (subText != null)
            b.setSubText(subText)

        return b.build()
    }

}