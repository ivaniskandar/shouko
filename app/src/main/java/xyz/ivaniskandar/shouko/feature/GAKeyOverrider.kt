package xyz.ivaniskandar.shouko.feature

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.AudioManager
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.*
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.activity.GAKeyOverriderKeyguardActivity
import xyz.ivaniskandar.shouko.feature.GAKeyOverrider.Companion.ASSISTANT_LAUNCHED_CUE
import xyz.ivaniskandar.shouko.feature.GAKeyOverrider.Companion.OPA_ACTIVITY_CLASS_NAME
import xyz.ivaniskandar.shouko.feature.MediaKeyAction.Key
import xyz.ivaniskandar.shouko.util.DeviceModel
import xyz.ivaniskandar.shouko.util.Prefs
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
 * ([ASSISTANT_LAUNCHED_CUE]).
 *
 * 2. When implementing service called [onAccessibilityEvent] on
 * window state is changed, it will check if the foreground
 * activity is Google Assistant ([OPA_ACTIVITY_CLASS_NAME])
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
 * @see GAKeyOverriderKeyguardActivity
 * @see Action
 */
class GAKeyOverrider(
    private val lifecycleOwner: LifecycleOwner,
    private val service: AccessibilityService,
) : LifecycleObserver, SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs = Prefs(service)
    private val keyguardManager = service.getSystemService(KeyguardManager::class.java)!!
    private val audioManager = service.getSystemService(AudioManager::class.java)!!

    private var customAction = prefs.assistButtonAction
    private var hideAssistantCue = prefs.hideAssistantCue

    private var isActive = false
    private val isReady: Boolean
        get() = customAction != null &&
                service.checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED

    private var assistButtonLastPressedTime = 0L
    private var assistButtonHandled = true

    private val logcatCallback = object : CallbackList<String>() {
        override fun onAddElement(e: String?) {
            if (e?.contains(ASSISTANT_LAUNCHED_CUE) == true ||
                e?.contains(ASSISTANT_LAUNCHED_CUE_DISABLED_ASSISTANT) == true) {
                Timber.d("Assistant Button event detected")
                assistButtonLastPressedTime = System.currentTimeMillis()
                assistButtonHandled = false
                if (muteMusicStreamJob == null && hideAssistantCue) {
                    muteMusicStreamJob = muteMusicStream()
                }
            }
        }
    }

    private var muteMusicStreamJob: Job? = null

    @Synchronized
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        updateOpaOverrider(isReady)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun stop() {
        updateOpaOverrider(false)
    }

    fun onAccessibilityEvent(event: AccessibilityEvent?) {
        var timeDelta = System.currentTimeMillis() - assistButtonLastPressedTime
        Timber.d("timeDelta : $timeDelta")
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            (event.className == OPA_ACTIVITY_CLASS_NAME  || event.className == OPA_ACTIVITY_CLASS_NAME_DISABLED_ASSISTANT) &&
            System.currentTimeMillis() - assistButtonLastPressedTime <= 1000
        ) {
            Timber.d("Opa on foreground after Assist Button event")
            if (!assistButtonHandled) {
                assistButtonHandled = true
                if (keyguardManager.isKeyguardLocked) {
                    onOpaLaunchedAboveKeyguard()
                } else {
                    onOpaLaunched()
                }
            } else if (customAction is MediaKeyAction){
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }
        }
    }

    private fun updateOpaOverrider(state: Boolean) {
        if (state) {
            if (!isActive) {
                Timber.d("Enabling logcat observer")
                Shell.sh("logcat -c").exec()
                Shell.sh("logcat").to(logcatCallback).submit()
                isActive = true
            }
        } else if (isActive) {
            Timber.d("Disabling logcat observer")
            Shell.getCachedShell()?.close()
            isActive = false
        }
    }

    private fun onOpaLaunched() {
        customAction?.let {
            if (service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)) {
                Timber.d("Back action dispatched, launching action $it")
                when (it) {
                    is IntentAction -> {
                        lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                            delay(100)
                            it.runAction(service)
                        }
                    }
                    is MediaKeyAction -> {
                        it.runAction(service)
                    }
                }
            } else {
                Timber.e("Failed to dispatch back action, retreat!")
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun onOpaLaunchedAboveKeyguard() {
        customAction?.let {
            Timber.d("With Keyguard running action $it")
            lifecycleOwner.lifecycleScope.launch(Dispatchers.Default) {
                if (service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)) {
                    when (it) {
                        is IntentAction -> {
                            delay(500)
                            Timber.d("Starting keyguard launch activity")
                            val i = Intent(service, GAKeyOverriderKeyguardActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            service.startActivity(i)
                        }
                        is MediaKeyAction -> {
                            Timber.d("Starting media action")
                            delay(200)
                            it.runAction(service)
                            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                        }
                    }
                } else {
                    Timber.e("Failed to dispatch back action, retreat!")
                }
            }
        }
    }

    private fun muteMusicStream() = lifecycleOwner.lifecycleScope.launch {
        val volumeBeforeMuted = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val minVolume = audioManager.getStreamMinVolume(AudioManager.STREAM_MUSIC)
        Timber.d("Muting music stream volume with previous value $volumeBeforeMuted")
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, minVolume, 0)
        delay(MEDIA_MUTE_PERIOD)
        Timber.d("Restoring music stream volume to $volumeBeforeMuted")
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeBeforeMuted, 0)
        muteMusicStreamJob = null
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            Prefs.ASSIST_BUTTON_ACTION -> {
                customAction = prefs.assistButtonAction
                start()
            }
            Prefs.HIDE_ASSISTANT_CUE -> {
                hideAssistantCue = prefs.hideAssistantCue
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
        prefs.registerListener(this)
    }

    companion object {
        private const val ASSISTANT_LAUNCHED_CUE = "WindowManager: startAssist launchMode=1"
        private const val ASSISTANT_LAUNCHED_CUE_DISABLED_ASSISTANT = "GAKeyEventHandler: launchAssistGuideActivity"
        private const val OPA_ACTIVITY_CLASS_NAME = "com.google.android.apps.gsa.staticplugins.opa.OpaActivity"
        private const val OPA_ACTIVITY_CLASS_NAME_DISABLED_ASSISTANT = "com.google.android.apps.gsa.velour.dynamichosts.TransparentVelvetDynamicHostActivity"
        
        private const val MEDIA_MUTE_PERIOD = 1000L // ms

        // Only supports Xperia 5 II
        val isSupported = DeviceModel.isPDX206
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
        return intent.toUri(0)
    }

    override fun toString(): String {
        return "IntentAction(${toPlainString()})"
    }

    companion object {
        const val PLAIN_STRING_PREFIX = "#Intent;"

        fun fromPlainString(string: String): IntentAction? {
            try {
                val intent = Intent.parseUri(string, 0)
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
        context.getSystemService(AudioManager::class.java)?.run {
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
                else -> null
            }
        }
    }
}
