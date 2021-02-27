package xyz.ivaniskandar.shouko.feature

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.*
import kotlinx.coroutines.*
import timber.log.Timber
import xyz.ivaniskandar.shouko.util.DeviceModel
import xyz.ivaniskandar.shouko.util.Prefs
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A feature module for [AccessibilityService]
 *
 * Enables Priority Do Not Disturb when device is put face down on
 * a flat surface and lock the screen.
 *
 * On a device without either wake up type of proximity sensor or
 * accelerometer sensor, the detection will only run when the screen
 * is on. Otherwise the gesture detection is always-on.
 *
 * Device screen will be turned off after enabling DND.
 */
class FlipToShush(
    private val lifecycleOwner: LifecycleOwner,
    private val service: AccessibilityService,
) : LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs = Prefs(service)
    private val sensorManager = service.getSystemService(SensorManager::class.java)!!
    private val notificationManager = service.getSystemService(NotificationManager::class.java)!!
    private val vibrator = service.getSystemService(Vibrator::class.java)!!
    private val checkerWakeLock = service.getSystemService(PowerManager::class.java)!!
        .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Shouko::ShushConditionChecker")

    private val shushOnVibrationEffect = VibrationEffect
        .createWaveform(longArrayOf(12L, 150L, 10L, 250L, 8L), intArrayOf(250, 0, 200, 0, 150), -1)
    private val shushOffVibrationEffect = VibrationEffect.createOneShot(20, 255)

    private var x = .1F
    private var y = .1f
    private var z = .1F
    private var isProximityNear = false
    private var isDndOnByService = false

    private val isDoNotDisturbOff: Boolean
        get() = notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL
    private val isDeviceFaceDown: Boolean
        get() = z < -6
    private val isDeviceFlatFaceDown: Boolean
        get() = if (isDeviceFaceDown) {
            // From https://stackoverflow.com/a/15149421/13755568
            val nG = sqrt(x.pow(2) + y.pow(2) + z.pow(2)).toDouble()
            val inclination = Math.toDegrees(acos(z / nG))
            inclination >= 170
        } else {
            false
        }

    private var accelerometerListenerRegistered = false
    private val accelerometerEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            when (event?.sensor?.type) {
                SOMC_WAKEUP_ACCELEROMETER, Sensor.TYPE_ACCELEROMETER -> {
                    // "I hate doing math so I'll keep the values and do it when I really need to" - The service (maybe)
                    x = event.values[0]
                    y = event.values[1]
                    z = event.values[2]

                    // Route to start shush
                    if (!isDndOnByService && currentJob == null && isDoNotDisturbOff && isDeviceFaceDown) {
                        registerSensors(proximity = true, accelerometer = true)
                        currentJob = checkWhileFacingDownAndShush()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Couldn't care less.
        }
    }

    private var proximityListenerRegistered = false
    private val proximityEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            when (event?.sensor?.type) {
                Sensor.TYPE_PROXIMITY -> {
                    isProximityNear = event.values[0] == 0F

                    // Route to end shush
                    if (isDndOnByService && currentJob == null && !isProximityNear) {
                        currentJob = delayCheckProximityFarBeforeUnshush()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Couldn't care less.
        }
    }

    private var screenEventReceiverRegistered = false
    private val screenEventReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_ON -> updateFlipToShush(true)
                Intent.ACTION_SCREEN_OFF -> updateFlipToShush(false)
            }
        }
    }

    private var currentJob: Job? = null

    @Synchronized
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        val shouldEnable = prefs.flipToShushEnabled && notificationManager.isNotificationPolicyAccessGranted
        updateFlipToShush(shouldEnable)
        updateScreenReceiverState(shouldEnable && !supportFullTimeListening(service))
        if (!shouldEnable) {
            switchDndState(false)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun stop() {
        updateFlipToShush(false)
        updateScreenReceiverState(false)
        switchDndState(false)
    }

    private fun updateScreenReceiverState(state: Boolean) {
        if (state) {
            if (!screenEventReceiverRegistered) {
                Timber.d("Registering screen listener")
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                }
                service.registerReceiver(screenEventReceiver, filter)
                screenEventReceiverRegistered = true
            }
        } else if (screenEventReceiverRegistered) {
            Timber.d("Unregistering screen listener")
            service.unregisterReceiver(screenEventReceiver)
            screenEventReceiverRegistered = false
        }
    }

    // Master switch
    private fun updateFlipToShush(state: Boolean) {
        if (state) {
            registerSensors(proximity = isDndOnByService, accelerometer = !isDndOnByService)
        } else {
            registerSensors(proximity = false, accelerometer = false)
            currentJob?.cancel()
            currentJob = null
        }
    }

    private fun registerSensors(proximity: Boolean, accelerometer: Boolean) {
        if (proximity) {
            if (!proximityListenerRegistered) {
                val sensor = sensorManager.getProximity()
                Timber.d("Registering $sensor to $proximityEventListener")
                sensorManager.registerListener(proximityEventListener, sensor, SENSOR_SAMPLING_PERIOD)
                proximityListenerRegistered = true
            }
        } else if (proximityListenerRegistered) {
            Timber.d("Unregistering proximity $proximityEventListener")
            sensorManager.unregisterListener(proximityEventListener)
            proximityListenerRegistered = false
        }
        if (accelerometer) {
            if (!accelerometerListenerRegistered) {
                val sensor = sensorManager.getAccelerometer()
                Timber.d("Registering sensor $sensor to $accelerometerEventListener")
                sensorManager.registerListener(accelerometerEventListener, sensor, SENSOR_SAMPLING_PERIOD)
                accelerometerListenerRegistered = true
            }
        } else if (accelerometerListenerRegistered) {
            Timber.d("Unregistering accelerometer $accelerometerEventListener")
            sensorManager.unregisterListener(accelerometerEventListener)
            accelerometerListenerRegistered = false
        }
    }

    private fun switchDndState(state: Boolean) {
        isDndOnByService = if (state) {
            if (!isDndOnByService) {
                Timber.d("Shush state on")
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                vibrator.vibrate(shushOnVibrationEffect)
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                true
            } else {
                Timber.d("User DND is active, shush state unchanged")
                false
            }
        } else {
            if (isDndOnByService) {
                Timber.d("Shush state off")
                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                vibrator.vibrate(shushOffVibrationEffect)
            }
            false
        }
    }

    // Make sure isDeviceFaceDown is true, no user and system DND enabled before starting this job
    private fun checkWhileFacingDownAndShush() = lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
        Timber.d("Continuously checking conditions while facing down")
        while (isActive && isDeviceFaceDown) {
            if (!isDndOnByService && isProximityNear && isDoNotDisturbOff && isDeviceFlatFaceDown) {
                Timber.d("Waiting period before rechecking conditions")
                checkerWakeLock.acquire(SHUSH_WAITING_PERIOD * 2)
                delay(SHUSH_WAITING_PERIOD)
                if (!isDndOnByService && isProximityNear && isDoNotDisturbOff && isDeviceFlatFaceDown) {
                    Timber.d("Shush conditions met and stopping check")
                    switchDndState(true)
                    registerSensors(proximity = true, accelerometer = false)
                    currentJob = null
                    return@launch
                } else {
                    Timber.d("Shush conditions unmet")
                }
                checkerWakeLock.release()
            }
        }
        Timber.d("Is cancelled or not facing down anymore, stopping check")
        registerSensors(proximity = false, accelerometer = true)
        currentJob = null
    }

    // Make sure the service DND is enabled and proximity far before starting this job
    private fun delayCheckProximityFarBeforeUnshush() = lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
        Timber.d("Waiting period before rechecking conditions")
        checkerWakeLock.acquire(UNSHUSH_WAITING_PERIOD * 2)
        delay(UNSHUSH_WAITING_PERIOD)
        if (isDndOnByService && !isProximityNear) {
            Timber.d("Unshush condition met")
            switchDndState(false)
            registerSensors(proximity = false, accelerometer = true)
        } else {
            Timber.d("Unshush condition unmet, shush state unchanged")
        }
        checkerWakeLock.release()
        currentJob = null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Prefs.FLIP_TO_SHUSH) {
            Timber.d("${Prefs.FLIP_TO_SHUSH} changed to ${prefs.flipToShushEnabled}")
            start()
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        prefs.registerListener(this)
    }

    companion object {
        private const val SENSOR_SAMPLING_PERIOD = 1000000 // us = 1s
        private const val SHUSH_WAITING_PERIOD = 500L // ms = 0.5s
        private const val UNSHUSH_WAITING_PERIOD = 350L // ms = 0.35s

        /**
         * name="lsm6dsm somc Accelerometer Wakeup"
         * vendor="STMicro"
         * version=140559
         * type=65661
         * maxRange=78.4532
         * resolution=0.0023928226
         * power=0.15
         * minDelay=2404
         */
        private const val SOMC_WAKEUP_ACCELEROMETER = 65661

        /**
         * Returns wake up accelerometer, null if can't find even the non-wake up one.
         */
        private fun SensorManager.getAccelerometer(): Sensor? {
            if (DeviceModel.isPDX206 || DeviceModel.isPDX203) {
                return getDefaultSensor(SOMC_WAKEUP_ACCELEROMETER, true)
            }

            // Try to get wake up variant first
            var sensor = getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
            if (sensor == null) {
                sensor = getDefaultSensor(Sensor.TYPE_ACCELEROMETER, false)
            }
            return sensor
        }

        /**
         * Returns wake up proximity, null if can't find even the non-wake up one.
         */
        private fun SensorManager.getProximity(): Sensor? {
            // Try to get wake up variant first
            var sensor = getDefaultSensor(Sensor.TYPE_PROXIMITY, true)
            if (sensor == null) {
                sensor = getDefaultSensor(Sensor.TYPE_PROXIMITY, false)
            }
            return sensor
        }

        fun supportFullTimeListening(context: Context): Boolean {
            val sensorManager = context.getSystemService(SensorManager::class.java)
            return sensorManager?.getAccelerometer()?.isWakeUpSensor == true &&
                    sensorManager.getProximity()?.isWakeUpSensor == true
        }
    }
}
