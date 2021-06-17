package xyz.ivaniskandar.shouko.ui

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
}
