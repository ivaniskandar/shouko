package xyz.ivaniskandar.shouko.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.content.Intent.ACTION_SCREEN_OFF
import android.content.Intent.ACTION_SCREEN_ON
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_PROXIMITY
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.FULL_WAKE_LOCK
import android.os.PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.service.quicksettings.TileService
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.theme.ShoukoAccent

/**
 * Foreground service for running Tile features.
 *
 * Coffee: Acquires [FULL_WAKE_LOCK] to make screen stays on.
 * Tea: Listens to proximity sensor. When near, acquires
 * [PROXIMITY_SCREEN_OFF_WAKE_LOCK] to keep the device awake
 * while screen is turned off.
 *
 * @see CoffeeTileService
 * @see TeaTileService
 */
class TadanoTileParentService : Service() {
    private val vibrator by lazy { getSystemService(Vibrator::class.java)!! }
    private val sensorManager by lazy { getSystemService(SensorManager::class.java)!! }

    private var isCoffeeReceiverRegistered = false
    private var isTeaReceiverRegistered = false

    @Suppress("DEPRECATION")
    private val coffeeWakeLock by lazy {
        getSystemService(PowerManager::class.java)!!.newWakeLock(FULL_WAKE_LOCK, "Shouko::Coffee")
    }
    private val teaWakeLock by lazy {
        getSystemService(PowerManager::class.java)!!.newWakeLock(PROXIMITY_SCREEN_OFF_WAKE_LOCK, "Shouko::Tea")
    }

    private val teaScreenActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_SCREEN_ON -> switchProximityListener(true)
                ACTION_SCREEN_OFF -> switchProximityListener(false)
            }
        }
    }
    private val coffeeScreenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            stop()
        }
    }

    private val proximitySensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == TYPE_PROXIMITY) {
                val distance = event.values[0]
                if (distance == 0f) {
                    vibrator.vibrate(VibrationEffect.createOneShot(21, VibrationEffect.MAX_AMPLITUDE))
                    switchTeaWakeLock(true)
                } else {
                    switchTeaWakeLock(false)
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            // Couldn't care less.
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        when (intent.action) {
            ACTION_START_SERVICE -> {
                getSystemService(NotificationManager::class.java)!!.run {
                    val channel = NotificationChannel(
                        CHANNEL_GENERAL,
                        getString(R.string.tadano_tile_service_notif_title),
                        NotificationManager.IMPORTANCE_LOW
                    ).apply {
                        description = getString(R.string.tadano_tile_service_notif_channel_desc)
                        setSound(null, null)
                    }
                    createNotificationChannel(channel)
                }
                val channelSettingsIntent = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                    putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_GENERAL)
                }
                val clickPendingIntent = PendingIntent.getActivity(
                    this,
                    FOREGROUND_SERVICE_ID,
                    channelSettingsIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )
                val type = intent.getSerializableExtra(EXTRA_SERVICE_TYPE) as Type
                val icon = when (type) {
                    Type.COFFEE -> R.drawable.ic_coffee
                    Type.TEA -> R.drawable.ic_tea
                }
                startForeground(
                    FOREGROUND_SERVICE_ID,
                    NotificationCompat.Builder(this, CHANNEL_GENERAL)
                        .setShowWhen(false)
                        .setSmallIcon(icon)
                        .setColor(ShoukoAccent.toArgb())
                        .setContentTitle(getString(R.string.tadano_tile_service_notif_title))
                        .setContentText(getString(R.string.tadano_tile_service_notif_text))
                        .setContentIntent(clickPendingIntent)
                        .build()
                )

                when (type) {
                    Type.COFFEE -> switchCoffeeMode(true)
                    Type.TEA -> switchTeaMode(true)
                }
            }
            ACTION_STOP_SERVICE -> stop()
            else -> throw Exception("wtf")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        switchCoffeeMode(false)
        switchTeaMode(false)
    }

    private fun switchTeaMode(active: Boolean) {
        if (active) {
            if (isCoffeeActive) {
                switchCoffeeMode(false)
            }
            val filter = IntentFilter().apply {
                addAction(ACTION_SCREEN_OFF)
                addAction(ACTION_SCREEN_ON)
                priority = 999
            }
            registerReceiver(teaScreenActionReceiver, filter)
            isTeaReceiverRegistered = true

            switchProximityListener(true)
        } else {
            if (isTeaReceiverRegistered) {
                unregisterReceiver(teaScreenActionReceiver)
                isTeaReceiverRegistered = false
            }
            switchProximityListener(false)
        }
        isTeaActive = active

        // Update tile state
        TileService.requestListeningState(
            this,
            ComponentName(this, TeaTileService::class.java)
        )
    }

    private fun switchCoffeeMode(active: Boolean) {
        if (active) {
            if (isTeaActive) {
                switchTeaMode(false)
            }
            val filter = IntentFilter().apply {
                addAction(ACTION_SCREEN_OFF)
                priority = 999
            }
            registerReceiver(coffeeScreenOffReceiver, filter)
            isCoffeeReceiverRegistered = true

            switchCoffeeWakeLock(true)
        } else {
            if (isCoffeeReceiverRegistered) {
                unregisterReceiver(coffeeScreenOffReceiver)
                isCoffeeReceiverRegistered = false
            }
            switchCoffeeWakeLock(false)
        }
        isCoffeeActive = active

        // Update tile state
        TileService.requestListeningState(
            this,
            ComponentName(this, CoffeeTileService::class.java)
        )
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    @SuppressLint("WakelockTimeout")
    private fun switchTeaWakeLock(acquire: Boolean) {
        if (acquire) {
            if (!teaWakeLock.isHeld) {
                teaWakeLock.acquire()
            }
        } else {
            if (teaWakeLock.isHeld) {
                teaWakeLock.release()
            }
        }
    }

    @SuppressLint("WakelockTimeout")
    private fun switchCoffeeWakeLock(acquire: Boolean) {
        if (acquire) {
            if (!coffeeWakeLock.isHeld) {
                coffeeWakeLock.acquire()
            }
        } else {
            if (coffeeWakeLock.isHeld) {
                coffeeWakeLock.release()
            }
        }
    }

    private fun switchProximityListener(on: Boolean) {
        if (on) {
            sensorManager.getDefaultSensor(TYPE_PROXIMITY)?.run {
                sensorManager.registerListener(proximitySensorListener, this, SENSOR_DELAY_NORMAL)
            }
        } else {
            sensorManager.unregisterListener(proximitySensorListener)
        }
    }

    override fun onBind(intent: Intent): IBinder? = null

    companion object {
        var isCoffeeActive = false
        var isTeaActive = false

        const val CHANNEL_GENERAL = "general"

        const val ACTION_START_SERVICE = "start_service"
        const val ACTION_STOP_SERVICE = "stop_service"

        const val EXTRA_SERVICE_TYPE = "service_type"

        const val FOREGROUND_SERVICE_ID = 14045
    }

    enum class Type {
        COFFEE, TEA
    }
}
