package xyz.ivaniskandar.shouko.ui

import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.activity.EmptyShortcutActivity
import xyz.ivaniskandar.shouko.activity.MainActivityViewModel
import xyz.ivaniskandar.shouko.feature.DoNothingAction
import xyz.ivaniskandar.shouko.feature.FlashlightAction
import xyz.ivaniskandar.shouko.feature.FlipToShush
import xyz.ivaniskandar.shouko.feature.GAKeyOverrider
import xyz.ivaniskandar.shouko.feature.IntentAction
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.LOCKSCREEN_LEFT_BUTTON
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.LOCKSCREEN_RIGHT_BUTTON
import xyz.ivaniskandar.shouko.feature.MediaKeyAction
import xyz.ivaniskandar.shouko.feature.ScreenshotAction
import xyz.ivaniskandar.shouko.feature.StatusBarAction
import xyz.ivaniskandar.shouko.service.TadanoAccessibilityService
import xyz.ivaniskandar.shouko.util.Prefs
import xyz.ivaniskandar.shouko.util.RELEASES_PAGE_INTENT
import xyz.ivaniskandar.shouko.util.canReadSystemLogs
import xyz.ivaniskandar.shouko.util.canWriteSecureSettings
import xyz.ivaniskandar.shouko.util.highlightSettingsTo
import xyz.ivaniskandar.shouko.util.loadLabel
import xyz.ivaniskandar.shouko.util.setAsAssistantAction
import xyz.ivaniskandar.shouko.util.toComponentName

@Composable
fun getAppBarTitle(navController: NavController): String {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return when (navBackStackEntry?.destination?.route) {
        Screen.AssistantButtonSettings.route -> stringResource(id = R.string.assistant_button_title)
        Screen.AssistantLaunchSelection.route -> stringResource(id = R.string.assistant_launch_selection_title)
        Screen.ReadLogsSetup.route, Screen.SecureSettingsSetup.route -> ""
        Screen.LockscreenShortcutSettings.route -> stringResource(id = R.string.lockscreen_shortcut_title)
        Screen.LockscreenShortcutSelection.route -> {
            when (navBackStackEntry?.arguments?.getString("key")) {
                LOCKSCREEN_RIGHT_BUTTON -> stringResource(id = R.string.lockscreen_shortcut_right)
                LOCKSCREEN_LEFT_BUTTON -> stringResource(id = R.string.lockscreen_shortcut_left)
                else -> stringResource(id = R.string.lockscreen_shortcut_title)
            }
        }
        else -> stringResource(id = R.string.app_name)
    }
}

@Composable
fun MainActivityActions(
    prefs: Prefs,
    navController: NavController
) {
    val context = LocalContext.current
    var showPopup by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val menuItems = mutableListOf<@Composable ColumnScope.() -> Unit>()
    when (navBackStackEntry?.destination?.route) {
        Screen.Home.route -> {
            menuItems += {
                DropdownMenuItem(
                    onClick = {
                        context.startActivity(RELEASES_PAGE_INTENT)
                        showPopup = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.check_for_update),
                        style = MaterialTheme.typography.body1
                    )
                }
            }
            menuItems += {
                DropdownMenuItem(
                    onClick = {
                        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                        showPopup = false
                    }
                ) {
                    Text(
                        text = stringResource(R.string.oss_license_title),
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
        Screen.AssistantLaunchSelection.route -> {
            menuItems += {
                DropdownMenuItem(
                    onClick = {
                        prefs.assistButtonAction = null
                        Toast.makeText(
                            context,
                            context.getString(R.string.assistant_action_reset_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                        showPopup = false
                        navController.popBackStack()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.reset_to_default),
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
        Screen.LockscreenShortcutSelection.route -> {
            menuItems += {
                DropdownMenuItem(
                    onClick = {
                        val key = navBackStackEntry?.arguments?.getString("key")
                        LockscreenShortcutHelper.getPreferences(context).edit {
                            remove(key)
                        }
                        Settings.Secure.putString(context.contentResolver, key, null)
                        showPopup = false
                        navController.popBackStack()
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.reset_to_default),
                        style = MaterialTheme.typography.body1
                    )
                }
            }
        }
    }

    if (menuItems.isNotEmpty()) {
        IconButton(onClick = { showPopup = true }) {
            Icon(imageVector = Icons.Rounded.MoreVert, contentDescription = null)
        }
        DropdownMenu(
            expanded = showPopup,
            onDismissRequest = { showPopup = false },
            modifier = Modifier.sizeIn(minWidth = 196.dp, maxWidth = 196.dp),
            offset = DpOffset(8.dp, 0.dp),
            content = { menuItems.forEach { it() } }
        )
    }
}

@Composable
fun Home(
    prefs: Prefs,
    navController: NavController
) {
    val context = LocalContext.current
    LazyColumn(contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)) {
        item {
            AccessibilityServiceCard(visible = !TadanoAccessibilityService.isActive) {
                val serviceCn = ComponentName(context, TadanoAccessibilityService::class.java).flattenToString()
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).highlightSettingsTo(serviceCn)
                context.startActivity(intent)
            }
        }
        if (GAKeyOverrider.isSupported) {
            item {
                Preference(
                    title = stringResource(id = R.string.assistant_button_title),
                    subtitle = if (!prefs.assistButtonEnabled) {
                        stringResource(R.string.off)
                    } else {
                        prefs.assistButtonAction?.getLabel(context)
                            ?: stringResource(id = R.string.assistant_action_select_default_value)
                    },
                    enabled = TadanoAccessibilityService.isActive
                ) {
                    navController.navigate(Screen.AssistantButtonSettings.route)
                }
            }
        }
        item {
            var flipToShush by remember { mutableStateOf(prefs.flipToShushEnabled) }
            val fullTimeFlipToShush = remember { FlipToShush.supportFullTimeListening(context) }
            val subtitle = remember {
                context.getString(R.string.flip_to_shush_desc) +
                    if (!fullTimeFlipToShush) {
                        "\n\n${context.getString(R.string.flip_to_shush_desc_extra_screen_on)}"
                    } else {
                        ""
                    }
            }
            val dndAccessCheck = rememberLauncherForActivityResult(StartActivityForResult()) {
                val isGrantedDndAccess = context.getSystemService<NotificationManager>()!!
                    .isNotificationPolicyAccessGranted
                if (isGrantedDndAccess) {
                    // Always true becos
                    prefs.flipToShushEnabled = true
                    flipToShush = true
                }
            }
            SwitchPreference(
                title = stringResource(R.string.flip_to_shush_title),
                subtitle = subtitle,
                checked = flipToShush,
                enabled = TadanoAccessibilityService.isActive
            ) {
                if (it) {
                    val isGrantedDndAccess = context.getSystemService<NotificationManager>()!!
                        .isNotificationPolicyAccessGranted
                    if (!isGrantedDndAccess) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.allow_dnd_access_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            .highlightSettingsTo(context.packageName)
                        dndAccessCheck.launch(intent)
                        return@SwitchPreference
                    }
                }
                prefs.flipToShushEnabled = it
                flipToShush = it
            }
        }
        item {
            var preventPocketTouch by remember { mutableStateOf(prefs.preventPocketTouchEnabled) }
            SwitchPreference(
                title = stringResource(R.string.pocket_no_touchy_title),
                subtitle = stringResource(R.string.pocket_no_touchy_desc),
                checked = preventPocketTouch,
                enabled = TadanoAccessibilityService.isActive
            ) {
                prefs.preventPocketTouchEnabled = it
                preventPocketTouch = it
            }
        }
        item {
            Preference(
                title = stringResource(R.string.lockscreen_shortcut_title),
                subtitle = stringResource(R.string.lockscreen_shortcut_desc),
                enabled = TadanoAccessibilityService.isActive
            ) {
                navController.navigate(Screen.LockscreenShortcutSettings.route)
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
        Image(
            painter = painterResource(id = R.drawable.shouko),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(64.dp)
        )
    }
}

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
                val createShortcut = rememberLauncherForActivityResult(StartActivityForResult()) {
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

@Composable
fun LockscreenShortcutSettings(
    navController: NavController
) {
    val context = LocalContext.current
    ComponentName.unflattenFromString("")
    LazyColumn(contentPadding = rememberInsetsPaddingValues(LocalWindowInsets.current.navigationBars)) {
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
                    modifier = Modifier.fillMaxSize()
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
