package xyz.ivaniskandar.shouko.feature

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.app.StatusBarManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdUnits
import androidx.compose.material.icons.rounded.Aod
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.getSystemService
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.activity.GAKeyOverriderKeyguardActivity
import xyz.ivaniskandar.shouko.feature.GAKeyOverrider.Companion.ASSISTANT_GUIDE_LAUNCHED_CUE
import xyz.ivaniskandar.shouko.feature.GAKeyOverrider.Companion.ASSISTANT_LAUNCHED_CUE
import xyz.ivaniskandar.shouko.feature.GAKeyOverrider.Companion.GOOGLE_PACKAGE_NAME
import xyz.ivaniskandar.shouko.feature.MediaKeyAction.Key
import xyz.ivaniskandar.shouko.util.DeviceModel
import xyz.ivaniskandar.shouko.util.Prefs
import xyz.ivaniskandar.shouko.util.canReadSystemLogs
import xyz.ivaniskandar.shouko.util.loadLabel
import java.net.URISyntaxException

/**
 * A feature module for [AccessibilityService]
 *
 * Overrides the Assistant Button action from launching Google
 * Assistant to any Intent. Only supports Sony Xperia 5 II (PDX-206).
 *
 * Implementing [AccessibilityService] needs to listen to window
 * state changes.
 *
 * This class reads logcat to listen for Assistant button event. The
 * rest of what this class does is as follows:
 * 1. User pressed the Assistant button as it shows on logcat
 * ([ASSISTANT_LAUNCHED_CUE] or [ASSISTANT_GUIDE_LAUNCHED_CUE]).
 *
 * 2. When implementing service called [onAccessibilityEvent] on
 * window state is changed, it will check if the foreground
 * activity is Google Assistant ([GOOGLE_PACKAGE_NAME])
 *
 * 3. From here depends from whether the device is in lock state
 * or not and the custom [Action] selected. But basically the
 * implementing [AccessibilityService] will dispatch back button
 * action to close the foreground Assistant activity and launches
 * the user-selected action.
 *
 * When it runs on locked state, [GAKeyOverriderKeyguardActivity]
 * will be called first to request unlock and launches the custom
 * action when user unlock completed.
 *
 * Also supports completely disabling the button as part of Game
 * Enhancer.
 *
 * @see GAKeyOverriderKeyguardActivity
 * @see Action
 */
class GAKeyOverrider(
    private val lifecycleOwner: LifecycleOwner,
    private val service: AccessibilityService,
) : LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs = Prefs(service)
    private val keyguardManager: KeyguardManager = service.getSystemService()!!
    private val audioManager: AudioManager = service.getSystemService()!!

    private var customAction = prefs.assistButtonAction
    private var hideAssistantCue = prefs.hideAssistantCue
    private var buttonEnabled = prefs.assistButtonEnabled

    private var isActive = false
    private val isReady: Boolean
        get() = buttonEnabled && customAction != null && service.canReadSystemLogs

    // When this observer is registered, the assistant button will always be disabled.
    private val gaKeyDisabler = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            if (uri?.lastPathSegment == GA_KEY_DISABLED_GLOBAL_SETTING_KEY) {
                isButtonSystemDisabled = true
            }
        }
    }
    private var isGAKeyDisablerRegistered = false
    private var isButtonSystemDisabled: Boolean
        get() = Settings.Global.getInt(service.contentResolver, GA_KEY_DISABLED_GLOBAL_SETTING_KEY) == 1
        set(value) {
            Settings.Global.putInt(service.contentResolver, GA_KEY_DISABLED_GLOBAL_SETTING_KEY, if (value) 1 else 0)
        }

    private var assistButtonPressHandled = true

    private val logcatCallback = object : CallbackList<String>() {
        override fun onAddElement(e: String) {
            if (e.contains(ASSISTANT_LAUNCHED_CUE) || e.contains(ASSISTANT_GUIDE_LAUNCHED_CUE)) {
                Timber.d("Assistant Button event detected")
                assistButtonPressHandled = false
                if (hideAssistantCue) {
                    muteMusicStream(true)
                }
            }
        }
    }

    private var audioPlaybackCallbackRegistered = false
    private var volumeBeforeMuted: Int? = null
    private val audioPlaybackCallback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<AudioPlaybackConfiguration>) {
            super.onPlaybackConfigChanged(configs)
            val assistantCuePlaying = configs.map { it.audioAttributes.usage }
                .contains(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
            if (!assistantCuePlaying) {
                lifecycleOwner.lifecycleScope.launch {
                    delay(500)
                    muteMusicStream(false)
                }
            }
        }
    }

    @Synchronized
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        updateGAKeyDisabler(!buttonEnabled)
        updateOpaOverrider(isReady)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun stop() {
        updateGAKeyDisabler(false)
        updateOpaOverrider(false)
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!assistButtonPressHandled && event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.packageName == GOOGLE_PACKAGE_NAME
        ) {
            Timber.d("Opa on foreground after Assist Button event")
            assistButtonPressHandled = true
            if (keyguardManager.isKeyguardLocked) {
                onOpaLaunchedAboveKeyguard()
            } else {
                onOpaLaunched()
            }
        }
    }

    private fun updateGAKeyDisabler(state: Boolean) {
        if (state) {
            if (!isGAKeyDisablerRegistered) {
                Timber.d("Registering Assist button disabler")
                isButtonSystemDisabled = true
                service.contentResolver.registerContentObserver(
                    Settings.Global.getUriFor(GA_KEY_DISABLED_GLOBAL_SETTING_KEY),
                    false,
                    gaKeyDisabler
                )
                isGAKeyDisablerRegistered = true
            }
        } else if (isGAKeyDisablerRegistered) {
            Timber.d("Unregistering Assist Button disabler")
            service.contentResolver.unregisterContentObserver(gaKeyDisabler)
            isButtonSystemDisabled = false
            isGAKeyDisablerRegistered = false
        }
    }

    private fun updateOpaOverrider(state: Boolean) {
        if (state) {
            if (!isActive) {
                Timber.d("Enabling logcat observer")
                Shell.sh("logcat -c").exec()
                Shell.sh("logcat").to(logcatCallback).submit {
                    Timber.d("Logcat observer stopped")
                    isActive = false
                    if (lifecycleOwner.lifecycle.currentState == Lifecycle.State.STARTED) {
                        start() // Restart when needed
                    }
                }
                isActive = true
            }
        } else {
            if (isActive) {
                Timber.d("Disabling logcat observer")
                Shell.getCachedShell()?.close()
                isActive = false
            }
        }

        // Audio listener for hiding assistant cue
        if (state && hideAssistantCue) {
            if (!audioPlaybackCallbackRegistered) {
                Timber.d("Registering audio playback listener")
                audioManager.registerAudioPlaybackCallback(audioPlaybackCallback, null)
                audioPlaybackCallbackRegistered = true
            }
        } else if (!hideAssistantCue && audioPlaybackCallbackRegistered) {
            Timber.d("Unregistering audio playback listener")
            audioManager.unregisterAudioPlaybackCallback(audioPlaybackCallback)
            audioPlaybackCallbackRegistered = false
        }
    }

    private fun onOpaLaunched() {
        customAction?.let {
            Timber.d("Launching action $it")
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            when (it) {
                is IntentAction -> {
                    lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                        delay(100)
                        it.runAction(service)
                    }
                }
                else -> {
                    it.runAction(service)
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun onOpaLaunchedAboveKeyguard() {
        customAction?.let {
            Timber.d("With Keyguard running action $it")
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                when (it) {
                    is IntentAction -> {
                        delay(500)
                        Timber.d("Starting keyguard launch activity")
                        val i = Intent(service, GAKeyOverriderKeyguardActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        service.startActivity(i)
                    }
                    else -> {
                        delay(200)
                        it.runAction(service)
                        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    }
                }
            }
        }
    }

    // Muting will need to followed by unmuting and vice versa, else it won't do anything
    private fun muteMusicStream(mute: Boolean) {
        if (mute) {
            if (volumeBeforeMuted == null) {
                volumeBeforeMuted = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                Timber.d("Muting music stream volume with previous value $volumeBeforeMuted")
                audioManager.setStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC),
                    0
                )
            }
        } else if (volumeBeforeMuted != null) {
            Timber.d("Restoring music stream volume to $volumeBeforeMuted")
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBeforeMuted!!, 0)
            volumeBeforeMuted = null
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Prefs.ASSIST_BUTTON_ACTION, Prefs.HIDE_ASSISTANT_CUE, Prefs.ASSIST_BUTTON_ENABLED -> {
                lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                    customAction = prefs.assistButtonAction
                    hideAssistantCue = prefs.hideAssistantCue
                    buttonEnabled = prefs.assistButtonEnabled
                    start()
                }
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        prefs.registerListener(this)
    }

    companion object {
        private const val ASSISTANT_LAUNCHED_CUE = "WindowManager: startAssist launchMode=1"
        private const val ASSISTANT_GUIDE_LAUNCHED_CUE = "GAKeyEventHandler: launchAssistGuideActivity"
        private const val GOOGLE_PACKAGE_NAME = "com.google.android.googlequicksearchbox"

        private const val GA_KEY_DISABLED_GLOBAL_SETTING_KEY = "somc.game_enhancer_gab_key_disabled"

        // Only supports Xperia 5 II
        val isSupported = DeviceModel.isPDX206 || DeviceModel.isPDX213
    }
}

/**
 * Intent launching action. Device needs to be in unlocked state
 * before launching the custom action.
 */
class IntentAction(private val intent: Intent) : Action() {
    override fun runAction(context: Context) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun getLabel(context: Context): String {
        return context.getString(R.string.intent_action_label, intent.loadLabel(context))
    }

    override fun toPlainString(): String {
        return PLAIN_STRING_PREFIX + intent.toUri(0)
    }

    override fun toString(): String {
        return "IntentAction(${toPlainString()})"
    }

    companion object {
        const val PLAIN_STRING_PREFIX = "IntentAction::"

        fun fromPlainString(string: String): IntentAction? {
            try {
                val intentUri = string.substringAfter(PLAIN_STRING_PREFIX, "null")
                val intent = Intent.parseUri(intentUri, 0)
                return IntentAction(intent)
            } catch (e: URISyntaxException) {
                Timber.e(e, "Malformed intent uri $string")
            }
            return null
        }
    }
}

/**
 * Custom action that dispatches media key event. Supported key
 * event is defined in [Key].
 *
 * It is possible to trigger this action in locked state.
 *
 * @see KeyEvent
 */
class MediaKeyAction(private val key: Key) : Action() {
    override fun runAction(context: Context) {
        context.getSystemService<AudioManager>()?.run {
            dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, key.code))
            dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, key.code))
        }
    }

    override fun getLabel(context: Context): String {
        return context.getString(R.string.media_key_action_label, context.getString(key.labelResId))
    }

    override fun toPlainString(): String {
        return PLAIN_STRING_PREFIX + key.name
    }

    override fun toString(): String {
        return "MediaKeyAction(${key.name})"
    }

    enum class Key(val code: Int, val labelResId: Int, val iconResId: Int) {
        PLAY(KeyEvent.KEYCODE_MEDIA_PLAY, R.string.media_key_play, R.drawable.ic_play),
        PAUSE(KeyEvent.KEYCODE_MEDIA_PAUSE, R.string.media_key_pause, R.drawable.ic_pause),
        PLAY_PAUSE(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, R.string.media_key_play_pause, R.drawable.ic_play_pause),
        STOP(KeyEvent.KEYCODE_MEDIA_STOP, R.string.media_key_stop, R.drawable.ic_stop),
        PREVIOUS(KeyEvent.KEYCODE_MEDIA_PREVIOUS, R.string.media_key_previous, R.drawable.ic_previous),
        NEXT(KeyEvent.KEYCODE_MEDIA_NEXT, R.string.media_key_next, R.drawable.ic_next)
    }

    companion object {
        const val PLAIN_STRING_PREFIX = "MediaKeyAction::"

        fun fromPlainString(string: String): MediaKeyAction? {
            val keyName = string.substringAfter(PLAIN_STRING_PREFIX, "null")
            try {
                return MediaKeyAction(Key.valueOf(keyName))
            } catch (e: Exception) {
                Timber.e(e, "$keyName doesn't exists in Key")
            }
            return null
        }
    }
}

/**
 * Custom action to toggle camera torch
 */
class FlashlightAction : Action() {
    private var flashCameraId: String? = null
    private var flashlightEnabled = false

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (cameraId == flashCameraId) {
                flashlightEnabled = enabled
            }
        }
    }

    @Synchronized
    override fun runAction(context: Context) {
        val cameraManager = context.getSystemService<CameraManager>()!!
        try {
            // Get cameraId and register torch callback
            if (flashCameraId == null) {
                flashCameraId = getCameraId(cameraManager)
                cameraManager.registerTorchCallback(torchCallback, null)
            }
            cameraManager.setTorchMode(flashCameraId!!, !flashlightEnabled)
        } catch (ignored: CameraAccessException) {
        }
    }

    override fun getLabel(context: Context): String {
        return context.getString(R.string.flashlight_action_label)
    }

    override fun toPlainString(): String {
        return PLAIN_STRING
    }

    override fun toString(): String {
        return toPlainString()
    }

    private fun getCameraId(cameraManager: CameraManager): String? {
        for (id in cameraManager.cameraIdList) {
            val c = cameraManager.getCameraCharacteristics(id)
            val flashAvailable = c.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            val lensFacingBack = c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
            if (flashAvailable && lensFacingBack) {
                return id
            }
        }
        return null
    }

    companion object {
        const val PLAIN_STRING = "FlashlightAction"

        fun isSupported(context: Context): Boolean {
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)
        }
    }
}

/**
 * Takes a screenshot. Needs AccessibilityService context to run action.
 */
class ScreenshotAction : Action() {
    override fun runAction(context: Context) {
        if (context is AccessibilityService) {
            context.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            Timber.e("Context is not AccessibilityService, do nothing.")
        }
    }

    override fun getLabel(context: Context): String {
        return context.getString(R.string.screenshot_action_label)
    }

    override fun toPlainString(): String {
        return PLAIN_STRING
    }

    companion object {
        const val PLAIN_STRING = "ScreenshotAction"
    }
}

/**
 * Expands status bar panel
 */
class StatusBarAction(private val type: PanelType) : Action() {
    override fun runAction(context: Context) {
        try {
            StatusBarManager::class.java
                .getMethod(type.method)
                .invoke(context.getSystemService<StatusBarManager>())
        } catch (e: Exception) {
            Timber.e(e, "Failed to run statusbar action.")
        }
    }

    override fun getLabel(context: Context): String {
        return context.getString(type.labelResId)
    }

    override fun toPlainString(): String {
        return PLAIN_STRING_PREFIX + type.name
    }

    enum class PanelType(val method: String, val labelResId: Int, val iconVector: ImageVector) {
        NOTIFICATION("expandNotificationsPanel", R.string.statusbar_action_notifications, Icons.Rounded.Aod),
        QS("expandSettingsPanel", R.string.statusbar_action_qs, Icons.Rounded.AdUnits)
    }

    companion object {
        const val PLAIN_STRING_PREFIX = "StatusBarAction::"

        fun fromPlainString(string: String): StatusBarAction? {
            val typeName = string.substringAfter(PLAIN_STRING_PREFIX, "null")
            try {
                return StatusBarAction(PanelType.valueOf(typeName))
            } catch (e: Exception) {
                Timber.e(e, "$typeName doesn't exists in PanelType")
            }
            return null
        }
    }
}

/**
 * Launches nothing. Basically makes the Assistant button to be a wakeup button.
 */
class DoNothingAction : Action() {
    override fun runAction(context: Context) {
        // do nothing duh.
    }

    override fun getLabel(context: Context): String {
        return context.getString(R.string.do_nothing)
    }

    override fun toPlainString(): String {
        return PLAIN_STRING
    }

    companion object {
        const val PLAIN_STRING = "DoNothingAction"
    }
}

/**
 * Base class for custom action
 */
sealed class Action {
    abstract fun runAction(context: Context)
    abstract fun getLabel(context: Context): String
    abstract fun toPlainString(): String

    companion object {
        fun fromPlainString(string: String): Action? {
            return when {
                string.startsWith(IntentAction.PLAIN_STRING_PREFIX) -> {
                    IntentAction.fromPlainString(string)
                }
                string.startsWith(MediaKeyAction.PLAIN_STRING_PREFIX) -> {
                    MediaKeyAction.fromPlainString(string)
                }
                string == FlashlightAction.PLAIN_STRING -> {
                    FlashlightAction()
                }
                string == ScreenshotAction.PLAIN_STRING -> {
                    ScreenshotAction()
                }
                string.startsWith(StatusBarAction.PLAIN_STRING_PREFIX) -> {
                    StatusBarAction.fromPlainString(string)
                }
                string == DoNothingAction.PLAIN_STRING -> {
                    DoNothingAction()
                }
                else -> {
                    Timber.e("Unrecognized string: $string")
                    null
                }
            }
        }
    }
}
