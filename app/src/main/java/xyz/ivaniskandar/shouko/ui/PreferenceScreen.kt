package xyz.ivaniskandar.shouko.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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
fun BasePreference(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    CompositionLocalProvider(
        LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled
    ) {
        Text(text = title, style = MaterialTheme.typography.subtitle1)
    }
    if (subtitle != null) {
        CompositionLocalProvider(
            LocalContentAlpha provides if (enabled) ContentAlpha.medium else ContentAlpha.disabled
        ) {
            Text(text = subtitle, modifier = Modifier.padding(top = 2.dp), style = MaterialTheme.typography.body2)
        }
    }
}

@Composable
fun Preference(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onPreferenceClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onPreferenceClick)
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .fillMaxWidth()
    ) {
        BasePreference(title = title, subtitle = subtitle, enabled = enabled)
    }
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(enabled = enabled) { onCheckedChanged.invoke(!checked) }
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            BasePreference(title = title, subtitle = subtitle, enabled = enabled)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChanged, enabled = enabled)
    }
}

@Preview
@Composable
fun PreferenceItemPreview() {
    ShoukoTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column {
                ReadLogsCard(onButtonClicked = {})
                WriteSettingsCard(onButtonClicked = {})
                Preference(title = "Preference", subtitle = "With subtitle") {}
                Preference(title = "Preference") {}
                SwitchPreference(title = "Switch preference", subtitle = "With subtitle") {}
                SwitchPreference(title = "Checked switch preference", checked = true) {}
            }
        }
    }
}

@Composable
fun AccessibilityServiceCard(visible: Boolean = true, onButtonClicked: () -> Unit) {
    if (visible) {
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
                    Text(
                        text = stringResource(R.string.accessibility_service_prompt_title),
                        style = MaterialTheme.typography.subtitle1
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        text = stringResource(R.string.accessibility_service_prompt_desc),
                        style = MaterialTheme.typography.body2
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = onButtonClicked) {
                        Text(
                            text = stringResource(R.string.button_open_settings),
                            style = MaterialTheme.typography.button
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BaseSettingsCard(
    title: String,
    description: String,
    buttonLabel: String,
    visible: Boolean = true,
    onButtonClicked: () -> Unit
) {
    if (visible) {
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
}

@Composable
fun ReadLogsCard(visible: Boolean = true, onButtonClicked: () -> Unit) {
    BaseSettingsCard(
        title = stringResource(id = R.string.logs_permission_prompt_title),
        description = stringResource(id = R.string.logs_permission_prompt_desc),
        buttonLabel = stringResource(id = R.string.button_grant_permission),
        visible = visible,
        onButtonClicked = onButtonClicked
    )
}

@Composable
fun WriteSettingsCard(visible: Boolean = true, onButtonClicked: () -> Unit) {
    BaseSettingsCard(
        title = stringResource(id = R.string.write_secure_settings_permission_prompt_title),
        description = stringResource(id = R.string.write_secure_settings_permission_prompt_desc),
        buttonLabel = stringResource(id = R.string.button_grant_permission),
        visible = visible,
        onButtonClicked = onButtonClicked
    )
}
