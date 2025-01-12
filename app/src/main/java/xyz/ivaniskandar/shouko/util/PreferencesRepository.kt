package xyz.ivaniskandar.shouko.util

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import logcat.LogPriority
import logcat.logcat
import xyz.ivaniskandar.shouko.Preferences
import xyz.ivaniskandar.shouko.feature.Action
import xyz.ivaniskandar.shouko.feature.LockscreenShortcutHelper
import java.io.IOException

class PreferencesRepository(
    private val preferencesStore: DataStore<Preferences>,
) {
    private val preferencesFlow: Flow<Preferences> =
        preferencesStore.data
            .catch { exception ->
                if (exception is IOException) {
                    logcat(priority = LogPriority.ERROR) { "Error reading preferences." }
                    emit(Preferences.getDefaultInstance())
                } else {
                    throw exception
                }
            }

    val assistButtonFlow: Flow<AssistButtonPrefs> =
        preferencesFlow.map {
            AssistButtonPrefs(
                it.assistButtonEnabled,
                Action.fromPlainString(it.assistButtonAction),
                it.hideAssistantCue,
            )
        }

    val preventPocketTouchEnabledFlow: Flow<Boolean> = preferencesFlow.map { it.preventPocketTouchEnabled }
    val flipToShushEnabledFlow: Flow<Boolean> = preferencesFlow.map { it.flipToShushEnabled }

    val coffeeBoardingDone: Flow<Boolean> = preferencesFlow.map { it.coffeeBoardingDone }
    val teaBoardingDone: Flow<Boolean> = preferencesFlow.map { it.teaBoardingDone }

    val lockscreenLeftAction: Flow<String?> =
        preferencesFlow.map {
            it.lockscreenLeftAction.takeIf { action -> action.isNotEmpty() }
        }
    val lockscreenRightAction: Flow<String?> =
        preferencesFlow.map {
            it.lockscreenRightAction.takeIf { action -> action.isNotEmpty() }
        }

    suspend fun setLockscreenAction(
        key: String,
        value: String?,
    ) {
        val newValue = value ?: ""
        preferencesStore.updateDataSilently {
            it
                .toBuilder()
                .apply {
                    when (key) {
                        LockscreenShortcutHelper.LOCKSCREEN_LEFT_BUTTON -> lockscreenLeftAction = newValue
                        LockscreenShortcutHelper.LOCKSCREEN_RIGHT_BUTTON -> lockscreenRightAction = newValue
                    }
                }.build()
        }
    }

    /**
     * App reference for Assistant button main switch. If false the listener will make
     * sure global settings somc.game_enhancer_gab_key_disabled is 1.
     *
     * @see xyz.ivaniskandar.shouko.feature.GAKeyOverrider
     * @see assistButtonFlow
     */
    suspend fun setAssistButtonEnabled(enabled: Boolean) {
        preferencesStore.updateDataSilently {
            it.toBuilder().setAssistButtonEnabled(enabled).build()
        }
    }

    /**
     * Action to run when assistant button is pressed
     *
     * @see xyz.ivaniskandar.shouko.feature.GAKeyOverrider
     * @see xyz.ivaniskandar.shouko.feature.Action
     * @see assistButtonFlow
     */
    suspend fun setAssistButtonAction(action: Action?) {
        preferencesStore.updateDataSilently {
            it.toBuilder().setAssistButtonAction(action?.toPlainString() ?: "").build()
        }
    }

    /**
     * When true, assistant audio stream will be muted for a brief
     * period of time after the assistant button is pressed
     *
     * @see xyz.ivaniskandar.shouko.feature.GAKeyOverrider
     * @see assistButtonFlow
     */
    suspend fun setHideAssistantCue(enabled: Boolean) {
        preferencesStore.updateDataSilently {
            it.toBuilder().setHideAssistantCue(enabled).build()
        }
    }

    /**
     * @see xyz.ivaniskandar.shouko.feature.PocketNoTouchy
     * @see preventPocketTouchEnabledFlow
     */
    suspend fun setPreventPocketTouchEnabled(enabled: Boolean) {
        preferencesStore.updateDataSilently {
            it.toBuilder().setPreventPocketTouchEnabled(enabled).build()
        }
    }

    /**
     * @see xyz.ivaniskandar.shouko.feature.FlipToShush
     * @see flipToShushEnabledFlow
     */
    suspend fun setFlipToShushEnabled(enabled: Boolean) {
        preferencesStore.updateDataSilently {
            it.toBuilder().setFlipToShushEnabled(enabled).build()
        }
    }

    /**
     * True when boarding activity for Coffee is showed
     *
     * @see xyz.ivaniskandar.shouko.activity.TileBoardingActivity
     * @see coffeeBoardingDone
     */
    suspend fun setCoffeeBoardingDone() {
        preferencesStore.updateDataSilently {
            it.toBuilder().setCoffeeBoardingDone(true).build()
        }
    }

    /**
     * True when boarding activity for Tea is showed
     *
     * @see xyz.ivaniskandar.shouko.activity.TileBoardingActivity
     * @see teaBoardingDone
     */
    suspend fun setTeaBoardingDone() {
        preferencesStore.updateDataSilently {
            it.toBuilder().setTeaBoardingDone(true).build()
        }
    }

    /**
     * Catch any [IOException] thrown when updating data
     */
    private suspend fun DataStore<Preferences>.updateDataSilently(t: (Preferences) -> Preferences) {
        try {
            preferencesStore.updateData(t)
        } catch (e: IOException) {
            logcat(priority = LogPriority.ERROR) { "Error writing preferences." }
        }
    }
}

data class AssistButtonPrefs(
    val enabled: Boolean = false,
    val action: Action? = null,
    val hideAssistantCue: Boolean = false,
)
