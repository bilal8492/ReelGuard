package com.reelguard.ui.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import com.reelguard.R
import com.reelguard.detection.CounterModel
import com.reelguard.domain.LimitStatus

/** Small, non-touchable pill at the top centre showing the live count while Reels/Shorts is on screen. */
class CounterOverlay(private val service: AccessibilityService) {
    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val themed = ContextThemeWrapper(service, android.R.style.Theme_DeviceDefault)
    private var view: TextView? = null

    fun show(model: CounterModel) {
        val text = themed.getString(R.string.counter_text, model.appLabel, model.appCount, model.total, model.limit)
        val tv = view ?: create().also { added -> if (!attach(added)) return }
        // A coloured status dot, then the text. The dot is paired with the words, never the only cue.
        tv.text =
            SpannableString("● $text").apply {
                setSpan(ForegroundColorSpan(colorFor(model.status)), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        tv.contentDescription = text
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
    }

    private fun attach(tv: TextView): Boolean {
        val lp =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            )
        lp.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        lp.y = statusBarHeight() + dp(6)
        return try {
            wm.addView(tv, lp)
            view = tv
            true
        } catch (_: RuntimeException) {
            false
        }
    }

    private fun create() =
        TextView(themed).apply {
            textSize = 14f
            setTextColor(Color.WHITE)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(6), dp(14), dp(6))
            background =
                GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(Color.parseColor("#D90D0D0D"))
                }
        }

    private fun colorFor(status: LimitStatus) =
        when (status) {
            LimitStatus.OK -> Color.parseColor("#3987E5")
            LimitStatus.WARN -> Color.parseColor("#FAB219")
            LimitStatus.REACHED -> Color.parseColor("#E66767")
        }

    private fun dp(v: Int) = (v * themed.resources.displayMetrics.density).toInt()

    private fun statusBarHeight(): Int {
        val id = themed.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) themed.resources.getDimensionPixelSize(id) else dp(24)
    }
}
