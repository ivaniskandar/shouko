package xyz.ivaniskandar.shouko.ui.destination

import android.content.ComponentName
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.activity.EmptyShortcutActivity
import xyz.ivaniskandar.shouko.activity.MainActivityViewModel
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.LOCKSCREEN_LEFT_BUTTON
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.LOCKSCREEN_RIGHT_BUTTON
import xyz.ivaniskandar.shouko.ui.Screen
import xyz.ivaniskandar.shouko.ui.component.ApplicationRow
import xyz.ivaniskandar.shouko.ui.component.DoNothingRow
import xyz.ivaniskandar.shouko.ui.component.M3SwipeRefreshIndicator
import xyz.ivaniskandar.shouko.ui.component.Preference
import xyz.ivaniskandar.shouko.ui.component.TabPager
import xyz.ivaniskandar.shouko.ui.component.WriteSettingsCard
import xyz.ivaniskandar.shouko.util.canWriteSecureSettings
import xyz.ivaniskandar.shouko.util.loadLabel
import xyz.ivaniskandar.shouko.util.toComponentName

@Composable
fun LockscreenShortcutSettings(
    navController: NavController
) {
    val context = LocalContext.current
    ComponentName.unflattenFromString("")
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)
    ) {
        item {
            WriteSettingsCard(visible = !context.canWriteSecureSettings) {
                navController.navigate(Screen.SecureSettingsSetup.route)
            }
        }
        item {
            Preference(
                title = stringResource(R.string.lockscreen_shortcut_left),
                subtitle = LockscreenShortcutHelper.getPreferences(context).getString(LOCKSCREEN_LEFT_BUTTON, null)
                    ?.toComponentName()?.loadLabel(context)
                    ?: stringResource(id = R.string.assistant_action_select_default_value),
                enabled = context.canWriteSecureSettings
            ) {
                navController.navigate(Screen.LockscreenShortcutSelection.createRoute(LOCKSCREEN_LEFT_BUTTON))
            }
        }
        item {
            Preference(
                title = stringResource(R.string.lockscreen_shortcut_right),
                subtitle = LockscreenShortcutHelper.getPreferences(context).getString(LOCKSCREEN_RIGHT_BUTTON, null)
                    ?.toComponentName()?.loadLabel(context)
                    ?: stringResource(id = R.string.assistant_action_select_default_value),
                enabled = context.canWriteSecureSettings
            ) {
                navController.navigate(Screen.LockscreenShortcutSelection.createRoute(LOCKSCREEN_RIGHT_BUTTON))
            }
        }
    }
}

@Composable
fun LockscreenShortcutSelection(
    mainViewModel: MainActivityViewModel = viewModel(),
    navController: NavController,
    settingsKey: String
) {
    val titles = listOf(
        stringResource(R.string.tab_title_apps),
        stringResource(R.string.tab_title_other)
    )
    TabPager(pageTitles = titles) { page ->
        val context = LocalContext.current
        when (page) {
            0 -> {
                val items by mainViewModel.appsList.observeAsState()
                val isRefreshing by mainViewModel.isRefreshingAppsList.collectAsState()
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing),
                    onRefresh = { mainViewModel.refreshAppsList() },
                    modifier = Modifier.fillMaxSize(),
                    indicator = { s, trigger ->
                        M3SwipeRefreshIndicator(state = s, refreshTriggerDistance = trigger)
                    }
                ) {
                    if (items != null) {
                        LazyColumn(
                            contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)
                        ) {
                            items(items!!) { item ->
                                ApplicationRow(item = item) {
                                    LockscreenShortcutHelper.getPreferences(context).edit {
                                        putString(settingsKey, it.flattenToString())
                                    }
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
            1 -> {
                LazyColumn(contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)) {
                    item {
                        DoNothingRow {
                            val emptyCn = ComponentName(context, EmptyShortcutActivity::class.java)
                            LockscreenShortcutHelper.getPreferences(context).edit {
                                putString(settingsKey, emptyCn.flattenToString())
                            }
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}
