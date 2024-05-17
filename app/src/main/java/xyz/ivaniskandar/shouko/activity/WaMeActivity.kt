package xyz.ivaniskandar.shouko.activity

import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.telephony.PhoneNumberUtils
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.window.Dialog
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme
import xyz.ivaniskandar.shouko.util.isPackageInstalled

/**
 * A glorified string builder to start a WhatsApp chat without adding
 * the phone number to your contacts first.
 *
 * More info of WA click to chat here: https://faq.whatsapp.com/452366545421244
 *
 * Two possible way to use this feature:
 * 1. Click a phone number detected by the system and open the intent with this app
 * 2. Copy a phone number to your clipboard and launch this activity
 * from the available App Shortcut.
 */
class WaMeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isPackageInstalled(WA_PACKAGE_NAME)) {
            Toast.makeText(applicationContext, R.string.wame_unavailable, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        when (intent.action) {
            Intent.ACTION_VIEW, Intent.ACTION_DIAL -> handle(PhoneNumberUtils.getNumberFromIntent(intent, this))
            WA_ME_CLIPBOARD -> setContent {
                ShoukoM3Theme {
                    Dialog(onDismissRequest = {}, content = {})
                }
                SideEffect {
                    val cm = getSystemService<ClipboardManager>()
                    if (cm == null) {
                        finish()
                        return@SideEffect
                    }
                    val copiedPhoneNumber = cm.primaryClip?.getItemAt(0)?.text ?: ""
                    if (!Patterns.PHONE.matcher(copiedPhoneNumber).matches()) {
                        Toast.makeText(applicationContext, R.string.wame_failed_clipboard, Toast.LENGTH_SHORT).show()
                        finish()
                        return@SideEffect
                    }
                    handle(copiedPhoneNumber)
                }
            }
        }
    }

    private fun handle(number: CharSequence) {
        val stripped = number.filter { it.isDigit() }
        val waIntent = Intent(Intent.ACTION_VIEW).apply {
            data = "$WA_ME_LINK$stripped".toUri()
            `package` = WA_PACKAGE_NAME
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(waIntent)
        finish()
    }

    companion object {
        private const val WA_ME_CLIPBOARD = "shouko.action.WA_ME_CLIPBOARD"
        private const val WA_ME_LINK = "https://wa.me/"
        private const val WA_PACKAGE_NAME = "com.whatsapp"
    }
}
