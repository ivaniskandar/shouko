package xyz.ivaniskandar.shouko.activity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import kotlinx.coroutines.launch
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.IconDrawableShadowWrapper
import xyz.ivaniskandar.shouko.ui.component.SoftDivider
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme
import xyz.ivaniskandar.shouko.util.loadIcon
import xyz.ivaniskandar.shouko.util.loadLabel
import xyz.ivaniskandar.shouko.util.queryIntentActivitiesCompat

/**
 * "Browser" activity as a workaround for App Link changes in S.
 *
 * When a link is clicked, this activity will show a chooser of supported
 * link targets.
 *
 * To setup, set as default browser and turn off the "Open supported links" settings
 * for every verified link target as needed.
 *
 */
class LinkTargetChooserActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val newIntent = Intent(intent).apply {
            component = null
            setPackage(null)
        }
        val resolverIntent = packageManager.queryIntentActivitiesCompat(newIntent, PackageManager.MATCH_ALL)
            .map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
            .filter { it.packageName != packageName }

        if (resolverIntent.size == 1) {
            start(resolverIntent[0])
            finish()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val shadowWrapper = IconDrawableShadowWrapper()
        val mapped = resolverIntent
            .map {
                Target(
                    component = it,
                    title = it.loadLabel(this).toString(),
                    icon = shadowWrapper.run(it.loadIcon(this))
                        .toBitmap()
                        .asImageBitmap()
                )
            }
            .sortedBy { it.title.lowercase() }

        setContent {
            ShoukoM3Theme {
                AppLinkChooserSheet(
                    targets = mapped,
                    onItemClick = { start(it) },
                    onItemLongClick = { startAppInfo(it) },
                    onSheetHidden = { finish() }
                )
            }
        }
    }

    private fun start(componentName: ComponentName) {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = intent.data
                component = componentName
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }

    private fun startAppInfo(componentName: ComponentName) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${componentName.packageName}")
            }
        )
    }
}

private data class Target(
    val component: ComponentName,
    val title: String,
    val icon: ImageBitmap
)

@Composable
private fun AppLinkChooserSheet(
    targets: List<Target>,
    onItemClick: (ComponentName) -> Unit,
    onItemLongClick: (ComponentName) -> Unit,
    onSheetHidden: () -> Unit
) {
    val state = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    Box {
        Scrim(
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f),
            onDismiss = {
                scope.launch { state.hide() }
            },
            visible = state.targetValue != ModalBottomSheetValue.Hidden
        )
        ModalBottomSheetLayout(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .align(Alignment.BottomCenter),
            scrimColor = Color.Transparent,
            sheetState = state,
            sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            sheetElevation = 0.dp,
            sheetBackgroundColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            sheetContent = {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    Text(
                        text = stringResource(R.string.link_chooser_dialog_title),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleLarge
                    )
                    SoftDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        items(targets) { item ->
                            Column(
                                modifier = Modifier
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = rememberRipple(bounded = false, radius = 56.dp),
                                        onLongClick = {
                                            scope.launch { state.hide() }
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onItemLongClick(item.component)
                                        },
                                        onClick = {
                                            scope.launch { state.hide() }
                                            onItemClick(item.component)
                                        }
                                    )
                                    .padding(horizontal = 8.dp)
                                    .size(width = 96.dp, height = 128.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Image(
                                    bitmap = item.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    modifier = Modifier.padding(top = 12.dp),
                                    text = item.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            },
            content = { /* Empty */ }
        )
    }
    var opened by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = state.currentValue) {
        if (!opened) scope.launch { state.show() }
        when (state.currentValue) {
            ModalBottomSheetValue.Hidden -> if (opened) onSheetHidden()
            ModalBottomSheetValue.Expanded, ModalBottomSheetValue.HalfExpanded -> opened = true
        }
    }
    BackHandler {
        scope.launch { state.hide() }
    }
}

@Composable
private fun Scrim(
    color: Color,
    onDismiss: () -> Unit,
    visible: Boolean
) {
    if (color.isSpecified) {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = TweenSpec()
        )
        val dismissModifier = if (visible) {
            Modifier
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
                .semantics(mergeDescendants = true) {
                    onClick { onDismiss(); true }
                }
        } else {
            Modifier
        }

        Canvas(
            Modifier
                .fillMaxSize()
                .then(dismissModifier)
        ) {
            drawRect(color = color, alpha = alpha)
        }
    }
}
