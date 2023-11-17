package eu.depau.etchdroid.utils.ktexts

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.widget.Toast

val Context.activity: Activity?
    get() {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) return context
            context = context.baseContext
        }
        return null
    }

fun Context.toast(message: CharSequence, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.startForegroundServiceCompat(intent: Intent) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        startForegroundService(intent)
    } else {
        startService(intent)
    }
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerExportedReceiver(
    receiver: android.content.BroadcastReceiver,
    intentFilter: android.content.IntentFilter,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
    } else {
        registerReceiver(receiver, intentFilter)
    }
}

@SuppressLint("UnspecifiedRegisterReceiverFlag")
fun Context.registerUnexportedReceiver(
    receiver: android.content.BroadcastReceiver,
    intentFilter: android.content.IntentFilter,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(receiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
    } else {
        registerReceiver(receiver, intentFilter)
    }
}