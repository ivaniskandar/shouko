package xyz.ivaniskandar.shouko.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
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
import xyz.ivaniskandar.shouko.feature.LinkCleaner
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme

class LinkCleanerTargetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (val action = intent.action) {
            Intent.ACTION_SEND -> {
                val oldLink = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (oldLink != null) {
                    val newLink = LinkCleaner.cleanLink(this, oldLink)
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
                            val newLink = LinkCleaner.cleanLink(this, oldLink.toString())
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
