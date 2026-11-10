package com.reelguard.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import androidx.glance.layout.ContentScale
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.reelguard.MainActivity
import com.reelguard.R
import com.reelguard.data.datastore.Apps
import com.reelguard.data.datastore.Settings
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.data.repo.CountRepository
import com.reelguard.di.ApplicationScope
import com.reelguard.domain.DayKey
import com.reelguard.domain.EvaluateLimit
import com.reelguard.domain.LimitState
import com.reelguard.domain.LimitStatus
import com.reelguard.domain.WidgetRefresher
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun counts(): CountRepository

    fun settings(): SettingsRepository
}

class ReelGuardWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, TALL, MEDIUM))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val s = ep.settings().current()
        val now = System.currentTimeMillis()
        val state = EvaluateLimit.evaluate(ep.counts().countsFor(DayKey.of(now, s.resetMinutes)), s)
        provideContent { WidgetContent(state, s, DayKey.nextReset(now, s.resetMinutes)) }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val TALL = DpSize(110.dp, 200.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
    }
}

class ReelGuardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReelGuardWidget()
}

// Same palette as the app (light, dark).
private val Ink = ColorProvider(day = Color(0xFF0B0B0B), night = Color(0xFFF0EFEC))
private val InkSoft = ColorProvider(day = Color(0xFF52514E), night = Color(0xFFC3C2B7))

/** Card background tinted by status, so the state reads from across the room. */
private fun cardColor(status: LimitStatus) =
    when (status) {
        LimitStatus.OK -> ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1A1A19))
        LimitStatus.WARN -> ColorProvider(day = Color(0xFFFFF3D6), night = Color(0xFF2A2415))
        LimitStatus.REACHED -> ColorProvider(day = Color(0xFFFCE6E6), night = Color(0xFF2B1A1A))
    }

private fun statusInk(status: LimitStatus) =
    when (status) {
        LimitStatus.OK -> ColorProvider(day = Color(0xFF2A78D6), night = Color(0xFF6FA8F0))
        LimitStatus.WARN -> ColorProvider(day = Color(0xFF8A5A00), night = Color(0xFFFAB219))
        LimitStatus.REACHED -> ColorProvider(day = Color(0xFFB02A2A), night = Color(0xFFFF8A8A))
    }

private fun pillColor(status: LimitStatus) =
    when (status) {
        LimitStatus.OK -> ColorProvider(day = Color(0x262A78D6), night = Color(0x336FA8F0))
        LimitStatus.WARN -> ColorProvider(day = Color(0x33C98500), night = Color(0x33FAB219))
        LimitStatus.REACHED -> ColorProvider(day = Color(0x26D03B3B), night = Color(0x33FF8A8A))
    }

private fun isNight(context: Context) =
    context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

private fun statusColor(
    status: LimitStatus,
    night: Boolean,
): Int =
    when (status) {
        LimitStatus.OK -> if (night) 0xFF3987E5 else 0xFF2A78D6
        LimitStatus.WARN -> if (night) 0xFFFAB219 else 0xFFC98500
        LimitStatus.REACHED -> if (night) 0xFFE66767 else 0xFFD03B3B
    }.toInt()

private fun appColor(
    pkg: String,
    night: Boolean,
): Int =
    when (pkg) {
        Apps.YOUTUBE -> if (night) 0xFF3987E5 else 0xFF2A78D6
        else -> if (night) 0xFFD95926 else 0xFFEB6834
    }.toInt()

@Composable
private fun WidgetContent(
    state: LimitState,
    settings: Settings,
    resetAt: Long,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val medium = size.width >= ReelGuardWidget.MEDIUM.width
    val tall = !medium && size.height >= ReelGuardWidget.TALL.height
    val night = isNight(context)
    val progress = if (state.limit > 0) state.total.toFloat() / state.limit else 0f
    val track = if (night) 0xFF3A3A37.toInt() else 0xFFE1E0D9.toInt()
    val ring = RingBitmap.draw(progress, 320, statusColor(state.status, night), track)
    val ringDesc = context.getString(R.string.widget_ring_desc, state.total, state.limit)
    val status =
        when (state.status) {
            LimitStatus.REACHED -> context.getString(R.string.widget_limit_reached)
            LimitStatus.WARN -> context.getString(R.string.widget_close, state.remaining)
            else -> context.getString(R.string.widget_left, state.remaining)
        }
    val resetTime =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(Instant.ofEpochMilli(resetAt).atZone(ZoneId.systemDefault()))
    val resets = context.getString(R.string.widget_resets, resetTime)
    val glow = RingBitmap.glow(300, statusColor(state.status, night), if (night) 0x4D else 0x38)
    val pkgs = Apps.all.filter { it in settings.monitoredApps }

    @Composable
    fun Rows(barWidthDp: Int) {
        pkgs.forEach { pkg ->
            val cap = settings.perAppLimits[pkg] ?: state.limit
            val count = state.counts[pkg] ?: 0
            val share = if (cap > 0) count.toFloat() / cap else 0f
            AppRow(
                Apps.shortLabel(pkg),
                count,
                RingBitmap.bar(share, barWidthDp * 3, 24, appColor(pkg, night), track),
                appColor(pkg, night),
                barWidthDp,
            )
        }
    }
    Box(
        modifier =
            GlanceModifier.fillMaxSize().background(cardColor(state.status)).cornerRadius(28.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        contentAlignment = Alignment.Center,
    ) {
        Image(ImageProvider(glow), null, modifier = GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(modifier = GlanceModifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
            if (medium) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RingWithCount(ring, ringDesc, state, 92)
                    Column(modifier = GlanceModifier.padding(start = 16.dp)) {
                        StatusPill(status, state.status, 13)
                        Spacer(GlanceModifier.height(8.dp))
                        Rows(96)
                        Spacer(GlanceModifier.height(6.dp))
                        Text(resets, style = TextStyle(color = InkSoft, fontSize = 11.sp))
                    }
                }
            } else if (tall) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RingWithCount(ring, ringDesc, state, 104)
                    Spacer(GlanceModifier.height(8.dp))
                    StatusPill(status, state.status, 12)
                    Spacer(GlanceModifier.height(10.dp))
                    Rows(52)
                    Spacer(GlanceModifier.height(6.dp))
                    Text(resets, style = TextStyle(color = InkSoft, fontSize = 11.sp))
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RingWithCount(ring, ringDesc, state, 76)
                    Spacer(GlanceModifier.height(6.dp))
                    StatusPill(status, state.status, 11)
                }
            }
        }
    }
}

/** Status as a tinted capsule, so it reads as a badge rather than loose text. */
@Composable
private fun StatusPill(
    text: String,
    status: LimitStatus,
    sizeSp: Int,
) {
    Box(
        modifier = GlanceModifier.background(pillColor(status)).cornerRadius(20.dp).padding(horizontal = 10.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = TextStyle(color = statusInk(status), fontSize = sizeSp.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun AppRow(
    label: String,
    count: Int,
    bar: Bitmap,
    dotColor: Int,
    barWidthDp: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.padding(vertical = 3.dp)) {
        Box(
            modifier =
                GlanceModifier.size(8.dp).cornerRadius(4.dp)
                    .background(ColorProvider(day = Color(dotColor), night = Color(dotColor))),
        ) {}
        Text(
            label,
            modifier = GlanceModifier.width(46.dp).padding(start = 6.dp),
            style = TextStyle(color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Medium),
        )
        Image(ImageProvider(bar), null, modifier = GlanceModifier.size(width = barWidthDp.dp, height = 8.dp))
        Text(
            "$count",
            modifier = GlanceModifier.padding(start = 8.dp),
            style = TextStyle(color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun RingWithCount(
    ring: Bitmap,
    desc: String,
    state: LimitState,
    sizeDp: Int,
) {
    val big = sizeDp >= 80
    val huge = sizeDp >= 100
    Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(sizeDp.dp)) {
        Image(ImageProvider(ring), desc, modifier = GlanceModifier.size(sizeDp.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${state.total}",
                style = TextStyle(color = Ink, fontSize = if (huge) 34.sp else if (big) 30.sp else 26.sp, fontWeight = FontWeight.Bold),
            )
            Text(
                "of ${state.limit}",
                style = TextStyle(color = InkSoft, fontSize = if (big) 12.sp else 10.sp, fontWeight = FontWeight.Medium),
            )
        }
    }
}

object RingBitmap {
    fun draw(
        progress: Float,
        sizePx: Int,
        color: Int,
        track: Int,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val stroke = sizePx * 0.085f
        val inset = sizePx * 0.03f + stroke / 2
        val rect = RectF(inset, inset, sizePx - inset, sizePx - inset)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
            }
        paint.color = track
        canvas.drawArc(rect, 0f, 360f, false, paint)
        if (progress > 0f) {
            val sweep = 360f * progress.coerceIn(0.01f, 1f)
            // Soft halo under the arc gives the progress some depth.
            paint.color = (color and 0x00FFFFFF) or 0x40000000
            paint.strokeWidth = stroke * 1.7f
            canvas.drawArc(rect, -90f, sweep, false, paint)
            paint.color = color
            paint.strokeWidth = stroke
            canvas.drawArc(rect, -90f, sweep, false, paint)
        }
        return bmp
    }

    /** A faint status-coloured glow from the top edge, layered over the card background. */
    fun glow(
        sizePx: Int,
        color: Int,
        alpha: Int,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val c = sizePx / 2f
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader =
                    RadialGradient(
                        c,
                        0f,
                        sizePx * 0.95f,
                        (color and 0x00FFFFFF) or (alpha shl 24),
                        color and 0x00FFFFFF,
                        Shader.TileMode.CLAMP,
                    )
            }
        Canvas(bmp).drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), paint)
        return bmp
    }

    /** A rounded progress bar: [share] (0..1+) of the width filled with [color] over [track]. */
    fun bar(
        share: Float,
        w: Int,
        h: Int,
        color: Int,
        track: Int,
    ): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val r = h / 2f
        paint.color = track
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), r, r, paint)
        if (share > 0f) {
            paint.color = color
            val fill = (w * share.coerceIn(0f, 1f)).coerceAtLeast(h.toFloat())
            canvas.drawRoundRect(RectF(0f, 0f, fill, h.toFloat()), r, r, paint)
        }
        return bmp
    }
}

/** At most one widget update per 10 s; extra requests in the window collapse into one trailing update. */
@Singleton
class GlanceWidgetRefresher
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @ApplicationScope private val scope: CoroutineScope,
        private val counts: CountRepository,
        private val settings: SettingsRepository,
    ) : WidgetRefresher {
        private var lastUpdate = 0L
        private var pending: Job? = null

        /** Set when a request arrives while an update is already scheduled or running, so that update is repeated. */
        @Volatile private var dirty = false

        private var started = false

        /**
         * Keeps the widget in step with the data: any change to today's counts, the limits/settings, or the day itself
         * (midnight / custom reset time) updates it, so it no longer depends on the 30-minute backup worker.
         */
        @Synchronized
        fun start() {
            if (started) return
            started = true
            scope.launch {
                combine(counts.todayCounts(), settings.settings) { c, s -> c to s }.collect { request() }
            }
            // A new day starts from zero: show it immediately, not after the throttle window.
            scope.launch { counts.todayKey().drop(1).collect { request(force = true) } }
        }

        @Synchronized
        override fun request(force: Boolean) {
            if (!force && pending?.isActive == true) {
                dirty = true
                return
            }
            pending?.cancel()
            val wait = if (force) 0L else (lastUpdate + MIN_INTERVAL_MS - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            pending =
                scope.launch {
                    delay(wait)
                    do {
                        dirty = false
                        lastUpdate = SystemClock.elapsedRealtime()
                        ReelGuardWidget().updateAll(context)
                        // A count that landed during the update above is shown by one more update after the minimum interval.
                        if (dirty) delay(MIN_INTERVAL_MS)
                    } while (dirty)
                }
        }

        companion object {
            const val MIN_INTERVAL_MS = 10_000L
        }
    }
