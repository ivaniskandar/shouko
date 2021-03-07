package xyz.ivaniskandar.shouko.activity

import android.Manifest.permission.READ_LOGS
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.view.WindowCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import dev.chrisbanes.accompanist.insets.*
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.feature.*
import xyz.ivaniskandar.shouko.service.TadanoAccessibilityService
import xyz.ivaniskandar.shouko.ui.*
import xyz.ivaniskandar.shouko.ui.theme.ShoukoTheme
import xyz.ivaniskandar.shouko.util.GITHUB_REPO_INTENT
import xyz.ivaniskandar.shouko.util.Prefs
import xyz.ivaniskandar.shouko.util.setAsAssistantAction
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
                                            Icon(
                                                imageVector = Icons.Rounded.ArrowBack,
                                                contentDescription = null
                                            )
                                        }
                                    }
                                } else null,
                                actions = {
                                    when (currentRoute) {
                                        ROUTE_HOME -> {
                                            IconButton(onClick = { startActivity(GITHUB_REPO_INTENT) }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Info,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                        ROUTE_ASSISTANT_LAUNCH_SELECTION -> {
                                            IconButton(onClick = {
                                                showAssistantActionSettings = true
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Settings,
                                                    contentDescription = null
                                                )
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
                            composable(ROUTE_HOME) { Home(navController) }
                            composable(ROUTE_READ_LOGS_PERMISSION_SETUP) {
                                ReadLogsPermissionSetup {
                                    finishAffinity()
                                    startActivity(intent)
                                    exitProcess(0)
                                }
                            }
                            composable(ROUTE_ASSISTANT_LAUNCH_SELECTION) {
                                AssistantActionSelection(
                                    navController,
                                    showAssistantActionSettings
                                ) {
                                    showAssistantActionSettings = false
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Home(navController: NavController) {
        val context = LocalContext.current
        LazyColumn(contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()) {
            item {
                ReadLogsCard(
                    visible = GAKeyOverrider.isSupported && context.checkSelfPermission(READ_LOGS) != PERMISSION_GRANTED
                ) {
                    navController.navigate(ROUTE_READ_LOGS_PERMISSION_SETUP)
                }
            }
            item {
                AccessibilityServiceCard(visible = !TadanoAccessibilityService.isActive) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                }
            }
            item {
                Preference(
                    title = stringResource(id = R.string.assistant_launch_selection_title),
                    subtitle = prefs.assistButtonAction?.getLabel(context) ?: stringResource(
                        id = if (GAKeyOverrider.isSupported) {
                            R.string.assistant_action_select_default_value
                        } else {
                            R.string.assistant_action_not_supported
                        }
                    ),
                    enabled = GAKeyOverrider.isSupported && TadanoAccessibilityService.isActive &&
                            context.checkSelfPermission(READ_LOGS) == PERMISSION_GRANTED
                ) {
                    navController.navigate(ROUTE_ASSISTANT_LAUNCH_SELECTION)
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
                        val isGrantedDndAccess =
                            context.getSystemService(NotificationManager::class.java)!!
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
                            val isIgnoringOptimizations =
                                context.getSystemService(PowerManager::class.java)!!
                                    .isIgnoringBatteryOptimizations(context.packageName)
                            if (!isIgnoringOptimizations) {
                                @SuppressLint("BatteryLife")
                                val i =
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
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
    fun AssistantActionSelection(
        navController: NavController,
        showSettingsDialog: Boolean,
        onSettingsDialogDismissRequest: () -> Unit
    ) {
        var selectedTabIndex by remember { mutableStateOf(0) }
        val titles = listOf(
            R.string.tab_title_apps,
            R.string.tab_title_shortcuts,
            R.string.tab_title_media_key
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
                            Text(text = stringResource(id = title))
                        },
                        unselectedContentColor = MaterialTheme.colors.onBackground.copy(alpha = ContentAlpha.medium)
                    )
                }
            }
            when (selectedTabIndex) {
                0 -> {
                    val appItems by viewModel.appsList.observeAsState()
                    if (appItems == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Column {
                            NothingRow(onClick = {
                                prefs.assistButtonAction = NothingAction()
                                navController.popBackStack()
                            })
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
                }
                1 -> {
                    val appItems by viewModel.shortcutList.observeAsState()
                    if (appItems == null) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val context = LocalContext.current
                        val createShortcut = androidx.activity.compose.registerForActivityResult(
                            contract = ActivityResultContracts.StartActivityForResult(),
                            onResult = {
                                if (it.resultCode == Activity.RESULT_OK) {
                                    val intent = it.data
                                    if (intent != null) {
                                        intent.setAsAssistantAction(prefs)
                                    } else {
                                        Toast.makeText(
                                            context,
                                            getString(R.string.assistant_action_save_failed_toast),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    navController.popBackStack()
                                }
                            }
                        )
                        Column {
                            NothingRow(onClick = {
                                prefs.assistButtonAction = NothingAction()
                                navController.popBackStack()
                            })
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
                }
                2 -> {
                    LazyColumn(contentPadding = LocalWindowInsets.current.navigationBars.toPaddingValues()) {
                        items(MediaKeyAction.Key.values()) { item ->
                            MediaKeyRow(key = item) {
                                prefs.assistButtonAction = it
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }

        if (showSettingsDialog) {
            AssistantActionSettingsDialog(onDismissRequest = onSettingsDialogDismissRequest)
        }
    }

    @Composable
    fun AssistantActionSettingsDialog(onDismissRequest: () -> Unit) {
        Dialog(onDismissRequest = onDismissRequest) {
            Surface(shape = MaterialTheme.shapes.medium, elevation = 24.dp) {
                LazyColumn(contentPadding = PaddingValues(12.dp)) {
                    item {
                        var hideAssistantCue by remember { mutableStateOf(prefs.hideAssistantCue) }
                        SwitchPreference(
                            title = stringResource(R.string.hide_assistant_cue_title),
                            subtitle = stringResource(R.string.hide_assistant_cue_desc),
                            checked = hideAssistantCue,
                            onCheckedChanged = {
                                prefs.hideAssistantCue = it
                                hideAssistantCue = it
                            }
                        )
                    }
                    item {
                        val context = LocalContext.current
                        Preference(
                            title = stringResource(R.string.assistant_action_reset),
                            onPreferenceClick = {
                                prefs.assistButtonAction = null
                                Toast.makeText(
                                    context,
                                    getString(R.string.assistant_action_reset_toast),
                                    Toast.LENGTH_SHORT
                                ).show()
                                onDismissRequest()
                            }
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun getAppBarTitle(currentRoute: String?): String {
        return when (currentRoute) {
            ROUTE_ASSISTANT_LAUNCH_SELECTION -> stringResource(id = R.string.assistant_launch_selection_title)
            ROUTE_READ_LOGS_PERMISSION_SETUP -> ""
            else -> stringResource(id = R.string.app_name)
        }
    }


    companion object {
        private const val ROUTE_HOME = "home"
        private const val ROUTE_READ_LOGS_PERMISSION_SETUP = "read_logs_permission_setup"
        private const val ROUTE_ASSISTANT_LAUNCH_SELECTION = "assistant_launch_selection"
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
