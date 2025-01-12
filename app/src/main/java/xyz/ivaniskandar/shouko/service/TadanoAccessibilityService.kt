package xyz.ivaniskandar.shouko.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ServiceLifecycleDispatcher
import logcat.logcat
import xyz.ivaniskandar.shouko.feature.FlipToShush
import xyz.ivaniskandar.shouko.feature.GAKeyOverrider
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper
import xyz.ivaniskandar.shouko.feature.PocketNoTouchy

/**
 * General purpose service, features implemented in separate package.
 *
 * @see xyz.ivaniskandar.shouko.feature
 */
class TadanoAccessibilityService :
    AccessibilityService(),
    LifecycleOwner {
    private val dispatcher = ServiceLifecycleDispatcher(this)

    /**
     * Feature modules
     */
    private var gaKeyOverrider: GAKeyOverrider? = null
    private var pocketNoTouchy: PocketNoTouchy? = null
    private var flipToShush: FlipToShush? = null
    private var lockscreenShortcutHelper: LockscreenShortcutHelper? = null

    override fun onServiceConnected() {
        dispatcher.onServicePreSuperOnBind()
        super.onServiceConnected()
        isActive = true
        logcat { "onServiceConnected" }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        gaKeyOverrider?.onAccessibilityEvent(event)
    }

    override fun onInterrupt() {
        logcat { "onInterrupt" }
    }

    override fun onCreate() {
        if (GAKeyOverrider.isSupported) {
            gaKeyOverrider = GAKeyOverrider(this, this)
        }
        pocketNoTouchy = PocketNoTouchy(this, this)
        flipToShush = FlipToShush(this, this)
        lockscreenShortcutHelper = LockscreenShortcutHelper(this, this)
        dispatcher.onServicePreSuperOnCreate()
        super.onCreate()
        logcat { "onCreate" }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        dispatcher.onServicePreSuperOnStart()
        logcat { "onStartCommand" }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        dispatcher.onServicePreSuperOnDestroy()
        isActive = false
        super.onDestroy()
        logcat { "onDestroy" }
    }

    override val lifecycle = dispatcher.lifecycle

    companion object {
        var isActive: Boolean by mutableStateOf(false)
            private set
    }
}
