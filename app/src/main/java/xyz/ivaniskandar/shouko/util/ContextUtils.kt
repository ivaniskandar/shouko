package xyz.ivaniskandar.shouko.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

val Context.canReadSystemLogs
    get() = checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED

val Context.canWriteSecureSettings: Boolean
    get() = checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

fun Context.isPackageInstalled(packageName: String): Boolean {
    return try {
        packageManager.getApplicationInfo(packageName, 0).enabled
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

fun Context.getPackageLabel(packageName: String): String {
    return try {
        val ai = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(ai).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        "null"
    }
}

fun checkDefaultBrowser(context: Context): Boolean {
    val i = Intent(Intent.ACTION_VIEW, Uri.parse("http://example.com"))
    val default = context.packageManager
        .resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo
        ?.packageName
    return default == context.packageName
}

fun openDefaultAppsSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
}

@RequiresApi(Build.VERSION_CODES.S)
fun openOpenByDefaultSettings(context: Context, packageName: String) {
    val i = Intent(
        Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS,
        Uri.parse("package:$packageName")
    )
    context.startActivity(i)
}
