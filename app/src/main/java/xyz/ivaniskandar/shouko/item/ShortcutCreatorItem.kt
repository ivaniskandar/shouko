package xyz.ivaniskandar.shouko.item

import android.content.ComponentName
import androidx.compose.ui.graphics.ImageBitmap

data class ShortcutCreatorItem(
    val componentName: ComponentName,
    val label: String,
    val icon: ImageBitmap,
    val applicationLabel: String,
)
