package xyz.ivaniskandar.shouko

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import com.topjohnwu.superuser.Shell
import timber.log.Timber
import xyz.ivaniskandar.shouko.service.TeaTileService
import xyz.ivaniskandar.shouko.util.isRootAvailable

class ShoukoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Prepare Shell builder
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR or if (!isRootAvailable) Shell.FLAG_NON_ROOT_SHELL else 0)
                .setTimeout(10)
        )

        // Enable when proximity exists
        val sensorManager = getSystemService(SensorManager::class.java)
        if (sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, TeaTileService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }
}
