package dev.weft.undercurrent.core.domain

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull

class AuthValidationTest : BehaviorSpec({

    Given("AuthValidation.displayNameError") {
        When("the value is empty after trim") {
            Then("returns an error") { AuthValidation.displayNameError("   ").shouldNotBeNull() }
        }
        When("the value is over 40 characters") {
            Then("returns an error") { AuthValidation.displayNameError("a".repeat(41)).shouldNotBeNull() }
        }
        When("the value is a single emoji") {
            Then("passes (unicode allowed)") { AuthValidation.displayNameError("🦊") shouldBe null }
        }
        When("the value is exactly 40 chars after trim") {
            Then("passes") { AuthValidation.displayNameError("a".repeat(40)) shouldBe null }
        }
    }

    Given("AuthValidation.emailError") {
        When("the value contains whitespace") {
            Then("returns an error") { AuthValidation.emailError("a b@c.com").shouldNotBeNull() }
        }
        When("the value has no @") {
            Then("returns an error") { AuthValidation.emailError("ac.com").shouldNotBeNull() }
        }
        When("the value has @ but no dot after") {
            Then("returns an error") { AuthValidation.emailError("a@com").shouldNotBeNull() }
        }
        When("the value ends with a dot") {
            Then("returns an error") { AuthValidation.emailError("a@c.").shouldNotBeNull() }
        }
        When("the value is the simplest valid form") {
            Then("passes") { AuthValidation.emailError("a@b.c") shouldBe null }
        }
        When("the value is a normal address") {
            Then("passes") { AuthValidation.emailError("phuc@example.com") shouldBe null }
        }
    }

    Given("AuthValidation.passwordError") {
        When("the value is 7 chars") {
            Then("returns an error") { AuthValidation.passwordError("1234567").shouldNotBeNull() }
        }
        When("the value is exactly 8 chars") {
            Then("passes") { AuthValidation.passwordError("12345678") shouldBe null }
        }
        When("the value is very long") {
            Then("passes (no max)") { AuthValidation.passwordError("p".repeat(200)) shouldBe null }
        }
    }
})
