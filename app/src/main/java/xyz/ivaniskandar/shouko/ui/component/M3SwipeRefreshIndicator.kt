package xyz.ivaniskandar.shouko.ui.component

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.SwipeRefreshState

/**
 * [SwipeRefreshIndicator] with M3 color
 */
@Composable
fun M3SwipeRefreshIndicator(
    state: SwipeRefreshState,
    refreshTriggerDistance: Dp,
    modifier: Modifier = Modifier,
    fade: Boolean = true,
    scale: Boolean = false,
    arrowEnabled: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    contentColor: Color = MaterialTheme.colorScheme.primary,
    refreshingOffset: Dp = 16.dp,
    largeIndication: Boolean = false,
) {
    SwipeRefreshIndicator(
        state,
        refreshTriggerDistance,
        modifier,
        fade,
        scale,
        arrowEnabled,
        backgroundColor,
        contentColor,
        RoundedCornerShape(50),
        refreshingOffset,
        largeIndication,
        0.dp
    )
}
