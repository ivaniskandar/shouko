package xyz.ivaniskandar.shouko.activity

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.verify.domain.DomainVerificationManager
import android.content.pm.verify.domain.DomainVerificationUserState
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.getSystemService
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
import xyz.ivaniskandar.shouko.item.LinkHandlerAppItem
import xyz.ivaniskandar.shouko.item.ShortcutCreatorItem
import xyz.ivaniskandar.shouko.ui.IconDrawableShadowWrapper

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    private val _isRefreshingAppsList = MutableStateFlow(true)
    private val _appsList = MutableLiveData<List<ApplicationItem>>()

    private val _isRefreshingShortcutList = MutableStateFlow(true)
    private val _shortcutList = MutableLiveData<List<ShortcutCreatorItem>>()

    private val _isRefreshingLinkHandlerList = MutableStateFlow(true)
    private val _linkHandlerList = MutableLiveData<List<LinkHandlerAppItem>>()

    val isRefreshingAppsList: StateFlow<Boolean>
        get() = _isRefreshingAppsList.asStateFlow()

    val isRefreshingShortcutList: StateFlow<Boolean>
        get() = _isRefreshingShortcutList.asStateFlow()

    val isRefreshingLinkHandlerList: StateFlow<Boolean>
        get() = _isRefreshingLinkHandlerList.asStateFlow()

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

    /**
     * LiveData of list containing apps that can open a supported link by default as [ApplicationItem]
     */
    val linkHandlerList: LiveData<List<LinkHandlerAppItem>>
        @RequiresApi(Build.VERSION_CODES.S)
        get() {
            if (_linkHandlerList.value == null) refreshLinkHandlerList()
            return _linkHandlerList
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

    @RequiresApi(Build.VERSION_CODES.S)
    fun refreshLinkHandlerList() {
        viewModelScope.launch(Dispatchers.Default) {
            _isRefreshingLinkHandlerList.emit(true)
            _linkHandlerList.postValue(getLinkHandlerList())
            _isRefreshingLinkHandlerList.emit(false)
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

    @RequiresApi(Build.VERSION_CODES.S)
    private fun getLinkHandlerList(): List<LinkHandlerAppItem> {
        val context = getApplication<Application>()
        val pm = context.packageManager
        val shadowWrapper = IconDrawableShadowWrapper()
        val manager = context.getSystemService<DomainVerificationManager>() ?: return emptyList()
        return pm.getInstalledApplications(0).asSequence()
            .map { Pair(manager.getDomainVerificationUserState(it.packageName), it) }
            .filter { !it.first?.hostToStateMap.isNullOrEmpty() }
            .map { (userState, ai) ->
                val verified = userState!!.hostToStateMap
                    ?.filterValues { it == DomainVerificationUserState.DOMAIN_STATE_VERIFIED }
                    ?.keys
                val selected = userState!!.hostToStateMap
                    ?.filterValues { it == DomainVerificationUserState.DOMAIN_STATE_SELECTED }
                    ?.keys
                val unapproved = userState!!.hostToStateMap
                    ?.filterValues { it == DomainVerificationUserState.DOMAIN_STATE_NONE }
                    ?.keys

                LinkHandlerAppItem(
                    userState!!.packageName,
                    ai.loadLabel(pm).toString(),
                    shadowWrapper.run(ai.loadIcon(pm)).toBitmap().asImageBitmap(),
                    userState!!.isLinkHandlingAllowed,
                    verified ?: emptySet(),
                    selected ?: emptySet(),
                    unapproved ?: emptySet()
                )
            }
            .sortedBy { it.label }
            .toList()
    }
}
