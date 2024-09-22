package xyz.ivaniskandar.shouko.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.net.toUri
import logcat.LogPriority
import logcat.logcat
import xyz.ivaniskandar.shouko.feature.LinkCleaner

class LinkArchiverTargetActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (val action = intent.action) {
            Intent.ACTION_SEND -> {
                val oldLink = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (oldLink != null) {
                    val newLink = LinkCleaner.resolveLink(this, oldLink)
                    if (newLink != null) {
                        openLink(newLink)
                    }
                }
                finish()
            }
            else -> {
                logcat(LogPriority.ERROR) { "Unknown action: $action" }
                finish()
            }
        }
    }

    private fun openLink(newLink: String) {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = "https://archive.ph/latest/$newLink".toUri()
            },
        )
    }
}
