package com.reelguard.ui.overlay

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
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
import android.widget.ScrollView
import android.widget.TextView
import com.reelguard.R
import com.reelguard.data.datastore.Apps
import com.reelguard.detection.OverlayApp
import com.reelguard.detection.OverlayModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

/** The block screen's colours: the same red-tinted "limit reached" look as the widget and the app, in light and dark. */
private class Palette(night: Boolean) {
    val scrim = (if (night) 0xF20D0D0D else 0xF2F6F6F3).toInt()
    val card = (if (night) 0xFF2B1A1A else 0xFFFCE6E6).toInt()
    val ink = (if (night) 0xFFF0EFEC else 0xFF0B0B0B).toInt()
    val soft = (if (night) 0xFFC3C2B7 else 0xFF52514E).toInt()
    val danger = (if (night) 0xFFFF8A8A else 0xFFB02A2A).toInt()
    val ring = (if (night) 0xFFE66767 else 0xFFD03B3B).toInt()
    val track = (if (night) 0xFF3A3A37 else 0xFFE1E0D9).toInt()
    val chip = (if (night) 0xFF3A2525 else 0xFFF6D3D3).toInt()
    val primary = (if (night) 0xFF3987E5 else 0xFF2A78D6).toInt()
    val onPrimary = (if (night) 0xFF001B3A else 0xFFFFFFFF).toInt()
    val secondary = (if (night) 0xFF242422 else 0xFFEDECE7).toInt()
    private val shorts = (if (night) 0xFF3987E5 else 0xFF2A78D6).toInt()
    private val reels = (if (night) 0xFFD95926 else 0xFFEB6834).toInt()

    fun app(pkg: String) = if (pkg == Apps.YOUTUBE) shorts else reels
}

/** A full ring in the status colour, drawn behind the count on the block screen. */
private class RingView(context: Context, private val ringColor: Int, private val trackColor: Int) : View(context) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
        }

    override fun onDraw(canvas: Canvas) {
        val stroke = width * 0.12f
        val r = RectF(stroke / 2, stroke / 2, width - stroke / 2, height - stroke / 2)
        paint.strokeWidth = stroke
        paint.color = trackColor
        canvas.drawArc(r, 0f, 360f, false, paint)
        paint.color = ringColor
        canvas.drawArc(r, -90f, 360f, false, paint)
    }
}

/** A thin rounded progress bar: [share] (0..1) of the width in [color] over [track]. */
private class BarView(context: Context, private val color: Int, private val track: Int, private val share: Float) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.color = track
        canvas.drawRoundRect(0f, 0f, w, h, h / 2, h / 2, paint)
        if (share > 0f) {
            paint.color = color
            canvas.drawRoundRect(0f, 0f, (w * share.coerceIn(0f, 1f)).coerceAtLeast(h), h, h / 2, h / 2, paint)
        }
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

    private fun isNight() = themed.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    private fun build(
        model: OverlayModel,
        onHome: () -> Unit,
        onSnooze: () -> Unit,
    ): View {
        val p = Palette(isNight())
        val card =
            LinearLayout(themed).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_HORIZONTAL
                setPadding(dp(24), dp(28), dp(24), dp(24))
                background = rounded(p.card, 32)
            }
        card.addView(ring(model, p), LinearLayout.LayoutParams(dp(200), dp(200)))
        card.addView(label(themed.getString(R.string.overlay_title), 24f, p.danger, bold = true).apply { setPadding(0, dp(20), 0, dp(4)) })
        card.addView(label(model.appLabel, 15f, p.soft))
        card.addView(appBars(model, p), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(16) })
        card.addView(chip(resetText(model.resetAt), p), LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = dp(18) })
        card.addView(pill(themed.getString(R.string.overlay_home), p.primary, p.onPrimary, onHome), buttonParams(top = 22))
        if (model.snoozeAvailable) {
            card.addView(pill(themed.getString(R.string.overlay_snooze), p.secondary, p.ink, onSnooze), buttonParams(top = 10))
        }
        return screen(card, p).apply { contentDescription = themed.getString(R.string.overlay_title) }
    }

    /** Dimmed background and a scroll container, so the card also fits in landscape and on small screens. */
    private fun screen(
        card: View,
        p: Palette,
    ): View {
        val holder = FrameLayout(themed).apply { setPadding(dp(20), dp(24), dp(20), dp(24)) }
        holder.addView(card, FrameLayout.LayoutParams(MATCH, WRAP, Gravity.CENTER))
        val scroll =
            ScrollView(themed).apply {
                isFillViewport = true
                isVerticalScrollBarEnabled = false
            }
        scroll.addView(holder, ViewGroup.LayoutParams(MATCH, WRAP))
        return FrameLayout(themed).apply {
            setBackgroundColor(p.scrim)
            addView(scroll, ViewGroup.LayoutParams(MATCH, MATCH))
        }
    }

    private fun ring(
        model: OverlayModel,
        p: Palette,
    ): View {
        val size = dp(200)
        return FrameLayout(themed).apply {
            contentDescription = themed.getString(R.string.widget_ring_desc, model.watched, model.limit)
            addView(RingView(themed, p.ring, p.track), ViewGroup.LayoutParams(size, size))
            val inner =
                LinearLayout(themed).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                }
            inner.addView(label("${model.watched}", 60f, p.ink, bold = true))
            inner.addView(label(themed.getString(R.string.dash_of, model.limit), 15f, p.soft))
            addView(inner, FrameLayout.LayoutParams(size, size, Gravity.CENTER))
        }
    }

    /** One row per monitored app, like the widget: name, coloured progress bar, count. */
    private fun appBars(
        model: OverlayModel,
        p: Palette,
    ) = LinearLayout(themed).apply {
        orientation = LinearLayout.VERTICAL
        model.apps.forEach { app ->
            addView(appRow(app, p), LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(8) })
        }
    }

    private fun appRow(
        app: OverlayApp,
        p: Palette,
    ) = LinearLayout(themed).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        val share = if (app.cap > 0) app.count.toFloat() / app.cap else 0f
        val name = label(app.label, 14f, p.soft).apply { gravity = Gravity.START or Gravity.CENTER_VERTICAL }
        val count = label("${app.count}", 14f, p.ink, bold = true).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL }
        addView(name, LinearLayout.LayoutParams(dp(56), WRAP))
        addView(
            BarView(themed, p.app(app.pkg), p.track, share),
            LinearLayout.LayoutParams(0, dp(8), 1f).apply {
                marginStart = dp(8)
                marginEnd = dp(10)
            },
        )
        addView(count, LinearLayout.LayoutParams(dp(32), WRAP))
    }

    private fun chip(
        text: String,
        p: Palette,
    ) = label(text, 14f, p.ink, bold = true).apply {
        setPadding(dp(16), dp(10), dp(16), dp(10))
        background = rounded(p.chip, 20)
    }

    /** "Resets at 12:00 AM · in 4 h 16 min", so it is clear when watching is possible again. */
    private fun resetText(resetAt: Long): String {
        val zone = ZoneId.systemDefault()
        val at = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(resetAt).atZone(zone))
        val mins = ((resetAt - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1L).toInt()
        val left =
            if (mins >= 60) themed.getString(R.string.time_hr_min, mins / 60, mins % 60) else themed.getString(R.string.time_min, mins)
        return themed.getString(R.string.overlay_resets, at, left)
    }

    private fun rounded(
        color: Int,
        radiusDp: Int,
    ) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun buttonParams(top: Int) = LinearLayout.LayoutParams(MATCH, dp(56)).apply { topMargin = dp(top) }

    private fun label(
        s: String,
        sp: Float,
        color: Int,
        bold: Boolean = false,
    ) = TextView(themed).apply {
        text = s
        textSize = sp
        setTextColor(color)
        gravity = Gravity.CENTER
        includeFontPadding = false
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }

    private fun pill(
        s: String,
        bg: Int,
        fg: Int,
        onClick: () -> Unit,
    ) = Button(themed).apply {
        text = s
        isAllCaps = false
        textSize = 16f
        setTypeface(typeface, Typeface.BOLD)
        setTextColor(fg)
        background = rounded(bg, 28)
        stateListAnimator = null
        minHeight = dp(56)
        minimumHeight = dp(56)
        setOnClickListener { onClick() }
    }
}
