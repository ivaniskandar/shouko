package xyz.ivaniskandar.shouko.item

import android.content.ComponentName
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.ImageBitmap

@Immutable
data class ApplicationItem(
    val componentName: ComponentName,
    val label: String,
    val icon: ImageBitmap,
)
