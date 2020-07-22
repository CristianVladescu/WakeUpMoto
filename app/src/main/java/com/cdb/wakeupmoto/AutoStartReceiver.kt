package com.cdb.wakeupmoto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager

class AutoStartReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context)
        debug = preferences.getBoolean("debug", false)
        log("Received $intent")
        enabled = preferences.getBoolean("enabled", false)
        if (enabled){
            val serviceIntent = Intent(context, WakeUpService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
