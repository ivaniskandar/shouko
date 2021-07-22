package xyz.ivaniskandar.shouko

import android.app.Application
import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.topjohnwu.superuser.Shell
import timber.log.Timber
import xyz.ivaniskandar.shouko.service.TeaTileService
import xyz.ivaniskandar.shouko.util.isRootAvailable

class ShoukoApplication : Application(), LifecycleObserver {
    private val wallpaperColorListener = WallpaperManager.OnColorsChangedListener { colors, which ->
        if (which == WallpaperManager.FLAG_SYSTEM) {
            wallpaperColors = colors
        }
    }

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
        if (getSystemService<SensorManager>()?.getDefaultSensor(Sensor.TYPE_PROXIMITY) != null) {
            packageManager.setComponentEnabledSetting(
                ComponentName(this, TeaTileService::class.java),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            ProcessLifecycleOwner.get().lifecycleScope.launchWhenCreated {
                // Prepare wallpaper colors
                WallpaperManager.getInstance(this@ShoukoApplication).apply {
                    addOnColorsChangedListener(wallpaperColorListener, Handler(Looper.getMainLooper()))
                    wallpaperColors = getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
                }
            }
        }
    }

    companion object {
        var wallpaperColors: WallpaperColors? by mutableStateOf(null)
            private set
    }
}
