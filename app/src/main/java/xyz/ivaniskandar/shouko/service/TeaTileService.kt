package xyz.ivaniskandar.shouko.service

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import android.service.quicksettings.TileService
import androidx.core.service.quicksettings.PendingIntentActivityWrapper
import androidx.core.service.quicksettings.TileServiceCompat
import xyz.ivaniskandar.shouko.activity.TileBoardingActivity
import xyz.ivaniskandar.shouko.service.TadanoTileParentService.Companion.ACTION_STOP_SERVICE
import xyz.ivaniskandar.shouko.service.TadanoTileParentService.Companion.EXTRA_SERVICE_TYPE

class TeaTileService : TileService() {

    override fun onBind(intent: Intent?): IBinder? {
        requestListeningState(this, ComponentName(this, TeaTileService::class.java))
        return super.onBind(intent)
    }

    override fun onStartListening() {
        super.onStartListening()
        updateQsState()
    }

    override fun onClick() {
        super.onClick()
        with(qsTile) {
            when (state) {
                STATE_ACTIVE -> {
                    switchService(false)
                }
                STATE_INACTIVE -> {
                    switchService(true)
                }
            }
        }
    }

    private fun updateQsState() {
        with(qsTile) {
            state = if (TadanoTileParentService.isTeaActive) {
                STATE_ACTIVE
            } else {
                STATE_INACTIVE
            }
            updateTile()
        }
    }

    private fun switchService(start: Boolean) {
        if (start) {
            val i = Intent(this, TileBoardingActivity::class.java).apply {
                putExtra(EXTRA_SERVICE_TYPE, TadanoTileParentService.Type.TEA)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pendingIntent = PendingIntentActivityWrapper(this, 42, i, PendingIntent.FLAG_UPDATE_CURRENT, false)
            TileServiceCompat.startActivityAndCollapse(this, pendingIntent)
        } else {
            startService(
                Intent(this, TadanoTileParentService::class.java).apply {
                    action = ACTION_STOP_SERVICE
                },
            )
        }
    }
}
