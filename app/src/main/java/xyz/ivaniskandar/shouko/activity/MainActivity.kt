package xyz.ivaniskandar.shouko.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.navigation.compose.*
import dev.chrisbanes.accompanist.insets.ProvideWindowInsets
import dev.chrisbanes.accompanist.insets.navigationBarsPadding
import dev.chrisbanes.accompanist.insets.statusBarsPadding
import xyz.ivaniskandar.shouko.ui.*
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme
import xyz.ivaniskandar.shouko.util.GITHUB_REPO_INTENT
import xyz.ivaniskandar.shouko.util.Prefs
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private val viewModel by viewModels<MainActivityViewModel>()
    private val prefs by lazy { Prefs(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val navController = rememberNavController()
            var showAssistantActionSettings by rememberSaveable { mutableStateOf(false) }
            ShoukoTheme {
                ProvideWindowInsets {
                    Scaffold(
                        topBar = {
                            val navBackStackEntry by navController.currentBackStackEntryAsState()
                            val currentRoute = navBackStackEntry?.arguments?.getString(KEY_ROUTE)
                            InsetAwareTopAppBar(
                                title = {
                                    Text(
                                        text = getAppBarTitle(currentRoute),
                                        color = MaterialTheme.colors.onBackground,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                },
                                navigationIcon = if (currentRoute != ROUTE_HOME) {
                                    {
                                        IconButton(onClick = { navController.popBackStack() }) {
                                            Icon(imageVector = Icons.Rounded.ArrowBack, contentDescription = null)
                                        }
                                    }
                                } else null,
                                actions = {
                                    when (currentRoute) {
                                        ROUTE_HOME -> {
                                            IconButton(onClick = { startActivity(GITHUB_REPO_INTENT) }) {
                                                Icon(imageVector = Icons.Rounded.Info, contentDescription = null)
                                            }
                                        }
                                        ROUTE_ASSISTANT_LAUNCH_SELECTION -> {
                                            IconButton(onClick = { showAssistantActionSettings = true }) {
                                                Icon(imageVector = Icons.Rounded.Settings, contentDescription = null)
                                            }
                                        }
                                    }
                                },
                                backgroundColor = MaterialTheme.colors.background,
                                elevation = 0.dp
                            )
                        },
                    ) {
                        NavHost(navController, startDestination = ROUTE_HOME) {
                            composable(ROUTE_HOME) { Home(prefs, navController) }
                            composable(ROUTE_READ_LOGS_PERMISSION_SETUP) {
                                ReadLogsPermissionSetup {
                                    finishAffinity()
                                    startActivity(intent)
                                    exitProcess(0)
                                }
                            }
                            composable(ROUTE_ASSISTANT_LAUNCH_SELECTION) {
                                AssistantActionSelection(viewModel, prefs, navController, showAssistantActionSettings) {
                                    showAssistantActionSettings = false
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
