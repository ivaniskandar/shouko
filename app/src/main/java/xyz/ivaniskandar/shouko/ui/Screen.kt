package xyz.ivaniskandar.shouko.ui

import androidx.compose.runtime.Immutable
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Immutable
@Serializable
sealed interface Screen : NavKey {
    @Serializable data object Home : Screen

    @Serializable data object ReadLogsSetup : Screen

    @Serializable data object SecureSettingsSetup : Screen

    @Serializable data object AssistantButtonSettings : Screen

    @Serializable data object AssistantLaunchSelection : Screen

    @Serializable data object LockscreenShortcutSettings : Screen

    @Serializable data class LockscreenShortcutSelection(val key: String) : Screen

    @Serializable data object AndroidAppLinkSettings : Screen

    @Serializable data object ApprovedLinkTargetList : Screen

    @Serializable data object UnapprovedLinkTargetList : Screen

    @Serializable data class LinkTargetInfoSheet(val packageName: String) : Screen
}
