package dev.weft.undercurrent.feature.miniapps

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest

class RoutingMiniAppActionInvokerTest : BehaviorSpec({

    val offerable = OfferableActions.readMostlyDefaults()

    Given("an invoker with a handler registered for an offerable action") {
        val handler = MiniAppActionHandler { _, args -> """{"echo":$args}""" }
        val invoker = RoutingMiniAppActionInvoker(offerable, mapOf("system_info" to handler))

        When("the offerable action is invoked") {
            Then("its handler runs and the args are passed through") {
                runTest {
                    invoker.invoke(null, "system_info", """{"k":1}""") shouldBe """{"echo":{"k":1}}"""
                }
            }
        }

        When("an action that is registered but not offerable is invoked") {
            val gated = RoutingMiniAppActionInvoker(
                offerable,
                mapOf("delete_data" to MiniAppActionHandler { _, _ -> "ran" }),
            )
            Then("it resolves to null without running the handler") {
                runTest {
                    gated.invoke(null, "delete_data", "null") shouldBe null
                }
            }
        }

        When("an offerable action with no registered handler is invoked") {
            Then("it resolves to null") {
                runTest {
                    invoker.invoke(null, "share", "null") shouldBe null
                }
            }
        }

        When("the handler throws") {
            val boom = RoutingMiniAppActionInvoker(
                offerable,
                mapOf("system_info" to MiniAppActionHandler { _, _ -> error("boom") }),
            )
            Then("the throwable propagates to the bridge") {
                runTest {
                    shouldThrow<IllegalStateException> { boom.invoke(null, "system_info", "null") }
                }
            }
        }
    }
})
