package xyz.ivaniskandar.shouko.activity

import android.app.KeyguardManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.ivaniskandar.shouko.ShoukoApplication
import xyz.ivaniskandar.shouko.ui.destination.KeyguardUnlock
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme

/**
 * Activity to be launched on top of keyguard.
 *
 * It request keyguard unlock and launches custom assistant
 * button action when succeeded.
 *
 * @see xyz.ivaniskandar.shouko.feature.GAKeyOverrider
 */
class GAKeyOverriderKeyguardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ShoukoM3Theme {
                KeyguardUnlock { dismissKeyguard() }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun dismissKeyguard() {
        getSystemService<KeyguardManager>()?.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    lifecycleScope.launch {
                        val action = runBlocking { ShoukoApplication.prefs.assistButtonFlow.first().action }
                        action?.runAction(this@GAKeyOverriderKeyguardActivity)
                        finish()
                    }
                }
            },
        )
    }

    init {
        lifecycleScope.launchWhenCreated {
            delay(700)
            dismissKeyguard()
        }
    }
}
