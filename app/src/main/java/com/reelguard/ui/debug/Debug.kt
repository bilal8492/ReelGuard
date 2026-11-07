package com.reelguard.ui.debug

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.reelguard.R
import com.reelguard.data.datastore.Apps
import com.reelguard.detection.AppDebug
import com.reelguard.detection.DebugState
import com.reelguard.detection.DetectionEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

data class DebugUi(val state: DebugState, val counts: Map<String, Int>)

@HiltViewModel
class DebugViewModel
    @Inject
    constructor(private val engine: DetectionEngine) : ViewModel() {
        val ui: StateFlow<DebugUi> =
            combine(engine.debug, engine.limitState) { d, ls -> DebugUi(d, ls?.counts.orEmpty()) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DebugUi(DebugState(), emptyMap()))

        fun setActive(active: Boolean) {
            engine.debugActive = active
        }

        fun toggleRecording(recording: Boolean) = if (recording) engine.stopRecording() else engine.startRecording()

        fun dump() = engine.requestDump(DUMP_DELAY_MS)

        companion object {
            const val DUMP_DELAY_MS = 8_000L
        }
    }

private fun share(
    context: Context,
    path: String,
) {
    val file = File(path)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
    val send =
        Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(send, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
fun DebugScreen(vm: DebugViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    DisposableEffect(Unit) {
        vm.setActive(true)
        onDispose { vm.setActive(false) }
    }
    val context = LocalContext.current
    val d = ui.state
    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(16.dp).fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.dbg_hint), style = MaterialTheme.typography.bodySmall)
        Text(stringResource(R.string.dbg_foreground, d.foregroundPkg.ifEmpty { "-" }))
        Text(stringResource(R.string.dbg_service, d.serviceConnected.toString()))
        Apps.all.forEach { pkg -> AppCard(pkg, d.perApp[pkg] ?: AppDebug(), d.versionCodes[pkg] ?: 0, ui.counts[pkg] ?: 0) }

        OutlinedButton(onClick = vm::dump, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(stringResource(R.string.dbg_dump, DebugViewModel.DUMP_DELAY_MS / 1000))
        }
        d.lastDump?.let {
            Text(stringResource(R.string.dbg_last_file, it), style = MaterialTheme.typography.bodySmall)
            if (File(it).exists()) OutlinedButton(onClick = { share(context, it) }) { Text(stringResource(R.string.dbg_share)) }
        }
        Button(onClick = { vm.toggleRecording(d.recording) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(if (d.recording) stringResource(R.string.dbg_stop_rec, d.recordedSignals) else stringResource(R.string.dbg_start_rec))
        }
        d.lastRecording?.let {
            Text(stringResource(R.string.dbg_last_file, it), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = { share(context, it) }) { Text(stringResource(R.string.dbg_share)) }
        }
    }
}

@Composable
private fun AppCard(
    pkg: String,
    a: AppDebug,
    version: Long,
    count: Int,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(Apps.label(pkg), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.dbg_version, version))
            Text(stringResource(R.string.dbg_signals, a.signalA.mark(), a.signalB.mark(), a.signalC.mark()))
            Text(stringResource(R.string.dbg_details, a.idMatches, a.actionMatches, a.pagerFound.mark()))
            Text(stringResource(R.string.dbg_surface, if (a.surfaceActive) pkgSurface(pkg) else stringResource(R.string.dbg_none)))
            Text(stringResource(R.string.dbg_signature, a.signature))
            Text(stringResource(R.string.dbg_count, count))
            Text(stringResource(R.string.dbg_perf, a.nodes, a.scanMs))
            if (a.outdated) Text(stringResource(R.string.banner_outdated, Apps.label(pkg)), color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun Boolean.mark() = if (this) "✔" else "✘"

private fun pkgSurface(pkg: String) = if (pkg == Apps.YOUTUBE) "Shorts" else "Reels"
