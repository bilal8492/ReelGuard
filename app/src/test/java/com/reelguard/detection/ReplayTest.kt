package com.reelguard.detection

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Record/replay harness. Recordings are produced by the Debug screen ("Record signals") and copied to
 * src/test/resources/recordings/<name>.json together with an expected count in <name>.expected.
 * The bundled sample is SYNTHETIC (hand-written), not a real device capture.
 */
class ReplayTest {
    private val dir = File("src/test/resources/recordings")

    private fun replay(
        signals: List<UiSignal>,
        countAds: Boolean = false,
    ): List<CountEvent> {
        val counter =
            ReelCounter(mapOf("com.google.android.youtube" to CounterParams(), "com.instagram.android" to CounterParams()), countAds)
        return signals.flatMap { counter.process(it) }
    }

    @Test
    fun `encode and decode round trip`() {
        val original =
            listOf(
                Frame(1, "p", signalA = true, signalB = false, signalC = true, signature = 42, pagerScrolling = true),
                Tick(5),
                AppLeft(9, "p"),
            )
        SignalJson.decode(SignalJson.encode(original)) shouldBe original
    }

    @Test
    fun `every recording replays to its expected count`() {
        val files = dir.listFiles { f -> f.name.endsWith(".json") }.orEmpty()
        (files.isNotEmpty()) shouldBe true
        for (f in files) {
            val expected = File(dir, f.nameWithoutExtension + ".expected").readText().trim().toInt()
            val events = replay(SignalJson.decode(f.readText()))
            withClue(f.name) { events.count { it is CountEvent.Counted } shouldBe expected }
        }
    }

    private inline fun <T> withClue(
        clue: String,
        block: () -> T,
    ): T = io.kotest.assertions.withClue(clue, block)
}
