package xyz.ivaniskandar.shouko.feature

import android.content.Context
import android.net.Uri
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
            when (oldUri.host?.replace("www.", "")) {
                "youtube.com" -> {
                    // Youtube is clean already
                    return oldLink
                }

                "google.com" -> {
                    if (oldUri.path == "/url") {
                        oldUri = oldUri.getQueryParameter("q").urlDecode()
                    }
                }

                "l.facebook.com" -> {
                    oldUri = oldUri.getQueryParameter("u").urlDecode()
                }

                "href.li" -> {
                    oldUri = oldUri.toString().substringAfter("?").toUri()
                }

                "fxtwitter.com", "fixupx.com", "fixvx.com", "vxtwitter.com" -> {
                    oldUri = oldUri.withHost("x.com")
                }

                "phixiv.net" -> {
                    oldUri = oldUri.withHost("www.pixiv.net")
                }

                "vxreddit.com" -> {
                    oldUri = oldUri.withHost("www.reddit.com")
                }

                "ddinstagram.com" -> {
                    oldUri = oldUri.withHost("instagram.com")
                }

                "tnktok.com" -> {
                    oldUri = oldUri.withHost("tiktok.com")
                }
            }
            val port = oldUri.port.takeIf { it != -1 }?.let { ":$it" } ?: ""
            val newUri =
                "${oldUri.scheme}://${oldUri.host}$port${oldUri.path}".toUri().buildUpon()
            oldUri.getQueryParameter("q")?.let {
                newUri.appendQueryParameter("q", it)
            }
            newUri.toString()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.stackTraceToString() }
            // Return oldLink
            Toast.makeText(context, context.getString(R.string.link_cleaner_failed), Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun Uri.withHost(host: String): Uri {
        return "https://$host$path".toUri()
    }

    private fun String?.urlDecode(): Uri {
        return URLDecoder.decode(this, "UTF-8").toUri()
    }
}
