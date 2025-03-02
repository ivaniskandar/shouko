package xyz.ivaniskandar.shouko.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme

@Composable
private fun BasePreference(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    widget: @Composable RowScope.() -> Unit = {},
) {
    val textAlpha = if (enabled) 1F else 0.38F
    Row(
        modifier = modifier
            .alpha(textAlpha)
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                modifier = Modifier.alpha(textAlpha),
                style = MaterialTheme.typography.titleLarge,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    modifier = Modifier.alpha(textAlpha).padding(top = 1.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        widget()
    }
}

@Composable
fun Preference(
    title: String,
    onPreferenceClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    BasePreference(
        modifier = modifier.clickable(enabled = enabled, onClick = onPreferenceClick),
        title = title,
        subtitle = subtitle,
        enabled = enabled,
    )
}

@Composable
fun SwitchPreference(
    title: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    checked: Boolean = false,
    enabled: Boolean = true,
) {
    BasePreference(
        modifier = modifier.clickable(enabled = enabled, onClick = { onCheckedChange.invoke(!checked) }),
        title = title,
        subtitle = subtitle,
        enabled = enabled,
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(start = 8.dp),
            enabled = enabled,
        )
    }
}

@Preview
@Composable
private fun PreferenceItemsPreview() {
    var darkTheme by remember { mutableStateOf(false) }
    ShoukoM3Theme(darkTheme = darkTheme) {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column {
                var count by remember { mutableIntStateOf(0) }
                Text(text = "Preference clicked $count time(s)")
                Preference(title = "Preference", subtitle = "With subtitle", onPreferenceClick = { count += 1 })
                Preference(title = "Preference", onPreferenceClick = { count += 1 })

                var switch by remember { mutableStateOf(true) }
                SwitchPreference(
                    title = "Switch preference",
                    subtitle = "With subtitle",
                    checked = switch,
                    onCheckedChange = { switch = !switch },
                )

                SwitchPreference(
                    title = "Dark theme",
                    checked = darkTheme,
                    onCheckedChange = { darkTheme = !darkTheme },
                )
            }
        }
    }
}
