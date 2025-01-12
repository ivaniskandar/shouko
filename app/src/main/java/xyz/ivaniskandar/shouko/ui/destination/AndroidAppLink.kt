package xyz.ivaniskandar.shouko.ui.destination

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.activity.MainActivityViewModel
import xyz.ivaniskandar.shouko.item.LinkHandlerAppItem
import xyz.ivaniskandar.shouko.ui.ComposeLifecycleCallback
import xyz.ivaniskandar.shouko.ui.Screen
import xyz.ivaniskandar.shouko.ui.component.Preference
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3PreviewTheme
import xyz.ivaniskandar.shouko.util.checkDefaultBrowser
import xyz.ivaniskandar.shouko.util.getPackageLabel

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AndroidAppLinkSettings(
    contentPadding: PaddingValues,
    navController: NavController,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    context: Context = LocalContext.current,
) {
    var isDefaultBrowser by remember { mutableStateOf(checkDefaultBrowser(context)) }
    var showEnableInfoDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        item {
            CustomChooserToggle(
                checked = isDefaultBrowser,
                onClick = {
                    if (!isDefaultBrowser) {
                        showEnableInfoDialog = true
                    } else {
                        onOpenSettings()
                    }
                },
            )
        }

        item {
            Preference(
                title = stringResource(R.string.approved_link_target_title),
                subtitle = stringResource(R.string.approved_link_target_subtitle),
                onPreferenceClick = { navController.navigate(Screen.ApprovedLinkTargetList.route) },
            )
        }
        item {
            Preference(
                title = stringResource(R.string.unapproved_link_target_title),
                subtitle = stringResource(R.string.unapproved_link_target_subtitle),
                onPreferenceClick = { navController.navigate(Screen.UnapprovedLinkTargetList.route) },
            )
        }

        item {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.padding(start = 20.dp, top = 16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Text(
                text = stringResource(id = R.string.link_chooser_info),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }

    if (showEnableInfoDialog) {
        AlertDialog(
            onDismissRequest = { showEnableInfoDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEnableInfoDialog = false
                        onOpenSettings()
                    },
                ) {
                    Text(text = stringResource(id = android.R.string.ok))
                }
            },
            title = { Text(text = stringResource(R.string.link_chooser_toggle_label)) },
            text = {
                Text(
                    text = stringResource(
                        R.string.link_chooser_enable_dialog,
                        context.getPackageLabel(context.packageName),
                    ),
                )
            },
        )
    }

    // Refresh default browser status on resume
    ComposeLifecycleCallback(onResume = { isDefaultBrowser = checkDefaultBrowser(context) })
}

@Composable
fun CustomChooserToggle(
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.link_chooser_toggle_label),
                modifier = Modifier.weight(1F),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
            )
            Switch(
                checked = checked,
                onCheckedChange = null,
            )
        }
    }
}

@Preview
@Composable
private fun CustomChooserTogglePreview() {
    ShoukoM3PreviewTheme {
        CustomChooserToggle(checked = true, onClick = {})
    }
}

/**
 * @param approved if true, show approved else unapproved
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun LinkTargetList(
    approved: Boolean,
    navController: NavController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    mainViewModel: MainActivityViewModel = viewModel(),
) {
    val items by mainViewModel.linkHandlerList.collectAsState()
    val isRefreshing by mainViewModel.isRefreshingLinkHandlerList.collectAsState()
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        modifier = modifier.fillMaxSize(),
        isRefreshing = isRefreshing,
        onRefresh = { mainViewModel.refreshLinkHandlerList() },
        state = state,
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.padding(contentPadding).align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = state,
            )
        },
    ) {
        val filteredItems = items.filter { if (approved) it.linkHandlingAllowed && it.isApproved else it.isUnapproved }
        val disabledItems = if (approved) items.filter { !it.linkHandlingAllowed && it.isApproved } else null

        LazyColumn(
            contentPadding = contentPadding,
        ) {
            items(items = filteredItems, key = { it.packageName }) { item ->
                LinkTargetListItem(
                    item = item,
                    onClick = {
                        navController.navigate(Screen.LinkTargetInfoSheet.createRoute(item.packageName))
                    },
                )
            }

            if (!disabledItems.isNullOrEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.disabled),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }

                items(items = disabledItems, key = { it.packageName }) { item ->
                    LinkTargetListItem(
                        item = item,
                        onClick = {
                            navController.navigate(Screen.LinkTargetInfoSheet.createRoute(item.packageName))
                        },
                    )
                }
            }
        }
    }

    ComposeLifecycleCallback(onResume = { mainViewModel.refreshLinkHandlerList() })
}

@Composable
private fun LinkTargetListItem(
    item: LinkHandlerAppItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        modifier = modifier.clickable(onClick = onClick),
        leadingContent = {
            Image(
                bitmap = item.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        },
        headlineContent = {
            Text(
                text = item.label,
                style = MaterialTheme.typography.titleLarge,
            )
        },
        supportingContent = {
            if (item.isApproved) {
                val count = remember { item.verifiedDomains.size + item.userSelectedDomains.size }
                Text(
                    text = pluralStringResource(
                        id = R.plurals.approved_link_list_item_subtitle,
                        count = count,
                        count,
                    ),
                )
            } else {
                val count = remember { item.unapprovedDomains.size }
                Text(
                    text = pluralStringResource(
                        id = R.plurals.unapproved_link_list_item_subtitle,
                        count = count,
                        count,
                    ),
                )
            }
        },
    )
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun LinkTargetInfoSheet(
    packageName: String,
    onOpenSettings: (String) -> Unit,
    modifier: Modifier = Modifier,
    mainViewModel: MainActivityViewModel = viewModel(),
) {
    val list by mainViewModel.linkHandlerList.collectAsState()
    val item = list.find { it.packageName == packageName }!!
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 20.dp, top = 24.dp, end = 20.dp),
    ) {
        Image(
            bitmap = item.icon,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .padding(bottom = 4.dp)
                .align(Alignment.CenterHorizontally),
        )
        Text(
            text = item.label,
            modifier = Modifier.align(Alignment.CenterHorizontally),
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (item.linkHandlingAllowed && (item.verifiedDomains.isNotEmpty() || item.userSelectedDomains.isNotEmpty())) {
            val domains = (item.verifiedDomains + item.userSelectedDomains).toList()
            val domainsCount = domains.count()
            Text(
                text = LocalContext.current.resources.getQuantityString(
                    R.plurals.approved_link_list_title,
                    domainsCount,
                    domainsCount,
                ),
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleSmall,
            )
            LazyColumn(
                modifier = Modifier
                    .weight(1F, fill = false)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                items(domains) { domain ->
                    Text(
                        text = domain,
                        modifier = Modifier.padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.approved_link_disabled_text),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        OutlinedButton(
            onClick = { onOpenSettings(packageName) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 8.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Launch,
                contentDescription = null,
                modifier = Modifier.size(ButtonDefaults.IconSize),
            )
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text(text = stringResource(R.string.open_settings))
        }
    }
}
