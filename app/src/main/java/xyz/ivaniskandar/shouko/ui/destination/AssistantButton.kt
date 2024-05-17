package xyz.ivaniskandar.shouko.ui.destination

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assistant
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.launch
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ShoukoApplication
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
import xyz.ivaniskandar.shouko.ui.component.CommonActionRow
import xyz.ivaniskandar.shouko.ui.component.M3SwipeRefreshIndicator
import xyz.ivaniskandar.shouko.ui.component.Preference
import xyz.ivaniskandar.shouko.ui.component.ReadLogsCard
import xyz.ivaniskandar.shouko.ui.component.ShortcutCreatorRow
import xyz.ivaniskandar.shouko.ui.component.SwitchPreference
import xyz.ivaniskandar.shouko.ui.component.TabPager
import xyz.ivaniskandar.shouko.ui.component.WriteSettingsCard
import xyz.ivaniskandar.shouko.util.AssistButtonPrefs
import xyz.ivaniskandar.shouko.util.canReadSystemLogs
import xyz.ivaniskandar.shouko.util.canWriteSecureSettings
import xyz.ivaniskandar.shouko.util.setAsAssistantAction

@Composable
fun AssistantButtonSettings(
    navController: NavController,
    contentPadding: PaddingValues,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = ShoukoApplication.prefs
    val buttonPrefs by prefs.assistButtonFlow.collectAsState(initial = AssistButtonPrefs())

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
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
                checked = buttonPrefs.enabled,
                enabled = context.canWriteSecureSettings,
            ) {
                scope.launch { prefs.setAssistButtonEnabled(it) }
            }
        }
        item {
            Preference(
                title = stringResource(id = R.string.assistant_launch_selection_title),
                subtitle = buttonPrefs.action?.getLabel(context)
                    ?: stringResource(id = R.string.assistant_action_select_default_value),
                enabled = buttonPrefs.enabled && context.canReadSystemLogs,
            ) {
                navController.navigate(Screen.AssistantLaunchSelection.route)
            }
        }
        item {
            SwitchPreference(
                title = stringResource(R.string.hide_assistant_cue_title),
                subtitle = stringResource(R.string.hide_assistant_cue_desc),
                checked = buttonPrefs.hideAssistantCue,
                enabled = buttonPrefs.enabled && buttonPrefs.action != null,
                onCheckedChanged = {
                    scope.launch { prefs.setHideAssistantCue(it) }
                },
            )
        }
    }
}

@Composable
fun AssistantActionSelection(
    mainViewModel: MainActivityViewModel = viewModel(),
    navController: NavController,
    contentPadding: PaddingValues,
) {
    val scope = rememberCoroutineScope()
    val prefs = ShoukoApplication.prefs
    val titles = listOf(
        stringResource(R.string.tab_title_apps),
        stringResource(R.string.tab_title_shortcuts),
        stringResource(R.string.tab_title_other),
    )
    TabPager(
        pageTitles = titles,
        contentPadding = contentPadding,
    ) { page ->
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
                    },
                ) {
                    if (items != null) {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                        ) {
                            items(items!!) { item ->
                                ApplicationRow(item = item) {
                                    scope.launch {
                                        val intent = Intent().apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            component = it
                                        }
                                        prefs.setAssistButtonAction(IntentAction(intent))
                                        navController.popBackStack()
                                    }
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
                                scope.launch { intent.setAsAssistantAction(prefs) }
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.assistant_action_save_failed_toast),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            }
                            navController.popBackStack()
                        }
                    }
                val isRefreshing by mainViewModel.isRefreshingShortcutList.collectAsState()
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing),
                    onRefresh = { mainViewModel.refreshShortcutCreatorList() },
                    modifier = Modifier.fillMaxSize(),
                    indicatorPadding = contentPadding,
                    indicator = { s, trigger ->
                        M3SwipeRefreshIndicator(state = s, refreshTriggerDistance = trigger)
                    },
                ) {
                    if (items != null) {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
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
                LazyColumn(
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding()),
                ) {
                    item { CategoryHeader(title = stringResource(id = R.string.category_title_media_key)) }
                    items(MediaKeyAction.Key.entries.toTypedArray()) { item ->
                        CommonActionRow(
                            iconPainter = painterResource(id = item.iconResId),
                            label = stringResource(id = item.labelResId),
                            onClick = {
                                scope.launch {
                                    prefs.setAssistButtonAction(MediaKeyAction(item))
                                    navController.popBackStack()
                                }
                            },
                        )
                    }

                    item { CategoryHeader(title = stringResource(id = R.string.tab_title_other), divider = true) }
                    if (FlashlightAction.isSupported(context)) {
                        item {
                            CommonActionRow(
                                iconVector = Icons.Rounded.FlashlightOn,
                                label = FlashlightAction().getLabel(context),
                                onClick = {
                                    scope.launch {
                                        prefs.setAssistButtonAction(FlashlightAction())
                                        navController.popBackStack()
                                    }
                                },
                            )
                        }
                    }
                    item {
                        CommonActionRow(
                            iconVector = Icons.Rounded.Screenshot,
                            label = ScreenshotAction().getLabel(context),
                            onClick = {
                                scope.launch {
                                    prefs.setAssistButtonAction(ScreenshotAction())
                                    navController.popBackStack()
                                }
                            },
                        )
                    }
                    items(StatusBarAction.PanelType.entries.toTypedArray()) { item ->
                        CommonActionRow(
                            iconVector = item.iconVector,
                            label = stringResource(id = item.labelResId),
                            onClick = {
                                scope.launch {
                                    prefs.setAssistButtonAction(StatusBarAction(item))
                                    navController.popBackStack()
                                }
                            },
                        )
                    }
                    item {
                        CommonActionRow(
                            iconVector = Icons.Rounded.Notifications,
                            label = RingerModeAction().getLabel(context),
                            onClick = {
                                scope.launch {
                                    prefs.setAssistButtonAction(RingerModeAction())
                                    navController.popBackStack()
                                }
                            },
                        )
                    }
                    item {
                        CommonActionRow(
                            iconVector = Icons.Rounded.MicOff,
                            label = MuteMicrophoneAction().getLabel(context),
                            onClick = {
                                scope.launch {
                                    prefs.setAssistButtonAction(MuteMicrophoneAction())
                                    navController.popBackStack()
                                }
                            },
                        )
                    }
                    item {
                        CommonActionRow(
                            iconVector = Icons.Rounded.Assistant,
                            label = DigitalAssistantAction().getLabel(context),
                            onClick = {
                                scope.launch {
                                    prefs.setAssistButtonAction(DigitalAssistantAction())
                                    navController.popBackStack()
                                }
                            },
                        )
                    }
                    item {
                        CommonActionRow(
                            iconVector = Icons.Rounded.Clear,
                            label = DoNothingAction().getLabel(context),
                            onClick = {
                                scope.launch {
                                    prefs.setAssistButtonAction(DoNothingAction())
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
