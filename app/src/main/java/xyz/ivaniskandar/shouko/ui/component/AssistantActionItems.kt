package xyz.ivaniskandar.shouko.ui.component

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assistant
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.feature.DigitalAssistantAction
import xyz.ivaniskandar.shouko.feature.FlashlightAction
import xyz.ivaniskandar.shouko.feature.MediaKeyAction
import xyz.ivaniskandar.shouko.feature.MuteMicrophoneAction
import xyz.ivaniskandar.shouko.feature.RingerModeAction
import xyz.ivaniskandar.shouko.feature.ScreenshotAction
import xyz.ivaniskandar.shouko.feature.StatusBarAction
import xyz.ivaniskandar.shouko.item.ApplicationItem
import xyz.ivaniskandar.shouko.item.ShortcutCreatorItem
import java.util.Locale

private val RowIconModifier = Modifier.size(36.dp)

@Composable
fun CategoryHeader(title: String, divider: Boolean = false) {
    if (divider) Divider()
    Text(
        text = title.uppercase(Locale.getDefault()),
        modifier = Modifier.padding(start = 72.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium
    )
}

@Composable
fun ApplicationRow(item: ApplicationItem, onClick: (ComponentName) -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = { onClick.invoke(item.componentName) })
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(bitmap = item.icon, contentDescription = null, modifier = RowIconModifier)
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = item.label, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ShortcutCreatorRow(item: ShortcutCreatorItem, onClick: (ComponentName) -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = { onClick.invoke(item.componentName) })
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(bitmap = item.icon, contentDescription = null, modifier = RowIconModifier)
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = item.label, style = MaterialTheme.typography.titleMedium)
            Text(
                text = item.applicationLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun MediaKeyRow(key: MediaKeyAction.Key, onClick: (MediaKeyAction) -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = { onClick.invoke(MediaKeyAction(key)) })
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = key.iconResId),
            contentDescription = null,
            modifier = Modifier.size(36.dp).padding(2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(id = key.labelResId), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun FlashlightRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.FlashlightOn,
            contentDescription = null,
            modifier = Modifier.size(36.dp).padding(2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val context = LocalContext.current
            Text(
                text = remember { FlashlightAction().getLabel(context) },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun ScreenshotRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Screenshot,
            contentDescription = null,
            modifier = Modifier.size(36.dp).padding(2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val context = LocalContext.current
            Text(
                text = remember { ScreenshotAction().getLabel(context) },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun StatusBarRow(type: StatusBarAction.PanelType, onClick: (StatusBarAction) -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = { onClick.invoke(StatusBarAction(type)) })
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = type.iconVector,
            contentDescription = null,
            modifier = Modifier.size(36.dp).padding(2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(text = stringResource(id = type.labelResId), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun RingerModeRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Notifications,
            contentDescription = null,
            modifier = Modifier.size(36.dp).padding(2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val context = LocalContext.current
            Text(
                text = remember { RingerModeAction().getLabel(context) },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun MuteMicrophoneRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.MicOff,
            contentDescription = null,
            modifier = Modifier.size(36.dp).padding(2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val context = LocalContext.current
            Text(
                text = remember { MuteMicrophoneAction().getLabel(context) },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun DigitalAssistantRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Assistant,
            contentDescription = null,
            modifier = Modifier.size(36.dp).padding(2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val context = LocalContext.current
            Text(
                text = remember { DigitalAssistantAction().getLabel(context) },
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun DoNothingRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Clear,
            contentDescription = null,
            modifier = Modifier.size(36.dp).padding(2.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = stringResource(R.string.do_nothing),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
