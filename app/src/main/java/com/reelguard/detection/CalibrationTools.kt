package com.reelguard.detection

import android.content.Context
import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class DumpNode(
    val depth: Int,
    val cls: String,
    val id: String? = null,
    /** Free text is replaced by "h:<hash>#<length>" unless it equals a label configured in selectors.json. */
    val desc: String? = null,
    val text: String? = null,
    val bounds: String,
    val flags: String,
)

/** Writes the accessibility tree to a local file for selector calibration. Free text is hashed (see [DumpNode.desc]). */
class NodeDumper(private val context: Context) {
    fun dump(
        root: AccessibilityNodeInfo,
        pkg: String,
        safeLabels: Set<String>,
    ): File {
        val out = ArrayList<DumpNode>()
        walk(root, 0, out, safeLabels.map { it.lowercase() }.toSet())
        val dir = File(context.filesDir, "dumps").apply { mkdirs() }
        val file = File(dir, "dump-$pkg-${System.currentTimeMillis()}.json")
        file.writeText(prettyJson.encodeToString(ListSerializer(DumpNode.serializer()), out))
        return file
    }

    private fun walk(
        n: AccessibilityNodeInfo,
        depth: Int,
        out: MutableList<DumpNode>,
        safe: Set<String>,
    ) {
        if (out.size >= MAX_NODES) return
        val r = Rect().also { n.getBoundsInScreen(it) }
        val flags =
            buildString {
                if (n.isClickable) append('C')
                if (n.isScrollable) append('S')
                if (n.isSelected) append('Z')
                if (n.isVisibleToUser) append('V')
                if (n.isChecked) append('K')
            }
        out +=
            DumpNode(
                depth = depth,
                cls = n.className?.toString().orEmpty(),
                id = n.viewIdResourceName,
                desc = scrub(n.contentDescription, safe),
                text = scrub(n.text, safe),
                bounds = "${r.left},${r.top},${r.right},${r.bottom}",
                flags = flags,
            )
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            try {
                walk(c, depth + 1, out, safe)
            } finally {
                c.recycleCompat()
            }
        }
    }

    private fun scrub(
        value: CharSequence?,
        safe: Set<String>,
    ): String? {
        val s = value?.toString()?.takeIf { it.isNotBlank() } ?: return null
        return if (s.lowercase().trim() in safe) s else "h:${Hashing.short(Hashing.hash(s))}#${s.length}"
    }

    companion object {
        private val prettyJson = Json { prettyPrint = true }
        const val MAX_NODES = 3000
    }
}

/** Records [UiSignal]s from a real session so they can be replayed in unit tests (docs/SELECTORS.md). */
class SignalRecorder(private val context: Context) {
    private val buffer = ArrayList<UiSignal>()

    @Volatile var recording = false
        private set

    val size: Int get() = synchronized(buffer) { buffer.size }

    fun start() {
        synchronized(buffer) { buffer.clear() }
        recording = true
    }

    fun record(signal: UiSignal) {
        if (!recording) return
        synchronized(buffer) {
            if (buffer.size < MAX_SIGNALS) buffer += signal
        }
    }

    fun stop(): File? {
        recording = false
        val copy = synchronized(buffer) { buffer.toList().also { buffer.clear() } }
        if (copy.isEmpty()) return null
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        return File(dir, "rec-${System.currentTimeMillis()}.json").also { it.writeText(SignalJson.encode(copy)) }
    }

    companion object {
        const val MAX_SIGNALS = 20_000
    }
}
