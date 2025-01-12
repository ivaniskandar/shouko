package xyz.ivaniskandar.shouko.feature

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.hardware.SensorManager.SENSOR_DELAY_UI
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.logcat
import xyz.ivaniskandar.shouko.ShoukoApplication
import xyz.ivaniskandar.shouko.util.DeviceModel
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A feature module for [AccessibilityService].
 *
 * Enables Priority Do Not Disturb when device is put face down on
 * a flat surface and lock the screen.
 *
 * On a device without wake up type of proximity sensor, the
 * detection will only run when the screen is on. Otherwise the
 * gesture detection is always-on.
 *
 * Device screen will be turned off after enabling DND.
 */
class FlipToShush(
    private val lifecycleOwner: LifecycleOwner,
    private val service: AccessibilityService,
) : DefaultLifecycleObserver {
    private val sensorManager: SensorManager = service.getSystemService()!!
    private val notificationManager: NotificationManager = service.getSystemService()!!
    private val vibrator: Vibrator = service.getSystemService()!!
    private val sensorWakeLock =
        service
            .getSystemService<PowerManager>()!!
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Shouko::FlipToShushSensor")

    private val isFullTimeListening = supportFullTimeListening(service)

    private val shushOnVibrationEffect =
        VibrationEffect
            .createWaveform(longArrayOf(16L, 150L, 14L, 250L, 12L), intArrayOf(200, 0, 150, 0, 100), -1)
    private val shushOffVibrationEffect = VibrationEffect.createOneShot(20, 255)

    private var deviceInclination = 0.0
    private var isProximityNear = false
    private var isDndOnByService = false

    private val isDoNotDisturbOff: Boolean
        get() = notificationManager.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL

    private val isDeviceFlatFaceDown: Boolean
        get() = deviceInclination >= 170

    private var accelerometerListenerRegistered = false
    private val accelerometerEventListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                when (event?.sensor?.type) {
                    SOMC_WAKEUP_ACCELEROMETER, Sensor.TYPE_ACCELEROMETER -> {
                        // Calculate inclination
                        // From https://stackoverflow.com/a/15149421/13755568
                        val (x, y, z) = event.values
                        val nG = sqrt(x.pow(2) + y.pow(2) + z.pow(2)).toDouble()
                        deviceInclination = Math.toDegrees(acos(z / nG))
                    }
                }
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int,
            ) {
                // Couldn't care less.
            }
        }

    private var proximityListenerRegistered = false
    private val proximityEventListener =
        object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                when (event?.sensor?.type) {
                    Sensor.TYPE_PROXIMITY -> {
                        isProximityNear = event.values[0] == 0F

                        // Cancel previous job
                        shushCheckerJob?.cancel()
                        unshushCheckerJob?.cancel()

                        // Route to start shush
                        if (!isDndOnByService && isDoNotDisturbOff && isProximityNear) {
                            shushCheckerJob = startCheckForShush()
                        }

                        // Route to end shush
                        if (isDndOnByService && !isProximityNear) {
                            unshushCheckerJob = startCheckForUnshush()
                        }
                    }
                }
            }

            override fun onAccuracyChanged(
                sensor: Sensor?,
                accuracy: Int,
            ) {
                // Couldn't care less.
            }
        }

    private var screenEventReceiverRegistered = false
    private val screenEventReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context?,
                intent: Intent?,
            ) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> updateFlipToShush(true)
                    Intent.ACTION_SCREEN_OFF -> updateFlipToShush(false)
                }
            }
        }

    private var shushCheckerJob: Job? = null
    private var unshushCheckerJob: Job? = null

    override fun onDestroy(owner: LifecycleOwner) {
        updateFlipToShush(false)
        updateScreenReceiverState(false)
        switchDndState(false)
    }

    private fun updateScreenReceiverState(state: Boolean) {
        if (state) {
            if (!screenEventReceiverRegistered) {
                logcat { "Registering screen listener" }
                val filter =
                    IntentFilter().apply {
                        addAction(Intent.ACTION_SCREEN_ON)
                        addAction(Intent.ACTION_SCREEN_OFF)
                    }
                service.registerReceiver(screenEventReceiver, filter)
                screenEventReceiverRegistered = true
            }
        } else if (screenEventReceiverRegistered) {
            logcat { "Unregistering screen listener" }
            service.unregisterReceiver(screenEventReceiver)
            screenEventReceiverRegistered = false
        }
    }

    // Master switch
    private fun updateFlipToShush(state: Boolean) {
        if (state) {
            registerSensors(proximity = true, accelerometer = false)
        } else {
            registerSensors(proximity = false, accelerometer = false)
        }
    }

    private fun registerSensors(
        proximity: Boolean,
        accelerometer: Boolean,
    ) {
        if (proximity) {
            if (!proximityListenerRegistered) {
                val sensor = sensorManager.getProximity()
                logcat { "Registering \"${sensor?.name}\" to $proximityEventListener" }
                sensorManager.registerListener(proximityEventListener, sensor, SENSOR_DELAY_NORMAL)
                proximityListenerRegistered = true
            }
        } else if (proximityListenerRegistered) {
            logcat { "Unregistering proximity $proximityEventListener" }
            sensorManager.unregisterListener(proximityEventListener)
            proximityListenerRegistered = false
        }
        if (accelerometer) {
            if (!accelerometerListenerRegistered) {
                val sensor = sensorManager.getAccelerometer()
                logcat { "Registering sensor \"${sensor?.name}\" to $accelerometerEventListener" }
                if (isFullTimeListening && !sensor!!.isWakeUpSensor) {
                    logcat { "Acquiring wakelock because \"${sensor.name}\" is a non-wakeup sensor" }
                    sensorWakeLock.acquire(SENSOR_WAKELOCK_TIMEOUT)
                }
                sensorManager.registerListener(accelerometerEventListener, sensor, SENSOR_DELAY_UI)
                accelerometerListenerRegistered = true
            }
        } else if (accelerometerListenerRegistered) {
            logcat { "Unregistering accelerometer $accelerometerEventListener" }
            sensorManager.unregisterListener(accelerometerEventListener)
            if (sensorWakeLock.isHeld) {
                sensorWakeLock.release()
            }
            accelerometerListenerRegistered = false
        }
    }

    private fun switchDndState(state: Boolean) {
        isDndOnByService =
            if (state) {
                if (!isDndOnByService) {
                    logcat { "Shush state on" }
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    vibrator.vibrate(shushOnVibrationEffect)
                    service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                    true
                } else {
                    logcat { "User DND is active, shush state unchanged" }
                    false
                }
            } else {
                if (isDndOnByService) {
                    logcat { "Shush state off" }
                    notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                    vibrator.vibrate(shushOffVibrationEffect)
                }
                false
            }
    }

    private fun startCheckForShush() = lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
        try {
            logcat { "Waiting period before rechecking conditions" }
            registerSensors(proximity = true, accelerometer = true)

            val inclinations = mutableListOf<Double>()
            val startWait = SystemClock.elapsedRealtime()
            var currentWaitTime = SystemClock.elapsedRealtime() - startWait
            while (currentWaitTime < SHUSH_WAITING_PERIOD) {
                if (!isActive) {
                    throw CancellationException()
                }
                if (currentWaitTime >= SHUSH_WAITING_PERIOD / 2) {
                    inclinations += deviceInclination
                }
                currentWaitTime = SystemClock.elapsedRealtime() - startWait
            }

            if (!isDndOnByService && isProximityNear && isDoNotDisturbOff && isDeviceFlatFaceDown) {
                val inclinationsAvg = inclinations.average().toBigDecimal().setScale(1, RoundingMode.HALF_EVEN)
                val currentInclination = deviceInclination.toBigDecimal().setScale(1, RoundingMode.HALF_EVEN)
                if ((inclinationsAvg - currentInclination).abs() <= BigDecimal.ONE) {
                    logcat { "Shush conditions met and stopping check" }
                    switchDndState(true)
                } else {
                    logcat { "No shush, device wasn't in stationary position" }
                }
            } else {
                logcat { "Shush conditions unmet" }
            }
        } catch (e: CancellationException) {
            logcat { "Job cancelled" }
        } finally {
            registerSensors(proximity = true, accelerometer = false)
        }
    }

    private fun startCheckForUnshush() = lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
        try {
            logcat { "Waiting period before rechecking conditions" }
            registerSensors(proximity = true, accelerometer = true)

            val startWait = SystemClock.elapsedRealtime()
            while (SystemClock.elapsedRealtime() - startWait < UNSHUSH_WAITING_PERIOD) {
                if (!isActive) {
                    throw CancellationException()
                }
            }

            if (isDndOnByService && !isProximityNear) {
                logcat { "Unshush condition met" }
                switchDndState(false)
            } else {
                logcat { "Unshush condition unmet, shush state unchanged" }
            }
        } catch (e: CancellationException) {
            logcat { "Job cancelled" }
        } finally {
            registerSensors(proximity = true, accelerometer = false)
        }
    }

    init {
        lifecycleOwner.lifecycleScope.launchWhenStarted {
            ShoukoApplication.prefs.flipToShushEnabledFlow.collect {
                val shouldEnable = it && notificationManager.isNotificationPolicyAccessGranted
                updateFlipToShush(shouldEnable)
                updateScreenReceiverState(shouldEnable && !isFullTimeListening)
                if (!shouldEnable) {
                    switchDndState(false)
                }
                logcat { "Flip2Shush enabled=$it" }
            }
        }
        lifecycleOwner.lifecycle.addObserver(this)
    }

    companion object {
        private const val SHUSH_WAITING_PERIOD = 2000L // ms = 2s
        private const val UNSHUSH_WAITING_PERIOD = 350L // ms = 0.35s

        private const val SENSOR_WAKELOCK_TIMEOUT = 2000L // ms = 2s

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
            var sensor: Sensor? = null
            if (DeviceModel.isPDX206 || DeviceModel.isPDX203) {
                sensor = getDefaultSensor(SOMC_WAKEUP_ACCELEROMETER, true)
            }

            // Try to get wake-up sensor
            if (sensor == null) {
                sensor = getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true)
            }

            // Fallback to non-wakeup sensor
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

        fun supportFullTimeListening(context: Context): Boolean = context.getSystemService<SensorManager>()?.getProximity()?.isWakeUpSensor == true
    }
}
