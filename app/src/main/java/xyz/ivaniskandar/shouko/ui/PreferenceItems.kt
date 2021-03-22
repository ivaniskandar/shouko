package xyz.ivaniskandar.shouko.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme

@Composable
private fun BasePreference(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    widget: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .padding(horizontal = 14.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            CompositionLocalProvider(
                LocalContentAlpha provides if (enabled) ContentAlpha.high else ContentAlpha.disabled
            ) {
                Text(text = title, style = MaterialTheme.typography.subtitle1)
            }
            if (subtitle != null) {
                CompositionLocalProvider(
                    LocalContentAlpha provides if (enabled) ContentAlpha.medium else ContentAlpha.disabled
                ) {
                    Text(
                        text = subtitle,
                        modifier = Modifier.padding(top = 2.dp),
                        style = MaterialTheme.typography.body2
                    )
                }
            }
        }
        widget()
    }
}

@Composable
fun Preference(
    title: String,
    subtitle: String? = null,
    enabled: Boolean = true,
    onPreferenceClick: () -> Unit
) {
    BasePreference(
        modifier = Modifier.clickable(enabled = enabled, onClick = onPreferenceClick),
        title = title,
        subtitle = subtitle,
        enabled = enabled
    )
}

@Composable
fun SwitchPreference(
    title: String,
    subtitle: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
    onCheckedChanged: (Boolean) -> Unit
) {
    BasePreference(
        modifier = Modifier.clickable(enabled = enabled, onClick = { onCheckedChanged.invoke(!checked) }),
        title = title,
        subtitle = subtitle,
        enabled = enabled
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChanged,
            modifier = Modifier.padding(start = 6.dp),
            enabled = enabled
        )
    }
}

@Preview
@Composable
fun PreferenceItemsPreview() {
    var darkTheme by remember { mutableStateOf(false) }
    ShoukoTheme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colors.background) {
            Column {
                var count by remember { mutableStateOf(0) }
                Text(text = "Preference clicked $count time(s)")
                Preference(title = "Preference", subtitle = "With subtitle") { count += 1 }
                Preference(title = "Preference") { count += 1 }

                var switch by remember { mutableStateOf(true) }
                SwitchPreference(
                    title = "Switch preference",
                    subtitle = "With subtitle",
                    checked = switch
                ) {
                    switch = !switch
                }

                SwitchPreference(
                    title = "Dark theme",
                    checked = darkTheme
                ) {
                    darkTheme = !darkTheme
                }
            }
        }
    }
}
