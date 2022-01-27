package xyz.ivaniskandar.shouko.ui.destination

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.activity.MainActivityViewModel
import xyz.ivaniskandar.shouko.feature.DigitalAssistantAction
import xyz.ivaniskandar.shouko.feature.DoNothingAction
import xyz.ivaniskandar.shouko.feature.FlashlightAction
import xyz.ivaniskandar.shouko.feature.IntentAction
import xyz.ivaniskandar.shouko.feature.MediaKeyAction
import xyz.ivaniskandar.shouko.feature.MuteMicrophoneAction
import xyz.ivaniskandar.shouko.feature.RingerModeAction
import xyz.ivaniskandar.shouko.feature.ScreenshotAction
import xyz.ivaniskandar.shouko.feature.StatusBarAction
import xyz.ivaniskandar.shouko.ui.Screen
import xyz.ivaniskandar.shouko.ui.component.ApplicationRow
import xyz.ivaniskandar.shouko.ui.component.CategoryHeader
import xyz.ivaniskandar.shouko.ui.component.DigitalAssistantRow
import xyz.ivaniskandar.shouko.ui.component.DoNothingRow
import xyz.ivaniskandar.shouko.ui.component.FlashlightRow
import xyz.ivaniskandar.shouko.ui.component.MediaKeyRow
import xyz.ivaniskandar.shouko.ui.component.MuteMicrophoneRow
import xyz.ivaniskandar.shouko.ui.component.Preference
import xyz.ivaniskandar.shouko.ui.component.ReadLogsCard
import xyz.ivaniskandar.shouko.ui.component.RingerModeRow
import xyz.ivaniskandar.shouko.ui.component.ScreenshotRow
import xyz.ivaniskandar.shouko.ui.component.ShortcutCreatorRow
import xyz.ivaniskandar.shouko.ui.component.StatusBarRow
import xyz.ivaniskandar.shouko.ui.component.SwitchPreference
import xyz.ivaniskandar.shouko.ui.component.TabPager
import xyz.ivaniskandar.shouko.ui.component.WriteSettingsCard
import xyz.ivaniskandar.shouko.util.Prefs
import xyz.ivaniskandar.shouko.util.canReadSystemLogs
import xyz.ivaniskandar.shouko.util.canWriteSecureSettings
import xyz.ivaniskandar.shouko.util.setAsAssistantAction

@Composable
fun AssistantButtonSettings(
    prefs: Prefs,
    navController: NavController
) {
    val context = LocalContext.current
    var buttonEnabled by remember { mutableStateOf(prefs.assistButtonEnabled) }
    LazyColumn(contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)) {
        item {
            ReadLogsCard(visible = !context.canReadSystemLogs) {
                navController.navigate(Screen.ReadLogsSetup.route)
            }
        }
        item {
            WriteSettingsCard(visible = !context.canWriteSecureSettings) {
                navController.navigate(Screen.SecureSettingsSetup.route)
            }
        }
        item {
            SwitchPreference(
                title = stringResource(id = R.string.assistant_button_title),
                checked = buttonEnabled,
                enabled = context.canWriteSecureSettings
            ) {
                prefs.assistButtonEnabled = it
                buttonEnabled = it
            }
        }
        item {
            Preference(
                title = stringResource(id = R.string.assistant_launch_selection_title),
                subtitle = prefs.assistButtonAction?.getLabel(context)
                    ?: stringResource(id = R.string.assistant_action_select_default_value),
                enabled = buttonEnabled && context.canReadSystemLogs
            ) {
                navController.navigate(Screen.AssistantLaunchSelection.route)
            }
        }
        item {
            var hideAssistantCue by remember { mutableStateOf(prefs.hideAssistantCue) }
            SwitchPreference(
                title = stringResource(R.string.hide_assistant_cue_title),
                subtitle = stringResource(R.string.hide_assistant_cue_desc),
                checked = hideAssistantCue,
                enabled = buttonEnabled && prefs.assistButtonAction != null,
                onCheckedChanged = {
                    prefs.hideAssistantCue = it
                    hideAssistantCue = it
                }
            )
        }
    }
}

@Composable
fun AssistantActionSelection(
    mainViewModel: MainActivityViewModel = viewModel(),
    prefs: Prefs,
    navController: NavController
) {
    val titles = listOf(
        stringResource(R.string.tab_title_apps),
        stringResource(R.string.tab_title_shortcuts),
        stringResource(R.string.tab_title_other)
    )
    TabPager(pageTitles = titles) { page ->
        when (page) {
            0 -> {
                val items by mainViewModel.appsList.observeAsState()
                val isRefreshing by mainViewModel.isRefreshingAppsList.collectAsState()
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing),
                    onRefresh = { mainViewModel.refreshAppsList() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (items != null) {
                        LazyColumn(
                            contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)
                        ) {
                            items(items!!) { item ->
                                ApplicationRow(item = item) {
                                    val intent = Intent().apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        component = it
                                    }
                                    prefs.assistButtonAction = IntentAction(intent)
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
            1 -> {
                val items by mainViewModel.shortcutList.observeAsState()
                val context = LocalContext.current
                val createShortcut =
                    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                        if (it.resultCode == Activity.RESULT_OK) {
                            val intent = it.data
                            if (intent != null) {
                                intent.setAsAssistantAction(prefs)
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.assistant_action_save_failed_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            navController.popBackStack()
                        }
                    }
                val isRefreshing by mainViewModel.isRefreshingShortcutList.collectAsState()
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing),
                    onRefresh = { mainViewModel.refreshShortcutCreatorList() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (items != null) {
                        LazyColumn(
                            contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)
                        ) {
                            items(items!!) { item ->
                                ShortcutCreatorRow(item = item) {
                                    val i = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
                                        component = it
                                    }
                                    createShortcut.launch(i)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                val context = LocalContext.current
                LazyColumn(contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)) {
                    item { CategoryHeader(title = stringResource(id = R.string.category_title_media_key)) }
                    items(MediaKeyAction.Key.values()) { item ->
                        MediaKeyRow(key = item) {
                            prefs.assistButtonAction = it
                            navController.popBackStack()
                        }
                    }

                    item { CategoryHeader(title = stringResource(id = R.string.tab_title_other), divider = true) }
                    if (FlashlightAction.isSupported(context)) {
                        item {
                            FlashlightRow {
                                prefs.assistButtonAction = FlashlightAction()
                                navController.popBackStack()
                            }
                        }
                    }
                    item {
                        ScreenshotRow {
                            prefs.assistButtonAction = ScreenshotAction()
                            navController.popBackStack()
                        }
                    }
                    items(StatusBarAction.PanelType.values()) { item ->
                        StatusBarRow(type = item) {
                            prefs.assistButtonAction = it
                            navController.popBackStack()
                        }
                    }
                    item {
                        RingerModeRow {
                            prefs.assistButtonAction = RingerModeAction()
                            navController.popBackStack()
                        }
                    }
                    item {
                        MuteMicrophoneRow {
                            prefs.assistButtonAction = MuteMicrophoneAction()
                            navController.popBackStack()
                        }
                    }
                    item {
                        DigitalAssistantRow {
                            prefs.assistButtonAction = DigitalAssistantAction()
                            navController.popBackStack()
                        }
                    }
                    item {
                        DoNothingRow {
                            prefs.assistButtonAction = DoNothingAction()
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}
