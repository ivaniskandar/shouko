package xyz.ivaniskandar.shouko.ui.destination

import android.content.ComponentName
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ShoukoApplication
import xyz.ivaniskandar.shouko.activity.EmptyShortcutActivity
import xyz.ivaniskandar.shouko.activity.MainActivityViewModel
import xyz.ivaniskandar.shouko.feature.DoNothingAction
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.LOCKSCREEN_LEFT_BUTTON
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.LOCKSCREEN_RIGHT_BUTTON
import xyz.ivaniskandar.shouko.ui.Screen
import xyz.ivaniskandar.shouko.ui.component.ApplicationRow
import xyz.ivaniskandar.shouko.ui.component.CommonActionRow
import xyz.ivaniskandar.shouko.ui.component.Preference
import xyz.ivaniskandar.shouko.ui.component.TabPager
import xyz.ivaniskandar.shouko.ui.component.WriteSettingsCard
import xyz.ivaniskandar.shouko.util.canWriteSecureSettings
import xyz.ivaniskandar.shouko.util.loadLabel
import xyz.ivaniskandar.shouko.util.toComponentName

@Composable
fun LockscreenShortcutSettings(
    navController: NavController,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        if (!context.canWriteSecureSettings) {
            item {
                WriteSettingsCard(
                    onButtonClick = {
                        navController.navigate(Screen.SecureSettingsSetup.route)
                    },
                )
            }
        }
        item {
            Preference(
                title = stringResource(R.string.lockscreen_shortcut_left),
                subtitle = ShoukoApplication.prefs.lockscreenLeftAction.collectAsState(initial = null).value
                    ?.toComponentName()?.loadLabel(context)
                    ?: stringResource(id = R.string.assistant_action_select_default_value),
                enabled = context.canWriteSecureSettings,
                onPreferenceClick = {
                    navController.navigate(Screen.LockscreenShortcutSelection.createRoute(LOCKSCREEN_LEFT_BUTTON))
                },
            )
        }
        item {
            Preference(
                title = stringResource(R.string.lockscreen_shortcut_right),
                subtitle = ShoukoApplication.prefs.lockscreenRightAction.collectAsState(initial = null).value
                    ?.toComponentName()?.loadLabel(context)
                    ?: stringResource(id = R.string.assistant_action_select_default_value),
                enabled = context.canWriteSecureSettings,
                onPreferenceClick = {
                    navController.navigate(Screen.LockscreenShortcutSelection.createRoute(LOCKSCREEN_RIGHT_BUTTON))
                },
            )
        }
    }
}

@Composable
fun LockscreenShortcutSelection(
    navController: NavController,
    settingsKey: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    mainViewModel: MainActivityViewModel = viewModel(),
) {
    val scope = rememberCoroutineScope()
    val titles = listOf(
        stringResource(R.string.tab_title_apps),
        stringResource(R.string.tab_title_other),
    )
    TabPager(
        modifier = modifier,
        pageTitles = titles,
        contentPadding = contentPadding,
    ) { page ->
        val context = LocalContext.current
        when (page) {
            0 -> {
                val items by mainViewModel.appsList.collectAsState()
                val isRefreshing by mainViewModel.isRefreshingAppsList.collectAsState()
                val state = rememberPullToRefreshState()
                PullToRefreshBox(
                    modifier = Modifier.fillMaxSize(),
                    isRefreshing = isRefreshing,
                    onRefresh = { mainViewModel.refreshAppsList() },
                    state = state,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            modifier = Modifier.padding(contentPadding).align(Alignment.TopCenter),
                            isRefreshing = isRefreshing,
                            state = state,
                        )
                    },
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                    ) {
                        items(items) { item ->
                            ApplicationRow(
                                item = item,
                                onClick = {
                                    scope.launch {
                                        ShoukoApplication.prefs.setLockscreenAction(
                                            key = settingsKey,
                                            value = it.flattenToString(),
                                        )
                                        navController.popBackStack()
                                    }
                                },
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            }

            1 -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                ) {
                    item {
                        CommonActionRow(
                            iconVector = Icons.Rounded.Clear,
                            label = DoNothingAction().getLabel(context),
                            onClick = {
                                scope.launch {
                                    val emptyCn = ComponentName(context, EmptyShortcutActivity::class.java)
                                    ShoukoApplication.prefs.setLockscreenAction(settingsKey, emptyCn.flattenToString())
                                    navController.popBackStack()
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
