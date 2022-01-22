package xyz.ivaniskandar.shouko.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.feature.PocketNoTouchy
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme

/**
 * A full-screen dialog activity to block accidental touches.
 *
 * @see PocketNoTouchy
 */
class PocketNoTouchyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (intent.getBooleanExtra(EXTRA_HIDE, false)) finish()
        super.onCreate(savedInstanceState)

        // Set full screen
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val wicc = WindowInsetsControllerCompat(window, window.decorView)
        wicc.hide(WindowInsetsCompat.Type.systemBars())
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContent {
            ShoukoTheme(darkTheme = true) {
                Surface(color = Color.Black) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        DialogCard {
                            lifecycleScope.launch { PocketNoTouchy.ignoreCheckFlow.emit(Unit) }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_HIDE, false)) finish()
    }

    @Composable
    fun DialogCard(onButtonClicked: () -> Unit = {}) {
        Card(
            modifier = Modifier.padding(24.dp),
            backgroundColor = Color.Black,
            contentColor = Color.White,
            border = BorderStroke(width = 1.dp, color = Color(0x1FFFFFFF)),
            elevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)) {
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                    Text(
                        text = stringResource(R.string.no_touchy_dialog_title),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        text = stringResource(R.string.no_touchy_dialog_text),
                        style = MaterialTheme.typography.body2
                    )
                }
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
    fun DialogCardPreview() {
        ShoukoTheme(darkTheme = true) {
            DialogCard()
        }
    }

    companion object {
        private const val EXTRA_HIDE = "PocketNoTouchyActivity.extra.HIDE"

        fun updateState(context: Context, show: Boolean) {
            val intent = Intent(context, PocketNoTouchyActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                if (!show) putExtra(EXTRA_HIDE, true)
            }
            context.startActivity(intent)
        }
    }
}
