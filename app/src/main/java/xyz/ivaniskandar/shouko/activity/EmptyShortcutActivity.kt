package xyz.ivaniskandar.shouko.activity

import android.app.Activity
import android.os.Bundle

/**
 * Short-lived activity used as a placeholder for lockscreen shortcut
 */
class EmptyShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
