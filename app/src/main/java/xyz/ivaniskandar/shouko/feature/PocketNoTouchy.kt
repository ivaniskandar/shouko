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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import timber.log.Timber
import xyz.ivaniskandar.shouko.activity.PocketNoTouchyActivity
import xyz.ivaniskandar.shouko.feature.PocketNoTouchy.Companion.PROXIMITY_LISTEN_DURATION
import xyz.ivaniskandar.shouko.util.Prefs

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
    lifecycleOwner: LifecycleOwner,
    private val service: AccessibilityService,
) : LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs = Prefs(service)
    private val sensorManager = service.getSystemService(SensorManager::class.java)!!
    private val audioManager = service.getSystemService(AudioManager::class.java)!!

    private val handler = Handler(Looper.getMainLooper())

    private var isProximityNear = false
    private var isScreenOnReceiverRegistered = false
    private var isListeningSensor = false

    private val proximityEventListener: SensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_PROXIMITY) {
                isProximityNear = event.values[0] == 0F
                Timber.d("Updating ${PocketNoTouchyActivity::class.simpleName} visible state $isProximityNear")
                PocketNoTouchyActivity.updateState(service, isProximityNear)

                // Only reset when proximity is far
                if (!isProximityNear) {
                    Timber.d("Resetting check timeout")
                    handler.removeCallbacks(proximityDelayedRunnable)
                    handler.postDelayed(proximityDelayedRunnable, PROXIMITY_LISTEN_DURATION)
                }
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
                        Timber.d("Screen on event and not in call, listening to proximity")
                        isListeningSensor = true
                        sensorManager.registerListener(
                            proximityEventListener,
                            sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY),
                            SensorManager.SENSOR_DELAY_UI
                        )
                        handler.postDelayed(proximityDelayedRunnable, PROXIMITY_LISTEN_DURATION)
                    } else {
                        Timber.d("Screen on event but in call, do nothing...")
                    }
                }
                Intent.ACTION_SCREEN_OFF -> {
                    ignoreCheck()
                }
            }
        }
    }
    private val proximityDelayedRunnable = Runnable {
        Timber.d("Check timeout")
        sensorManager.unregisterListener(proximityEventListener)
        if (isProximityNear) {
            Timber.d("Proximity still near after timeout, locking screen...")
            // Turn off screen when timeout and proximity is still in near position
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
    private val ignoreActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Timber.d("Ignore action received")
            // Ignore button clicked, just unregister sensor and cancel delayed runnable.
            ignoreCheck()
        }
    }

    private val isInCall: Boolean
        get() = when (audioManager.mode) {
            AudioManager.MODE_IN_CALL,
            AudioManager.MODE_IN_COMMUNICATION,
            AudioManager.MODE_RINGTONE -> true
            else -> false
        }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        updatePocketNoTouchy(prefs.preventPocketTouchEnabled)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun stop() {
        updatePocketNoTouchy(false)
    }

    private fun ignoreCheck() {
        if (isListeningSensor) {
            Timber.d("Go to idle state")
            sensorManager.unregisterListener(proximityEventListener)
            handler.removeCallbacks(proximityDelayedRunnable)
            PocketNoTouchyActivity.updateState(service, false)
        }
    }

    private fun updatePocketNoTouchy(state: Boolean) {
        val lbm = LocalBroadcastManager.getInstance(service)
        if (state) {
            if (!isScreenOnReceiverRegistered) {
                Timber.d("Enabling screen on event receiver")
                val screenEventFilter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_ON)
                    addAction(Intent.ACTION_SCREEN_OFF)
                }
                service.registerReceiver(screenOnReceiver, screenEventFilter)
                lbm.registerReceiver(ignoreActionReceiver, IntentFilter(ACTION_IGNORE_SENSOR))
                isScreenOnReceiverRegistered = true
            }
        } else if (isScreenOnReceiverRegistered) {
            Timber.d("Disabling screen on event receiver")
            lbm.unregisterReceiver(ignoreActionReceiver)
            service.unregisterReceiver(screenOnReceiver)
            isScreenOnReceiverRegistered = false
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == Prefs.PREVENT_POCKET_TOUCH) {
            Timber.d("${Prefs.PREVENT_POCKET_TOUCH} changed to ${prefs.preventPocketTouchEnabled}")
            start()
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        prefs.registerListener(this)
    }

    companion object {
        // Time for proximity sensor listening after screen on
        private const val PROXIMITY_LISTEN_DURATION = 2000L // 2s

        const val ACTION_IGNORE_SENSOR = "PocketNoTouchy.action.IGNORE_SENSOR"
    }
}
