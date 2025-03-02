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
    /**
     * Resolves a link to common redirects.
     */
    fun resolveLink(
        context: Context,
        link: String,
    ): String? {
        return try {
            val uri = link.toUri()
            return when (uri.host?.replace("www.", "")) {
                "google.com" -> {
                    if (uri.path == "/url") {
                        uri.getQueryParameter("q").urlDecode()
                    } else {
                        uri
                    }
                }

                "l.facebook.com" -> uri.getQueryParameter("u").urlDecode()

                "href.li" -> uri.toString().substringAfter("?").toUri()

                "fxtwitter.com", "fixupx.com", "fixvx.com", "vxtwitter.com" -> {
                    uri.withHost("x.com")
                }

                "phixiv.net" -> uri.withHost("www.pixiv.net")

                "vxreddit.com" -> uri.withHost("www.reddit.com")

                "ddinstagram.com" -> uri.withHost("instagram.com")

                "tnktok.com" -> uri.withHost("tiktok.com")

                else -> uri
            }.toString()
        } catch (e: Exception) {
            logcat(LogPriority.ERROR) { e.stackTraceToString() }
            // Return oldLink
            Toast.makeText(context, context.getString(R.string.link_cleaner_failed), Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Strips a link of typical junk like tracking query params.
     */
    fun cleanLink(
        context: Context,
        oldLink: String,
    ): String? {
        return try {
            val oldUri = resolveLink(context, oldLink)?.toUri() ?: return null

            if (oldUri.isYouTube) {
                val builder = oldUri.buildUpon().clearQuery()
                oldUri.getQueryParameter("v")?.let { builder.appendQueryParameter("v", it) }
                oldUri.getQueryParameter("t")?.let { builder.appendQueryParameter("t", it) }
                return builder.toString()
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

    private fun Uri.withHost(host: String): Uri = "https://$host$path".toUri()

    private val Uri.isYouTube: Boolean
        inline get() = host?.endsWith("youtube.com") == true || host?.endsWith("youtu.be") == true

    private fun String?.urlDecode(): Uri = URLDecoder.decode(this, "UTF-8").toUri()
}
