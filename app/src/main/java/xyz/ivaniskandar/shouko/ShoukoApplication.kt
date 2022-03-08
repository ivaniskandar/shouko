package xyz.ivaniskandar.shouko

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import androidx.core.content.getSystemService
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import androidx.lifecycle.LifecycleObserver
import androidx.preference.PreferenceManager
import com.kieronquinn.monetcompat.core.MonetCompat
import com.topjohnwu.superuser.Shell
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import xyz.ivaniskandar.shouko.activity.LinkTargetChooserActivity
import xyz.ivaniskandar.shouko.service.TeaTileService
import xyz.ivaniskandar.shouko.util.PreferencesRepository
import xyz.ivaniskandar.shouko.util.PreferencesSerializer
import xyz.ivaniskandar.shouko.util.isRootAvailable

private val Context.preferencesStore: DataStore<Preferences> by dataStore(
    fileName = "preferences.pb",
    serializer = PreferencesSerializer(),
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                produceSharedPreferences = { PreferenceManager.getDefaultSharedPreferences(context) }
            ) { sharedPrefs: SharedPreferencesView, currentData: Preferences ->
                val builder = currentData.toBuilder()
                if (sharedPrefs.contains("assist_button_enabled")) {
                    builder.assistButtonEnabled = sharedPrefs.getBoolean("assist_button_enabled", true)
                }
                if (sharedPrefs.contains("assist_button_action")) {
                    builder.assistButtonAction = sharedPrefs.getString("assist_button_action", "")
                }
                if (sharedPrefs.contains("hide_assistant_cue")) {
                    builder.hideAssistantCue = sharedPrefs.getBoolean("hide_assistant_cue", false)
                }
                if (sharedPrefs.contains("prevent_pocket_touch")) {
                    builder.preventPocketTouchEnabled = sharedPrefs.getBoolean("prevent_pocket_touch", false)
                }
                if (sharedPrefs.contains("flip_to_shush")) {
                    builder.flipToShushEnabled = sharedPrefs.getBoolean("flip_to_shush", false)
                }
                if (sharedPrefs.contains("coffee_boarding_done")) {
                    builder.coffeeBoardingDone = sharedPrefs.getBoolean("coffee_boarding_done", false)
                }
                if (sharedPrefs.contains("tea_boarding_done")) {
                    builder.teaBoardingDone = sharedPrefs.getBoolean("tea_boarding_done", false)
                }
                builder.build()
            }
        )
    }
)

class ShoukoApplication : Application(), LifecycleObserver {

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

        // Setup monet
        MonetCompat.setup(this).apply {
            updateMonetColors()
        }

        // Prepare Shell builder
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR or if (!isRootAvailable) Shell.FLAG_NON_ROOT_SHELL else 0)
                .setTimeout(10)
        )

        // Enable when proximity exists
        if (getSystemService<SensorManager>()?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, TeaTileService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Enable app link chooser on S
        val state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        packageManager.setComponentEnabledSetting(
            ComponentName(this, LinkTargetChooserActivity::class.java),
            state,
            PackageManager.DONT_KILL_APP
        )

        // Init preferences
        prefs = PreferencesRepository(preferencesStore)
    }

    companion object {
        lateinit var prefs: PreferencesRepository
            private set
    }
}
