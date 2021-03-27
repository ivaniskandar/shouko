package xyz.ivaniskandar.shouko.util

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import xyz.ivaniskandar.shouko.feature.Action

/**
 * Helper for accessing user preferences
 */
class Prefs(context: Context) {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    /**
     * App reference for Assistant button main switch. If false the listener will make
     * sure global settings somc.game_enhancer_gab_key_disabled is 1.
     *
     * @see xyz.ivaniskandar.shouko.feature.GAKeyOverrider
     */
    var assistButtonEnabled: Boolean
        get() = sharedPreferences.getBoolean(ASSIST_BUTTON_ENABLED, true)
        set(value) = sharedPreferences.edit(commit = true) { putBoolean(ASSIST_BUTTON_ENABLED, value) }

    /**
     * Action to run when assistant button is pressed
     *
     * @see xyz.ivaniskandar.shouko.feature.GAKeyOverrider
     * @see xyz.ivaniskandar.shouko.feature.Action
     */
    var assistButtonAction: Action?
        get() = sharedPreferences.getString(ASSIST_BUTTON_ACTION, null)?.let { Action.fromPlainString(it) }
        set(value) = sharedPreferences.edit(commit = true) { putString(ASSIST_BUTTON_ACTION, value?.toPlainString()) }

    /**
     * When true, assistant audio stream will be muted for a brief
     * period of time after the assistant button is pressed
     *
     * @see xyz.ivaniskandar.shouko.feature.GAKeyOverrider
     */
    var hideAssistantCue: Boolean
        get() = sharedPreferences.getBoolean(HIDE_ASSISTANT_CUE, false)
        set(value) = sharedPreferences.edit(commit = true) { putBoolean(HIDE_ASSISTANT_CUE, value) }

    /**
     * @see xyz.ivaniskandar.shouko.feature.PocketNoTouchy
     */
    var preventPocketTouchEnabled: Boolean
        get() = sharedPreferences.getBoolean(PREVENT_POCKET_TOUCH, false)
        set(value) = sharedPreferences.edit(commit = true) { putBoolean(PREVENT_POCKET_TOUCH, value) }

    /**
     * @see xyz.ivaniskandar.shouko.feature.FlipToShush
     */
    var flipToShushEnabled: Boolean
        get() = sharedPreferences.getBoolean(FLIP_TO_SHUSH, false)
        set(value) = sharedPreferences.edit(commit = true) { putBoolean(FLIP_TO_SHUSH, value) }

    /**
     * True when boarding activity for Coffee is showed
     *
     * @see xyz.ivaniskandar.shouko.activity.TileBoardingActivity
     */
    var coffeeBoardingDone: Boolean
        get() = sharedPreferences.getBoolean(COFFEE_BOARDING_DONE, false)
        set(value) = sharedPreferences.edit(commit = true) { putBoolean(COFFEE_BOARDING_DONE, value) }

    /**
     * True when boarding activity for Tea is showed
     *
     * @see xyz.ivaniskandar.shouko.activity.TileBoardingActivity
     */
    var teaBoardingDone: Boolean
        get() = sharedPreferences.getBoolean(TEA_BOARDING_DONE, false)
        set(value) = sharedPreferences.edit(commit = true) { putBoolean(TEA_BOARDING_DONE, value) }

    /**
     * Convenience method for registering preference listener
     */
    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val ASSIST_BUTTON_ENABLED = "assist_button_enabled"
        const val ASSIST_BUTTON_ACTION = "assist_button_action"
        const val HIDE_ASSISTANT_CUE = "hide_assistant_cue"
        const val PREVENT_POCKET_TOUCH = "prevent_pocket_touch"
        const val FLIP_TO_SHUSH = "flip_to_shush"
        private const val COFFEE_BOARDING_DONE = "coffee_boarding_done"
        private const val TEA_BOARDING_DONE = "tea_boarding_done"
    }
}
