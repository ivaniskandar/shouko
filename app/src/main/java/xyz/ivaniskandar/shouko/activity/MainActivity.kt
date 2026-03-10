package xyz.ivaniskandar.shouko.activity

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import androidx.navigationevent.NavigationEvent
import com.google.android.gms.oss.licenses.v2.OssLicensesMenuActivity
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ShoukoApplication
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.LOCKSCREEN_LEFT_BUTTON
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.LOCKSCREEN_RIGHT_BUTTON
import xyz.ivaniskandar.shouko.ui.Navigator
import xyz.ivaniskandar.shouko.ui.Screen
import xyz.ivaniskandar.shouko.ui.component.Scaffold
import xyz.ivaniskandar.shouko.ui.destination.AndroidAppLinkSettings
import xyz.ivaniskandar.shouko.ui.destination.AssistantActionSelection
import xyz.ivaniskandar.shouko.ui.destination.AssistantButtonSettings
import xyz.ivaniskandar.shouko.ui.destination.Home
import xyz.ivaniskandar.shouko.ui.destination.LinkTargetInfoSheet
import xyz.ivaniskandar.shouko.ui.destination.LinkTargetList
import xyz.ivaniskandar.shouko.ui.destination.LockscreenShortcutSelection
import xyz.ivaniskandar.shouko.ui.destination.LockscreenShortcutSettings
import xyz.ivaniskandar.shouko.ui.destination.PermissionSetup
import xyz.ivaniskandar.shouko.ui.rememberNavigationState
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme
import xyz.ivaniskandar.shouko.ui.toEntries
import xyz.ivaniskandar.shouko.util.RELEASES_PAGE_INTENT
import xyz.ivaniskandar.shouko.util.isRootAvailable
import xyz.ivaniskandar.shouko.util.openDefaultAppsSettings
import xyz.ivaniskandar.shouko.util.openOpenByDefaultSettings
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        setContent {
            val navigationState = rememberNavigationState(
                startRoute = Screen.Home,
                topLevelRoutes = setOf(Screen.Home),
            )
            val navigator = remember { Navigator(navigationState) }

            ShoukoM3Theme {
                val rootAvailable = remember { isRootAvailable }
                val entryProvider = entryProvider<NavKey> {
                    entry<Screen.Home> {
                        EntryDecorator(
                            navigator = navigator,
                            currentRoute = it,
                        ) { innerPadding ->
                            Home(navigator, innerPadding)
                        }
                    }
                    entry<Screen.ReadLogsSetup> {
                        EntryDecorator(
                            navigator = navigator,
                            currentRoute = it,
                        ) { innerPadding ->
                            PermissionSetup(
                                contentPadding = innerPadding,
                                title = stringResource(id = R.string.read_logs_permission_setup_title),
                                permissionName = Manifest.permission.READ_LOGS,
                                isRootAvailable = rootAvailable,
                                onFinishSetup = {
                                    finishAffinity()
                                    startActivity(intent)
                                    exitProcess(0)
                                },
                            )
                        }
                    }
                    entry<Screen.SecureSettingsSetup> {
                        EntryDecorator(
                            navigator = navigator,
                            currentRoute = it,
                        ) { innerPadding ->
                            PermissionSetup(
                                contentPadding = innerPadding,
                                title = stringResource(id = R.string.write_secure_settings_permission_setup_title),
                                permissionName = Manifest.permission.WRITE_SECURE_SETTINGS,
                                isRootAvailable = rootAvailable,
                                onFinishSetup = {
                                    finishAffinity()
                                    startActivity(intent)
                                    exitProcess(0)
                                },
                            )
                        }
                    }
                    entry<Screen.AssistantButtonSettings> {
                        EntryDecorator(
                            navigator = navigator,
                            currentRoute = it,
                        ) { innerPadding ->
                            AssistantButtonSettings(navigator, innerPadding)
                        }
                    }
                    entry<Screen.AssistantLaunchSelection> {
                        EntryDecorator(
                            navigator = navigator,
                            currentRoute = it,
                        ) { innerPadding ->
                            AssistantActionSelection(
                                navigator = navigator,
                                contentPadding = innerPadding,
                                mainViewModel = viewModel,
                            )
                        }
                    }
                    entry<Screen.LockscreenShortcutSettings> {
                        EntryDecorator(
                            navigator = navigator,
                            currentRoute = it,
                        ) { innerPadding ->
                            LockscreenShortcutSettings(navigator, innerPadding)
                        }
                    }
                    entry<Screen.LockscreenShortcutSelection> { key ->
                        EntryDecorator(
                            navigator = navigator,
                            currentRoute = key,
                        ) { innerPadding ->
                            LockscreenShortcutSelection(
                                mainViewModel = viewModel,
                                navigator = navigator,
                                settingsKey = key.key,
                                contentPadding = innerPadding,
                            )
                        }
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        entry<Screen.AndroidAppLinkSettings> {
                            EntryDecorator(
                                navigator = navigator,
                                currentRoute = it,
                            ) { innerPadding ->
                                AndroidAppLinkSettings(
                                    navigator = navigator,
                                    contentPadding = innerPadding,
                                    onOpenSettings = {
                                        val roleManager = getSystemService<RoleManager>()
                                        if (roleManager?.isRoleHeld(RoleManager.ROLE_BROWSER) == true) {
                                            openDefaultAppsSettings(this@MainActivity)
                                        } else if (roleManager != null) {
                                            val i = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                                            @Suppress("DEPRECATION") // we don't care about results here
                                            startActivityForResult(i, 286444)
                                        }
                                    },
                                )
                            }
                        }
                        entry<Screen.ApprovedLinkTargetList> {
                            EntryDecorator(
                                navigator = navigator,
                                currentRoute = it,
                            ) { innerPadding ->
                                LinkTargetList(
                                    approved = true,
                                    mainViewModel = viewModel,
                                    navigator = navigator,
                                    contentPadding = innerPadding,
                                )
                            }
                        }
                        entry<Screen.UnapprovedLinkTargetList> {
                            EntryDecorator(
                                navigator = navigator,
                                currentRoute = it,
                            ) { innerPadding ->
                                LinkTargetList(
                                    approved = false,
                                    mainViewModel = viewModel,
                                    navigator = navigator,
                                    contentPadding = innerPadding,
                                )
                            }
                        }
                        entry<Screen.LinkTargetInfoSheet>(metadata = DialogSceneStrategy.dialog()) { key ->
                            Surface(shape = MaterialTheme.shapes.extraLarge) {
                                LinkTargetInfoSheet(
                                    packageName = key.packageName,
                                    mainViewModel = viewModel,
                                    onOpenSettings = {
                                        openOpenByDefaultSettings(this@MainActivity, key.packageName)
                                    },
                                )
                            }
                        }
                    }
                }

                val slideDistance = rememberSlideDistance()
                NavDisplay(
                    entries = navigationState.toEntries(entryProvider),
                    onBack = navigator::goBack,
                    sceneStrategies = listOf(DialogSceneStrategy()),
                    transitionSpec = { materialSharedAxisX(forward = true, slideDistance = slideDistance) },
                    popTransitionSpec = { materialSharedAxisX(forward = false, slideDistance = slideDistance) },
                    predictivePopTransitionSpec = { swipeEdge ->
                        val towards = when (swipeEdge) {
                            NavigationEvent.EDGE_LEFT -> SlideDirection.Right
                            NavigationEvent.EDGE_RIGHT -> SlideDirection.Left
                            else -> SlideDirection.End
                        }
                        ContentTransform(
                            targetContentEnter = fadeIn() + slideIntoContainer(towards = towards, initialOffset = { it / 4 }),
                            initialContentExit = slideOutOfContainer(towards = towards),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun EntryDecorator(
    navigator: Navigator,
    currentRoute: Screen,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit,
) {
    val scrollState = rememberTopAppBarState()
    val scrollBehavior = when (currentRoute) {
        // Disable scroll effect because tabs
        is Screen.AssistantLaunchSelection, is Screen.LockscreenShortcutSelection -> null

        else -> TopAppBarDefaults.pinnedScrollBehavior(scrollState)
    }
    Scaffold(
        modifier = if (scrollBehavior != null) {
            modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            modifier
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = getAppBarTitle(currentRoute = currentRoute)) },
                navigationIcon = {
                    if (currentRoute != Screen.Home) {
                        IconButton(onClick = { navigator.goBack() }) {
                            Icon(imageVector = Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                        }
                    }
                },
                actions = { MainActivityActions(navigator = navigator, currentRoute = currentRoute) },
                scrollBehavior = scrollBehavior,
            )
        },
        contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        content = content,
    )
}

@Composable
@ReadOnlyComposable
private fun getAppBarTitle(
    currentRoute: Screen?,
): String = when (currentRoute) {
    is Screen.AssistantButtonSettings -> stringResource(id = R.string.assistant_button_title)

    is Screen.AssistantLaunchSelection -> stringResource(id = R.string.assistant_launch_selection_title)

    is Screen.ReadLogsSetup, is Screen.SecureSettingsSetup -> ""

    is Screen.LockscreenShortcutSettings -> stringResource(id = R.string.lockscreen_shortcut_title)

    is Screen.LockscreenShortcutSelection -> {
        when (currentRoute.key) {
            LOCKSCREEN_RIGHT_BUTTON -> stringResource(id = R.string.lockscreen_shortcut_right)
            LOCKSCREEN_LEFT_BUTTON -> stringResource(id = R.string.lockscreen_shortcut_left)
            else -> stringResource(id = R.string.lockscreen_shortcut_title)
        }
    }

    is Screen.AndroidAppLinkSettings -> stringResource(id = R.string.android_app_link_title)

    is Screen.ApprovedLinkTargetList -> stringResource(id = R.string.approved_link_target_title)

    is Screen.UnapprovedLinkTargetList -> stringResource(id = R.string.unapproved_link_target_title)

    is Screen.LinkTargetInfoSheet -> ""

    else -> stringResource(id = R.string.app_name)
}

@Suppress("UnusedReceiverParameter")
@Composable
private fun RowScope.MainActivityActions(
    navigator: Navigator,
    currentRoute: Screen?,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPopup by remember { mutableStateOf(false) }

    val menuItems = mutableListOf<@Composable ColumnScope.() -> Unit>()
    when (currentRoute) {
        is Screen.Home -> {
            menuItems += {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.check_for_update),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    },
                    onClick = {
                        context.startActivity(RELEASES_PAGE_INTENT)
                        showPopup = false
                    },
                )
            }
            menuItems += {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(com.google.android.gms.oss.licenses.R.string.oss_license_title),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    },
                    onClick = {
                        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                        showPopup = false
                    },
                )
            }
        }

        is Screen.AssistantLaunchSelection -> {
            menuItems += {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.reset_to_default),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    },
                    onClick = {
                        scope.launch {
                            ShoukoApplication.prefs.setAssistButtonAction(null)
                            Toast.makeText(
                                context,
                                context.getString(R.string.assistant_action_reset_toast),
                                Toast.LENGTH_SHORT,
                            ).show()
                            showPopup = false
                            navigator.goBack()
                        }
                    },
                )
            }
        }

        is Screen.LockscreenShortcutSelection -> {
            menuItems += {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.reset_to_default),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                        )
                    },
                    onClick = {
                        val key = currentRoute.key
                        scope.launch {
                            ShoukoApplication.prefs.setLockscreenAction(key, null)
                            // Note: Secure Settings requires permission, which we check elsewhere
                            // This might fail if permission is revoked but it's consistent with original code
                            try {
                                android.provider.Settings.Secure.putString(context.contentResolver, key, null)
                            } catch (e: Exception) {
                                logcat(LogPriority.ERROR) { "Failed to reset secure setting: $e" }
                            }
                        }
                        showPopup = false
                        navigator.goBack()
                    },
                )
            }
        }

        else -> {}
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
            content = { menuItems.forEach { it() } },
        )
    }
}
