package xyz.ivaniskandar.shouko.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Switch
import androidx.compose.material.SwitchColors
import androidx.compose.material.SwitchDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.tooling.preview.Preview
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3PreviewTheme

@Composable
fun m3SwitchColors(
    checkedThumbColor: Color = MaterialTheme.colorScheme.primary,
    checkedTrackColor: Color = checkedThumbColor,
    checkedTrackAlpha: Float = 0.54f,
    uncheckedThumbColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    uncheckedTrackColor: Color = MaterialTheme.colorScheme.onSurface,
    uncheckedTrackAlpha: Float = 0.38f,
    disabledCheckedThumbColor: Color = checkedThumbColor
        .copy(alpha = ContentAlpha.disabled)
        .compositeOver(MaterialTheme.colorScheme.surface),
    disabledCheckedTrackColor: Color = checkedTrackColor
        .copy(alpha = ContentAlpha.disabled)
        .compositeOver(MaterialTheme.colorScheme.surface),
    disabledUncheckedThumbColor: Color = uncheckedThumbColor
        .copy(alpha = ContentAlpha.disabled)
        .compositeOver(MaterialTheme.colorScheme.surface),
    disabledUncheckedTrackColor: Color = uncheckedTrackColor
        .copy(alpha = ContentAlpha.disabled)
        .compositeOver(MaterialTheme.colorScheme.surface)
): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = checkedThumbColor,
    checkedTrackColor = checkedTrackColor.copy(alpha = checkedTrackAlpha),
    uncheckedThumbColor = uncheckedThumbColor,
    uncheckedTrackColor = uncheckedTrackColor.copy(alpha = uncheckedTrackAlpha),
    disabledCheckedThumbColor = disabledCheckedThumbColor,
    disabledCheckedTrackColor = disabledCheckedTrackColor.copy(alpha = checkedTrackAlpha),
    disabledUncheckedThumbColor = disabledUncheckedThumbColor,
    disabledUncheckedTrackColor = disabledUncheckedTrackColor.copy(alpha = uncheckedTrackAlpha)
)

/**
 * [Switch] with M3 color
 */
@Composable
fun M3Switch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    colors: SwitchColors = m3SwitchColors()
) {
    Switch(checked, onCheckedChange, modifier, enabled, interactionSource, colors)
}

@Preview
@Composable
fun M3SwitchPreview() {
    ShoukoM3PreviewTheme {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
            Row {
                M3Switch(checked = false, onCheckedChange = {})
                M3Switch(checked = false, enabled = false, onCheckedChange = {})
            }
            Row {
                M3Switch(checked = true, onCheckedChange = {})
                M3Switch(checked = true, enabled = false, onCheckedChange = {})
            }
        }
    }
}
