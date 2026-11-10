package com.reelguard.detection

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** FNV-1a 64-bit. Used so that text is only ever compared as a hash, in memory. */
object Hashing {
    private const val OFFSET = -3750763034362895579L
    private const val PRIME = 1099511628211L

    fun hash(text: CharSequence): Long {
        var h = OFFSET
        for (ch in text) {
            h = (h xor ch.code.toLong()) * PRIME
        }
        return h
    }

    fun combine(
        a: Long,
        b: Long,
    ): Long = (a xor b) * PRIME + 31

    fun short(h: Long): String = (h ushr 32).toInt().toUInt().toString(16).padStart(8, '0')
}

@Suppress("DEPRECATION")
internal fun AccessibilityNodeInfo.recycleCompat() = recycle()

data class ScanDebug(
    val a: Boolean,
    val b: Boolean,
    val c: Boolean,
    val idMatches: Int,
    val actionMatches: Int,
    val pagerFound: Boolean,
    val nodes: Int,
    val scanMs: Long,
    val signature: Long?,
    val isAd: Boolean,
    /** Which signature source produced [signature]: ids, index, duration, text, none. */
    val signatureSource: String = "none",
)

/**
 * Turns an accessibility node tree into one [Frame] using the per-app [SelectorEntry].
 * One bounded DFS (invisible subtrees are skipped). No text is stored or logged: only booleans and hashes leave this class.
 */
class SurfaceDetector {
    private class Ctx(val entry: SelectorEntry, val screen: Rect) {
        var nodes = 0
        val idMatches = HashSet<String>()
        val actionHits = HashSet<String>()
        val signatureHashes = ArrayList<Long>(4)
        val fallbackHashes = ArrayList<Long>(6)
        var pagerFound = false
        var itemIndex = -1
        var durationHash: Long? = null
        var isAd = false
        var signalC = false
        var tabSelected = false
        val actions = entry.actionDescriptions.map { it.lowercase() }
        val tabs = entry.tabLabels.map { it.lowercase() }
        val titles = entry.titleKeywords.map { it.lowercase() }
    }

    fun scan(
        root: AccessibilityNodeInfo,
        entry: SelectorEntry,
        pkg: String,
        timeMs: Long,
        verticalScroll: Boolean,
    ): Pair<Frame, ScanDebug> {
        val t0 = System.nanoTime()
        val screen = Rect().also { root.getBoundsInScreen(it) }
        val ctx = Ctx(entry, screen)
        visit(root, 0, ctx, false)

        val a = ctx.idMatches.isNotEmpty()
        val b = ctx.pagerFound && ctx.actionHits.size >= entry.minActionMatches
        val c = ctx.signalC
        // Signature sources, best first: configured text ids, the pager's adapter position, the video length, caption text.
        val (sig, source) =
            when {
                ctx.signatureHashes.isNotEmpty() -> ctx.signatureHashes.reduce(Hashing::combine) to "ids"
                ctx.itemIndex >= 0 -> Hashing.hash("item#${ctx.itemIndex}") to "index"
                ctx.durationHash != null -> ctx.durationHash to "duration"
                ctx.fallbackHashes.isNotEmpty() -> ctx.fallbackHashes.reduce(Hashing::combine) to "text"
                else -> null to "none"
            }
        val frame =
            Frame(
                timeMs = timeMs,
                pkg = pkg,
                signalA = a,
                signalB = b,
                signalC = c,
                signature = sig,
                isAd = ctx.isAd,
                pagerScrolling = verticalScroll && ctx.pagerFound,
                tabSelected = ctx.tabSelected,
            )
        val ms = (System.nanoTime() - t0) / 1_000_000
        return frame to ScanDebug(a, b, c, ctx.idMatches.size, ctx.actionHits.size, ctx.pagerFound, ctx.nodes, ms, sig, ctx.isAd, source)
    }

    private fun visit(
        node: AccessibilityNodeInfo,
        depth: Int,
        ctx: Ctx,
        inPager: Boolean,
    ) {
        ctx.nodes++
        val idName = node.viewIdResourceName?.substringAfter(":id/")
        val label = (node.contentDescription ?: node.text)?.toString()
        val lower = label?.lowercase()
        val cls = node.className?.toString().orEmpty()

        val isPager = checkPager(node, cls, ctx)
        collectItemIdentity(node, cls, label, inPager || isPager, ctx)
        if (idName != null) collectIds(idName, label, ctx)
        if (lower != null && lower.isNotBlank()) collectLabel(node, lower, cls, inPager || isPager, ctx)

        if (depth >= MAX_DEPTH) return
        val count = node.childCount
        for (i in 0 until count) {
            if (ctx.nodes >= MAX_NODES) return
            val child = node.getChild(i) ?: continue
            try {
                if (child.isVisibleToUser) visit(child, depth + 1, ctx, inPager || isPager)
            } finally {
                child.recycleCompat()
            }
        }
    }

    /** A scrollable pager-like node that fills (almost) the whole window. */
    private fun checkPager(
        node: AccessibilityNodeInfo,
        cls: String,
        ctx: Ctx,
    ): Boolean {
        // Not requiring isScrollable: YouTube's Shorts RecyclerView reports scrollable=false on some versions.
        if (ctx.entry.pagerClasses.none { cls.contains(it) }) return false
        val r = Rect().also { node.getBoundsInScreen(it) }
        val w = ctx.screen.width().coerceAtLeast(1)
        val h = ctx.screen.height().coerceAtLeast(1)
        val fills = r.width() >= w * ctx.entry.pagerFillRatio && r.height() >= h * ctx.entry.pagerFillRatio
        if (fills) ctx.pagerFound = true
        return fills
    }

    /**
     * Text-free identity of the current item, for apps whose Short/Reel exposes no text: the pager's adapter position
     * (CollectionItemInfo) or the video length from the seek bar ("... of 1 minute 23 seconds"; hashed).
     */
    private fun collectItemIdentity(
        node: AccessibilityNodeInfo,
        cls: String,
        label: String?,
        inPager: Boolean,
        ctx: Ctx,
    ) {
        if (ctx.itemIndex < 0 && inPager) {
            node.collectionItemInfo?.let { ctx.itemIndex = maxOf(it.rowIndex, it.columnIndex) }
        }
        val seekBarLabel = if (cls.endsWith("SeekBar")) label else null
        if (ctx.durationHash == null && seekBarLabel != null && seekBarLabel.contains(" of ")) {
            ctx.durationHash = Hashing.hash(seekBarLabel.substringAfter(" of "))
        }
    }

    private fun collectIds(
        idName: String,
        label: String?,
        ctx: Ctx,
    ) {
        if (idName in ctx.entry.viewIds) ctx.idMatches += idName
        if (idName in ctx.entry.adViewIds) ctx.isAd = true
        if (idName in ctx.entry.signatureViewIds && !label.isNullOrBlank()) ctx.signatureHashes += Hashing.hash(label)
    }

    private fun collectLabel(
        node: AccessibilityNodeInfo,
        lower: String,
        cls: String,
        inPager: Boolean,
        ctx: Ctx,
    ) {
        var actionHit = false
        for (act in ctx.actions) {
            if (lower.contains(act)) {
                ctx.actionHits += act
                actionHit = true
            }
        }
        val selectedTab = node.isSelected && ctx.tabs.any { lower.contains(it) }
        if (selectedTab) ctx.tabSelected = true
        if (!ctx.signalC) ctx.signalC = selectedTab || ctx.titles.any { lower.trim() == it }
        val looksLikeCaption = cls.endsWith("TextView") && lower.length >= 3 && lower.any { it.isLetter() }
        val wantsMore = inPager && !actionHit
        if (wantsMore && looksLikeCaption && ctx.fallbackHashes.size < 6) {
            ctx.fallbackHashes += Hashing.hash(lower)
        }
    }

    companion object {
        const val MAX_NODES = 500
        const val MAX_DEPTH = 40
    }
}
