package xyz.ivaniskandar.shouko.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Wrapper to Material3's Scaffold for easy content padding append
 */
@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable (PaddingValues) -> Unit,
) {
    androidx.compose.material3.Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
    ) { padding ->
        val newContentPadding = remember {
            object : PaddingValues {
                override fun calculateBottomPadding(): Dp = contentPadding.calculateBottomPadding() + padding.calculateBottomPadding()

                override fun calculateLeftPadding(layoutDirection: LayoutDirection): Dp = contentPadding.calculateLeftPadding(layoutDirection) + padding.calculateLeftPadding(layoutDirection)

                override fun calculateRightPadding(layoutDirection: LayoutDirection): Dp = contentPadding.calculateRightPadding(layoutDirection) + padding.calculateRightPadding(layoutDirection)

                override fun calculateTopPadding(): Dp = contentPadding.calculateTopPadding() + padding.calculateTopPadding()
            }
        }
        content(newContentPadding)
    }
}
