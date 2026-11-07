package com.reelguard.ui.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.reelguard.R
import com.reelguard.detection.OverlayModel

/** A full ring in a status colour, drawn behind the count on the block screen. */
private class RingView(context: Context, private val ringColor: Int, private val trackColor: Int) : View(context) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

    override fun onDraw(canvas: Canvas) {
        val stroke = width * 0.085f
        val r = RectF(stroke / 2, stroke / 2, width - stroke / 2, height - stroke / 2)
        paint.strokeWidth = stroke
        paint.color = trackColor
        canvas.drawArc(r, 0f, 360f, false, paint)
        paint.color = ringColor
        canvas.drawArc(r, -90f, 360f, false, paint)
    }
}

/** Full-screen "limit reached" window (TYPE_ACCESSIBILITY_OVERLAY). Plain Views: no Compose lifecycle needed in a service. */
class BlockOverlay(private val service: AccessibilityService) {
    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val themed = ContextThemeWrapper(service, android.R.style.Theme_DeviceDefault)
    private var view: View? = null
    private var shownFor: OverlayModel? = null

    fun show(
        model: OverlayModel,
        onHome: () -> Unit,
        onSnooze: () -> Unit,
        onFallbackNeeded: () -> Unit,
    ) {
        if (view != null && shownFor == model) return
        hide()
        val v = build(model, onHome, onSnooze)
        val lp =
            WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            )
        try {
            wm.addView(v, lp)
            view = v
            shownFor = model
        } catch (_: RuntimeException) {
            onFallbackNeeded()
        }
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        shownFor = null
    }

    private fun dp(v: Int) = (v * themed.resources.displayMetrics.density).toInt()

    private fun build(
        model: OverlayModel,
        onHome: () -> Unit,
        onSnooze: () -> Unit,
    ): View {
        val root = FrameLayout(themed).apply { setBackgroundColor(Color.parseColor("#F70D0D0D")) }
        val col =
            LinearLayout(themed).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(28), dp(24), dp(28), dp(24))
            }
        val ringSize = dp(208)
        val ring =
            FrameLayout(themed).apply {
                addView(
                    RingView(themed, Color.parseColor("#E66767"), Color.parseColor("#2C2C2A")),
                    ViewGroup.LayoutParams(ringSize, ringSize),
                )
                val inner =
                    LinearLayout(themed).apply {
                        orientation = LinearLayout.VERTICAL
                        gravity = Gravity.CENTER
                    }
                inner.addView(label("${model.watched}", 64f, "#F0EFEC", bold = true))
                inner.addView(label(themed.getString(R.string.dash_of, model.limit), 16f, "#C3C2B7"))
                addView(inner, FrameLayout.LayoutParams(ringSize, ringSize, Gravity.CENTER))
            }
        col.addView(ring, LinearLayout.LayoutParams(ringSize, ringSize))
        col.addView(label(themed.getString(R.string.overlay_title), 28f, "#F0EFEC", bold = true).apply { setPadding(0, dp(28), 0, dp(6)) })
        col.addView(label(model.appLabel, 16f, "#C3C2B7"))
        col.addView(pill(themed.getString(R.string.overlay_home), "#3987E5", "#001B3A", onHome).also { it.setMargin(top = 36) })
        if (model.snoozeAvailable) {
            col.addView(pill(themed.getString(R.string.overlay_snooze), "#242422", "#F0EFEC", onSnooze).also { it.setMargin(top = 12) })
        }
        root.addView(
            col,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER),
        )
        root.contentDescription = themed.getString(R.string.overlay_title)
        return root
    }

    private fun View.setMargin(top: Int) {
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)).apply { topMargin = dp(top) }
    }

    private fun label(
        s: String,
        sp: Float,
        color: String,
        bold: Boolean = false,
    ) = TextView(themed).apply {
        text = s
        textSize = sp
        setTextColor(Color.parseColor(color))
        gravity = Gravity.CENTER
        includeFontPadding = false
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun pill(
        s: String,
        bg: String,
        fg: String,
        onClick: () -> Unit,
    ) = Button(themed).apply {
        text = s
        isAllCaps = false
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(Color.parseColor(fg))
        background =
            GradientDrawable().apply {
                setColor(Color.parseColor(bg))
                cornerRadius = dp(28).toFloat()
            }
        stateListAnimator = null
        minHeight = dp(56)
        minimumHeight = dp(56)
        setOnClickListener { onClick() }
    }
}
