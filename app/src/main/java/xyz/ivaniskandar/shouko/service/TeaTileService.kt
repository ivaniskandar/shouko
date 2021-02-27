package xyz.ivaniskandar.shouko.service

import android.content.Intent
import android.service.quicksettings.Tile.STATE_ACTIVE
import android.service.quicksettings.Tile.STATE_INACTIVE
import android.service.quicksettings.TileService
import xyz.ivaniskandar.shouko.activity.TileBoardingActivity
import xyz.ivaniskandar.shouko.service.TadanoTileParentService.Companion.ACTION_START_SERVICE
import xyz.ivaniskandar.shouko.service.TadanoTileParentService.Companion.ACTION_STOP_SERVICE
import xyz.ivaniskandar.shouko.service.TadanoTileParentService.Companion.EXTRA_SERVICE_TYPE
import xyz.ivaniskandar.shouko.util.Prefs

class TeaTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateQsState()
    }

    override fun onStopListening() {
        super.onStopListening()
        updateQsState()
    }

    override fun onClick() {
        super.onClick()
        with(qsTile) {
            when (state) {
                STATE_ACTIVE -> {
                    state = STATE_INACTIVE
                    switchService(false)
                }
                STATE_INACTIVE -> {
                    state = STATE_ACTIVE
                    switchService(true)
                }
            }
            updateTile()
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
        val serviceIntent = Intent(this, TadanoTileParentService::class.java)
        if (start) {
            val prefs = Prefs(this)
            if (!prefs.teaBoardingDone) {
                startActivityAndCollapse(
                    Intent(this, TileBoardingActivity::class.java).apply {
                        putExtra(EXTRA_SERVICE_TYPE, TadanoTileParentService.Type.TEA)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                prefs.teaBoardingDone = true
            }
            startForegroundService(
                serviceIntent.apply {
                    action = ACTION_START_SERVICE
                    putExtra(EXTRA_SERVICE_TYPE, TadanoTileParentService.Type.TEA)
                }
            )
        } else {
            startService(
                serviceIntent.apply {
                    action = ACTION_STOP_SERVICE
                }
            )
        }
    }
}
