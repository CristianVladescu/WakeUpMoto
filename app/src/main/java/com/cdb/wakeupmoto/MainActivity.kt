package com.cdb.wakeupmoto

import android.Manifest
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager


private const val TITLE_TAG = "settingsActivityTitle"

class MainActivity : AppCompatActivity(), PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private lateinit var mService: WakeUpService
    private var mBound: Boolean = false
    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as WakeUpService.LocalBinder
            mService = binder.getService()
            mBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
        }
    }

    private val prefListener = OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "enabled") {
            enabled = prefs.getBoolean(key, false)
            if (enabled) {
                val serviceIntent = Intent(this, WakeUpService::class.java)
                startService(serviceIntent)
            }
        }
        if (key == "debug") { debug = prefs.getBoolean(key, false) }
        if (key == "displayCheckInterval" && mBound) { mService.displayCheckInterval = prefs.getString(key, "0")?.toInt()!! }
        if (key == "wake_up_interval" && mBound) { mService.wakeUpInterval = prefs.getString(key, "0")?.toInt()!! }
        if (key == "wake_up_stop" && mBound) { mService.wakeUpStop = prefs.getString(key, "0")?.toInt()!! }
        if (key == "wake_up_suppressed_after" && mBound) { mService.wakeUpSuppressedAfter = prefs.getString(key, "0")?.toInt()!! }
        if (key == "wake_up_suppressed_until" && mBound) { mService.wakeUpSuppressedUntil = prefs.getString(key, "0")?.toInt()!! }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        } else {
            title = savedInstanceState.getCharSequence(TITLE_TAG)
        }
        supportFragmentManager.addOnBackStackChangedListener {
            if (supportFragmentManager.backStackEntryCount == 0) {
                setTitle(R.string.app_name)
            }
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.WAKE_LOCK, Manifest.permission.FOREGROUND_SERVICE, Manifest.permission.RECEIVE_BOOT_COMPLETED), 0)

        if (Build.VERSION.SDK_INT >= 26 && !(getSystemService(Context.POWER_SERVICE) as PowerManager)?.isIgnoringBatteryOptimizations(packageName)) {
            AlertDialog.Builder(this).setMessage("Battery Optimization is enabled for $packageName and it will stop the service when phone is idle").setTitle("Warning").create().show()
        }

        if (Build.MODEL != "motorola one zoom")
            AlertDialog.Builder(this).setMessage("This app is meant for Motorola phones and was developed on Motorola One Zoom. It might not work with your model, ${Build.MODEL}").setTitle("Warning").create().show()

        preferences = PreferenceManager.getDefaultSharedPreferences(this)
        enabled = preferences.getBoolean("enabled", false)
        debug = preferences.getBoolean("debug", false)
        if (enabled){
            val serviceIntent = Intent(this, WakeUpService::class.java)
            startForegroundService(serviceIntent)
            //        // Bind to LocalService
            Intent(this, WakeUpService::class.java).also { intent ->
                bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(prefListener)
    }

    override fun onDestroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(prefListener)
        super.onDestroy()
    }

    override fun onPreferenceStartFragment(caller: PreferenceFragmentCompat, pref: Preference): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }
        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings, fragment)
            .addToBackStack(null)
            .commit()
        title = pref.title
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.preference, rootKey)
        }
    }
}

