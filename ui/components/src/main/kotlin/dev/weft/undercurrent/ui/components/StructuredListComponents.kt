package dev.weft.undercurrent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// StructuredList — sectioned titled lists (recipes, packing lists, guides)
// =============================================================================

@Serializable
internal data class StructuredListSection(
    val title: String,
    val items: List<String>,
    /** bullet | number | check — how to mark each item. */
    val style: String = "bullet",
    /** Small caption shown next to the section title (e.g. "20 min", "4 servings"). */
    val meta: String = "",
)

@Serializable
internal data class StructuredListProps(
    val title: String = "",
    val subtitle: String = "",
    val sections: List<StructuredListSection>,
)

internal class StructuredListComponent : WeftComponent<StructuredListProps>(
    name = "StructuredList",
    description = "Sectioned titled lists — generic enough for recipes (Ingredients + Steps), packing lists, multi-step guides, task lists. title/subtitle: optional doc-level header. sections: list of {title, items, style, meta}. style: 'bullet' (default), 'number' (1,2,3…), 'check' (round empty checks). meta: tiny caption like 'serves 4' next to section title.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = StructuredListProps.serializer(),
    example = """{"type": "StructuredList", "props": {"title": "Carbonara", "subtitle": "20 min · 2 servings", "sections": [{"title": "Ingredients", "meta": "for 2", "style": "bullet", "items": ["200g spaghetti", "100g guanciale", "2 egg yolks + 1 whole egg", "60g pecorino, finely grated", "Black pepper, lots"]}, {"title": "Steps", "style": "number", "items": ["Boil salted water; cook pasta to 1 min before al dente.", "Render guanciale in a wide pan over medium-low.", "Whisk eggs + cheese + black pepper in a bowl.", "Drain pasta (save 1 cup water); toss into guanciale.", "Off heat, add egg mix + splash of pasta water; stir until creamy."]}]}}""",
) {
    @Composable
    override fun Render(props: StructuredListProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (props.title.isNotBlank()) {
                Column {
                    Text(
                        text = props.title,
                        style = tp.serifBodyLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
                        color = cs.ink,
                    )
                    if (props.subtitle.isNotBlank()) {
                        Text(
                            text = props.subtitle,
                            style = tp.sansSmall.copy(letterSpacing = 0.5.sp),
                            color = cs.inkMuted,
                        )
                    }
                }
            }
            props.sections.forEach { section ->
                SectionBlock(section = section, cs = cs, tp = tp)
            }
        }
    }

    @Composable
    private fun SectionBlock(
        section: StructuredListSection,
        cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors,
        tp: dev.weft.undercurrent.core.designsystem.UndercurrentTypography,
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = section.title.uppercase(),
                    style = tp.sansSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.8.sp,
                    ),
                    color = cs.accent,
                    modifier = Modifier.weight(1f),
                )
                if (section.meta.isNotBlank()) {
                    Text(
                        text = section.meta,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 4.dp)) {
                section.items.forEachIndexed { i, item ->
                    StructuredItemRow(
                        index = i + 1,
                        text = item,
                        style = section.style,
                        cs = cs,
                        tp = tp,
                    )
                }
            }
        }
    }

    @Composable
    private fun StructuredItemRow(
        index: Int,
        text: String,
        style: String,
        cs: dev.weft.undercurrent.core.designsystem.UndercurrentColors,
        tp: dev.weft.undercurrent.core.designsystem.UndercurrentTypography,
    ) {
        Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
            when (style.lowercase()) {
                "number" -> Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(cs.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = index.toString(),
                        style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = cs.accent,
                    )
                }
                "check" -> Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(cs.background)
                        .border(1.5.dp, cs.divider, CircleShape),
                )
                else -> Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(cs.accent),
                )
            }
            Text(
                text = text,
                style = tp.serifBody.copy(fontSize = 15.sp, lineHeight = 23.sp),
                color = cs.ink,
                modifier = Modifier.padding(start = 10.dp).weight(1f),
            )
        }
    }
}

/** StructuredList components. */
internal val undercurrentStructuredListComponents: List<WeftComponent<*>> = listOf(
    StructuredListComponent(),
)
