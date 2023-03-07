package xyz.ivaniskandar.shouko.ui.component

import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SoftDivider(
    modifier: Modifier = Modifier,
) {
    Divider(
        modifier = modifier,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12F),
    )
}
