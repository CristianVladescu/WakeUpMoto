package com.cdb.wakeupmoto

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class PhoneStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This method is called when the BroadcastReceiver is receiving an Intent broadcast.
        log("Received $intent")
        if (intent.action == Intent.ACTION_SCREEN_ON)
            phoneState.screenOnSinceDisplayOn = true
        if (intent.action == Intent.ACTION_USER_PRESENT)
            phoneState.screenUnlockedSinceDisplayOn = true
        if (intent.action == "android.intent.action.PHONE_STATE") {
            var state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            log("Phone state changed to $state")
            if (state == TelephonyManager.EXTRA_STATE_OFFHOOK)
                phoneState.callAnsweredSinceDisplayOn = true
        }

    }
}
