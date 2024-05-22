package xyz.ivaniskandar.shouko.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.service.TadanoTileParentService
import xyz.ivaniskandar.shouko.service.TadanoTileParentService.Companion.EXTRA_SERVICE_TYPE
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3PreviewTheme
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme

class TileBoardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getSerializableExtra(EXTRA_SERVICE_TYPE) as? TadanoTileParentService.Type
        if (type == null) {
            finish()
        } else {
            setContent {
                ShoukoM3Theme {
                    val onDismissRequest = { finish() }
                    Dialog(
                        onDismissRequest = onDismissRequest,
                        properties = DialogProperties(dismissOnClickOutside = false),
                    ) {
                        TileBoardingScreen(type = type, onDismissRequest = onDismissRequest)
                    }
                }
            }

            // Start service
            startForegroundService(
                Intent(this, TadanoTileParentService::class.java).apply {
                    action = TadanoTileParentService.ACTION_START_SERVICE
                    putExtra(EXTRA_SERVICE_TYPE, type)
                },
            )
        }
    }

    @Composable
    fun TileBoardingScreen(type: TadanoTileParentService.Type, onDismissRequest: () -> Unit) {
        Surface(shape = RoundedCornerShape(28.dp)) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val iconResId = when (type) {
                    TadanoTileParentService.Type.COFFEE -> R.drawable.ic_coffee
                    TadanoTileParentService.Type.TEA -> R.drawable.ic_tea
                }
                Icon(
                    painter = painterResource(id = iconResId),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(56.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )

                val titleResId = when (type) {
                    TadanoTileParentService.Type.COFFEE -> R.string.coffee_boarding_title
                    TadanoTileParentService.Type.TEA -> R.string.tea_boarding_title
                }
                Text(
                    text = stringResource(titleResId),
                    modifier = Modifier.padding(bottom = 20.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall,
                )

                val subtitleResId = when (type) {
                    TadanoTileParentService.Type.COFFEE -> R.string.coffee_boarding_desc
                    TadanoTileParentService.Type.TEA -> R.string.tea_boarding_desc
                }
                Text(
                    text = stringResource(subtitleResId),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )

                FilledTonalButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .padding(top = 32.dp)
                        .fillMaxWidth(),
                ) {
                    Text(text = stringResource(R.string.button_boarding_acknowledgement))
                }
            }
        }
    }

    @Preview
    @Composable
    private fun APreview() {
        ShoukoM3PreviewTheme {
            TileBoardingScreen(type = TadanoTileParentService.Type.COFFEE, onDismissRequest = {})
        }
    }
}
