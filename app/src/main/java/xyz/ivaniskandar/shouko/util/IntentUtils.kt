package xyz.ivaniskandar.shouko.util

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import logcat.LogPriority
import logcat.logcat
import xyz.ivaniskandar.shouko.feature.IntentAction

val RELEASES_PAGE_INTENT = Intent(ACTION_VIEW, "https://github.com/ivaniskandar/shouko/releases/latest".toUri())

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
suspend fun Intent.setAsAssistantAction(prefs: PreferencesRepository) {
    if (!isValidExtraType(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)) {
        logcat(LogPriority.ERROR) { "Returned intent doesn't have shortcut intent extra!" }
        return
    }
    val name = getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
    logcat { "Preparing to save intent action with label $name" }
    val intent = Intent(getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT)).apply {
        // For UI
        putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
    }
    prefs.setAssistButtonAction(IntentAction(intent))
}

/**
 * Returns intent label saved as an extra ([Intent.EXTRA_SHORTCUT_NAME])
 * If not found, the target package label will be used instead.
 *
 * @see [loadLabel]
 */
fun Intent.loadLabel(context: Context): String {
    val pm = context.packageManager

    // Try to get shortcut label from intent first
    @Suppress("DEPRECATION")
    return getStringExtra(Intent.EXTRA_SHORTCUT_NAME)
        ?: (
            // Backoff to activity label
            pm.resolveActivityCompat(this, PackageManager.MATCH_ALL)?.loadLabel(pm)?.toString()
                // Backoff to app label
                ?: pm.getApplicationLabel(pm.getApplicationInfoCompat(component!!.packageName, 0)).toString()
            )
}

/**
 * Returns true if intent extra class type for [key] is the same with [type].
 */
private fun <T> Intent?.isValidExtraType(key: String, type: Class<T>): Boolean {
    @Suppress("DEPRECATION") // https://issuetracker.google.com/issues/240585930
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
