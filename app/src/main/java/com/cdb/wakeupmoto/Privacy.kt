package com.cdb.wakeupmoto

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_privacy.*

class Privacy : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy)
        textView6.text = "This app does not collect any user personal data. It only monitors motion/gesture sensors to determine if the display was turned on by a missed notification or user approching the device, and listens for android.intent.action.PHONE_STATE TelephonyManager.EXTRA_STATE_OFFHOOK to determine if the incoming phone call was answered or missed."
    }
}