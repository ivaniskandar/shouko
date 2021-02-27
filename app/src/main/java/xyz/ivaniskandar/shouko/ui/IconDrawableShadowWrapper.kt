package xyz.ivaniskandar.shouko.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.DrawableWrapper
import android.util.SparseArray
import androidx.core.graphics.withSave

/**
 * Only works with AdaptiveIconDrawable
 */
class IconDrawableShadowWrapper {
    private val mShadowCache = SparseArray<Bitmap>()

    fun run(drawable: Drawable): Drawable {
        if (drawable !is AdaptiveIconDrawable) {
            return drawable
        }
        val shadow = getShadowBitmap(drawable)
        return ShadowDrawable(shadow, drawable)
    }

    private fun getShadowBitmap(d: AdaptiveIconDrawable): Bitmap {
        val shadowSize = d.intrinsicHeight
        synchronized(mShadowCache) {
            val shadow = mShadowCache[shadowSize]
            if (shadow != null) {
                return shadow
            }
        }

        d.setBounds(0, 0, shadowSize, shadowSize)

        val blur = ICON_SIZE_BLUR_FACTOR * shadowSize
        val keyShadowDistance = ICON_SIZE_KEY_SHADOW_DELTA_FACTOR * shadowSize
        val bitmapSize = (shadowSize + 2 * blur + keyShadowDistance).toInt()
        val shadow = Bitmap.createBitmap(bitmapSize, bitmapSize, Bitmap.Config.ARGB_8888)

        Canvas(shadow).apply {
            translate(blur + keyShadowDistance / 2, blur)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.TRANSPARENT
            }

            // Draw ambient shadow
            paint.setShadowLayer(blur, 0f, 0f, AMBIENT_SHADOW_ALPHA shl 24)
            drawPath(d.iconMask, paint)

            // Draw key shadow
            translate(0f, keyShadowDistance)
            paint.setShadowLayer(blur, 0f, 0f, KEY_SHADOW_ALPHA shl 24)
            drawPath(d.iconMask, paint)
            setBitmap(null)
        }
        synchronized(mShadowCache) { mShadowCache.put(shadowSize, shadow) }
        return shadow
    }

    /**
     * A drawable which draws a shadow bitmap behind a drawable
     */
    private class ShadowDrawable : DrawableWrapper {
        val mState: MyConstantState

        constructor(shadow: Bitmap?, dr: Drawable) : super(dr) {
            mState = MyConstantState(shadow, dr.constantState)
        }

        constructor(state: MyConstantState) : super(state.mChildState!!.newDrawable()) {
            mState = state
        }

        override fun getConstantState(): ConstantState? {
            return mState
        }

        override fun draw(canvas: Canvas) {
            canvas.apply {
                drawBitmap(mState.mShadow!!, null, bounds, mState.mPaint)
                withSave {
                    // Ratio of child drawable size to shadow bitmap size
                    val factor = 1 / (1 + 2 * ICON_SIZE_BLUR_FACTOR + ICON_SIZE_KEY_SHADOW_DELTA_FACTOR)
                    translate(
                        bounds.width() * factor * (ICON_SIZE_BLUR_FACTOR + ICON_SIZE_KEY_SHADOW_DELTA_FACTOR / 2),
                        bounds.height() * factor * ICON_SIZE_BLUR_FACTOR
                    )
                    scale(factor, factor)
                    super.draw(this)
                }
            }
        }

        private class MyConstantState(
            val mShadow: Bitmap?,
            val mChildState: ConstantState?
        ) : ConstantState() {
            val mPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            override fun newDrawable(): Drawable {
                return ShadowDrawable(this)
            }

            override fun getChangingConfigurations(): Int {
                return mChildState!!.changingConfigurations
            }
        }
    }

    companion object {
        // Percent of actual icon size
        private const val ICON_SIZE_BLUR_FACTOR = 0.5f / 48

        // Percent of actual icon size
        private const val ICON_SIZE_KEY_SHADOW_DELTA_FACTOR = 1f / 48
        private const val KEY_SHADOW_ALPHA = 61
        private const val AMBIENT_SHADOW_ALPHA = 30
    }
}
