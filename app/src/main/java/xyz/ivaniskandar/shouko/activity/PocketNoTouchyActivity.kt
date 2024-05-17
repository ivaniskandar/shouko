package xyz.ivaniskandar.shouko.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import kotlinx.coroutines.launch
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.feature.PocketNoTouchy
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3PreviewTheme
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme

/**
 * A full-screen dialog activity to block accidental touches.
 *
 * @see PocketNoTouchy
 */
class PocketNoTouchyActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.getBooleanExtra(EXTRA_HIDE, false)) finish()
        super.onCreate(savedInstanceState)

        // Set full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            ShoukoM3Theme(darkTheme = true) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center,
                ) {
                    DialogCard {
                        lifecycleScope.launch { PocketNoTouchy.ignoreCheckFlow.emit(Unit) }
                    }
                }
            }
            val systemUiController = rememberSystemUiController()
            SideEffect {
                systemUiController.isSystemBarsVisible = false
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_HIDE, false)) finish()
    }

    override fun onResume() {
        super.onResume()
        running = true
    }

    override fun onPause() {
        super.onPause()
        running = false
    }

    @Composable
    fun DialogCard(onButtonClicked: () -> Unit = {}) {
        OutlinedCard(
            modifier = Modifier.padding(24.dp),
            border = BorderStroke(width = 1.dp, color = Color(0x1FFFFFFF)),
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
                Text(
                    text = stringResource(R.string.no_touchy_dialog_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.no_touchy_dialog_text),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = onButtonClicked) {
                        Text(text = stringResource(id = R.string.no_touchy_dialog_ignore_button))
                    }
                }
            }
        }
    }

    @Preview
    @Composable
    private fun DialogCardPreview() {
        ShoukoM3PreviewTheme(darkTheme = true) {
            Box(modifier = Modifier.background(Color.Black)) {
                DialogCard()
            }
        }
    }

    companion object {
        private var running = false
        private const val EXTRA_HIDE = "PocketNoTouchyActivity.extra.HIDE"

        fun updateState(context: Context, show: Boolean) {
            if ((show && running) || (!show && !running)) return
            val intent = Intent(context, PocketNoTouchyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                if (!show) putExtra(EXTRA_HIDE, true)
            }
            context.startActivity(intent)
        }
    }
}
