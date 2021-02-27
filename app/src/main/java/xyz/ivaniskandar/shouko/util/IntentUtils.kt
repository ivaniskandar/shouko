package xyz.ivaniskandar.shouko.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import xyz.ivaniskandar.shouko.feature.IntentAction

val GITHUB_REPO_INTENT = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ivaniskandar/shouko"))

/**
 * Sets an intent as the custom Assistant button action.
 *
 * @see xyz.ivaniskandar.shouko.feature.GAKeyOverrider
 * @see xyz.ivaniskandar.shouko.feature.IntentAction
 */
@Suppress("DEPRECATION")
fun Intent.setAsAssistantAction(prefs: Prefs) {
    if (!isValidExtraType(Intent.EXTRA_SHORTCUT_INTENT, Intent::class.java)) {
        return
    }
    val intent = Intent(getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT)).apply {
        // For UI
        putExtra(Intent.EXTRA_SHORTCUT_NAME, getStringExtra(Intent.EXTRA_SHORTCUT_NAME))
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
        ri.loadLabel(context.packageManager).toString()
    }
}

/**
 * Returns true if intent extra class type for [key] is the same with [type].
 */
private fun <T> Intent?.isValidExtraType(key: String, type: Class<T>): Boolean {
    return type.isInstance(this?.getParcelableExtra(key))
}
