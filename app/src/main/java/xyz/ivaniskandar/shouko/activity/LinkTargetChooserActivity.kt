package xyz.ivaniskandar.shouko.activity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ListItem
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import kotlinx.coroutines.launch
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.IconDrawableShadowWrapper
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme
import xyz.ivaniskandar.shouko.util.loadIcon
import xyz.ivaniskandar.shouko.util.loadLabel

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
class LinkTargetChooserActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val newIntent = Intent(intent).apply {
            component = null
            setPackage(null)
        }
        val resolverIntent = packageManager.queryIntentActivities(newIntent, PackageManager.MATCH_ALL)
            .map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
            .filter { it.packageName != packageName }

        if (resolverIntent.size == 1) {
            start(resolverIntent[0])
            finish()
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            ShoukoM3Theme {
                ProvideWindowInsets {
                    AppLinkChooserSheet(
                        resolverIntent = resolverIntent,
                        onItemClick = { start(it) },
                        onItemLongClick = { startAppInfo(it) },
                        onSheetHidden = { finish() }
                    )
                }
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

    ModalBottomSheetLayout(
        sheetState = state,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContent = {
            Text(
                text = stringResource(R.string.link_chooser_dialog_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                style = MaterialTheme.typography.displaySmall,
                textAlign = TextAlign.Center
            )
            Divider()
            LazyColumn(
                contentPadding = rememberInsetsPaddingValues(
                    insets = LocalWindowInsets.current.navigationBars,
                    applyBottom = true
                )
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
                        icon = {
                            Image(
                                bitmap = shadowWrapper.run(item.loadIcon(context)!!)
                                    .toBitmap()
                                    .asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        },
                        text = { Text(item.loadLabel(context)!!) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        },
    ) {
        // Empty
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
