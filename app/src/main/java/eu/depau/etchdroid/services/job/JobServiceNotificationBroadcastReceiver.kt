package eu.depau.etchdroid.services.job

import android.app.IntentService
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import eu.depau.etchdroid.R
import eu.depau.etchdroid.broadcasts.JobProgressUpdateBroadcast
import eu.depau.etchdroid.broadcasts.dto.JobProgressUpdateBroadcastDTO
import eu.depau.etchdroid.db.entity.Job
import eu.depau.etchdroid.notification.IServiceNotificationBuilder
import eu.depau.etchdroid.ui.activities.ErrorActivity
import java.io.Serializable
import kotlin.math.roundToInt


class JobServiceNotificationBroadcastReceiver(
        private val foregroundId: Int,
        private val job: Job,
        private val notificationBuilder: IServiceNotificationBuilder
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != JobProgressUpdateBroadcast.ACTION)
            return

        val dto = intent.getSerializableExtra(JobProgressUpdateBroadcastDTO.EXTRA)
                as JobProgressUpdateBroadcastDTO

        val notification = if (dto.completed) {
            notificationBuilder.getBuilderDone().apply {
                if (dto.error == null) {
                    val activityIntent = Intent(context, ErrorActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        putExtra("error", dto.error as Serializable)
                    }
                    val pendingIntent = PendingIntent.getActivity(
                            context, 0, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                    setSubText(job.jobProcedure.name.build(context))
                    setContentTitle(context.getString(R.string.failed_tap_for_info))
                    setContentIntent(pendingIntent)
                    setAutoCancel(true)
                } else {
                    // TODO: add success activity intent
                    setContentTitle(context.getString(R.string.done))
                    setContentText(job.jobProcedure.name.build(context))
                }
            }.build()
        } else {
            notificationBuilder.getBuilder().apply {
                val subTextContent = mutableListOf<String>()
                setContentTitle(job.jobProcedure.name.build(context))
                if (dto.showProgressBar) {
                    if (dto.indefinite || dto.percentage == null) {
                        setProgress(100, 0, true)
                    } else {
                        setProgress(100, dto.percentage.roundToInt(), false)
                        subTextContent.add("${dto.percentage.roundToInt()}%")
                    }
                }
                if (dto.step != null) {
                    subTextContent.add(context.getString(R.string.step_x_of_y, dto.step + 1, job.jobProcedure.size + 1))
                }
                if (dto.currentMessage != null) {
                    setContentText(dto.currentMessage)
                }
                setSubText(subTextContent.joinToString(" • "))

                // TODO: add rate
            }.build()
        }


        val notificationManager = context
                .getSystemService(IntentService.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(foregroundId, notification)
    }
}