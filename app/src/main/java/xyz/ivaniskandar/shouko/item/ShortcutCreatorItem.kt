package xyz.ivaniskandar.shouko.item

import android.content.ComponentName
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class ShortcutCreatorItem(
    val componentName: ComponentName,
    val label: String,
    val icon: ImageBitmap,
    val applicationLabel: String,
)
