package com.example.wakeupmoto

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_logs.*

class Logs : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)
        refreshLogs(null)
    }

    fun refreshLogs(view: View?) {
        var index = logs.start
        var logsText = ""
        do {
            if (logs.array[index].isNullOrBlank())
                break
            logsText += logs.array[index] + "\n"
            index = (index+1)%logs.array.size
        } while (index != logs.start)
        this.textView4.text = logsText
    }
}