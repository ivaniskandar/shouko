package xyz.ivaniskandar.shouko.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable

fun String?.toComponentName(): ComponentName? = if (this != null) {
    ComponentName.unflattenFromString(this)
} else {
    null
}

fun ComponentName?.loadLabel(context: Context): String? = if (this != null) {
    Intent().apply { component = this@loadLabel }.loadLabel(context)
} else {
    null
}

fun ComponentName.loadIcon(context: Context): Drawable = context.packageManager.getApplicationIcon(packageName)
