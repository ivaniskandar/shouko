package xyz.ivaniskandar.shouko.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Short-lived activity used as a placeholder for lockscreen shortcut
 */
class EmptyShortcutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        finish()
    }
}
