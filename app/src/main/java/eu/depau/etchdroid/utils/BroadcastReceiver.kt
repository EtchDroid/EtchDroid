package eu.depau.etchdroid.utils

import android.content.Context
import android.content.Intent

inline fun broadcastReceiver(crossinline onReceive: (context: Context, intent: Intent) -> Unit) =
    object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onReceive(context, intent)
        }
    }

inline fun broadcastReceiver(crossinline onReceive: (intent: Intent) -> Unit) =
    object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            onReceive(intent)
        }
    }