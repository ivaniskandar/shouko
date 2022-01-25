package xyz.ivaniskandar.shouko.ui

import android.graphics.Canvas
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.DrawableWrapper

class AdaptiveForegroundDrawable : DrawableWrapper(ColorDrawable()) {
    private var scaleX: Float
    private var scaleY: Float

    override fun draw(canvas: Canvas) {
        val saveCount = canvas.save()
        canvas.scale(
            scaleX, scaleY,
            bounds.exactCenterX(), bounds.exactCenterY()
        )
        super.draw(canvas)
        canvas.restoreToCount(saveCount)
    }

    fun setScale(scale: Float) {
        val h = intrinsicHeight.toFloat()
        val w = intrinsicWidth.toFloat()
        scaleX = scale * LEGACY_ICON_SCALE
        scaleY = scale * LEGACY_ICON_SCALE
        if (h > w && w > 0) {
            scaleX *= w / h
        } else if (w > h && h > 0) {
            scaleY *= h / w
        }
    }

    companion object {
        private const val LEGACY_ICON_SCALE = .7f * .6667f
    }

    init {
        scaleX = LEGACY_ICON_SCALE
        scaleY = LEGACY_ICON_SCALE
    }
}
