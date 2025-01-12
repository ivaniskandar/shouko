package xyz.ivaniskandar.shouko.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import logcat.LogPriority
import logcat.logcat
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.feature.LinkCleaner
import xyz.ivaniskandar.shouko.util.shareLink

class FixTweetTargetActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        when (val action = intent.action) {
            Intent.ACTION_SEND -> {
                val oldLink = intent.getStringExtra(Intent.EXTRA_TEXT)
                if (oldLink != null) {
                    val newLink = LinkCleaner.cleanLink(this, oldLink)
                    if (newLink != null) {
                        val fixedLink = handle(newLink)
                        if (fixedLink != null) {
                            shareLink(fixedLink)
                        } else {
                            Toast.makeText(this, R.string.fixtweet_failed_unknown, Toast.LENGTH_SHORT).show()
                        }
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

    private fun handle(link: String): String? {
        val uri = Uri.parse(link)
        val builder = uri.buildUpon()
        val newHost =
            when (uri.authority) {
                "twitter.com" -> "fxtwitter.com"
                "x.com" -> "fixupx.com"
                else -> return null
            }
        builder.authority(newHost)
        return builder.build().toString()
    }
}
