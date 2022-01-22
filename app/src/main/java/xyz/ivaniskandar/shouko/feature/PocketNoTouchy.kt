package xyz.ivaniskandar.shouko.feature

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import logcat.logcat
import xyz.ivaniskandar.shouko.activity.PocketNoTouchyActivity
import xyz.ivaniskandar.shouko.feature.PocketNoTouchy.Companion.PROXIMITY_LISTEN_DURATION
import xyz.ivaniskandar.shouko.util.Prefs
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A feature module for [AccessibilityService]
 *
 * Accidental touch prevention by using proximity sensor to see
 * if the device screen is possibly switched on inside pocket or bag.
 *
 * When screen on event received and the device is not in a call mode,
 * it will check the proximity sensor if an object is near or not.
 * If not near, then it will stop checking immediately. If an object
 * is near, then it will show a full-screen blocker activity.
 *
 * When the timeout ([PROXIMITY_LISTEN_DURATION]) is reached and an
 * object is still near, it will lock the screen.
 *
 * @see PocketNoTouchyActivity
 */
class PocketNoTouchy(
    private val lifecycleOwner: LifecycleOwner,
    private val service: AccessibilityService,
) : DefaultLifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs = Prefs(service)
    private val sensorManager: SensorManager = service.getSystemService()!!
    private val audioManager: AudioManager = service.getSystemService()!!

    private val handler = Handler(Looper.getMainLooper())

    private val isProximityNear = AtomicBoolean(false)
    private var isScreenOnReceiverRegistered = false
    private var isListeningSensor = false

    private val proximityEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
                isProximityNear.set(event.values[0] == 0F)

                handler.removeCallbacks(activityDelayedRunnable)
                if (isProximityNear.get()) {
                    // Delay show activity
                    handler.postDelayed(activityDelayedRunnable, ACTIVITY_DELAY_DURATION)
                } else {
                    handler.post(activityDelayedRunnable)
                }

                handler.removeCallbacks(proximityDelayedRunnable)
                handler.postDelayed(proximityDelayedRunnable, PROXIMITY_LISTEN_DURATION)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Couldn't care less.
        }
    }

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> {
                    if (!isInCall) {
                        logcat { "Screen on event and not in call, listening to proximity" }
                        isListeningSensor = true
                        sensorManager.registerListener(
                            proximityEventListener,
                            sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                            SensorManager.SENSOR_DELAY_UI
                        )
                    } else {
                        logcat { "Screen on event but in call, do nothing..." }
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    ignoreCheck()
                }
            }
        }
    }
    private val proximityDelayedRunnable = Runnable {
        if (isProximityNear.get()) {
            // Turn off screen when timeout and proximity is still in near position
            logcat { "Proximity still near after timeout, locking screen..." }
            sensorManager.unregisterListener(proximityEventListener)
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            logcat { "Proximity still far after timeout, stop listening..." }
            sensorManager.unregisterListener(proximityEventListener)
        }
    }

    private val activityDelayedRunnable = Runnable {
        logcat { "Updating ${PocketNoTouchyActivity::class.simpleName} visible state ${isProximityNear.get()}" }
        PocketNoTouchyActivity.updateState(service, isProximityNear.get())
    }

    private val isInCall: Boolean
        get() = when (audioManager.mode) {
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION,
            AudioManager.MODE_RINGTONE -> true
            else -> false
        }

    override fun onStart(owner: LifecycleOwner) {
        updatePocketNoTouchy(prefs.preventPocketTouchEnabled)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        updatePocketNoTouchy(false)
    }

    private fun ignoreCheck() {
        if (isListeningSensor) {
            logcat { "Go to idle state" }
            sensorManager.unregisterListener(proximityEventListener)
            handler.removeCallbacks(proximityDelayedRunnable)
            handler.removeCallbacks(activityDelayedRunnable)
            PocketNoTouchyActivity.updateState(service, false)
        }
    }

    private fun updatePocketNoTouchy(state: Boolean) {
        if (state) {
            if (!isScreenOnReceiverRegistered) {
                logcat { "Enabling screen on event receiver" }
                val screenEventFilter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                }
                service.registerReceiver(screenOnReceiver, screenEventFilter)
                isScreenOnReceiverRegistered = true
            }
        } else if (isScreenOnReceiverRegistered) {
            logcat { "Disabling screen on event receiver" }
            service.unregisterReceiver(screenOnReceiver)
            isScreenOnReceiverRegistered = false
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Prefs.PREVENT_POCKET_TOUCH) {
            logcat { "${Prefs.PREVENT_POCKET_TOUCH} changed to ${prefs.preventPocketTouchEnabled}" }
            onStart(lifecycleOwner)
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        prefs.registerListener(this)

        // Ignore button listener
        lifecycleOwner.lifecycleScope.launch {
            ignoreCheckFlow.collect {
                logcat { "Ignore action received" }
                // Ignore button clicked, just unregister sensor and cancel delayed runnable.
                ignoreCheck()
            }
        }
    }

    companion object {
        // Time for proximity sensor listening after screen on
        private const val PROXIMITY_LISTEN_DURATION = 2000L // 2s

        private const val ACTIVITY_DELAY_DURATION = 500L // .5s

        val ignoreCheckFlow = MutableSharedFlow<Unit>()
    }
}
