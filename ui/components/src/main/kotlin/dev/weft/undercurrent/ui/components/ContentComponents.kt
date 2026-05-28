package dev.weft.undercurrent.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// Markdown — render simple inline markdown
// Supported: **bold**, _italic_, `code`, [text](url) — no block-level parsing
// =============================================================================

@Serializable
internal data class MarkdownProps(
    val text: String,
    /** body (default serif) | sans | small. */
    val variant: String = "body",
)

internal class MarkdownComponent : WeftComponent<MarkdownProps>(
    name = "Markdown",
    description = "Inline markdown rendering. Supports **bold**, _italic_ or *italic*, `code`, and [text](url) (links are styled but not interactive). text: required. variant: body (serif, default) / sans / small. Use for prose where you want emphasis without manual styling.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = MarkdownProps.serializer(),
) {
    @Composable
    override fun Render(props: MarkdownProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val style = when (props.variant.lowercase()) {
            "sans" -> tp.sansLabel.copy(fontSize = 15.sp, lineHeight = 22.sp)
            "small" -> tp.sansSmall
            else -> tp.serifBody.copy(fontSize = 15.sp, lineHeight = 23.sp)
        }
        Text(
            text = parseMarkdown(props.text, cs.accent, cs.surfaceMuted, cs.codeInk),
            style = style,
            color = cs.ink,
        )
    }
}

/**
 * Minimal regex-based markdown→AnnotatedString. Supports:
 *  - **bold**
 *  - _italic_ / *italic*
 *  - `inline code`
 *  - [text](url)  (no click handling — just styled)
 *
 * Cheap and predictable. Not a full markdown engine — the model is
 * instructed via the description to stick to these four constructs.
 */
private fun parseMarkdown(
    src: String,
    accentColor: androidx.compose.ui.graphics.Color,
    codeBg: androidx.compose.ui.graphics.Color,
    codeFg: androidx.compose.ui.graphics.Color,
): AnnotatedString = buildAnnotatedString {
    // Single regex with alternation; we walk matches in order and emit the
    // intervening plain text between them. Avoids nested-pattern surprises.
    val pattern = Regex("""\*\*([^*]+)\*\*|__([^_]+)__|_([^_]+)_|\*([^*]+)\*|`([^`]+)`|\[([^\]]+)\]\(([^)]+)\)""")
    var cursor = 0
    pattern.findAll(src).forEach { m ->
        if (m.range.first > cursor) append(src.substring(cursor, m.range.first))
        when {
            m.groupValues[1].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold), m.groupValues[1])
            m.groupValues[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.Bold), m.groupValues[2])
            m.groupValues[3].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic), m.groupValues[3])
            m.groupValues[4].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic), m.groupValues[4])
            m.groupValues[5].isNotEmpty() -> withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    background = codeBg,
                    color = codeFg,
                    fontSize = 13.sp,
                ),
                m.groupValues[5],
            )
            m.groupValues[6].isNotEmpty() -> withStyle(
                SpanStyle(color = accentColor, textDecoration = TextDecoration.Underline),
                m.groupValues[6],
            )
        }
        cursor = m.range.last + 1
    }
    if (cursor < src.length) append(src.substring(cursor))
}

private fun AnnotatedString.Builder.withStyle(style: SpanStyle, text: String) {
    pushStyle(style)
    append(text)
    pop()
}

// =============================================================================
// CodeBlock — monospace code with language label
// =============================================================================

@Serializable
internal data class CodeBlockProps(
    val code: String,
    val language: String = "",
    /** Max height in dp before the block scrolls vertically. 0 = no cap. */
    val maxHeight: Int = 320,
)

internal class CodeBlockComponent : WeftComponent<CodeBlockProps>(
    name = "CodeBlock",
    description = "A formatted code block in a tinted box with optional language label. code: required (preserves whitespace and newlines). language: optional label shown in the corner. maxHeight: caps vertical size before scrolling (default 320; 0 = no cap). Use for code samples, JSON output, command snippets.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = CodeBlockProps.serializer(),
) {
    @Composable
    override fun Render(props: CodeBlockProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val shape = UndercurrentTheme.shapes.small
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(cs.codeBg)
                .border(1.dp, cs.codeBorder, shape),
        ) {
            if (props.language.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = props.language.lowercase(),
                        style = tp.sansSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = cs.accent,
                    )
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cs.codeBorder))
            }
            val codeModifier = if (props.maxHeight > 0) {
                Modifier
                    .fillMaxWidth()
                    .height(props.maxHeight.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp)
            } else {
                Modifier.fillMaxWidth().padding(12.dp)
            }
            Text(
                text = props.code,
                style = tp.serifBody.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                ),
                color = cs.codeInk,
                modifier = codeModifier,
            )
        }
    }
}

// =============================================================================
// Diff — +/- line-prefixed diff with red/green tinting
// =============================================================================

@Serializable
internal data class DiffProps(
    /** Lines as they would appear in `git diff` — each starting with '+', '-', or ' '. */
    val lines: List<String>,
    val title: String = "",
)

internal class DiffComponent : WeftComponent<DiffProps>(
    name = "Diff",
    description = "git-style diff. lines: required — each line should start with '+', '-', or ' ' (space). '+' lines tint green, '-' tint red, others are context. title: optional filename/header shown above.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = DiffProps.serializer(),
    example = """{"type": "Diff", "props": {"title": "config.yaml", "lines": [" name: app", "-version: 1.0", "+version: 1.1", " debug: false"]}}""",
) {
    @Composable
    override fun Render(props: DiffProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val shape = UndercurrentTheme.shapes.small
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(cs.codeBg)
                .border(1.dp, cs.codeBorder, shape),
        ) {
            if (props.title.isNotBlank()) {
                Text(
                    text = props.title,
                    style = tp.sansSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = cs.accent,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(cs.codeBorder))
            }
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                props.lines.forEach { line ->
                    val (bg, fg, marker) = when {
                        line.startsWith("+") -> Triple(cs.accent.copy(alpha = 0.10f), cs.accent, "+")
                        line.startsWith("-") -> Triple(cs.error.copy(alpha = 0.10f), cs.error, "−")
                        else -> Triple(androidx.compose.ui.graphics.Color.Transparent, cs.codeInk, " ")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().background(bg).padding(horizontal = 12.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = marker,
                            style = tp.serifBody.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                            ),
                            color = fg,
                            modifier = Modifier.width(16.dp),
                        )
                        Text(
                            text = if (line.startsWith("+") || line.startsWith("-")) line.substring(1) else line.removePrefix(" "),
                            style = tp.serifBody.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                            ),
                            color = if (line.startsWith("+") || line.startsWith("-")) cs.ink else cs.codeInk,
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// Spotlight — featured content card (image + title + body + optional action)
// =============================================================================

@Serializable
internal data class SpotlightProps(
    val title: String,
    val body: String = "",
    val imageUrl: String = "",
    /** Short label above the title (uppercase tracking). */
    val kicker: String = "",
    /** Optional CTA. */
    val actionLabel: String = "",
    val actionKey: String = "",
)

internal class SpotlightComponent(private val imageLoader: ImageLoader) : WeftComponent<SpotlightProps>(
    name = "Spotlight",
    description = "Featured content card — large image at top, kicker + title + body + optional CTA below. Use for highlights, recommended items, hero sections. title: required. imageUrl: optional. kicker/body/actionLabel/actionKey: optional.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = SpotlightProps.serializer(),
) {
    @Composable
    override fun Render(props: SpotlightProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val shape = UndercurrentTheme.shapes.medium
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(cs.surfaceMuted),
        ) {
            if (props.imageUrl.isNotBlank()) {
                SubcomposeAsyncImage(
                    model = props.imageUrl,
                    imageLoader = imageLoader,
                    contentDescription = props.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    loading = {
                        Box(modifier = Modifier.fillMaxWidth().height(180.dp).background(cs.surface))
                    },
                    error = {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(180.dp).background(cs.surface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("⌗", style = tp.serifBodyLarge.copy(fontSize = 32.sp), color = cs.inkSubtle)
                        }
                    },
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (props.kicker.isNotBlank()) {
                    Text(
                        text = props.kicker.uppercase(),
                        style = tp.sansSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                        ),
                        color = cs.accent,
                    )
                }
                Text(
                    text = props.title,
                    style = tp.serifBodyLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.ink,
                )
                if (props.body.isNotBlank()) {
                    Text(
                        text = props.body,
                        style = tp.serifBody.copy(fontSize = 14.sp, lineHeight = 21.sp),
                        color = cs.inkMuted,
                    )
                }
                if (props.actionLabel.isNotBlank() && props.actionKey.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .clip(UndercurrentTheme.shapes.small)
                            .background(cs.accent)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = props.actionLabel,
                            style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                            color = cs.onAccent,
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// PhotoFrame — image with caption (editorial photo treatment)
// =============================================================================

@Serializable
internal data class PhotoFrameProps(
    val imageUrl: String,
    val caption: String = "",
    val credit: String = "",
    /** Aspect ratio of the image (width/height). */
    val aspect: Float = 16f / 9f,
)

internal class PhotoFrameComponent(private val imageLoader: ImageLoader) : WeftComponent<PhotoFrameProps>(
    name = "PhotoFrame",
    description = "Editorial photo treatment — image with optional caption and credit underneath, indented like a newspaper figure. imageUrl: required. caption: figure caption. credit: small attribution. aspect: width/height ratio (default 16:9).",
    category = ComponentCategory.DISPLAY,
    propsSerializer = PhotoFrameProps.serializer(),
) {
    @Composable
    override fun Render(props: PhotoFrameProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(modifier = Modifier.fillMaxWidth()) {
            SubcomposeAsyncImage(
                model = props.imageUrl,
                imageLoader = imageLoader,
                contentDescription = props.caption.ifBlank { "photo" },
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(props.aspect)
                    .clip(RoundedCornerShape(2.dp)),
                error = {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp).background(cs.surfaceMuted),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("⌗", style = tp.serifBodyLarge.copy(fontSize = 32.sp), color = cs.inkSubtle)
                    }
                },
            )
            if (props.caption.isNotBlank() || props.credit.isNotBlank()) {
                Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
                    if (props.caption.isNotBlank()) {
                        Text(
                            text = props.caption,
                            style = tp.serifBody.copy(
                                fontSize = 13.sp,
                                fontStyle = FontStyle.Italic,
                                lineHeight = 19.sp,
                            ),
                            color = cs.inkMuted,
                        )
                    }
                    if (props.credit.isNotBlank()) {
                        Text(
                            text = props.credit,
                            style = tp.sansSmall.copy(letterSpacing = 0.5.sp),
                            color = cs.inkSubtle,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Every content-tier component. ImageLoader-bound ones are constructed by the factory. */
internal fun contentComponents(imageLoader: ImageLoader): List<WeftComponent<*>> = listOf(
    MarkdownComponent(),
    CodeBlockComponent(),
    DiffComponent(),
    SpotlightComponent(imageLoader),
    PhotoFrameComponent(imageLoader),
)
