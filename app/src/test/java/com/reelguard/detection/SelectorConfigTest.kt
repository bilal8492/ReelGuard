package com.reelguard.detection

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.io.File

class SelectorConfigTest {
    private val bundled = File("src/main/assets/selectors.json").readText()

    @Test
    fun `bundled selectors parse and cover both apps`() {
        val cfg = SelectorConfig.parse(bundled)
        cfg.apps.keys shouldBe setOf("com.google.android.youtube", "com.instagram.android")
        cfg.notice.contains("VERIFY") shouldBe true
    }

    @Test
    fun `version range entry wins over default`() {
        val text =
            """
            {"apps":{"p":{"label":"x","entries":[
              {"isDefault":true,"viewIds":["old"]},
              {"minVersionCode":100,"maxVersionCode":200,"viewIds":["new"]}
            ]}}}
            """.trimIndent()
        val cfg = SelectorConfig.parse(text)
        cfg.entryFor("p", 150)!!.viewIds shouldBe listOf("new")
        cfg.entryFor("p", 99)!!.viewIds shouldBe listOf("old")
        cfg.entryFor("p", 201)!!.viewIds shouldBe listOf("old")
        cfg.entryFor("missing", 1) shouldBe null
    }

    @Test
    fun `params come from the matching entry`() {
        val cfg =
            SelectorConfig.parse(
                """{"apps":{"p":{"label":"x","entries":[{"isDefault":true,"settleMs":500,"threshold":3}]}}}""",
            )
        cfg.paramsFor(mapOf("p" to 1L))["p"] shouldBe CounterParams(threshold = 3, settleMs = 500)
    }

    @Test
    fun `malformed or empty config is rejected`() {
        shouldThrow<Exception> { SelectorConfig.parse("not json") }
        shouldThrow<IllegalArgumentException> { SelectorConfig.parse("""{"apps":{}}""") }
    }
}
