package xyz.ivaniskandar.shouko.feature

import android.app.KeyguardManager
import android.content.*
import android.provider.Settings
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import timber.log.Timber
import xyz.ivaniskandar.shouko.util.canWriteSecureSettings

/**
 * A feature module to apply custom lockscreen shortcut
 *
 * Since the right side lockscreen shortcut is tied to the
 * "Double tap power button to launch camera" gesture, we need to
 * keep the values default when the keyguard is locked. This module
 * does all the heavy lifting.
 *
 * Custom values inside local preferences ([getPreferences]) will
 * be applied when the keyguard is shown to user. Otherwise, those
 * will be set to system default a.k.a null.
 *
 * The caveat is the power button double tap gesture will be
 * "broken" if it's triggered when the keyguard is showing and
 * custom shortcut is applied. But hey, we can't have all nice
 * things in the world, can we?
 */
class LockscreenShortcutHelper(
    lifecycleOwner: LifecycleOwner,
    private val context: Context,
) : LifecycleObserver {
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!context.canWriteSecureSettings) {
                return
            }
            val keyguardLocked = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
            val screenOn = intent.action != Intent.ACTION_SCREEN_OFF
            if (screenOn && keyguardLocked) {
                Timber.d("Set lockscreen shortcuts to custom")
                // Keyguard is showing
                val localSettings = getPreferences(context)
                Settings.Secure.putString(
                    context.contentResolver,
                    LOCKSCREEN_LEFT_BUTTON,
                    localSettings.getString(LOCKSCREEN_LEFT_BUTTON, null)
                )
                Settings.Secure.putString(
                    context.contentResolver,
                    LOCKSCREEN_RIGHT_BUTTON,
                    localSettings.getString(LOCKSCREEN_RIGHT_BUTTON, null)
                )
            } else {
                Timber.d("Set lockscreen shortcuts to system default")
                Settings.Secure.putString(context.contentResolver, LOCKSCREEN_LEFT_BUTTON, null)
                Settings.Secure.putString(context.contentResolver, LOCKSCREEN_RIGHT_BUTTON, null)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        Timber.d("Registering receiver")
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
            priority = 999
        }
        context.registerReceiver(receiver, filter)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun stop() {
        Timber.d("Unregistering receiver")
        context.unregisterReceiver(receiver)
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    companion object {
        /**
         * System's Secure Settings key
         */
        const val LOCKSCREEN_LEFT_BUTTON = "sysui_keyguard_left"
        const val LOCKSCREEN_RIGHT_BUTTON = "sysui_keyguard_right"

        fun getPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences("secure_settings", Context.MODE_PRIVATE)
        }
    }
}