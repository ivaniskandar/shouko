package xyz.ivaniskandar.shouko.util

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import androidx.core.os.bundleOf
import timber.log.Timber
import xyz.ivaniskandar.shouko.feature.IntentAction

val RELEASES_PAGE_INTENT = Intent(ACTION_VIEW, Uri.parse("https://github.com/ivaniskandar/shouko/releases/latest"))

/**
 * Extras for settings
 */
const val EXTRA_FRAGMENT_ARG_KEY = ":settings:fragment_args_key"
const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = ":settings:show_fragment_args"

/**
 * Sets an intent as the custom Assistant button action.
 *
 * @see xyz.ivaniskandar.shouko.feature.GAKeyOverrider
 * @see xyz.ivaniskandar.shouko.feature.IntentAction
 */
@Suppress("DEPRECATION")
fun Intent.setAsAssistantAction(prefs: Prefs) {
    if (!isValidExtraType(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)) {
        Timber.e("Returned intent doesn't have shortcut intent extra!")
        return
    }
    val name = getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
    Timber.d("Preparing to save intent action with label $name")
    val intent = Intent(getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT)).apply {
        // For UI
        putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
    }
    prefs.assistButtonAction = IntentAction(intent)
}

/**
 * Returns intent label saved as an extra ([Intent.EXTRA_SHORTCUT_NAME])
 * If not found, the target package label will be used instead.
 *
 * @see [loadLabel]
 */
fun Intent.loadLabel(context: Context): String {
    // Try to get shortcut label from intent first
    @Suppress("DEPRECATION") val shortcutLabel = getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
    return if (shortcutLabel != null) {
        shortcutLabel
    } else {
        val ri = context.packageManager.resolveActivity(this, 0)
        ri?.loadLabel(context.packageManager).toString()
    }
}

/**
 * Returns true if intent extra class type for [key] is the same with [type].
 */
private fun <T> Intent?.isValidExtraType(key: String, type: Class<T>): Boolean {
    return type.isInstance(this?.getParcelableExtra(key))
}

/**
 * Undocumented feature to highlight an item in system settings.
 *
 * @param string package name or ComponentName string, can also be a key to the setting.
 */
fun Intent.highlightSettingsTo(string: String): Intent {
    putExtra(EXTRA_FRAGMENT_ARG_KEY, string)
    val bundle = bundleOf(EXTRA_FRAGMENT_ARG_KEY to string)
    putExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS, bundle)
    return this
}

/**
 * Returns a chooser intent for the specified text string
 */
fun createShareTextIntent(text: String): Intent {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    return Intent.createChooser(sendIntent, null)
}
