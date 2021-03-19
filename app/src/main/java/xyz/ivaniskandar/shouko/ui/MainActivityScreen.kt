package xyz.ivaniskandar.shouko.ui

import android.Manifest.permission.*
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager.*
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.registerForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.KEY_ROUTE
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigate
import dev.chrisbanes.accompanist.insets.LocalWindowInsets
import dev.chrisbanes.accompanist.insets.navigationBarsPadding
import dev.chrisbanes.accompanist.insets.toPaddingValues
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.activity.EmptyShortcutActivity
import xyz.ivaniskandar.shouko.activity.MainActivityViewModel
import xyz.ivaniskandar.shouko.feature.*
import xyz.ivaniskandar.shouko.service.TadanoAccessibilityService
import xyz.ivaniskandar.shouko.util.*
import java.util.*

const val LOCKSCREEN_SHORTCUT_SELECTION_KEY_ARG = "key"

const val ROUTE_HOME = "home"
const val ROUTE_READ_LOGS_PERMISSION_SETUP = "read_logs_permission_setup"
const val ROUTE_WRITE_SECURE_SETTINGS_PERMISSION_SETUP = "write_secure_settings_permission_setup"
const val ROUTE_ASSISTANT_BUTTON_SETTINGS = "assistant_button_settings"
const val ROUTE_ASSISTANT_LAUNCH_SELECTION = "assistant_launch_selection"
const val ROUTE_LOCKSCREEN_SHORTCUT_SETTINGS = "lockscreen_shortcut_settings"
const val ROUTE_LOCKSCREEN_SHORTCUT_SELECTION = "lockscreen_shortcut_selection/{$LOCKSCREEN_SHORTCUT_SELECTION_KEY_ARG}"

@Composable
fun getAppBarTitle(navController: NavController): String {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    return when (navBackStackEntry?.arguments?.getString(KEY_ROUTE)) {
        ROUTE_ASSISTANT_BUTTON_SETTINGS -> stringResource(id = R.string.assistant_button_title)
        ROUTE_ASSISTANT_LAUNCH_SELECTION -> stringResource(id = R.string.assistant_launch_selection_title)
        ROUTE_READ_LOGS_PERMISSION_SETUP,
        ROUTE_WRITE_SECURE_SETTINGS_PERMISSION_SETUP -> ""
        ROUTE_LOCKSCREEN_SHORTCUT_SETTINGS -> stringResource(id = R.string.lockscreen_shortcut_title)
        ROUTE_LOCKSCREEN_SHORTCUT_SELECTION -> {
            when (navBackStackEntry?.arguments?.getString(LOCKSCREEN_SHORTCUT_SELECTION_KEY_ARG)) {
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
    val currentRoute = navBackStackEntry?.arguments?.getString(KEY_ROUTE)

    val menuItems = mutableListOf<@Composable ColumnScope.() -> Unit>()
    when (currentRoute) {
        ROUTE_HOME -> {
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
        }
        ROUTE_ASSISTANT_LAUNCH_SELECTION -> {
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
        ROUTE_LOCKSCREEN_SHORTCUT_SELECTION -> {
            menuItems += {
                DropdownMenuItem(
                    onClick = {
                        val key = navBackStackEntry?.arguments?.getString(LOCKSCREEN_SHORTCUT_SELECTION_KEY_ARG)
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
    LazyColumn(contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()) {
        item {
            AccessibilityServiceCard(visible = !TadanoAccessibilityService.isActive) {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
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
                    navController.navigate(ROUTE_ASSISTANT_BUTTON_SETTINGS)
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
            SwitchPreference(
                title = stringResource(R.string.flip_to_shush_title),
                subtitle = subtitle,
                checked = flipToShush,
                enabled = TadanoAccessibilityService.isActive
            ) {
                if (it) {
                    val isGrantedDndAccess = context.getSystemService(NotificationManager::class.java)!!
                        .isNotificationPolicyAccessGranted
                    if (!isGrantedDndAccess) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.allow_dnd_access_toast),
                            Toast.LENGTH_LONG
                        ).show()
                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        return@SwitchPreference
                    }

                    if (fullTimeFlipToShush) {
                        val isIgnoringOptimizations = context.getSystemService(PowerManager::class.java)!!
                            .isIgnoringBatteryOptimizations(context.packageName)
                        if (!isIgnoringOptimizations) {
                            @SuppressLint("BatteryLife")
                            val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            context.startActivity(i)
                            return@SwitchPreference
                        }
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
                subtitle = stringResource(R.string.lockscreen_shortcut_desc)
            ) {
                navController.navigate(ROUTE_LOCKSCREEN_SHORTCUT_SETTINGS)
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
    LazyColumn(contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()) {
        item {
            ReadLogsCard(visible = !context.canReadSystemLogs) {
                navController.navigate(ROUTE_READ_LOGS_PERMISSION_SETUP)
            }
        }
        item {
            WriteSettingsCard(visible = !context.canWriteSecureSettings) {
                navController.navigate(ROUTE_WRITE_SECURE_SETTINGS_PERMISSION_SETUP)
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
                navController.navigate(ROUTE_ASSISTANT_LAUNCH_SELECTION)
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
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val titles = listOf(
        R.string.tab_title_apps,
        R.string.tab_title_shortcuts,
        R.string.tab_title_other
    )
    Column {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.navigationBarsPadding(bottom = false),
            backgroundColor = MaterialTheme.colors.background,
            contentColor = MaterialTheme.colors.primary
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(text = stringResource(id = title).toUpperCase(Locale.getDefault()))
                    },
                    unselectedContentColor = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
                )
            }
        }
        when (selectedTabIndex) {
            0 -> {
                val appItems by mainViewModel.appsList.observeAsState()
                if (appItems == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()) {
                        items(appItems!!) { item ->
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
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
            1 -> {
                val appItems by mainViewModel.shortcutList.observeAsState()
                if (appItems == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val context = LocalContext.current
                    val createShortcut = registerForActivityResult(
                        contract = ActivityResultContracts.StartActivityForResult(),
                        onResult = {
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
                    )
                    LazyColumn(contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()) {
                        items(appItems!!) { item ->
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
            2 -> {
                val context = LocalContext.current
                LazyColumn(contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()) {
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
    LazyColumn(contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()) {
        item {
            WriteSettingsCard(visible = !context.canWriteSecureSettings) {
                navController.navigate(ROUTE_WRITE_SECURE_SETTINGS_PERMISSION_SETUP)
            }
        }
        item {
            Preference(
                title = stringResource(R.string.lockscreen_shortcut_left),
                subtitle = Settings.Secure.getString(context.contentResolver, LOCKSCREEN_LEFT_BUTTON)
                    ?.toComponentName()?.loadLabel(context)
                    ?: stringResource(id = R.string.assistant_action_select_default_value),
                enabled = context.canWriteSecureSettings
            ) {
                navController.navigate(
                    ROUTE_LOCKSCREEN_SHORTCUT_SELECTION
                        .replace("{$LOCKSCREEN_SHORTCUT_SELECTION_KEY_ARG}", LOCKSCREEN_LEFT_BUTTON)
                )
            }
        }
        item {
            Preference(
                title = stringResource(R.string.lockscreen_shortcut_right),
                subtitle = Settings.Secure.getString(context.contentResolver, LOCKSCREEN_RIGHT_BUTTON)
                    ?.toComponentName()?.loadLabel(context)
                    ?: stringResource(id = R.string.assistant_action_select_default_value),
                enabled = context.canWriteSecureSettings
            ) {
                navController.navigate(
                    ROUTE_LOCKSCREEN_SHORTCUT_SELECTION
                        .replace("{$LOCKSCREEN_SHORTCUT_SELECTION_KEY_ARG}", LOCKSCREEN_RIGHT_BUTTON)
                )
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
    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    val titles = listOf(
        R.string.tab_title_apps,
        R.string.tab_title_other
    )
    Column {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.navigationBarsPadding(bottom = false),
            backgroundColor = MaterialTheme.colors.background,
            contentColor = MaterialTheme.colors.primary
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(text = stringResource(id = title).toUpperCase(Locale.getDefault()))
                    },
                    unselectedContentColor = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
                )
            }
        }
        val context = LocalContext.current
        when (selectedTabIndex) {
            0 -> {
                val appItems by mainViewModel.appsList.observeAsState()
                if (appItems == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()) {
                        items(appItems!!) { item ->
                            ApplicationRow(item = item) {
                                Settings.Secure.putString(context.contentResolver, settingsKey, it.flattenToString())
                                navController.popBackStack()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.navigationBarsPadding())
                }
            }
            1 -> {
                LazyColumn(contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()) {
                    item {
                        DoNothingRow {
                            val emptyCn = ComponentName(context, EmptyShortcutActivity::class.java)
                            Settings.Secure.putString(context.contentResolver, settingsKey, emptyCn.flattenToString())
                            navController.popBackStack()
                        }
                    }
                }
            }
        }
    }
}
