package xyz.ivaniskandar.shouko.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme

@Composable
private fun BasePreferenceCard(
    title: String,
    description: String,
    buttonLabel: String,
    onButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = description,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                TextButton(onClick = onButtonClick) {
                    Text(text = buttonLabel)
                }
            }
        }
    }
}

@Composable
fun AccessibilityServiceCard(
    onButtonClick: () -> Unit,
    visible: Boolean = true,
) {
    if (visible) {
        BasePreferenceCard(
            title = stringResource(id = R.string.accessibility_service_prompt_title),
            description = stringResource(id = R.string.accessibility_service_prompt_desc),
            buttonLabel = stringResource(id = R.string.button_open_settings),
            onButtonClick = onButtonClick,
        )
    }
}

@Composable
fun ReadLogsCard(
    onButtonClick: () -> Unit,
    visible: Boolean = true,
) {
    if (visible) {
        BasePreferenceCard(
            title = stringResource(id = R.string.logs_permission_prompt_title),
            description = stringResource(id = R.string.logs_permission_prompt_desc),
            buttonLabel = stringResource(id = R.string.button_grant_permission),
            onButtonClick = onButtonClick,
        )
    }
}

@Composable
fun WriteSettingsCard(
    onButtonClick: () -> Unit,
    visible: Boolean = true,
) {
    if (visible) {
        BasePreferenceCard(
            title = stringResource(id = R.string.write_secure_settings_permission_prompt_title),
            description = stringResource(id = R.string.write_secure_settings_permission_prompt_desc),
            buttonLabel = stringResource(id = R.string.button_grant_permission),
            onButtonClick = onButtonClick,
        )
    }
}

@Preview
@Composable
private fun PreferenceCardsPreview() {
    ShoukoM3Theme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                AccessibilityServiceCard(onButtonClick = {})
                ReadLogsCard(onButtonClick = {})
                WriteSettingsCard(onButtonClick = {})
            }
        }
    }
}
