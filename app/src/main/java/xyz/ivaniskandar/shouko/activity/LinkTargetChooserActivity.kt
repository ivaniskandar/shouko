package xyz.ivaniskandar.shouko.activity

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.feature.LinkCleaner
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
 */
class LinkTargetChooserActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cleanedUri = LinkCleaner.resolveLink(this, intent.data.toString())?.toUri()
        val newIntent = Intent(intent).apply {
            component = null
            data = cleanedUri
            setPackage(null)
        }
        val resolverIntent = packageManager.queryIntentActivitiesCompat(newIntent, PackageManager.MATCH_ALL)
            .map { ComponentName(it.activityInfo.packageName, it.activityInfo.name) }
            .filter { it.packageName != packageName }

        if (resolverIntent.size == 1) {
            start(resolverIntent[0], newIntent)
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
                        .asImageBitmap(),
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })

        setContent {
            ShoukoM3Theme {
                AppLinkChooserSheet(
                    targets = mapped,
                    onItemClick = { start(it, newIntent) },
                    onItemLongClick = { startAppInfo(it) },
                    onFinish = { finish() },
                )
            }
        }
    }

    private fun start(componentName: ComponentName, intent: Intent) {
        startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                data = intent.data
                component = componentName
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    private fun startAppInfo(componentName: ComponentName) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${componentName.packageName}".toUri()
            },
        )
    }
}

private data class Target(
    val component: ComponentName,
    val title: String,
    val icon: ImageBitmap,
)

@Composable
private fun AppLinkChooserSheet(
    targets: List<Target>,
    onItemClick: (ComponentName) -> Unit,
    onItemLongClick: (ComponentName) -> Unit,
    onFinish: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    ModalBottomSheet(onDismissRequest = onFinish) {
        Text(
            text = stringResource(R.string.link_chooser_dialog_title),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
        )
        SoftDivider()
        LazyColumn(
            modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom)),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(targets) { item ->
                ListItem(
                    modifier = Modifier
                        .combinedClickable(
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onItemLongClick(item.component)
                            },
                            onClick = {
                                onItemClick(item.component)
                            },
                        ),
                    leadingContent = {
                        Image(
                            bitmap = item.icon,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                        )
                    },
                    headlineContent = {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                )
            }
        }
    }
}
