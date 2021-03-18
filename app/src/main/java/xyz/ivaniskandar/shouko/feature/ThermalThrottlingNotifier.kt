package xyz.ivaniskandar.shouko.feature

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.PowerManager
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import timber.log.Timber
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.theme.ShoukoAccent

// TODO switch to disable feature
class ThermalThrottlingNotifier(
    lifecycleOwner: LifecycleOwner,
    private val context: Context,
) : LifecycleObserver {
    private val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
        Timber.d("Device thermal status changed: $status")
        val notification = when (status) {
            PowerManager.THERMAL_STATUS_LIGHT -> {
                createLightNotification()
            }
            PowerManager.THERMAL_STATUS_MODERATE,
            PowerManager.THERMAL_STATUS_SEVERE,
            PowerManager.THERMAL_STATUS_CRITICAL -> {
                createNotification(context.getString(R.string.system_throttling_notif_text_moderate))
            }
            PowerManager.THERMAL_STATUS_EMERGENCY -> {
                createNotification(context.getString(R.string.system_throttling_notif_text_emergency))
            }
            PowerManager.THERMAL_STATUS_SHUTDOWN -> {
                createNotification(context.getString(R.string.system_throttling_notif_text_shutdown))
            }
            else -> null // No throttling
        }
        
        val notificationManager = NotificationManager.from(context)
        if (notification != null) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } else {
            notificationManager.cancel(NOTIFICATION_ID)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun start() {
        context.getSystemService(NotificationManager::class.java)!!.run {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                context.getString(R.string.system_throttling_notif_channel_title),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setSound(null, null)
                setShowBadge(false)
                enableLights(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            createNotificationChannel(channel)
        }
        Timber.d("Registering thermal status listener")
        context.getSystemService(PowerManager::class.java).addThermalStatusListener(thermalListener)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun stop() {
        Timber.d("Unregistering thermal status listener and cancelling ongoing notification")
        context.getSystemService(PowerManager::class.java).removeThermalStatusListener(thermalListener)
        NotificationManager.from(context).cancel(NOTIFICATION_ID)
    }

    private fun createLightNotification(): Notification {
        val text = context.getString(R.string.system_throttling_notif_text_light)
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_temp)
            .setColor(ShoukoAccent.toArgb())
            .setContentTitle(context.getString(R.string.system_throttling_notif_title_light))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_temp)
            .setColor(context.getColor(R.color.error))
            .setContentTitle(context.getString(R.string.system_throttling_notif_title))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .build()
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "thermal_notifier"
        private const val NOTIFICATION_ID = 14000
    }
}
