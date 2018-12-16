package eu.depau.etchdroid.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import eu.depau.etchdroid.R
import eu.depau.etchdroid.activities.ErrorActivity
import eu.depau.etchdroid.kotlinexts.toHRSize
import eu.depau.etchdroid.kotlinexts.toHRTime
import java.util.*
import kotlin.math.max


abstract class UsbWriteService(name: String) : IntentService(name) {
    val tag = name
    val foregroundId = Random().nextInt()
    val resultNotificationId = Random().nextInt()
    val writeProgressChannelId = "eu.depau.etchdroid.notifications.USB_WRITE_PROGRESS"
    val writeResultChannelId = "eu.depau.etchdroid.notifications.USB_WRITE_RESULT"
    val wakelockTag = "eu.depau.etchdroid.wakelocks.USB_WRITING-$foregroundId"

    private var prevTime = System.currentTimeMillis()
    private var prevBytes = 0L
    private var notifyChanRegistered = false
    private var mWakeLock: PowerManager.WakeLock? = null
    private var wlAcquireTime = -1L
    private val wlTimeout = 10 * 60 * 1000L

    override fun onHandleIntent(intent: Intent?) {
        startForeground(foregroundId, buildForegroundNotification(null, null, -1))

        try {
            writeImage(intent!!)
        } finally {
            stopForeground(true)
        }
    }

    abstract fun writeImage(intent: Intent): Long

    fun getNotificationBuilder(channel: String = writeProgressChannelId): NotificationCompat.Builder {
        if (!notifyChanRegistered) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val statusChannel = NotificationChannel(
                        writeProgressChannelId,
                        getString(R.string.notchan_writestatus_title),
                        NotificationManager.IMPORTANCE_LOW
                )
                statusChannel.description = getString(R.string.notchan_writestatus_desc)

                val resultChannel = NotificationChannel(
                        writeResultChannelId,
                        getString(R.string.result_channel_name),
                        NotificationManager.IMPORTANCE_DEFAULT
                )
                resultChannel.description = getString(R.string.result_channel_desc)

                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager!!.createNotificationChannel(statusChannel)
                notificationManager.createNotificationChannel(resultChannel)
            }
            notifyChanRegistered = true
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            NotificationCompat.Builder(this, channel)
        else
            NotificationCompat.Builder(this)
    }

    fun updateNotification(usbDevice: String, filename: String?, bytes: Long, progr: Int) {
        // Notification rate limiting
        val time = System.currentTimeMillis()
        if (time <= prevTime + 1000)
            return

        val speed = max((bytes - prevBytes).toDouble() / (time - prevTime).toDouble() * 1000, 0.0).toHRSize()
        prevTime = time
        prevBytes = bytes

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(foregroundId, buildForegroundNotification(usbDevice, filename, progr, "$progr% • $speed/s"))
    }

    fun resultNotification(usbDevice: String, filename: String, exception: Throwable?, bytes: Long = 0, startTime: Long = 0) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val b = getNotificationBuilder(writeResultChannelId)
                .setOngoing(false)

        val dt = System.currentTimeMillis() - startTime

        if (exception != null) {
            val intent = Intent(this, ErrorActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            intent.putExtra("error", exception.message)
            val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

            b.setContentTitle(getString(R.string.write_failed_tap_for_info))
                    .setContentText(getString(R.string.error_notif_content_text, usbDevice))
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setSubText(dt.toHRTime())
        } else {
            val speed = max(bytes.toDouble() / dt.toDouble() * 1000, 0.0).toHRSize() + "/s"
            b.setContentTitle(getString(R.string.write_finished))
                    .setContentText(getString(R.string.success_notif_content_text, filename, usbDevice))
                    .setSubText("${dt.toHRTime()} • ${bytes.toHRSize()} • $speed")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            b.setSmallIcon(R.drawable.ic_usb_white_24dp)

        notificationManager.notify(resultNotificationId, b.build())
    }

    fun buildForegroundNotification(usbDevice: String?, filename: String?, progr: Int, subText: String? = null, title: String = getString(R.string.notif_writing_img)): Notification {
        val indet: Boolean
        val prog: Int

        if (progr < 0) {
            prog = 0
            indet = true
        } else {
            prog = progr
            indet = false
        }

        val b = getNotificationBuilder()

        b.setContentTitle(title)
                .setOngoing(true)
                .setProgress(100, prog, indet)

        if (usbDevice != null && filename != null)
            b.setContentText("$filename to $usbDevice")
        else
            b.setContentText(getString(R.string.notif_initializing))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            b.setSmallIcon(R.drawable.ic_usb_white_24dp)

        if (subText != null)
            b.setSubText(subText)

        return b.build()
    }


    fun wakeLock(acquire: Boolean) {
        // Do not reacquire wakelock if timeout not expired
        if (acquire && mWakeLock != null && wlAcquireTime > 0 && System.currentTimeMillis() < wlAcquireTime + wlTimeout - 5000)
            return

        wlAcquireTime = if (acquire)
            System.currentTimeMillis()
        else
            -1

        val powerMgr = getSystemService(Context.POWER_SERVICE) as PowerManager

        powerMgr.run {
            if (mWakeLock == null)
                mWakeLock = newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakelockTag)

            mWakeLock.apply {
                if (acquire)
                    this!!.acquire(wlTimeout /*10 minutes*/)
                else
                    this!!.release()
            }
        }
    }
}