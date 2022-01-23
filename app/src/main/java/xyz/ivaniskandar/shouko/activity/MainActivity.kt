package xyz.ivaniskandar.shouko.activity

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import logcat.LogPriority
import logcat.logcat
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper
import xyz.ivaniskandar.shouko.ui.Screen
import xyz.ivaniskandar.shouko.ui.component.InsetAwareTopAppBar
import xyz.ivaniskandar.shouko.ui.destination.AssistantActionSelection
import xyz.ivaniskandar.shouko.ui.destination.AssistantButtonSettings
import xyz.ivaniskandar.shouko.ui.destination.Home
import xyz.ivaniskandar.shouko.ui.destination.LockscreenShortcutSelection
import xyz.ivaniskandar.shouko.ui.destination.LockscreenShortcutSettings
import xyz.ivaniskandar.shouko.ui.destination.PermissionSetup
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme
import xyz.ivaniskandar.shouko.util.Prefs
import xyz.ivaniskandar.shouko.util.RELEASES_PAGE_INTENT
import xyz.ivaniskandar.shouko.util.isRootAvailable
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainActivityViewModel>()
    private val prefs by lazy { Prefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val navController = rememberNavController()
            ShoukoTheme {
                ProvideWindowInsets {
                    Scaffold(
                        topBar = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.destination?.route
                            InsetAwareTopAppBar(
                                title = {
                                    Crossfade(targetState = getAppBarTitle(navController = navController)) {
                                        Text(
                                            text = it,
                                            modifier = Modifier.fillMaxWidth(),
                                            color = MaterialTheme.colors.onBackground,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                },
                                navigationIcon = if (currentRoute != null && currentRoute != Screen.Home.route) {
                                    {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = null)
                                        }
                                    }
                                } else null,
                                actions = { MainActivityActions(prefs = prefs, navController = navController) },
                                backgroundColor = MaterialTheme.colors.background,
                                elevation = 0.dp
                            )
                        },
                    ) {
                        val rootAvailable = remember { isRootAvailable }
                        NavHost(navController = navController, startDestination = Screen.Home.route) {
                            composable(Screen.Home.route) { Home(prefs, navController) }
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
                                AssistantButtonSettings(prefs, navController)
                            }
                            composable(Screen.AssistantLaunchSelection.route) {
                                AssistantActionSelection(viewModel, prefs, navController)
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
                        }
                    }
                }
            }
        }
    }
}

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
                LockscreenShortcutHelper.LOCKSCREEN_RIGHT_BUTTON -> stringResource(id = R.string.lockscreen_shortcut_right)
                LockscreenShortcutHelper.LOCKSCREEN_LEFT_BUTTON -> stringResource(id = R.string.lockscreen_shortcut_left)
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
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
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
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
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
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
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
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
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
