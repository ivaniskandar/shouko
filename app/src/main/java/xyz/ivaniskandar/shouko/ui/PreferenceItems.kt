package xyz.ivaniskandar.shouko.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
fun PreferenceItemsPreview() {
    ShoukoTheme {
        Surface(color = MaterialTheme.colors.background) {
            Column {
                Preference(title = "Preference", subtitle = "With subtitle") {}
                Preference(title = "Preference") {}
                SwitchPreference(title = "Switch preference", subtitle = "With subtitle") {}
                SwitchPreference(title = "Checked switch preference", checked = true) {}
            }
        }
    }
}
