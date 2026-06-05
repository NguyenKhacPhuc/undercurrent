package dev.weft.undercurrent.feature.miniapps

import dev.weft.undercurrent.core.model.MiniApp
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

class HtmlMiniAppRenderTreeTest : BehaviorSpec({

    val app = MiniApp(
        id = "calc.1",
        name = "Calc",
        emoji = "C",
        triggerPrompt = "",
        createdAtEpochMs = 0,
        htmlDocument = "<h1>Hi</h1>",
        declaredScopes = setOf("http_fetch"),
    )

    Given("a saved HTML mini-app") {
        When("its render tree is built") {
            val tree = htmlMiniAppRenderTree(app)
            Then("it is a single bridged Html node carrying the document, id and runScripts") {
                tree.type shouldBe "Html"
                tree.children.shouldBeEmpty()
                tree.props["html"]!!.jsonPrimitive.content shouldBe "<h1>Hi</h1>"
                tree.props["miniAppId"]!!.jsonPrimitive.content shouldBe "calc.1"
                tree.props["runScripts"]!!.jsonPrimitive.boolean shouldBe true
            }
        }
    }
})
