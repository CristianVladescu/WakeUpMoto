package com.example.wakeupmoto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.*
import android.hardware.display.DisplayManager
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.Display
import android.widget.Toast


const val NOTIFICATION_CHANNEL_ID = 0
const val NOTIFICATION_CHANNEL_STRING = "0"

enum class MotoSensors(val type: Int) {
    // {Sensor name="Display Rotate  Non-wakeup", vendor="Motorola", version=256, type=27, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
//    DISPLAY_ROTATE_SENSOR(27),
    // {Sensor name="Flat Up  Wakeup", vendor="Motorola", version=256, type=65537, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
    FLAT_UP_SENSOR(65537),
    // {Sensor name="Flat Down  Wakeup", vendor="Motorola", version=256, type=65538, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
    FLAT_DOWN_SENSOR(65538),
    // {Sensor name="Stowed  Wakeup", vendor="Motorola", version=1, type=65539, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
    STOWED_SENSOR(65539), // display covered/not visible
    // {Sensor name="Camera Gesture  Wakeup", vendor="Motorola", version=1, type=65540, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
//    CAMERA_GESTURE_SENSOR(65540),
    // {Sensor name="ChopChop  Wakeup", vendor="Motorola", version=1, type=65546, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
//    CHOP_CHOP_SENSOR(65546),
    // {Sensor name="Moto Glance", vendor="Motorola", version=1, type=65548, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
    MOTO_GLANCE_SENSOR(65548),
    // {Sensor name="Lift to Silence  Wakeup", vendor="Motorola", version=1, type=65553, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
    LIFT_TO_SILENCE_SENSOR(65553),
    // {Sensor name="Flip to Mute  Wakeup", vendor="Motorola", version=1, type=65554, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
    FLIP_TO_MUTE_SENSOR(65554),
    // {Sensor name="Moto Glance: Approach", vendor="Motorola", version=1, type=65555, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
    MOTO_GLANCE_APPROACH_SENSOR(65555),
    // {Sensor name="Lift to View  Wakeup", vendor="Motorola", version=1, type=65556, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
    LIFT_TO_VIEW_SENSOR(65556),
    // {Sensor name="Log  Wakeup", vendor="Motorola", version=1, type=65557, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
//    LOG_SENSOR(65557),
    // {Sensor name="Off Body  Wakeup", vendor="Motorola", version=1, type=65560, maxRange=1.0, resolution=0.1, power=0.001, minDelay=0}
    OFF_BODY_SENSOR(65560), // 2 when sitting on desk, 1 when picked up, 0 when ???
}

val phoneStateReceiver = PhoneStateReceiver()
class PhoneState {
    var flatUp = false
    var flatDown = false
    var stowed = false
    var offBody = false
    var liftToView = false
    var liftToSilence = false
    var flipToMute = false
    var displayOnPreviously = false
    var displayTurnedOnByUser = false
    var userInteractedSinceDisplayOn = false
}
val phoneState = PhoneState()


var notificationsAlertCooldown = 0
var unreadNotifications = false
var unreadNotificationsCooldown = 0



class StringCircularBuffer {
    var array: Array<String>
    var start = 0
    var end = 0

    constructor(bufferSize: Int){
        array = Array<String>(bufferSize) { _ -> "" }
    }

    fun add(entry: String) {
        array[start] = entry
        end = (end + 1) % array.size
        if (start == end)
            start = (start + 1) % array.size
    }
}

const val LOGGER_TAG = "WakeUpMoto::Logger"
var logs = StringCircularBuffer(100)
fun log(msg: String){
    logs.add(msg)
    Log.d(LOGGER_TAG, msg)
}

class WakeUpService : Service() {
    private lateinit var displayManager: DisplayManager
    private val binder = LocalBinder()
    private var thread: Thread? = null

    private lateinit var sensorManager: SensorManager
    private lateinit var screenLock: PowerManager.WakeLock

    inner class LocalBinder : Binder() {
        // Return this instance of WakeUpService so clients can call public methods
        fun getService(): WakeUpService = this@WakeUpService
    }

    inner class SensorMeasure: SensorEventListener {
        override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
            log("onAccuracyChanged $p0 $p1")
        }

        override fun onSensorChanged(p0: SensorEvent?) {
            log("onSensorChanged " + p0?.sensor?.type + " " + p0?.values?.contentToString())

        }

    }

    inner class SensorTrigger : TriggerEventListener() {
        override fun onTrigger(p0: TriggerEvent?) {
            log("onTrigger $p0")
            if (p0 != null) {
                sensorManager.requestTriggerSensor(SensorTrigger(), p0.sensor)
            }
        }

    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager;
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        
        val channel = NotificationChannel(NOTIFICATION_CHANNEL_STRING, "Wake Up Moto", NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = "Notification indicating that Moto will be woken because there are other notifications"

        val notificationManager =
            getSystemService<NotificationManager>(
                NotificationManager::class.java
            )
        notificationManager!!.createNotificationChannel(channel)

//        val notifyBuilder =
//            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_STRING)
//                .setContentTitle("You've been notified!")
//                .setContentText("This is your notification text.")
//                .setSmallIcon(R.drawable.ic_launcher_background)
//
//
//        val myNotification: Notification = notifyBuilder.build()
//        val mNotifyManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
//        mNotifyManager.notify(NOTIFICATION_CHANNEL_ID, myNotification)




    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        log("Starting service")



        if (thread?.isAlive != true){
            val builder: Notification.Builder = Notification.Builder(this, NOTIFICATION_CHANNEL_STRING)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Foreground")
                .setAutoCancel(true)

            val notification: Notification = builder.build()
            startForeground(1, notification)

            log("Starting monitoring thread")

            val filter = IntentFilter();
//            filter.addAction( Intent.ACTION_USER_BACKGROUND );
//            filter.addAction( Intent.ACTION_USER_FOREGROUND );
            filter.addAction(Intent.ACTION_SCREEN_ON)
//            filter.addAction(Intent.ACTION_SCREEN_OFF)
//            filter.addAction(Intent.ACTION_USER_UNLOCKED)
            filter.addAction(Intent.ACTION_USER_PRESENT)
//            filter.addAction(Intent.ACTION_PROVIDER_CHANGED)
//            filter.addAction(Intent.ACTION_VIEW)
            registerReceiver(phoneStateReceiver, filter);

            val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
            for (sensor in sensorList) {
                if (sensor.isWakeUpSensor && sensor.type in MotoSensors.values().map { i -> i.type}){
                    log("Sensor: $sensor ${sensor.fifoMaxEventCount} ${sensor.fifoReservedEventCount}")
                    sensorManager.registerListener(object: SensorEventListener{
                        override fun onAccuracyChanged(p0: Sensor?, p1: Int) { }
                        override fun onSensorChanged(p0: SensorEvent?) {
                            var sensorName = ""
                            for (sensorType in MotoSensors.values())
                                if (sensorType.type == p0?.sensor?.type)
                                    sensorName = sensorType.name
                            log("onSensorChanged " + sensorName + " " + p0?.values?.contentToString())
                            if (sensor.type == MotoSensors.OFF_BODY_SENSOR.type) { phoneState.offBody = p0?.values!![0] == 2.0.toFloat() }
                            if (sensor.type == MotoSensors.STOWED_SENSOR.type) { phoneState.stowed = p0?.values!![0] == 1.0.toFloat() }
                            if (sensor.type in arrayOf(MotoSensors.MOTO_GLANCE_SENSOR.type, MotoSensors.MOTO_GLANCE_APPROACH_SENSOR.type) ) { phoneState.displayTurnedOnByUser = true }
                            if (sensor.type == MotoSensors.FLAT_DOWN_SENSOR.type ) { phoneState.flatDown = p0?.values!![0] == 1.0.toFloat() }
                            if (sensor.type == MotoSensors.FLAT_UP_SENSOR.type ) { phoneState.flatUp = p0?.values!![0] == 1.0.toFloat() }
                            if (sensor.type == MotoSensors.FLIP_TO_MUTE_SENSOR.type ) { phoneState.flipToMute = p0?.values!![0] == 1.0.toFloat() }
                            if (sensor.type == MotoSensors.LIFT_TO_SILENCE_SENSOR.type ) { phoneState.liftToSilence = p0?.values!![0] == 1.0.toFloat() }
                            if (sensor.type == MotoSensors.LIFT_TO_VIEW_SENSOR.type ) { phoneState.liftToView = p0?.values!![0] == 1.0.toFloat() }
                        }
                    }, sensor, sensor.minDelay)
                }
            }

            thread = Thread(Runnable {

                while (true){
                    var displayOn = false
                    for (display in displayManager.displays) {
                        if (display.state != Display.STATE_OFF) {
                            displayOn = true
//                            if (phoneState.userInteractedSinceDisplayOn){
//                                phoneState.displayTurnedOnByUser = true
//                            }
                            break
                        }
                    }

                    if (!displayOn) {
                        if (unreadNotifications && notificationsAlertCooldown == 0 && !phoneState.displayTurnedOnByUser && unreadNotificationsCooldown == 0)
                        {
                            log( "Unread notifications");
                            notificationsAlertCooldown = 0
                            screenLock =
                                (getSystemService(Context.POWER_SERVICE) as PowerManager).newWakeLock(
                                    PowerManager.SCREEN_DIM_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, LOGGER_TAG
                                )
                            screenLock.acquire()
                            screenLock.release()
                        }
                        else {
                            if (phoneState.displayOnPreviously)
                            {
                                log("Reset phone state")
                                if (unreadNotifications && phoneState.displayTurnedOnByUser)
                                    unreadNotificationsCooldown = 10
                                unreadNotifications = !phoneState.displayTurnedOnByUser && unreadNotificationsCooldown == 0
                                phoneState.userInteractedSinceDisplayOn = false
                                phoneState.displayTurnedOnByUser = false
                            }
                            if (notificationsAlertCooldown > 0)
                                notificationsAlertCooldown--
                            if (unreadNotificationsCooldown > 0)
                                unreadNotificationsCooldown--
                        }
                    }

                    phoneState.displayOnPreviously = displayOn
                    Thread.sleep(1000)
                }
            })
            thread!!.start()
        } else {
            log("Monitoring thread already started")
        }
        log("Service started")
        return START_STICKY
    }

    override fun onDestroy() {
        Toast.makeText(this, "My Service Stopped", Toast.LENGTH_LONG).show()
        log("Service stopped")
    }
}
