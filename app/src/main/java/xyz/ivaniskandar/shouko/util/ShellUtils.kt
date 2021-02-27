package xyz.ivaniskandar.shouko.util

import java.io.File

/**
 * Checks root availability without triggering the root prompt
 */
val isRootAvailable: Boolean
    get() {
        val path = System.getenv("PATH")
        if (!path.isNullOrEmpty()) {
            path.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().forEach { dir ->
                if (File(dir, "su").canExecute()) {
                    return true
                }
            }
        }
        return false
    }
