package xyz.ivaniskandar.shouko.service

import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import android.service.quicksettings.TileService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import xyz.ivaniskandar.shouko.ShoukoApplication
import xyz.ivaniskandar.shouko.activity.TileBoardingActivity
import xyz.ivaniskandar.shouko.service.TadanoTileParentService.Companion.ACTION_START_SERVICE
import xyz.ivaniskandar.shouko.service.TadanoTileParentService.Companion.ACTION_STOP_SERVICE
import xyz.ivaniskandar.shouko.service.TadanoTileParentService.Companion.EXTRA_SERVICE_TYPE

@DelicateCoroutinesApi
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

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun switchService(start: Boolean) {
        val serviceIntent = Intent(this, TadanoTileParentService::class.java)
        if (start) {
            GlobalScope.launch {
                val prefs = ShoukoApplication.prefs
                val boardingDone = runBlocking { prefs.teaBoardingDone.first() }
                if (!boardingDone) {
                    startActivityAndCollapse(
                        Intent(this@TeaTileService, TileBoardingActivity::class.java).apply {
                            putExtra(EXTRA_SERVICE_TYPE, TadanoTileParentService.Type.TEA)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                    prefs.setTeaBoardingDone()
                }
                startForegroundService(
                    serviceIntent.apply {
                        action = ACTION_START_SERVICE
                        putExtra(EXTRA_SERVICE_TYPE, TadanoTileParentService.Type.TEA)
                    }
                )
            }
        } else {
            startService(
                serviceIntent.apply {
                    action = ACTION_STOP_SERVICE
                }
            )
        }
    }
}
