package xyz.ivaniskandar.shouko.ui

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.feature.FlashlightAction
import xyz.ivaniskandar.shouko.feature.MediaKeyAction
import xyz.ivaniskandar.shouko.item.ApplicationItem
import xyz.ivaniskandar.shouko.item.ShortcutCreatorItem
import java.util.*

private val RowIconModifier = Modifier.size(36.dp)

@Composable
fun CategoryHeader(title: String, divider: Boolean = false) {
    if (divider) {
        Divider()
    }
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(
            text = title.toUpperCase(Locale.getDefault()),
            modifier = Modifier.padding(start = 72.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
            style = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        )
    }
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
        Image(bitmap = item.icon, contentDescription = null, modifier = RowIconModifier)
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
            modifier = RowIconModifier,
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
            modifier = RowIconModifier,
            tint = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            val context = LocalContext.current
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Text(
                    text = remember { FlashlightAction().getLabel(context) },
                    style = MaterialTheme.typography.subtitle1
                )
            }
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
            modifier = RowIconModifier,
            tint = MaterialTheme.colors.primary
        )
        Spacer(modifier = Modifier.width(24.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Text(
                    text = stringResource(R.string.do_nothing),
                    style = MaterialTheme.typography.subtitle1
                )
            }
        }
    }
}
