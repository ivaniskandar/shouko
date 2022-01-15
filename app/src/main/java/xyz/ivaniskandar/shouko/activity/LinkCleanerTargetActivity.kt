package xyz.ivaniskandar.shouko.activity

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import xyz.ivaniskandar.shouko.R
import java.net.URLDecoder

class LinkCleanerTargetActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.action == Intent.ACTION_SEND) {
            val oldLink = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (oldLink != null) {
                shareLink(cleanLink(oldLink))
            }
        }
        finish()
    }

    private fun cleanLink(oldLink: String): String {
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
            Toast.makeText(this, getString(R.string.link_cleaner_success), Toast.LENGTH_SHORT).show()
            return newUri.toString()
        } catch (e: Exception) {
            // Return oldLink
            Toast.makeText(this, getString(R.string.link_cleaner_failed), Toast.LENGTH_SHORT).show()
            return oldLink
        }
    }

    private fun shareLink(newLink: String) {
        val newIntent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = intent.type
                putExtra(Intent.EXTRA_TEXT, newLink)
            },
            null
        ).apply {
            // Don't loop lol
            putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(componentName))
        }
        startActivity(newIntent)
    }
}
