package xyz.ivaniskandar.shouko.ui.component

import android.content.ComponentName
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import xyz.ivaniskandar.shouko.item.ApplicationItem
import xyz.ivaniskandar.shouko.item.ShortcutCreatorItem
import java.util.Locale

private val RowIconModifier = Modifier.size(36.dp)

@Composable
fun CategoryHeader(
    title: String,
    modifier: Modifier = Modifier,
    divider: Boolean = false,
) {
    Column(modifier = modifier) {
        if (divider) SoftDivider()
        Text(
            text = title.uppercase(Locale.getDefault()),
            modifier = Modifier.padding(start = 68.dp, top = 20.dp, end = 16.dp, bottom = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
fun ApplicationRow(
    item: ApplicationItem,
    onClick: (ComponentName) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clickable(onClick = { onClick.invoke(item.componentName) })
            .fillMaxWidth(),
        headlineContent = { Text(text = item.label) },
        leadingContent = {
            Image(
                bitmap = item.icon,
                contentDescription = null,
                modifier = RowIconModifier,
            )
        },
    )
}

@Composable
fun ShortcutCreatorRow(
    item: ShortcutCreatorItem,
    onClick: (ComponentName) -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clickable(onClick = { onClick.invoke(item.componentName) })
            .fillMaxWidth(),
        headlineContent = { Text(text = item.label) },
        supportingContent = { Text(text = item.applicationLabel) },
        leadingContent = {
            Image(
                bitmap = item.icon,
                contentDescription = null,
                modifier = RowIconModifier,
            )
        },
    )
}

@Composable
fun CommonActionRow(
    iconPainter: Painter,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clickable(onClick = onClick)
            .fillMaxWidth(),
        headlineContent = { Text(text = label) },
        leadingContent = {
            Icon(
                painter = iconPainter,
                contentDescription = null,
                modifier = RowIconModifier,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}

@Composable
fun CommonActionRow(
    iconVector: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
            .fillMaxWidth(),
        headlineContent = { Text(text = label) },
        leadingContent = {
            Icon(
                imageVector = iconVector,
                contentDescription = null,
                modifier = RowIconModifier,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
    )
}
