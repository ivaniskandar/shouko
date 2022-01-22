package xyz.ivaniskandar.shouko

import android.app.Application
import android.content.ComponentName
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.kieronquinn.monetcompat.core.MonetCompat
import com.kieronquinn.monetcompat.interfaces.MonetColorsChangedListener
import com.topjohnwu.superuser.Shell
import dev.kdrag0n.monet.theme.ColorScheme
import logcat.AndroidLogcatLogger
import logcat.LogPriority
import xyz.ivaniskandar.shouko.service.TeaTileService
import xyz.ivaniskandar.shouko.util.isRootAvailable

class ShoukoApplication : Application(), LifecycleObserver, MonetColorsChangedListener {

    override fun onCreate() {
        super.onCreate()
        AndroidLogcatLogger.installOnDebuggableApp(this, minPriority = LogPriority.VERBOSE)

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
    }

    override fun onMonetColorsChanged(monet: MonetCompat, monetColors: ColorScheme, isInitialChange: Boolean) {
        ShoukoApplication.monetColors = monetColors
    }

    init {
        // Setup MonetCompat
        ProcessLifecycleOwner.get().lifecycleScope.launchWhenCreated {
            MonetCompat.setup(this@ShoukoApplication).apply {
                addMonetColorsChangedListener(this@ShoukoApplication)
                updateMonetColors()
            }
        }
    }

    companion object {
        var monetColors: ColorScheme? by mutableStateOf(null)
    }
}
