package com.example.wakeupmoto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AutoStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        log("Received $intent")

        val serviceIntent = Intent(context, WakeUpService::class.java)
        context.startForegroundService(serviceIntent)
    }
}
