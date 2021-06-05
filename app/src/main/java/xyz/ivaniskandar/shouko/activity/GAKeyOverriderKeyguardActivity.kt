package xyz.ivaniskandar.shouko.activity

import android.app.KeyguardManager
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme
import xyz.ivaniskandar.shouko.util.Prefs

/**
 * Activity to be launched on top of keyguard.
 *
 * It request keyguard unlock and launches custom assistant
 * button action when succeeded.
 *
 * @see xyz.ivaniskandar.shouko.feature.GAKeyOverrider
 */
class GAKeyOverriderKeyguardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ShoukoTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Box(
                        modifier = Modifier
                            .clickable { dismissKeyguard() }
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    private fun dismissKeyguard() {
        getSystemService<KeyguardManager>()?.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    super.onDismissSucceeded()
                    Prefs(this@GAKeyOverriderKeyguardActivity).assistButtonAction
                        ?.runAction(this@GAKeyOverriderKeyguardActivity)
                    finish()
                }
            }
        )
    }

    init {
        lifecycleScope.launchWhenCreated {
            delay(700)
            dismissKeyguard()
        }
    }
}
