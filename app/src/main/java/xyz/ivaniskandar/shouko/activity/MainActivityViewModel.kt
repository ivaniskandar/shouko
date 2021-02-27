package xyz.ivaniskandar.shouko.activity

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import xyz.ivaniskandar.shouko.item.ApplicationItem
import xyz.ivaniskandar.shouko.item.ShortcutCreatorItem
import xyz.ivaniskandar.shouko.ui.IconDrawableShadowWrapper

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    /**
     * LiveData of list containing available launcher intents as [ApplicationItem]
     *
     * @see ApplicationItem
     */
    val appsList = liveData(viewModelScope.coroutineContext + Dispatchers.Default) {
        val pm = getApplication<Application>().packageManager
        val i = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val lst = pm.queryIntentActivities(i, 0)
        val shadowWrapper = IconDrawableShadowWrapper()
        emit(
            lst.map {
                val cn = ComponentName(
                    it.activityInfo.packageName,
                    it.activityInfo.name
                )
                ApplicationItem(
                    cn,
                    it.activityInfo.loadLabel(pm).toString(),
                    shadowWrapper.run(it.activityInfo.loadIcon(pm)).toBitmap().asImageBitmap(),
                )
            }.sortedBy { it.label }
        )
    }

    /**
     * LiveData of list containing available intents to create an app shortcut as [ShortcutCreatorItem]
     *
     * @see ShortcutCreatorItem
     */
    val shortcutList = liveData(viewModelScope.coroutineContext + Dispatchers.Default) {
        val pm = getApplication<Application>().packageManager
        val i = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        val lst = pm.queryIntentActivities(i, 0)
        val shadowWrapper = IconDrawableShadowWrapper()
        emit(
            lst.map {
                ShortcutCreatorItem(
                    ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                    it.activityInfo.loadLabel(pm).toString(),
                    shadowWrapper.run(it.activityInfo.loadIcon(pm)).toBitmap().asImageBitmap(),
                    pm.getApplicationLabel(pm.getApplicationInfo(it.activityInfo.packageName, 0)).toString()
                )
            }.sortedBy { it.label }
        )
    }
}
