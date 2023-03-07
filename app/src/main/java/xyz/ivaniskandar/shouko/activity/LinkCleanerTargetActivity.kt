package xyz.ivaniskandar.shouko.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.window.Dialog
import androidx.core.content.getSystemService
import logcat.LogPriority
import logcat.logcat
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme
import java.net.URLDecoder

class LinkCleanerTargetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (val action = intent.action) {
            Intent.ACTION_SEND -> {
                val oldLink = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (oldLink != null) {
                    val newLink = cleanLink(oldLink)
                    if (newLink != null) {
                        shareLink(newLink)
                        Toast.makeText(this, R.string.link_cleaner_success, Toast.LENGTH_SHORT).show()
                    }
                }
                finish()
            }
            ACTION_CLEAN_CLIPBOARD -> {
                setContent {
                    ShoukoM3Theme {
                        Dialog(onDismissRequest = {}, content = {})
                    }
                    SideEffect {
                        val cm = getSystemService<ClipboardManager>()
                        if (cm == null) {
                            finish()
                            return@SideEffect
                        }
                        val oldLink = cm.primaryClip?.getItemAt(0)?.text
                        if (oldLink.isValidUrl()) {
                            val newLink = cleanLink(oldLink.toString())
                            if (newLink != null) {
                                cm.setPrimaryClip(ClipData.newPlainText("cleaned link", newLink))
                                Toast.makeText(this, R.string.link_cleaner_success_clipboard, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this, R.string.link_cleaner_failed_clipboard, Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    }
                }
            }
            else -> {
                logcat(LogPriority.ERROR) { "Unknown action: $action" }
                finish()
            }
        }
    }

    private fun CharSequence?.isValidUrl(): Boolean {
        return this != null && Patterns.WEB_URL.matcher(this).matches()
    }

    private fun cleanLink(oldLink: String): String? {
        try {
            var oldUri = Uri.parse(oldLink)
            when (oldUri.host) {
                "youtube.com" -> {
                    // Youtube is clean already
                    return oldLink
                }
                "l.facebook.com" -> {
                    oldUri = Uri.parse(URLDecoder.decode(oldUri.getQueryParameter("u"), "UTF-8"))
                }
                "href.li" -> {
                    oldUri = Uri.parse(oldUri.toString().substringAfter("?"))
                }
            }
            val port = oldUri.port.takeIf { it != -1 }?.let { ":$it" } ?: ""
            val newUri = Uri.parse("${oldUri.scheme}://${oldUri.host}$port${oldUri.path}").buildUpon()
            val q = oldUri.getQueryParameter("q")
            if (q != null) {
                newUri.appendQueryParameter("q", q)
            }
            return newUri.toString()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.stackTraceToString() }
            // Return oldLink
            Toast.makeText(this, getString(R.string.link_cleaner_failed), Toast.LENGTH_SHORT).show()
            return null
        }
    }

    private fun shareLink(newLink: String) {
        val newIntent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = intent.type
                putExtra(Intent.EXTRA_TEXT, newLink)
            },
            null,
        ).apply {
            // Don't loop lol
            putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(componentName))
        }
        startActivity(newIntent)
    }

    companion object {
        private const val ACTION_CLEAN_CLIPBOARD = "shouko.action.CLEAN_CLIPBOARD"
    }
}
