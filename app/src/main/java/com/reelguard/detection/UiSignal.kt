package com.reelguard.detection

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Input of the pure-Kotlin counting engine. Produced by the Android-side SurfaceDetector, or loaded from a recording.
 * Contains only booleans, timestamps and a hashed signature. Never any text.
 */
@Serializable
sealed interface UiSignal {
    val timeMs: Long
    val pkg: String
}

/** One observation of a monitored app's window. */
@Serializable
@SerialName("frame")
data class Frame(
    override val timeMs: Long,
    override val pkg: String,
    val signalA: Boolean,
    val signalB: Boolean,
    val signalC: Boolean,
    /** Hash of the visible item's identity (creator + caption/title). Null when it could not be read. */
    val signature: Long? = null,
    val isAd: Boolean = false,
    /** A vertical, full-screen pager is being scrolled right now (used only by the "detection outdated" heuristic). */
    val pagerScrolling: Boolean = false,
) : UiSignal

/** The monitored app left the foreground. */
@Serializable
@SerialName("left")
data class AppLeft(override val timeMs: Long, override val pkg: String) : UiSignal

/** Clock tick so the engine can settle a pending item when no new UI event arrives. [pkg] is ignored. */
@Serializable
@SerialName("tick")
data class Tick(override val timeMs: Long, override val pkg: String = "") : UiSignal

/** Output of the counting engine. */
sealed interface CountEvent {
    val timeMs: Long
    val pkg: String

    data class Counted(override val timeMs: Long, override val pkg: String) : CountEvent

    data class SurfaceEntered(override val timeMs: Long, override val pkg: String) : CountEvent

    data class SurfaceExited(override val timeMs: Long, override val pkg: String) : CountEvent

    /** [outdated] true: selectors seem to no longer match. false: detection works again. */
    data class OutdatedChanged(override val timeMs: Long, override val pkg: String, val outdated: Boolean) : CountEvent
}

/** JSON helpers for the record/replay harness. */
object SignalJson {
    val json =
        Json {
            ignoreUnknownKeys = true
            prettyPrint = false
            encodeDefaults = false
        }

    fun encode(signals: List<UiSignal>): String =
        json.encodeToString(kotlinx.serialization.builtins.ListSerializer(UiSignal.serializer()), signals)

    fun decode(text: String): List<UiSignal> =
        json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(UiSignal.serializer()), text)
}
