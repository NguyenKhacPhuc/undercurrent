package dev.weft.undercurrent.core.ext

import app.cash.turbine.test
import dev.weft.undercurrent.core.domain.Result
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest

class AsResultTest : BehaviorSpec({

    Given("a flow that emits a single value") {
        When("asResult() is collected") {
            Then("it emits [Loading, Success(value)] in order") {
                runTest {
                    flowOf("hello").asResult().test {
                        awaitItem() shouldBe Result.Loading
                        awaitItem() shouldBe Result.Success("hello")
                        awaitComplete()
                    }
                }
            }
        }
    }

    Given("a flow that throws") {
        When("asResult() is collected") {
            Then("it emits [Loading, Error(exception)] in order, and the exception is the one thrown") {
                runTest {
                    val boom = IllegalStateException("boom")
                    flow<String> { throw boom }.asResult().test {
                        awaitItem() shouldBe Result.Loading
                        val err = awaitItem()
                        err.shouldBeInstanceOf<Result.Error>()
                        err.exception shouldBe boom
                        awaitComplete()
                    }
                }
            }
        }
    }

    Given("a flow that emits multiple values") {
        When("asResult() is collected") {
            Then("it emits [Loading, Success(v1), Success(v2), …] preserving order") {
                runTest {
                    flowOf(1, 2, 3).asResult().test {
                        awaitItem() shouldBe Result.Loading
                        awaitItem() shouldBe Result.Success(1)
                        awaitItem() shouldBe Result.Success(2)
                        awaitItem() shouldBe Result.Success(3)
                        awaitComplete()
                    }
                }
            }
        }
    }
})
