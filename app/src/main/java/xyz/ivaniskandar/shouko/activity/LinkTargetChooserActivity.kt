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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
import androidx.compose.material.MaterialTheme as M2Theme

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
        setContent {
            ShoukoM3Theme {
                AppLinkChooserSheet(
                    resolverIntent = resolverIntent,
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

@Composable
fun AppLinkChooserSheet(
    resolverIntent: List<ComponentName>,
    onItemClick: (ComponentName) -> Unit,
    onItemLongClick: (ComponentName) -> Unit,
    onSheetHidden: () -> Unit
) {
    val shadowWrapper = remember { IconDrawableShadowWrapper() }
    val state = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Box {
        Scrim(
            color = M2Theme.colors.onSurface.copy(alpha = 0.32f),
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
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetElevation = 0.dp,
            sheetBackgroundColor = MaterialTheme.colorScheme.surface,
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            sheetContent = {
                Text(
                    text = stringResource(R.string.link_chooser_dialog_title),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
                SoftDivider()
                LazyColumn(
                    contentPadding = WindowInsets.navigationBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(resolverIntent) { item ->
                        ListItem(
                            modifier = Modifier
                                .combinedClickable(
                                    onLongClick = {
                                        scope.launch { state.hide() }
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onItemLongClick(item)
                                    },
                                    onClick = {
                                        scope.launch { state.hide() }
                                        onItemClick(item)
                                    }
                                ),
                            leadingContent = {
                                Image(
                                    bitmap = remember {
                                        shadowWrapper.run(item.loadIcon(context)!!)
                                            .toBitmap()
                                            .asImageBitmap()
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp)
                                )
                            },
                            headlineText = {
                                Text(
                                    text = remember { item.loadLabel(context)!! },
                                    style = MaterialTheme.typography.titleLarge
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent // Doesn't used -- M3 1.0.0-alpha14 issue
                            )
                        )
                    }
                }
            }
        ) {
            // Empty
        }
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
