package xyz.ivaniskandar.shouko.feature

import android.content.Context
import android.widget.Toast
import androidx.core.net.toUri
import logcat.LogPriority
import logcat.logcat
import xyz.ivaniskandar.shouko.R
import java.net.URLDecoder

object LinkCleaner {
    fun cleanLink(context: Context, oldLink: String): String? {
        return try {
            var oldUri = oldLink.toUri()
            when (oldUri.host) {
                "youtube.com" -> {
                    // Youtube is clean already
                    return oldLink
                }

                "l.facebook.com" -> {
                    oldUri = URLDecoder.decode(oldUri.getQueryParameter("u"), "UTF-8").toUri()
                }

                "href.li" -> {
                    oldUri = oldUri.toString().substringAfter("?").toUri()
                }
            }
            val port = oldUri.port.takeIf { it != -1 }?.let { ":$it" } ?: ""
            val newUri =
                "${oldUri.scheme}://${oldUri.host}$port${oldUri.path}".toUri().buildUpon()
            val q = oldUri.getQueryParameter("q")
            if (q != null) {
                newUri.appendQueryParameter("q", q)
            }
            newUri.toString()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.stackTraceToString() }
            // Return oldLink
            Toast.makeText(context, context.getString(R.string.link_cleaner_failed), Toast.LENGTH_SHORT).show()
            null
        }
    }
}
