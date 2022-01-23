package xyz.ivaniskandar.shouko.ui.destination

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import androidx.navigation.NavController
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.feature.FlipToShush
import xyz.ivaniskandar.shouko.feature.GAKeyOverrider
import xyz.ivaniskandar.shouko.service.TadanoAccessibilityService
import xyz.ivaniskandar.shouko.ui.Screen
import xyz.ivaniskandar.shouko.ui.component.AccessibilityServiceCard
import xyz.ivaniskandar.shouko.ui.component.Preference
import xyz.ivaniskandar.shouko.ui.component.SwitchPreference
import xyz.ivaniskandar.shouko.util.Prefs
import xyz.ivaniskandar.shouko.util.highlightSettingsTo

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
            val dndAccessCheck = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
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
