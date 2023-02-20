package eu.depau.etchdroid.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import eu.depau.etchdroid.R
import eu.depau.etchdroid.utils.ktexts.toHRSize
import eu.depau.etchdroid.utils.ktexts.toHRTime
import java.util.*
import kotlin.math.max


abstract class UsbWriteService(val usbWriteName: String) : IntentService(usbWriteName) {
    private var time: Long = 0
    private var speed: Double = 0.0
    private var previousTime = System.currentTimeMillis()
    private var previousBytes = 0L
    private var notifyChanRegistered = false
    private var mWakeLock: PowerManager.WakeLock? = null
    private var wlAcquireTime = -1L

    companion object {
        private const val WL_TIMEOUT = 10 * 60 * 1000L
        const val WRITE_PROGRESS_CHANNEL_ID = "eu.depau.etchdroid.notifications.USB_WRITE_PROGRESS"
        const val WRITE_RESULT_CHANNEL_ID = "eu.depau.etchdroid.notifications.USB_WRITE_RESULT"
        val FOREGROUND_ID = Random().nextInt()
        val RESULT_NOTIFICATION_ID = Random().nextInt()
        val WAKELOCK_TAG = "eu.depau.etchdroid.wakelocks.USB_WRITING-$FOREGROUND_ID"
    }

    override fun onHandleIntent(intent: Intent?) {
        startForeground(FOREGROUND_ID, buildForegroundNotification(null, null, -1))

        try {
            writeImage(intent!!)
        } finally {
            stopForeground(true)
        }
    }

    abstract fun writeImage(intent: Intent): Long

    private fun calculateWriteSpeed(bytes: Long): Double =
        max((bytes - previousBytes).toDouble() / (time - previousTime) * 1000, 0.0)

    private fun calculateLastWriteSpeed(bytes: Long, deltaTime: Long): Double =
        max(bytes.toDouble() / deltaTime * 1000, 0.0)

    // Notification rate limiting
    private fun shouldUpdateNotification(): Boolean {
        time = System.currentTimeMillis()
        return time <= previousTime + 1000
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val statusChannel = NotificationChannel(
                WRITE_PROGRESS_CHANNEL_ID,
                getString(R.string.notchan_writestatus_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notchan_writestatus_desc)
            }

            val resultChannel = NotificationChannel(
                WRITE_RESULT_CHANNEL_ID,
                getString(R.string.result_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.result_channel_desc)
            }

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(statusChannel)
            notificationManager.createNotificationChannel(resultChannel)
        }
    }

    private fun getNotificationBuilder(channel: String = WRITE_PROGRESS_CHANNEL_ID):
            NotificationCompat.Builder {
        if (!notifyChanRegistered) {
            createNotificationChannels()
            notifyChanRegistered = true
        }
        return NotificationCompat.Builder(this, channel)
    }

    fun updateNotification(usbDevice: String, filename: String?, bytes: Long, progr: Int) {
        if (shouldUpdateNotification())
            return

        val speed = calculateWriteSpeed(bytes).toHRSize()
        previousTime = time
        previousBytes = bytes

        with(NotificationManagerCompat.from(this)) {
            notify(
                FOREGROUND_ID, buildForegroundNotification(
                    usbDevice,
                    filename,
                    progr,
                    "$progr% • $speed/s"
                )
            )
        }
    }

    fun resultNotification(
        usbDevice: String,
        filename: String,
        exception: Throwable?,
        bytes: Long = 0,
        startTime: Long = 0
    ) {
        val notificationCompatBuilder = getNotificationBuilder(WRITE_RESULT_CHANNEL_ID)
            .setOngoing(false)

        val deltaTime = System.currentTimeMillis() - startTime

        if (exception != null) {
            TODO("Error notification")
//            val intent = Intent(this, ErrorActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            intent.putExtra("error", exception.message)
//            intent.putExtra("stacktrace", Log.getStackTraceString(exception))
//
//            val pendingIntent = PendingIntent.getActivity(
//                    this,
//                    0,
//                    intent,
//                    PendingIntent.FLAG_UPDATE_CURRENT
//            )
//
//            notificationCompatBuilder.setContentTitle(getString(R.string.failed_tap_for_info))
//                    .setContentText(getString(R.string.error_notif_content_text, usbDevice))
//                    .setStyle(NotificationCompat.BigTextStyle()
//                            .bigText(getString(R.string.error_notif_content_text, usbDevice)))
//                    .setContentIntent(pendingIntent)
//                    .setAutoCancel(true)
//                    .setSubText(deltaTime.toHRTime())
        } else {
            val formattedLastWriteSpeed =
                calculateLastWriteSpeed(bytes, deltaTime).toHRSize() + "/s"

            notificationCompatBuilder.setContentTitle(getString(R.string.write_finished))
                .setContentText(
                    getString(
                        R.string.success_notif_content_text,
                        filename,
                        usbDevice
                    )
                )
                .setSubText(
                    "%1s • %2s • %3s".format(
                        deltaTime.toHRTime(),
                        bytes.toHRSize(),
                        formattedLastWriteSpeed
                    )
                )
        }

        notificationCompatBuilder.setSmallIcon(R.drawable.ic_usb)

        with(NotificationManagerCompat.from(this)) {
            notify(RESULT_NOTIFICATION_ID, notificationCompatBuilder.build())
        }
    }

    private fun buildForegroundNotification(
        usbDevice: String?,
        filename: String?,
        progress: Int,
        subText: String? = null,
        title: String = getString(R.string.notif_writing_img)
    ): Notification {

        val notificationBuilder = getNotificationBuilder()

        with(notificationBuilder) {
            setContentTitle(title)
            setOngoing(true)

            if (progress < 0) {
                setProgress(100, 0, true)
            } else {
                setProgress(100, progress, false)
            }

            if (usbDevice != null && filename != null) {
                setContentText("$filename to $usbDevice")
                setStyle(NotificationCompat.BigTextStyle().bigText("$filename to $usbDevice"))
            } else {
                setContentText(getString(R.string.notif_initializing))
            }

            setSmallIcon(R.drawable.ic_usb)

            if (subText != null)
                setSubText(subText)
        }

        return notificationBuilder.build()
    }

    private fun istTimeoutExpired(acquire: Boolean): Boolean =
        acquire && mWakeLock != null && wlAcquireTime > 0 &&
                System.currentTimeMillis() < wlAcquireTime + WL_TIMEOUT - 5000

    fun wakeLock(acquire: Boolean) {
        // Do not reacquire wakelock if timeout not expired
        if (istTimeoutExpired(acquire))
            return

        wlAcquireTime = if (acquire)
            System.currentTimeMillis()
        else
            -1

        val powerMgr = getSystemService(Context.POWER_SERVICE) as PowerManager

        powerMgr.run {
            if (mWakeLock == null)
                mWakeLock = newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG)

            mWakeLock?.run {
                if (acquire)
                    this.acquire(WL_TIMEOUT /*10 minutes*/)
                else
                    this.release()
            }
        }

    }

}