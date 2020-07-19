package com.example.wakeupmoto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        log("Received $intent")
        if (intent.action == Intent.ACTION_SCREEN_ON)
            phoneState.userInteractedSinceDisplayOn = true
    }
}
