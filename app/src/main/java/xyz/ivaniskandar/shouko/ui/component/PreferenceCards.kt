package xyz.ivaniskandar.shouko.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme

@Composable
fun BasePreferenceCard(
    title: String,
    description: String,
    buttonLabel: String,
    onButtonClicked: () -> Unit
) {
    Card(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
                .fillMaxWidth()
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Text(text = title, style = MaterialTheme.typography.subtitle1)
            }
            Spacer(modifier = Modifier.height(16.dp))
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(text = description, style = MaterialTheme.typography.body2)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onButtonClicked) {
                    Text(text = buttonLabel, style = MaterialTheme.typography.button)
                }
            }
        }
    }
}

@Composable
fun AccessibilityServiceCard(visible: Boolean = true, onButtonClicked: () -> Unit) {
    if (visible) {
        BasePreferenceCard(
            title = stringResource(id = R.string.accessibility_service_prompt_title),
            description = stringResource(id = R.string.accessibility_service_prompt_desc),
            buttonLabel = stringResource(id = R.string.button_open_settings),
            onButtonClicked = onButtonClicked
        )
    }
}

@Composable
fun ReadLogsCard(visible: Boolean = true, onButtonClicked: () -> Unit) {
    if (visible) {
        BasePreferenceCard(
            title = stringResource(id = R.string.logs_permission_prompt_title),
            description = stringResource(id = R.string.logs_permission_prompt_desc),
            buttonLabel = stringResource(id = R.string.button_grant_permission),
            onButtonClicked = onButtonClicked
        )
    }
}

@Composable
fun WriteSettingsCard(visible: Boolean = true, onButtonClicked: () -> Unit) {
    if (visible) {
        BasePreferenceCard(
            title = stringResource(id = R.string.write_secure_settings_permission_prompt_title),
            description = stringResource(id = R.string.write_secure_settings_permission_prompt_desc),
            buttonLabel = stringResource(id = R.string.button_grant_permission),
            onButtonClicked = onButtonClicked
        )
    }
}

@Preview
@Composable
fun PreferenceCardsPreview() {
    ShoukoTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column {
                AccessibilityServiceCard {}
                ReadLogsCard {}
                WriteSettingsCard {}
            }
        }
    }
}
