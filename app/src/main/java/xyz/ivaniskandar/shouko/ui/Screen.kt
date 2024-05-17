package xyz.ivaniskandar.shouko.ui

import androidx.navigation.NavBackStackEntry

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object ReadLogsSetup : Screen("read_logs_setup")
    data object SecureSettingsSetup : Screen("secure_settings_setup")
    data object AssistantButtonSettings : Screen("assistant_button_settings")
    data object AssistantLaunchSelection : Screen("assistant_launch_selection")
    data object LockscreenShortcutSettings : Screen("lockscreen_shortcut_settings")
    data object LockscreenShortcutSelection : Screen("lockscreen_shortcut_selection/{key}") {
        fun createRoute(key: String) = "lockscreen_shortcut_selection/$key"
    }
    data object AndroidAppLinkSettings : Screen("android_app_link_settings")
    data object ApprovedLinkTargetList : Screen("approved_link_target_list")
    data object UnapprovedLinkTargetList : Screen("unapproved_link_target_list")
    data object LinkTargetInfoSheet : Screen("link_target_info_sheet/{packageName}") {
        fun createRoute(packageName: String) = "link_target_info_sheet/$packageName"

        fun getPackageName(backStackEntry: NavBackStackEntry): String {
            return backStackEntry.arguments!!.getString("packageName")!!
        }
    }
}
