package xyz.ivaniskandar.shouko.activity

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import xyz.ivaniskandar.shouko.item.ApplicationItem
import xyz.ivaniskandar.shouko.item.ShortcutCreatorItem
import xyz.ivaniskandar.shouko.ui.IconDrawableShadowWrapper

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val _isRefreshingAppsList = MutableStateFlow(true)
    private val _appsList = MutableLiveData<List<ApplicationItem>>()

    private val _isRefreshingShortcutList = MutableStateFlow(true)
    private val _shortcutList = MutableLiveData<List<ShortcutCreatorItem>>()

    val isRefreshingAppsList: StateFlow<Boolean>
        get() = _isRefreshingAppsList.asStateFlow()

    val isRefreshingShortcutList: StateFlow<Boolean>
        get() = _isRefreshingShortcutList.asStateFlow()

    /**
     * LiveData of list containing available launcher intents as [ApplicationItem]
     *
     * @see ApplicationItem
     */
    val appsList: LiveData<List<ApplicationItem>>
        get() {
            if (_appsList.value == null) refreshAppsList()
            return _appsList
        }

    /**
     * LiveData of list containing available intents to create an app shortcut as [ShortcutCreatorItem]
     *
     * @see ShortcutCreatorItem
     */
    val shortcutList: LiveData<List<ShortcutCreatorItem>>
        get() {
            if (_shortcutList.value == null) refreshShortcutCreatorList()
            return _shortcutList
        }

    fun refreshAppsList() {
        viewModelScope.launch(Dispatchers.Default) {
            _isRefreshingAppsList.emit(true)
            _appsList.postValue(getAppsList())
            _isRefreshingAppsList.emit(false)
        }
    }

    fun refreshShortcutCreatorList() {
        viewModelScope.launch(Dispatchers.Default) {
            _isRefreshingShortcutList.emit(true)
            _shortcutList.postValue(getShortcutCreatorList())
            _isRefreshingShortcutList.emit(false)
        }
    }

    private fun getAppsList(): List<ApplicationItem> {
        val pm = getApplication<Application>().packageManager
        val i = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val lst = pm.queryIntentActivities(i, 0)
        val shadowWrapper = IconDrawableShadowWrapper()
        return lst.map {
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
    }

    private fun getShortcutCreatorList(): List<ShortcutCreatorItem> {
        val pm = getApplication<Application>().packageManager
        val i = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
        }
        val lst = pm.queryIntentActivities(i, 0)
        val shadowWrapper = IconDrawableShadowWrapper()
        return lst.map {
            ShortcutCreatorItem(
                ComponentName(it.activityInfo.packageName, it.activityInfo.name),
                it.activityInfo.loadLabel(pm).toString(),
                shadowWrapper.run(it.activityInfo.loadIcon(pm)).toBitmap().asImageBitmap(),
                pm.getApplicationLabel(pm.getApplicationInfo(it.activityInfo.packageName, 0)).toString()
            )
        }.sortedBy { it.label }
    }
}
