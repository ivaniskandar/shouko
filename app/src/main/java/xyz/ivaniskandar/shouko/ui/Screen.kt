package xyz.ivaniskandar.shouko.ui

import androidx.navigation.NavBackStackEntry

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ReadLogsSetup : Screen("read_logs_setup")
    object SecureSettingsSetup : Screen("secure_settings_setup")
    object AssistantButtonSettings : Screen("assistant_button_settings")
    object AssistantLaunchSelection : Screen("assistant_launch_selection")
    object LockscreenShortcutSettings : Screen("lockscreen_shortcut_settings")
    object LockscreenShortcutSelection : Screen("lockscreen_shortcut_selection/{key}") {
        fun createRoute(key: String) = "lockscreen_shortcut_selection/$key"
    }
    object AndroidAppLinkSettings : Screen("android_app_link_settings")
    object ApprovedLinkTargetList : Screen("approved_link_target_list")
    object UnapprovedLinkTargetList : Screen("unapproved_link_target_list")
    object LinkTargetInfoSheet : Screen("link_target_info_sheet/{packageName}") {
        fun createRoute(packageName: String) = "link_target_info_sheet/$packageName"

        fun getPackageName(backStackEntry: NavBackStackEntry): String {
            return backStackEntry.arguments!!.getString("packageName")!!
        }
    }
}
