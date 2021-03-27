package xyz.ivaniskandar.shouko.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme

class TileBoardingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val type = intent.getSerializableExtra(EXTRA_SERVICE_TYPE) as? TadanoTileParentService.Type
        if (type == null) {
            finish()
        } else {
            setContent {
                ShoukoTheme {
                    TileBoardingScreen(type = type) { finish() }
                }
            }
        }
    }

    @Composable
    fun TileBoardingScreen(type: TadanoTileParentService.Type, onDismissRequest: () -> Unit) {
        Dialog(onDismissRequest = onDismissRequest, properties = DialogProperties(dismissOnClickOutside = false)) {
            Surface(shape = MaterialTheme.shapes.medium, elevation = 24.dp) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val iconResId = when (type) {
                        TadanoTileParentService.Type.COFFEE -> R.drawable.ic_coffee
                        TadanoTileParentService.Type.TEA -> R.drawable.ic_tea
                    }
                    Icon(
                        painter = painterResource(id = iconResId),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 8.dp, bottom = 16.dp)
                            .size(48.dp),
                        tint = MaterialTheme.colors.primary
                    )

                    val titleResId = when (type) {
                        TadanoTileParentService.Type.COFFEE -> R.string.coffee_boarding_title
                        TadanoTileParentService.Type.TEA -> R.string.tea_boarding_title
                    }
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                        Text(
                            text = stringResource(titleResId),
                            modifier = Modifier.padding(bottom = 20.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.h6
                        )
                    }

                    val subtitleResId = when (type) {
                        TadanoTileParentService.Type.COFFEE -> R.string.coffee_boarding_desc
                        TadanoTileParentService.Type.TEA -> R.string.tea_boarding_desc
                    }
                    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                        Text(
                            text = stringResource(subtitleResId),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.body2
                        )
                    }

                    OutlinedButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(top = 28.dp)
                    ) {
                        Text(text = stringResource(R.string.button_boarding_acknowledgement))
                    }
                }
            }
        }
    }

    @Preview
    @Composable
    fun APreview() {
        ShoukoTheme {
            TileBoardingScreen(type = TadanoTileParentService.Type.COFFEE, onDismissRequest = {})
        }
    }
}
