package xyz.ivaniskandar.shouko.util

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

fun PackageManager.queryIntentActivitiesCompat(
    intent: Intent,
    flags: Int,
): List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
} else {
    queryIntentActivities(intent, flags)
}

fun PackageManager.resolveActivityCompat(
    intent: Intent,
    flags: Int,
): ResolveInfo? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    resolveActivity(intent, PackageManager.ResolveInfoFlags.of(flags.toLong()))
} else {
    resolveActivity(intent, flags)
}

fun PackageManager.getApplicationInfoCompat(
    packageName: String,
    flags: Int,
): ApplicationInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(flags.toLong()))
} else {
    getApplicationInfo(packageName, flags)
}

fun PackageManager.getInstalledApplicationsCompat(flags: Int): List<ApplicationInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    getInstalledApplications(PackageManager.ApplicationInfoFlags.of(flags.toLong()))
} else {
    getInstalledApplications(flags)
}
