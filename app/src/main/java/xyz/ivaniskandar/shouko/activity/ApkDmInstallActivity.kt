package xyz.ivaniskandar.shouko.activity

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.window.SplashScreen
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.lifecycle.lifecycleScope
import androidx.profileinstaller.DexProfileTranscoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.ivaniskandar.shouko.R
import xyz.ivaniskandar.shouko.ui.theme.ShoukoM3Theme
import java.io.File

/**
 * APK installer activity.
 *
 * If a given APK contains a Baseline Profile, it will be transcoded into
 * a DexMetadata (DM) file and installed alongside the APK. This eliminates
 * the need for the system's bg-dexopt job to optimize the installed APK.
 */
class ApkDmInstallActivity : ComponentActivity() {
    private var state by mutableStateOf<State>(State.Loading)
    private var sessionId = 0

    private val packageActionReceiver =
        object : BroadcastReceiver() {
            @SuppressLint("UnsafeIntentLaunch")
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
                    PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                        val userAction = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                        if (userAction == null) {
                            finish()
                            return
                        }
                        userAction.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(userAction)
                    }

                    PackageInstaller.STATUS_SUCCESS -> {
                        if (state is State.Ready) {
                            state = (state as State.Ready).copy(installing = false, installed = true, error = null)
                        }
                    }

                    PackageInstaller.STATUS_FAILURE,
                    PackageInstaller.STATUS_FAILURE_BLOCKED,
                    PackageInstaller.STATUS_FAILURE_ABORTED,
                    PackageInstaller.STATUS_FAILURE_INVALID,
                    PackageInstaller.STATUS_FAILURE_CONFLICT,
                    PackageInstaller.STATUS_FAILURE_STORAGE,
                    PackageInstaller.STATUS_FAILURE_INCOMPATIBLE,
                    PackageInstaller.STATUS_FAILURE_TIMEOUT,
                    -> {
                        val reason = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
                        if (state is State.Ready) {
                            state = (state as State.Ready).copy(installing = false, installed = false, error = reason)
                        }
                    }

                    else -> {
                        // Do nothing
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            val uri = resolveIntent()
            if (uri == null) {
                finish()
                return@launch
            }
            val file = makeCache(uri)
            if (file == null) {
                finish()
                return@launch
            }
            val info = makeInfo(file)
            if (info == null) {
                finish()
                return@launch
            }
            val dmFile = DexProfileTranscoder.run(this@ApkDmInstallActivity, file)
            state = State.Ready(info = info, apkFile = file, dmFile = dmFile)
        }

        setContent {
            ShoukoM3Theme {
                Dialog(
                    onDismissRequest = ::finish,
                    properties = DialogProperties(dismissOnClickOutside = false),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                    ) {
                        when (val state = state) {
                            State.Loading -> {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.padding(16.dp))
                                }
                            }
                            is State.Ready -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp),
                                ) {
                                    val icon =
                                        remember {
                                            state.info.applicationInfo
                                                ?.loadIcon(packageManager)
                                                ?.toBitmapOrNull()
                                                ?.asImageBitmap()
                                        }
                                    val iconModifier = Modifier.size(72.dp)
                                    if (icon != null) {
                                        Image(
                                            bitmap = icon,
                                            modifier = iconModifier,
                                            contentDescription = null,
                                        )
                                    } else {
                                        Spacer(modifier = iconModifier)
                                    }

                                    Text(
                                        text =
                                        state.info.applicationInfo
                                            ?.loadLabel(packageManager)
                                            .toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                                    )

                                    Text(
                                        text =
                                        if (!state.installed) {
                                            state.error ?: stringResource(
                                                id =
                                                if (state.dmFile != null) {
                                                    R.string.apk_installer_profile_available
                                                } else {
                                                    R.string.apk_installer_profile_unavailable
                                                },
                                            )
                                        } else {
                                            stringResource(id = R.string.apk_installer_success)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    if (state.installing) {
                                        CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                                    } else if (!state.installed) {
                                        Button(
                                            onClick = {
                                                this@ApkDmInstallActivity.state = state.copy(installing = true)
                                                startInstall(state.apkFile, state.dmFile)
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text =
                                                stringResource(
                                                    id =
                                                    if (state.error != null) {
                                                        R.string.button_retry
                                                    } else {
                                                        R.string.button_continue
                                                    },
                                                ),
                                            )
                                        }
                                        TextButton(
                                            onClick = ::finish,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(text = stringResource(id = R.string.button_cancel))
                                        }
                                    } else {
                                        Button(
                                            onClick = ::launchInstalledPackageAndFinishActivity,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(text = stringResource(id = R.string.button_open))
                                        }
                                        TextButton(
                                            onClick = ::finish,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(text = stringResource(id = R.string.button_done))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val state = state
        if (state is State.Ready) {
            state.apkFile.delete()
            state.dmFile?.delete()
        }
        val packageInstaller = packageManager.packageInstaller
        packageInstaller.mySessions.forEach {
            try {
                packageInstaller.abandonSession(it.sessionId)
            } catch (e: Exception) {
                // ignore errors
            }
        }
        try {
            unregisterReceiver(packageActionReceiver)
        } catch (e: Exception) {
            // already unregistered
        }
    }

    private fun resolveIntent(): Uri? = when {
        intent.action == Intent.ACTION_SEND -> {
            IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
        }
        intent.action == Intent.ACTION_VIEW && intent.data?.scheme == "content" -> {
            intent.data
        }
        else -> null
    }

    private suspend fun makeCache(uri: Uri): File? {
        val tempApkFile = File(cacheDir, "base.apk")
        withContext(Dispatchers.IO) {
            tempApkFile.delete()
            try {
                contentResolver.openInputStream(uri).use { input ->
                    tempApkFile.outputStream().use { output ->
                        input?.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                // handle error
            }
        }
        return tempApkFile.takeIf { it.exists() }
    }

    private fun makeInfo(file: File): PackageInfo? = packageManager.getPackageArchiveInfo(file.absolutePath, 0)?.also {
        it.applicationInfo?.apply {
            sourceDir = file.absolutePath
            publicSourceDir = file.absolutePath
        }
    }

    private fun startInstall(
        apkFile: File,
        dmFile: File?,
    ) {
        val packageInstaller = packageManager.packageInstaller

        val installParams = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            installParams.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED)
        }

        sessionId = packageInstaller.createSession(installParams)
        packageInstaller.openSession(sessionId).use { session ->
            apkFile.inputStream().use { inputStream ->
                session.openWrite(apkFile.name, 0, -1).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    session.fsync(outputStream)
                }
            }
            dmFile?.inputStream()?.use { inputStream ->
                session.openWrite(dmFile.name, 0, -1).use { outputStream ->
                    inputStream.copyTo(outputStream)
                    session.fsync(outputStream)
                }
            }

            ContextCompat.registerReceiver(
                this,
                packageActionReceiver,
                IntentFilter(INSTALL_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )

            val intentSender =
                PendingIntent
                    .getBroadcast(
                        this,
                        sessionId,
                        Intent(INSTALL_ACTION).setPackage(packageName),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0,
                    ).intentSender
            session.commit(intentSender)
        }
    }

    private fun launchInstalledPackageAndFinishActivity() {
        val pkgName = (state as? State.Ready)?.info?.packageName ?: return
        val launcher = packageManager.getLaunchIntentForPackage(pkgName) ?: return
        val bundle =
            ActivityOptions
                .makeBasic()
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        splashScreenStyle = SplashScreen.SPLASH_SCREEN_STYLE_ICON
                    }
                }.toBundle()
        startActivity(launcher, bundle)
        finish()
    }

    private sealed interface State {
        data object Loading : State

        data class Ready(
            val info: PackageInfo,
            val apkFile: File,
            val dmFile: File?,
            val installing: Boolean = false,
            val installed: Boolean = false,
            val error: String? = null,
        ) : State
    }

    companion object {
        private const val INSTALL_ACTION = "ApkInstallActivity.INSTALL_ACTION"
    }
}
