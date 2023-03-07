package xyz.ivaniskandar.shouko.feature

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import logcat.logcat
import xyz.ivaniskandar.shouko.ShoukoApplication
import xyz.ivaniskandar.shouko.util.canWriteSecureSettings

/**
 * A feature module to apply custom lockscreen shortcut
 *
 * Since the right side lockscreen shortcut is tied to the
 * "Double tap power button to launch camera" gesture, we need to
 * keep the values default when the keyguard is locked. This module
 * does all the heavy lifting.
 *
 * Custom values from preferences will be applied when the keyguard
 * is shown to user. Otherwise, those will be set to system default
 * a.k.a null.
 *
 * The caveat is the power button double tap gesture will be
 * "broken" if it's triggered when the keyguard is showing and
 * custom shortcut is applied. But hey, we can't have all nice
 * things in the world, can we?
 */
class LockscreenShortcutHelper(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
) : DefaultLifecycleObserver {
    private var receiverRegistered = false
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!context.canWriteSecureSettings) {
                return
            }
            val keyguardLocked = context.getSystemService<KeyguardManager>()!!.isKeyguardLocked
            val screenOn = intent.action != Intent.ACTION_SCREEN_OFF
            if (screenOn && keyguardLocked) {
                lifecycleOwner.lifecycleScope.launch {
                    delay(75) // 5 II camera button action fix
                    logcat { "Set camera lockscreen shortcuts to custom ${intent.action}" }
                    // Keyguard is showing
                    val prefs = ShoukoApplication.prefs
                    Settings.Secure.putString(
                        context.contentResolver,
                        LOCKSCREEN_LEFT_BUTTON,
                        prefs.lockscreenLeftAction.first(),
                    )
                    Settings.Secure.putString(
                        context.contentResolver,
                        LOCKSCREEN_RIGHT_BUTTON,
                        prefs.lockscreenRightAction.first(),
                    )
                }
            } else {
                logcat { "Set lockscreen shortcuts to system default" }
                Settings.Secure.putString(context.contentResolver, LOCKSCREEN_LEFT_BUTTON, null)
                Settings.Secure.putString(context.contentResolver, LOCKSCREEN_RIGHT_BUTTON, null)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        updateReceiverState(false)
    }

    private fun updateReceiverState(state: Boolean) {
        if (state) {
            if (!receiverRegistered) {
                logcat { "Registering receiver" }
                val filter = IntentFilter().apply {
                    addAction(Intent.ACTION_SCREEN_OFF)
                    addAction(Intent.ACTION_SCREEN_ON)
                    priority = 999
                }
                context.registerReceiver(receiver, filter)
                receiverRegistered = true
            }
        } else if (receiverRegistered) {
            logcat { "Unregistering receiver" }
            context.unregisterReceiver(receiver)
            receiverRegistered = false
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        lifecycleOwner.lifecycleScope.launchWhenStarted {
            val prefs = ShoukoApplication.prefs
            prefs.lockscreenLeftAction
                .combine(prefs.lockscreenRightAction) { a, b -> a != null || b != null }
                .collect { updateReceiverState(it) }
        }
    }

    companion object {
        /**
         * System's Secure Settings key
         */
        const val LOCKSCREEN_LEFT_BUTTON = "sysui_keyguard_left"
        const val LOCKSCREEN_RIGHT_BUTTON = "sysui_keyguard_right"
    }
}
