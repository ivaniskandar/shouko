package xyz.ivaniskandar.shouko.activity

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.core.content.edit
import androidx.core.content.getSystemService
import androidx.core.view.WindowCompat
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.navigation.material.BottomSheetNavigator
import com.google.accompanist.navigation.material.ModalBottomSheetLayout
import com.google.accompanist.navigation.material.bottomSheet
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import kotlinx.coroutines.launch
import logcat.LogPriority
import logcat.logcat
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ShoukoApplication
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.LOCKSCREEN_LEFT_BUTTON
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper.Companion.LOCKSCREEN_RIGHT_BUTTON
import xyz.ivaniskandar.shouko.ui.Screen
import xyz.ivaniskandar.shouko.ui.component.InsetAwareCenterAlignedTopAppBar
import xyz.ivaniskandar.shouko.ui.destination.AndroidAppLinkSettings
import xyz.ivaniskandar.shouko.ui.destination.AssistantActionSelection
import xyz.ivaniskandar.shouko.ui.destination.AssistantButtonSettings
import xyz.ivaniskandar.shouko.ui.destination.Home
import xyz.ivaniskandar.shouko.ui.destination.LinkTargetInfoSheet
import xyz.ivaniskandar.shouko.ui.destination.LinkTargetList
import xyz.ivaniskandar.shouko.ui.destination.LockscreenShortcutSelection
import xyz.ivaniskandar.shouko.ui.destination.LockscreenShortcutSettings
import xyz.ivaniskandar.shouko.ui.destination.PermissionSetup
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme
import xyz.ivaniskandar.shouko.util.RELEASES_PAGE_INTENT
import xyz.ivaniskandar.shouko.util.isRootAvailable
import xyz.ivaniskandar.shouko.util.openDefaultAppsSettings
import xyz.ivaniskandar.shouko.util.openOpenByDefaultSettings
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainActivityViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val sheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Hidden,
                skipHalfExpanded = true
            )
            val bottomSheetNavigator = remember(sheetState) { BottomSheetNavigator(sheetState = sheetState) }
            val navController = rememberNavController(bottomSheetNavigator)
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val scrollBehavior = remember(navBackStackEntry) {
                when (navBackStackEntry?.destination?.route) {
                    // Disable scroll effect because tabs
                    Screen.AssistantLaunchSelection.route, Screen.LockscreenShortcutSelection.route -> null
                    else -> TopAppBarDefaults.pinnedScrollBehavior()
                }
            }
            ShoukoM3Theme {
                ProvideWindowInsets {
                    ModalBottomSheetLayout(bottomSheetNavigator) {
                        val scaffoldModifier = if (scrollBehavior != null) {
                            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
                        } else {
                            Modifier
                        }
                        Scaffold(
                            modifier = scaffoldModifier,
                            topBar = {
                                val currentRoute = navBackStackEntry?.destination?.route
                                InsetAwareCenterAlignedTopAppBar(
                                    title = {
                                        Text(
                                            text = getAppBarTitle(
                                                navController = navController,
                                                navBackStackEntry = navBackStackEntry
                                            )
                                        )
                                    },
                                    navigationIcon = if (currentRoute != null && currentRoute != Screen.Home.route) {
                                        {
                                            IconButton(onClick = { navController.popBackStack() }) {
                                                Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = null)
                                            }
                                        }
                                    } else null,
                                    actions = { MainActivityActions(navController = navController) },
                                    scrollBehavior = scrollBehavior
                                )
                            },
                        ) {
                            val rootAvailable = remember { isRootAvailable }
                            NavHost(navController = navController, startDestination = Screen.Home.route) {
                                composable(Screen.Home.route) { Home(navController) }
                                composable(Screen.ReadLogsSetup.route) {
                                    PermissionSetup(
                                        title = stringResource(id = R.string.read_logs_permission_setup_title),
                                        permissionName = Manifest.permission.READ_LOGS,
                                        isRootAvailable = rootAvailable
                                    ) {
                                        finishAffinity()
                                        startActivity(intent)
                                        exitProcess(0)
                                    }
                                }
                                composable(Screen.SecureSettingsSetup.route) {
                                    PermissionSetup(
                                        title = stringResource(id = R.string.write_secure_settings_permission_setup_title),
                                        permissionName = Manifest.permission.WRITE_SECURE_SETTINGS,
                                        isRootAvailable = rootAvailable
                                    ) {
                                        finishAffinity()
                                        startActivity(intent)
                                        exitProcess(0)
                                    }
                                }
                                composable(Screen.AssistantButtonSettings.route) {
                                    AssistantButtonSettings(navController)
                                }
                                composable(Screen.AssistantLaunchSelection.route) {
                                    AssistantActionSelection(viewModel, navController)
                                }
                                composable(Screen.LockscreenShortcutSettings.route) {
                                    LockscreenShortcutSettings(navController)
                                }
                                composable(Screen.LockscreenShortcutSelection.route) {
                                    val key = it.arguments?.getString("key")
                                    if (key != null) {
                                        LockscreenShortcutSelection(
                                            mainViewModel = viewModel,
                                            navController = navController,
                                            settingsKey = key
                                        )
                                    } else {
                                        logcat(LogPriority.ERROR) { "Lockscreen shortcut settings key is not specified." }
                                        navController.popBackStack()
                                    }
                                }

                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    composable(Screen.AndroidAppLinkSettings.route) {
                                        AndroidAppLinkSettings(navController = navController) {
                                            val roleManager = getSystemService<RoleManager>()
                                            if (roleManager?.isRoleHeld(RoleManager.ROLE_BROWSER) == true) {
                                                openDefaultAppsSettings(this@MainActivity)
                                            } else if (roleManager != null) {
                                                val i = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                                                @Suppress("DEPRECATION") // we don't care about results here
                                                startActivityForResult(i, 286444)
                                            }
                                        }
                                    }
                                    composable(Screen.ApprovedLinkTargetList.route) {
                                        LinkTargetList(
                                            approved = true,
                                            mainViewModel = viewModel,
                                            navController = navController
                                        )
                                    }
                                    composable(Screen.UnapprovedLinkTargetList.route) {
                                        LinkTargetList(
                                            approved = false,
                                            mainViewModel = viewModel,
                                            navController = navController
                                        )
                                    }
                                    bottomSheet(Screen.LinkTargetInfoSheet.route) {
                                        // TODO: Remove surface when bottom sheet uses M3 colors
                                        Surface(color = MaterialTheme.colorScheme.surface) {
                                            val packageName = Screen.LinkTargetInfoSheet.getPackageName(it)
                                            LinkTargetInfoSheet(
                                                packageName = packageName,
                                                mainViewModel = viewModel
                                            ) {
                                                openOpenByDefaultSettings(this@MainActivity, packageName)
                                                navController.popBackStack()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getAppBarTitle(navController: NavController, navBackStackEntry: NavBackStackEntry?): String {
    return if (navBackStackEntry?.destination is BottomSheetNavigator.Destination) {
        // Keep previous destination title when showing bottom sheet
        val currentIndex = navController.backQueue.indexOf(navBackStackEntry)
        val prevEntry = navController.backQueue[currentIndex - 1]
        getAppBarTitle(navController = navController, navBackStackEntry = prevEntry)
    } else {
        when (navBackStackEntry?.destination?.route) {
            Screen.AssistantButtonSettings.route -> stringResource(id = R.string.assistant_button_title)
            Screen.AssistantLaunchSelection.route -> stringResource(id = R.string.assistant_launch_selection_title)
            Screen.ReadLogsSetup.route, Screen.SecureSettingsSetup.route -> ""
            Screen.LockscreenShortcutSettings.route -> stringResource(id = R.string.lockscreen_shortcut_title)
            Screen.LockscreenShortcutSelection.route -> {
                when (navBackStackEntry.arguments?.getString("key")) {
                    LOCKSCREEN_RIGHT_BUTTON -> stringResource(id = R.string.lockscreen_shortcut_right)
                    LOCKSCREEN_LEFT_BUTTON -> stringResource(id = R.string.lockscreen_shortcut_left)
                    else -> stringResource(id = R.string.lockscreen_shortcut_title)
                }
            }
            Screen.AndroidAppLinkSettings.route -> stringResource(id = R.string.android_app_link_title)
            Screen.ApprovedLinkTargetList.route -> stringResource(id = R.string.approved_link_target_title)
            Screen.UnapprovedLinkTargetList.route -> stringResource(id = R.string.unapproved_link_target_title)
            else -> stringResource(id = R.string.app_name)
        }
    }
}

@Composable
fun MainActivityActions(
    navController: NavController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPopup by remember { mutableStateOf(false) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val menuItems = mutableListOf<@Composable ColumnScope.() -> Unit>()
    when (navBackStackEntry?.destination?.route) {
        Screen.Home.route -> {
            menuItems += {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.check_for_update),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    },
                    onClick = {
                        context.startActivity(RELEASES_PAGE_INTENT)
                        showPopup = false
                    }
                )
            }
            menuItems += {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.oss_license_title),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    },
                    onClick = {
                        context.startActivity(Intent(context, OssLicensesMenuActivity::class.java))
                        showPopup = false
                    }
                )
            }
        }
        Screen.AssistantLaunchSelection.route -> {
            menuItems += {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.reset_to_default),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    },
                    onClick = {
                        scope.launch {
                            ShoukoApplication.prefs.setAssistButtonAction(null)
                            Toast.makeText(
                                context,
                                context.getString(R.string.assistant_action_reset_toast),
                                Toast.LENGTH_SHORT
                            ).show()
                            showPopup = false
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
        Screen.LockscreenShortcutSelection.route -> {
            menuItems += {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(id = R.string.reset_to_default),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1
                        )
                    },
                    onClick = {
                        val key = navBackStackEntry?.arguments?.getString("key")
                        LockscreenShortcutHelper.getPreferences(context).edit {
                            remove(key)
                        }
                        Settings.Secure.putString(context.contentResolver, key, null)
                        showPopup = false
                        navController.popBackStack()
                    }
                )
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
