package dev.weft.undercurrent.core.model

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain

class ActionDescriptionTest : BehaviorSpec({

    // Phrases the user reads (present/past/failure) must be plain language —
    // never a raw snake_case action name. The icon field is exempt: icon
    // tokens may legitimately contain underscores (e.g. "arrow_back").
    fun ActionDescription.phrases() = listOf(present, past, failure)

    Given("a recognized action") {
        val d = describeAction("open_map")
        Then("the present form is in-progress (ends with an ellipsis) and reads plainly") {
            d.present shouldEndWith "…"
            d.present.lowercase() shouldContain "map"
        }
        Then("the past form is settled (no ellipsis) and reads plainly") {
            d.past shouldNotContain "…"
            d.past.lowercase() shouldContain "map"
        }
        Then("no phrase leaks the raw action name or an underscore") {
            d.phrases().forEach {
                it shouldNotContain "_"
                it.lowercase() shouldNotContain "open_map"
            }
        }
        Then("it carries an icon token") {
            d.icon.isNotBlank() shouldBe true
        }
    }

    Given("the signature actions the product is built around") {
        val signature = listOf(
            "open_personas", "open_memories", "open_conversations",
            "set_theme_mode", "create_persona", "create_mini_app",
            "data_upsert", "share", "http_fetch", "clipboard_read",
        )
        Then("each resolves to an explicit, non-generic friendly description") {
            signature.forEach { name ->
                describeAction(name) shouldNotBe GenericActionDescription
            }
        }
    }

    Given("the whole action description map") {
        Then("every entry reads as plain language — no snake_case leaks through") {
            // No underscore in any phrase is the real anti-leak guard: every
            // multi-word technical action name carries one, so its absence
            // proves no raw name reached the user.
            knownActions.forEach { name ->
                val d = describeAction(name)
                d.present shouldEndWith "…"
                listOf(d.present, d.past, d.failure).forEach { phrase ->
                    phrase shouldNotContain "_"
                }
                d.icon.isNotBlank() shouldBe true
            }
        }
    }

    Given("an unrecognized action — e.g. one a mini-app introduced") {
        val d = describeAction("frobnicate_widget_42")
        Then("it falls back to a generic, friendly phrase set") {
            d.present shouldEndWith "…"
            d.past.isNotBlank() shouldBe true
            d.failure.isNotBlank() shouldBe true
        }
        Then("the fallback never exposes the raw action name or an underscore") {
            d.phrases().forEach {
                it shouldNotContain "_"
                it.lowercase() shouldNotContain "frobnicate"
            }
        }
    }
})
