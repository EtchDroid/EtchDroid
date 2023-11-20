package eu.depau.etchdroid.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import eu.depau.etchdroid.Intents
import eu.depau.etchdroid.JobStatusInfo
import eu.depau.etchdroid.R
import eu.depau.etchdroid.getErrorIntent
import eu.depau.etchdroid.getFinishedIntent
import eu.depau.etchdroid.getProgressActivityPendingIntent
import eu.depau.etchdroid.getProgressUpdateIntent
import eu.depau.etchdroid.massstorage.BlockDeviceInputStream
import eu.depau.etchdroid.massstorage.BlockDeviceOutputStream
import eu.depau.etchdroid.massstorage.EtchDroidUsbMassStorageDevice
import eu.depau.etchdroid.massstorage.UsbMassStorageDeviceDescriptor
import eu.depau.etchdroid.massstorage.setUpLibUSB
import eu.depau.etchdroid.service.WorkerServiceFlowImpl.verifyImage
import eu.depau.etchdroid.service.WorkerServiceFlowImpl.writeImage
import eu.depau.etchdroid.ui.ProgressActivity
import eu.depau.etchdroid.utils.broadcastReceiver
import eu.depau.etchdroid.utils.exception.InitException
import eu.depau.etchdroid.utils.exception.NotEnoughSpaceException
import eu.depau.etchdroid.utils.exception.OpenFileException
import eu.depau.etchdroid.utils.exception.UnknownException
import eu.depau.etchdroid.utils.exception.UsbCommunicationException
import eu.depau.etchdroid.utils.exception.VerificationFailedException
import eu.depau.etchdroid.utils.exception.base.EtchDroidException
import eu.depau.etchdroid.utils.exception.base.FatalException
import eu.depau.etchdroid.utils.ktexts.broadcastLocally
import eu.depau.etchdroid.utils.ktexts.broadcastLocallySync
import eu.depau.etchdroid.utils.ktexts.getFileName
import eu.depau.etchdroid.utils.ktexts.getFilePath
import eu.depau.etchdroid.utils.ktexts.getFileSize
import eu.depau.etchdroid.utils.ktexts.safeParcelableExtra
import eu.depau.etchdroid.utils.ktexts.startForegroundSpecialUse
import eu.depau.etchdroid.utils.ktexts.toHRSize
import eu.depau.etchdroid.utils.lateInit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.jahnen.libaums.core.driver.BlockDeviceDriver
import java.io.BufferedInputStream
import java.io.InputStream
import java.util.Random
import kotlin.math.max

private const val TAG = "WorkerService"

private const val JOB_RESULT_CHANNEL = "eu.depau.etchdroid.notifications.JOB_RESULT"
private const val JOB_PROGRESS_CHANNEL = "eu.depau.etchdroid.notifications.JOB_PROGRESS"

private const val WAKELOCK_TIMEOUT = 10 * 60 * 1000L
private const val PROGRESS_UPDATE_INTERVAL = 1000L
const val BUFFER_BLOCKS_SIZE = 2048

class WorkerService : LifecycleService() {
    private var mLoggedNotificationWarning = false
    private var mNotificationsSetUp = false
    private val mProgressNotificationId = Random().nextInt()
    private lateinit var mSourceUri: Uri
    private lateinit var mDestDevice: UsbMassStorageDeviceDescriptor
    private var mJobId by lateInit<Int>(mutable = true)
    private var mWakelockAcquireTime = -1L
    private var mWakeLock: PowerManager.WakeLock? = null
    private var mVerificationCancelled = false

    private val mNotificationManager by lazy {
        getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
    }

    private val notificationsAllowed: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mNotificationManager.areNotificationsEnabled()
        } else {
            true
        }

    init {
        setUpLibUSB()
    }

    override fun onCreate() {
        super.onCreate()
        println("WorkerService created")

        LocalBroadcastManager.getInstance(this)
            .registerReceiver(mBroadcastReceiver, IntentFilter().apply {
                addAction(Intents.SKIP_VERIFY)
                addAction(Intents.JOB_PROGRESS)
                addAction(Intents.ERROR)
                addAction(Intents.FINISHED)
            })

        trySetupNotifications()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
    }

    private fun finish() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION") stopForeground(true)
        }
    }

    /**
     * Broadcast receiver that forwards the received intents as notifications, if the user has
     * allowed notifications.
     */
    private val mBroadcastReceiver = broadcastReceiver { intent ->
        if (!mNotificationsSetUp) trySetupNotifications()

        when (intent.action) {
            Intents.SKIP_VERIFY -> {
                mVerificationCancelled = true
            }

            Intents.JOB_PROGRESS -> {
                if (!notificationsAllowed) return@broadcastReceiver
                val status: JobStatusInfo =
                    intent.safeParcelableExtra("status") ?: return@broadcastReceiver

                mNotificationManager.notify(
                    mProgressNotificationId,
                    NotificationCompat.Builder(this@WorkerService, JOB_PROGRESS_CHANNEL)
                        .setSmallIcon(R.drawable.ic_write_to_usb_anim)
                        .setContentTitle(
                            getString(
                                if (status.isVerifying) R.string.notification_verify_progress_title
                                else R.string.notification_write_progress_title
                            )
                        )
                        .setContentText(
                            if (status.isVerifying) getString(
                                R.string.notification_verify_progress_content, mDestDevice.name,
                                filenameStr
                            )
                            else getString(
                                R.string.notification_write_progress_content, filenameStr,
                                mDestDevice.name
                            )
                        )
                        .setContentIntent(
                            getProgressUpdateIntent(
                                mSourceUri, mDestDevice, mJobId, status.speed,
                                status.processedBytes, status.totalBytes,
                                isVerifying = status.isVerifying,
                                packageContext = this@WorkerService,
                                cls = ProgressActivity::class.java
                            ).getProgressActivityPendingIntent(this@WorkerService)
                        )
                        .setSubText(
                            "${status.processedBytes.toHRSize()} • ${status.speed.toHRSize()}/s"
                        )
                        .setProgress(100, max(status.percent, 0), status.percent < 0)
                        .setOngoing(true)
                        .build()
                )
            }

            Intents.ERROR -> {
                if (!notificationsAllowed) return@broadcastReceiver
                val status: JobStatusInfo =
                    intent.safeParcelableExtra("status") ?: return@broadcastReceiver

                status.exception!!

                mNotificationManager.notify(
                    mJobId, NotificationCompat.Builder(this@WorkerService, JOB_RESULT_CHANNEL)
                        .setSmallIcon(R.drawable.ic_write_to_usb_failed)
                        .setContentTitle(
                            getString(
                                if (status.exception is FatalException) R.string.write_failed
                                else R.string.action_required
                            )
                        )
                        .setContentText(status.exception.getUiMessage(this@WorkerService))
                        .setContentIntent(
                            getErrorIntent(
                                mSourceUri, mDestDevice, mJobId, status.processedBytes,
                                status.totalBytes, status.exception,
                                packageContext = this@WorkerService,
                                cls = ProgressActivity::class.java
                            ).getProgressActivityPendingIntent(this@WorkerService)
                        )
                        .setOngoing(false)
                        .build()
                )
            }

            Intents.FINISHED -> {
                if (!notificationsAllowed) return@broadcastReceiver
                val status: JobStatusInfo =
                    intent.safeParcelableExtra("status") ?: return@broadcastReceiver

                mNotificationManager.notify(
                    mJobId, NotificationCompat.Builder(this@WorkerService, JOB_RESULT_CHANNEL)
                        .setSmallIcon(R.drawable.ic_written_to_usb)
                        .setContentTitle(getString(R.string.write_finished))
                        .setContentText(
                            getString(
                                R.string.success_notif_content_text, filenameStr, mDestDevice.name
                            )
                        )
                        .setContentIntent(
                            getFinishedIntent(
                                mSourceUri, mDestDevice, status.totalBytes,
                                packageContext = this@WorkerService,
                                cls = ProgressActivity::class.java
                            ).getProgressActivityPendingIntent(this@WorkerService)
                        )
                        .setOngoing(false)
                        .build()
                )
            }
        }
    }

    private fun ensureWakelock() {
        if (mWakeLock == null) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            mWakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "EtchDroid::WorkerService[$mProgressNotificationId]"
            )
        }
        if (!mWakeLock!!.isHeld || System.currentTimeMillis() - mWakelockAcquireTime > WAKELOCK_TIMEOUT * .9) {
            mWakeLock!!.acquire(WAKELOCK_TIMEOUT)
            mWakelockAcquireTime = System.currentTimeMillis()
        }
    }

    private fun releaseWakelock() {
        mWakeLock?.release()
        mWakelockAcquireTime = -1L
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        if (intent?.action != Intents.START_JOB) {
            Log.e(TAG, "Received invalid intent action: ${intent?.action}")
            stopSelf()
            return START_NOT_STICKY
        }

        mSourceUri = intent.data!!
        mDestDevice = intent.safeParcelableExtra("destDevice")!!
        mJobId = intent.getIntExtra("jobId", -1)
        val offset = intent.getLongExtra("offset", 0L)
        val verifyOnly = intent.getBooleanExtra("verifyOnly", false)

        if (mNotificationsSetUp) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            try {
                notificationManager.cancel(mJobId)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cancel notification", e)
            }
        }

        require(mJobId != -1) { "Job ID not set" }

        Log.i(
            TAG, "Starting worker service for job: '${
                mSourceUri.getFilePath(
                    this
                ) ?: "Unknown file"
            }' -> '${mDestDevice.name}' (offset: ${offset.toHRSize(false)})"
        )

        startForegroundSpecialUse(mProgressNotificationId, basicForegroundNotification)

        lifecycleScope.launch(Dispatchers.IO) {
            Log.d(
                TAG,
                "Job coroutine scope started; thread ${Thread.currentThread().name} (${Thread.currentThread().id})"
            )

            var massStorageDev by lateInit<EtchDroidUsbMassStorageDevice>()
            var blockDev by lateInit<BlockDeviceDriver>()
            var rawSourceStream: InputStream? = null
            var currentOffset = offset
            var imageSize: Long = 0

            val coroScope = CoroutineScope(Dispatchers.IO)

            try {
                try {
                    massStorageDev = mDestDevice.buildDevice(this@WorkerService).apply {
                        init()
                    }
                    blockDev = massStorageDev.blockDevices[0]!!
                } catch (e: Exception) {
                    throw if (e is EtchDroidException) e else InitException(
                        "Initialization failed", e
                    )
                }
                currentOffset = offset - (offset % blockDev.blockSize)

                // Resume a few blocks earlier in case things went haywire earlier
                currentOffset = max(currentOffset - blockDev.blockSize * 10, 0L)

                val devSize = blockDev.blocks * blockDev.blockSize
                Log.d(
                    TAG, "Device size: ${
                        devSize.toHRSize(
                            false
                        )
                    } " + "(block size: ${
                        blockDev.blockSize.toHRSize(
                            false
                        )
                    }, " + "num blocks: ${blockDev.blocks})"
                )
                imageSize = mSourceUri.getFileSize(this@WorkerService)

                if (devSize < imageSize) {
                    Log.e(TAG, "Device size is smaller than image size")
                    throw NotEnoughSpaceException(sourceSize = imageSize, destSize = devSize)
                }

                getProgressUpdateIntent(
                    mSourceUri, mDestDevice, mJobId, 0f, currentOffset, imageSize,
                    isVerifying = verifyOnly
                ).broadcastLocallySync(this@WorkerService)

                val bufferSize = BUFFER_BLOCKS_SIZE * blockDev.blockSize

                // Write image
                if (!verifyOnly) {
                    try {
                        rawSourceStream = contentResolver.openInputStream(mSourceUri)!!
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to open image file", e)
                        throw OpenFileException("Failed to open image file", e)
                    }

                    writeImage(
                        rawSourceStream, blockDev, imageSize, bufferSize, currentOffset, coroScope,
                        ::ensureWakelock, ::sendProgressUpdate
                    )
                }

                // Verify written image
                try {
                    rawSourceStream = contentResolver.openInputStream(mSourceUri)!!
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to open image file", e)
                    throw OpenFileException("Failed to open image file", e)
                }

                // Always verify the whole image, not just the part that was written
                currentOffset = 0

                verifyImage(
                    rawSourceStream, blockDev, imageSize, bufferSize, coroScope,
                    ::sendProgressUpdate, { mVerificationCancelled }, ::ensureWakelock
                )

                getFinishedIntent(mSourceUri, mDestDevice, imageSize).broadcastLocallySync(
                    this@WorkerService
                )

            } catch (exception: Exception) {
                Log.e(TAG, "Operation failed", exception)
                val downstreamException = if (exception is EtchDroidException) exception
                else UnknownException(exception)
                getErrorIntent(
                    mSourceUri, mDestDevice, mJobId, currentOffset, imageSize,
                    exception = downstreamException
                ).broadcastLocallySync(this@WorkerService)
            } finally {
                finish()
                releaseWakelock()

                coroScope.cancel("Job finished or aborted")
                if (rawSourceStream != null) {
                    try {
                        rawSourceStream.close()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to close image file", e)
                    }
                }
                try {
                    massStorageDev.close()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to close USB drive", e)
                }
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private var mLastProgressUpdate = -1L
    private var mBytesSinceLastUpdate: Double = 0.0
    private var mLast5Speeds = mutableListOf(0f, 0f, 0f, 0f, 0f)

    private fun sendProgressUpdate(
        lastWrittenBytes: Int,
        processedBytes: Long,
        imageSize: Long,
        isVerifying: Boolean = false,
    ) {
        mBytesSinceLastUpdate += lastWrittenBytes
        val newTime = System.currentTimeMillis()

        if (mLastProgressUpdate + PROGRESS_UPDATE_INTERVAL > newTime) return

        val interval = newTime - mLastProgressUpdate
        val speed =
            (mBytesSinceLastUpdate / (if (interval > 0) interval else PROGRESS_UPDATE_INTERVAL) * 1000).toFloat()
        mLast5Speeds.add(speed)
        if (mLast5Speeds.size > 5) mLast5Speeds.removeAt(0)
        // Calculate average with weights: 1, 2, 3, 4, 5
        val newSpeed = mLast5Speeds.mapIndexed { index, f -> f * (index + 1) }.sum() / 15

        getProgressUpdateIntent(
            mSourceUri, mDestDevice, mJobId, newSpeed, processedBytes, imageSize,
            isVerifying = isVerifying
        ).broadcastLocally(this@WorkerService)

        mLastProgressUpdate = newTime
        mBytesSinceLastUpdate = 0.0
    }

    private val filenameStr: String by lazy {
        mSourceUri.getFileName(this) ?: getString(R.string.unknown_filename)
    }

    private val basicForegroundNotification: Notification
        get() = NotificationCompat.Builder(this, JOB_PROGRESS_CHANNEL)
            .setSmallIcon(R.drawable.ic_write_to_usb_anim)
            .setContentTitle(getString(R.string.notification_write_progress_title))
            .setContentText(
                getString(
                    if (notificationsAllowed) R.string.notification_write_tap_for_progress_text
                    else R.string.notification_write_no_notifications_text, filenameStr,
                    mDestDevice.name
                )
            )
            .setProgress(100, 0, true)
            .setOngoing(true)
            .build()

    private fun trySetupNotifications() {
        if (mNotificationsSetUp || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        try {
            mNotificationManager.createNotificationChannel(NotificationChannel(
                JOB_PROGRESS_CHANNEL, getString(R.string.notif_channel_job_progress_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_channel_job_progress_desc)
                setShowBadge(false)
            })
            mNotificationManager.createNotificationChannel(NotificationChannel(
                JOB_RESULT_CHANNEL, getString(R.string.notif_channel_job_result_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notif_channel_job_result_desc)
            })

            mNotificationsSetUp = true
        } catch (e: SecurityException) {
            if (!mLoggedNotificationWarning) {
                Log.w(TAG, "Could not register notification channels", e)
                mLoggedNotificationWarning = true
            }
        }
    }
}

object WorkerServiceFlowImpl {
    suspend fun writeImage(
        rawSourceStream: InputStream,
        blockDev: BlockDeviceDriver,
        imageSize: Long,
        bufferSize: Int,
        initialOffset: Long,
        coroScope: CoroutineScope,
        grabWakeLock: () -> Unit,
        sendProgressUpdate: (
            lastWrittenBytes: Int,
            processedBytes: Long,
            imageSize: Long,
            isVerifying: Boolean,
        ) -> Unit,
    ) {
        val buffer = ByteArray(bufferSize)
        var currentOffset = initialOffset

        val src = BufferedInputStream(rawSourceStream, bufferSize * 4)
        val dst = BlockDeviceOutputStream(blockDev, BUFFER_BLOCKS_SIZE, coroScope)

        src.skip(currentOffset)
        dst.seekAsync(currentOffset)

        while (currentOffset < imageSize) {
            grabWakeLock()

            val read = src.read(buffer)
            if (read == -1) break
            try {
                dst.writeAsync(buffer)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write to USB drive", e)
                throw UsbCommunicationException(e)
            }
            currentOffset += read

            sendProgressUpdate(read, currentOffset, imageSize, false)
        }
        dst.flushAsync()
    }

    fun verifyImage(
        rawSourceStream: InputStream,
        blockDev: BlockDeviceDriver,
        imageSize: Long,
        bufferSize: Int,
        lifecycleScope: CoroutineScope,
        sendProgressUpdate: (
            lastWrittenBytes: Int,
            processedBytes: Long,
            imageSize: Long,
            isVerifying: Boolean,
        ) -> Unit,
        isVerificationCanceled: () -> Boolean,
        grabWakeLock: () -> Unit,
    ) {

        val src = BufferedInputStream(rawSourceStream, bufferSize * 4)
        val dst = BlockDeviceInputStream(blockDev, BUFFER_BLOCKS_SIZE, lifecycleScope)
        var currentOffset = 0L

        src.skip(currentOffset)
        dst.skip(currentOffset)

//    val fileBuffer = ByteArray(bufferSize)
//    val deviceBuffer = ByteArray(bufferSize)

        val fileBuffer = ByteArray(1024)
        val deviceBuffer = ByteArray(1024)

        while (!isVerificationCanceled()) {
            grabWakeLock()

            val read = src.read(fileBuffer)
            if (read == -1) break

            try {
                val deviceRead = dst.read(deviceBuffer, 0, read)
                require(
                    deviceRead >= read
                ) { "Device read $deviceRead < file read $read" }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read from USB drive", e)
                throw UsbCommunicationException(e)
            }

            if (!fileBuffer.contentEquals(deviceBuffer)) {
                Log.e(TAG, "Verification failed")
                if (Build.VERSION.SDK_INT > 999999) {
                    // Prevent the compiler from optimizing out the buffers, for debugging
                    // purposes
                    print(deviceBuffer)
                    print(fileBuffer)
                }
                throw VerificationFailedException()
            }
            currentOffset += read

            sendProgressUpdate(read, currentOffset, imageSize, true)
        }
    }
}