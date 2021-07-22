package xyz.ivaniskandar.shouko.activity

import android.Manifest
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.primarySurface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsPadding
import com.google.accompanist.insets.statusBarsPadding
import timber.log.Timber
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.AssistantActionSelection
import xyz.ivaniskandar.shouko.ui.AssistantButtonSettings
import xyz.ivaniskandar.shouko.ui.Home
import xyz.ivaniskandar.shouko.ui.LockscreenShortcutSelection
import xyz.ivaniskandar.shouko.ui.LockscreenShortcutSettings
import xyz.ivaniskandar.shouko.ui.MainActivityActions
import xyz.ivaniskandar.shouko.ui.PermissionSetup
import xyz.ivaniskandar.shouko.ui.Screen
import xyz.ivaniskandar.shouko.ui.getAppBarTitle
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme
import xyz.ivaniskandar.shouko.util.Prefs
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
                                navigationIcon = if (currentRoute != Screen.Home.route) {
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
                                    Timber.e("Lockscreen shortcut settings key is not specified.")
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

/**
 * A wrapper around [TopAppBar] which uses [Modifier.statusBarsPadding] to shift the app bar's
 * contents down, but still draws the background behind the status bar too.
 */
@Composable
fun InsetAwareTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    backgroundColor: Color = MaterialTheme.colors.primarySurface,
    contentColor: Color = contentColorFor(backgroundColor),
    elevation: Dp = 4.dp
) {
    Surface(
        color = backgroundColor,
        elevation = elevation,
        modifier = modifier
    ) {
        TopAppBar(
            modifier = Modifier
                .statusBarsPadding()
                .navigationBarsPadding(bottom = false),
            backgroundColor = Color.Transparent,
            contentColor = contentColor,
            elevation = 0.dp,
            contentPadding = AppBarDefaults.ContentPadding,
        ) {
            if (navigationIcon == null) {
                Spacer(TitleIconModifier)
            } else {
                Row(TitleIconModifier, verticalAlignment = Alignment.CenterVertically) {
                    CompositionLocalProvider(
                        LocalContentAlpha provides ContentAlpha.high,
                        content = navigationIcon
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxHeight()
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                ProvideTextStyle(value = MaterialTheme.typography.h6) {
                    CompositionLocalProvider(
                        LocalContentAlpha provides ContentAlpha.high,
                        content = title
                    )
                }
            }

            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
                Row(
                    Modifier
                        .fillMaxHeight()
                        .width(72.dp - AppBarHorizontalPadding),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

val AppBarHorizontalPadding = 4.dp
val TitleIconModifier = Modifier
    .fillMaxHeight()
    .width(72.dp - AppBarHorizontalPadding)
