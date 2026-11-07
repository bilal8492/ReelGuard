package com.reelguard.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.reelguard.R
import com.reelguard.data.datastore.Apps
import com.reelguard.data.datastore.SettingsRepository
import com.reelguard.data.repo.CountRepository
import com.reelguard.domain.DayTotal
import com.reelguard.domain.HistoryStats
import com.reelguard.ui.common.AppCard
import com.reelguard.ui.common.LegendDot
import com.reelguard.ui.common.ScreenTitle
import com.reelguard.ui.common.Segmented
import com.reelguard.ui.common.StackedBarChart
import com.reelguard.ui.common.StatTile
import com.reelguard.ui.theme.extra
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class HistoryState(
    val days: List<DayTotal>,
    val limit: Int,
    val stats: HistoryStats,
    val busiestHour: Int?,
)

@HiltViewModel
class HistoryViewModel
    @Inject
    constructor(
        private val counts: CountRepository,
        private val settings: SettingsRepository,
    ) : ViewModel() {
        val range = MutableStateFlow(7)

        @OptIn(ExperimentalCoroutinesApi::class)
        val state: StateFlow<HistoryState?> =
            range.flatMapLatest { n ->
                combine(counts.history(n), settings.settings) { days, s ->
                    val first = counts.firstDay()
                    val hours = counts.hourHistogram(System.currentTimeMillis())
                    val busiest = hours.withIndex().maxByOrNull { it.value }?.takeIf { it.value > 0 }?.index
                    HistoryState(days, s.combinedLimit, HistoryStats.compute(days, s.combinedLimit, first), busiest)
                }
            }.map<HistoryState, HistoryState?> { it }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    }

private val dayFormat = DateTimeFormatter.ofPattern("EEE d MMM")
private val shortFormat = DateTimeFormatter.ofPattern("d MMM")

private fun fmt(
    key: String,
    f: DateTimeFormatter,
) = LocalDate.parse(key).format(f)

@Composable
fun HistoryScreen(vm: HistoryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val range by vm.range.collectAsStateWithLifecycle()
    var selected by remember(range) { mutableStateOf<Int?>(null) }
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 112.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle(stringResource(R.string.nav_history), stringResource(R.string.hist_subtitle))
        Segmented(
            listOf(stringResource(R.string.hist_7), stringResource(R.string.hist_30)),
            if (range == 7) 0 else 1,
            { vm.range.value = if (it == 0) 7 else 30 },
        )
        val s = state
        if (s != null) {
            val idx = selected ?: s.days.lastIndex
            val day = s.days[idx]
            AppCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            LegendDot(MaterialTheme.colorScheme.extra.seriesShorts, Apps.shortLabel(Apps.YOUTUBE))
                            LegendDot(MaterialTheme.colorScheme.extra.seriesReels, Apps.shortLabel(Apps.INSTAGRAM))
                        }
                        Text(
                            stringResource(R.string.hist_limit_label, s.limit),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StackedBarChart(
                        days = s.days,
                        shortsKey = Apps.YOUTUBE,
                        reelsKey = Apps.INSTAGRAM,
                        limit = s.limit,
                        selected = idx,
                        onSelect = { selected = it },
                        description = stringResource(R.string.hist_chart_desc, s.days.size, s.limit),
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            fmt(s.days.first().dayKey, shortFormat),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            fmt(s.days.last().dayKey, shortFormat),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        stringResource(
                            R.string.hist_selected,
                            fmt(day.dayKey, dayFormat),
                            day.perApp[Apps.YOUTUBE] ?: 0,
                            day.perApp[Apps.INSTAGRAM] ?: 0,
                        ),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            if (s.days.all { it.total == 0 }) {
                Text(
                    stringResource(R.string.hist_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatTile("${s.stats.streak}", stringResource(R.string.hist_streak_caption), Modifier.weight(1f))
                StatTile(s.busiestHour?.let { "%02d:00".format(it) } ?: "-", stringResource(R.string.hist_busiest), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                s.stats.best?.let {
                    StatTile("${it.total}", stringResource(R.string.hist_best_caption, fmt(it.dayKey, shortFormat)), Modifier.weight(1f))
                }
                s.stats.worst?.let {
                    StatTile("${it.total}", stringResource(R.string.hist_worst_caption, fmt(it.dayKey, shortFormat)), Modifier.weight(1f))
                }
            }
        }
    }
}
