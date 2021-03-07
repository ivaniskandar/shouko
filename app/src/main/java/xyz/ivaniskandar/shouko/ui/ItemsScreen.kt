package xyz.ivaniskandar.shouko.ui

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.feature.MediaKeyAction
import xyz.ivaniskandar.shouko.item.ApplicationItem
import xyz.ivaniskandar.shouko.item.ShortcutCreatorItem

@Composable
fun ApplicationRow(item: ApplicationItem, onClick: (ComponentName) -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = { onClick.invoke(item.componentName) })
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(bitmap = item.icon, contentDescription = null, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Text(text = item.label, style = MaterialTheme.typography.subtitle1)
            }
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
        Image(bitmap = item.icon, contentDescription = null, modifier = Modifier.size(36.dp))
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Text(text = item.label, style = MaterialTheme.typography.subtitle1)
            }
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                Text(text = item.applicationLabel, style = MaterialTheme.typography.body2)
            }
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
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Text(text = stringResource(id = key.labelResId), style = MaterialTheme.typography.subtitle1)
            }
        }
    }
}

@Composable
fun NothingRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable(onClick = { onClick.invoke() })
            .padding(horizontal = 12.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_close),
            contentDescription = null,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Text(text = stringResource(id = R.string.nothing_action_label), style = MaterialTheme.typography.subtitle1)
            }
        }
    }
}
