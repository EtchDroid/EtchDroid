package eu.depau.etchdroid.utils.ktexts

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

fun Intent.sendBroadcast(context: Context) = context.sendBroadcast(this)
fun Intent.sendBroadcast(context: Context, receiverPermission: String) = context.sendBroadcast(this, receiverPermission)

fun Intent.sendLocalBroadcast(context: Context) = LocalBroadcastManager.getInstance(context).sendBroadcast(this)
fun Intent.sendLocalBroadcastSync(context: Context) = LocalBroadcastManager.getInstance(context).sendBroadcastSync(this)