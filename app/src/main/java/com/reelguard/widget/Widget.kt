package com.reelguard.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
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
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.reelguard.MainActivity
import com.reelguard.R
import com.reelguard.data.datastore.Apps
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
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun counts(): CountRepository

    fun settings(): SettingsRepository
}

class ReelGuardWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val ep = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
        val s = ep.settings().current()
        val now = System.currentTimeMillis()
        val state = EvaluateLimit.evaluate(ep.counts().countsFor(DayKey.of(now, s.resetMinutes)), s)
        provideContent { WidgetContent(state) }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
    }
}

class ReelGuardWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ReelGuardWidget()
}

// Same palette as the app (light, dark).
private val Surface = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1A1A19))
private val Ink = ColorProvider(day = Color(0xFF0B0B0B), night = Color(0xFFF0EFEC))
private val InkSoft = ColorProvider(day = Color(0xFF52514E), night = Color(0xFFC3C2B7))

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

@Composable
private fun WidgetContent(state: LimitState) {
    val context = LocalContext.current
    val medium = LocalSize.current.width >= ReelGuardWidget.MEDIUM.width
    val night = isNight(context)
    val progress = if (state.limit > 0) state.total.toFloat() / state.limit else 0f
    val track = if (night) 0xFF2C2C2A.toInt() else 0xFFE1E0D9.toInt()
    val ring = RingBitmap.draw(progress, 260, statusColor(state.status, night), track)
    val ringDesc = context.getString(R.string.widget_ring_desc, state.total, state.limit)
    val status =
        when (state.status) {
            LimitStatus.REACHED -> context.getString(R.string.widget_limit_reached)
            else -> context.getString(R.string.widget_left, state.remaining)
        }
    Box(
        modifier =
            GlanceModifier.fillMaxSize().background(Surface).cornerRadius(24.dp).padding(10.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        contentAlignment = Alignment.Center,
    ) {
        if (medium) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RingWithCount(ring, ringDesc, state, 88)
                Column(modifier = GlanceModifier.padding(start = 16.dp)) {
                    Text(status, style = TextStyle(color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                    Apps.all.forEach { pkg ->
                        Text("${Apps.shortLabel(pkg)}  ${state.counts[pkg] ?: 0}", style = TextStyle(color = InkSoft, fontSize = 13.sp))
                    }
                }
            }
        } else {
            RingWithCount(ring, ringDesc, state, 100)
        }
    }
}

@Composable
private fun RingWithCount(
    ring: Bitmap,
    desc: String,
    state: LimitState,
    sizeDp: Int,
) {
    Box(contentAlignment = Alignment.Center, modifier = GlanceModifier.size(sizeDp.dp)) {
        Image(ImageProvider(ring), desc, modifier = GlanceModifier.size(sizeDp.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${state.total}", style = TextStyle(color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold))
            Text("of ${state.limit}", style = TextStyle(color = InkSoft, fontSize = 11.sp))
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
        val stroke = sizePx * 0.11f
        val rect = RectF(stroke / 2, stroke / 2, sizePx - stroke / 2, sizePx - stroke / 2)
        val paint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeCap = Paint.Cap.ROUND
            }
        paint.color = track
        canvas.drawArc(rect, 0f, 360f, false, paint)
        paint.color = color
        if (progress > 0f) canvas.drawArc(rect, -90f, 360f * progress.coerceIn(0.01f, 1f), false, paint)
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
    ) : WidgetRefresher {
        private var lastUpdate = 0L
        private var pending: Job? = null

        /** Set when a request arrives while an update is already scheduled or running, so that update is repeated. */
        @Volatile private var dirty = false

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
