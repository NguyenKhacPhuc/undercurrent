package dev.weft.undercurrent.feature.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme

/**
 * Minimal markdown renderer tuned for chat responses (paragraphs,
 * lists, code blocks, simple inline emphasis). Hand-rolled — no
 * dependency footprint, total styling control, streaming-safe (a
 * half-written code fence renders as text until the closing token
 * arrives).
 *
 * KMP — commonMain. Moved from
 * `app/.../features/chat/MarkdownText.kt`. Adjustments:
 *   - `openInBrowser` (Android Custom Tabs) + `Color.toArgb()` removed;
 *     the host passes [onLinkClick] which decides how to handle URLs
 *     (CCT on Android, `SFSafariViewController` on iOS).
 *   - Theme imports from `:core:design-system`.
 *   - All parsing is pure stdlib (Regex, StringBuilder, String ops).
 */
@Composable
public fun MarkdownText(
    text: String,
    onLinkClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    val blocks = remember(text) { parseMarkdown(text) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Paragraph -> Text(
                    text = renderInline(block.text, accent = colors.accent, codeInk = colors.codeInk, onLinkClick = onLinkClick),
                    style = typography.serifBody.copy(color = colors.ink),
                    modifier = Modifier.fillMaxWidth(),
                )
                is MdBlock.Heading -> Text(
                    text = renderInline(block.text, accent = colors.accent, codeInk = colors.codeInk, onLinkClick = onLinkClick),
                    style = typography.sansHeader.copy(
                        color = colors.ink,
                        fontSize = when (block.level) {
                            1 -> typography.sansHeader.fontSize * 1.4f
                            2 -> typography.sansHeader.fontSize * 1.15f
                            else -> typography.sansHeader.fontSize
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                is MdBlock.BulletList -> Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    for (item in block.items) {
                        Row {
                            Text(
                                text = "•",
                                style = typography.serifBody.copy(color = colors.inkMuted),
                                modifier = Modifier.width(20.dp),
                            )
                            Text(
                                text = renderInline(item, accent = colors.accent, codeInk = colors.codeInk, onLinkClick = onLinkClick),
                                style = typography.serifBody.copy(color = colors.ink),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                is MdBlock.NumberedList -> Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    block.items.forEachIndexed { idx, item ->
                        Row {
                            Text(
                                text = "${idx + 1}.",
                                style = typography.serifBody.copy(color = colors.inkMuted),
                                modifier = Modifier.width(24.dp),
                            )
                            Text(
                                text = renderInline(item, accent = colors.accent, codeInk = colors.codeInk, onLinkClick = onLinkClick),
                                style = typography.serifBody.copy(color = colors.ink),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
                is MdBlock.CodeBlock -> Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shapes.small)
                        .background(colors.codeBg)
                        .border(width = 1.dp, color = colors.codeBorder, shape = shapes.small)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (!block.language.isNullOrBlank()) {
                        Text(
                            text = block.language,
                            style = typography.sansLabel.copy(color = colors.inkSubtle),
                        )
                        Spacer(Modifier.padding(2.dp))
                    }
                    Text(
                        text = block.code,
                        style = typography.mono.copy(color = colors.codeInk),
                    )
                }
            }
        }
    }
}

private sealed interface MdBlock {
    data class Paragraph(val text: String) : MdBlock
    data class Heading(val level: Int, val text: String) : MdBlock
    data class BulletList(val items: List<String>) : MdBlock
    data class NumberedList(val items: List<String>) : MdBlock
    data class CodeBlock(val language: String?, val code: String) : MdBlock
}

private val BulletLine = Regex("^[-*] (.+)$")
private val NumberedLine = Regex("^\\d+\\. (.+)$")

private fun parseMarkdown(source: String): List<MdBlock> {
    val blocks = mutableListOf<MdBlock>()
    val lines = source.split('\n')
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        when {
            line.startsWith("```") -> {
                val lang = line.removePrefix("```").trim().ifBlank { null }
                val sb = StringBuilder()
                i++
                while (i < lines.size && !lines[i].startsWith("```")) {
                    if (sb.isNotEmpty()) sb.append('\n')
                    sb.append(lines[i])
                    i++
                }
                if (i < lines.size) i++
                blocks += MdBlock.CodeBlock(lang, sb.toString())
            }
            line.startsWith("### ") -> {
                blocks += MdBlock.Heading(3, line.substring(4))
                i++
            }
            line.startsWith("## ") -> {
                blocks += MdBlock.Heading(2, line.substring(3))
                i++
            }
            line.startsWith("# ") -> {
                blocks += MdBlock.Heading(1, line.substring(2))
                i++
            }
            BulletLine.matches(line) -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val m = BulletLine.matchEntire(lines[i]) ?: break
                    items += m.groupValues[1]
                    i++
                }
                blocks += MdBlock.BulletList(items)
            }
            NumberedLine.matches(line) -> {
                val items = mutableListOf<String>()
                while (i < lines.size) {
                    val m = NumberedLine.matchEntire(lines[i]) ?: break
                    items += m.groupValues[1]
                    i++
                }
                blocks += MdBlock.NumberedList(items)
            }
            line.isBlank() -> i++
            else -> {
                val sb = StringBuilder()
                while (i < lines.size && lines[i].isNotBlank()
                    && !lines[i].startsWith("```")
                    && !lines[i].startsWith("# ")
                    && !lines[i].startsWith("## ")
                    && !lines[i].startsWith("### ")
                    && !BulletLine.matches(lines[i])
                    && !NumberedLine.matches(lines[i])
                ) {
                    if (sb.isNotEmpty()) sb.append(' ')
                    sb.append(lines[i])
                    i++
                }
                blocks += MdBlock.Paragraph(sb.toString())
            }
        }
    }
    return blocks
}

private fun renderInline(
    text: String,
    accent: Color,
    codeInk: Color,
    onLinkClick: (String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    scanInline(this, text, accent = accent, codeInk = codeInk, onLinkClick = onLinkClick)
}

private fun scanInline(
    out: AnnotatedString.Builder,
    text: String,
    accent: Color,
    codeInk: Color,
    onLinkClick: (String) -> Unit,
) {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", i) -> {
                val close = text.indexOf("**", i + 2)
                if (close != -1) {
                    out.withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        scanInline(this, text.substring(i + 2, close), accent, codeInk, onLinkClick)
                    }
                    i = close + 2
                } else {
                    out.append(text[i]); i++
                }
            }
            (text[i] == '*' || text[i] == '_') -> {
                val marker = text[i]
                if (marker == '*' && i + 1 < text.length && text[i + 1] == '*') {
                    out.append(text[i]); i++
                    continue
                }
                val close = text.indexOf(marker, i + 1)
                if (close != -1 && close > i + 1) {
                    out.withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        scanInline(this, text.substring(i + 1, close), accent, codeInk, onLinkClick)
                    }
                    i = close + 1
                } else {
                    out.append(text[i]); i++
                }
            }
            text[i] == '`' -> {
                val close = text.indexOf('`', i + 1)
                if (close != -1) {
                    out.withStyle(
                        SpanStyle(
                            color = codeInk,
                            fontFamily = FontFamily.Monospace,
                        ),
                    ) {
                        append(text.substring(i + 1, close))
                    }
                    i = close + 1
                } else {
                    out.append(text[i]); i++
                }
            }
            text[i] == '[' -> {
                val textClose = text.indexOf(']', i + 1)
                if (textClose != -1
                    && textClose + 1 < text.length
                    && text[textClose + 1] == '('
                ) {
                    val urlClose = text.indexOf(')', textClose + 2)
                    if (urlClose != -1) {
                        val label = text.substring(i + 1, textClose)
                        val url = text.substring(textClose + 2, urlClose)
                        out.withLink(
                            LinkAnnotation.Url(
                                url = url,
                                styles = TextLinkStyles(
                                    style = SpanStyle(
                                        color = accent,
                                        textDecoration = TextDecoration.Underline,
                                    ),
                                ),
                                linkInteractionListener = LinkInteractionListener {
                                    onLinkClick(url)
                                },
                            ),
                        ) {
                            scanInline(this, label, accent, codeInk, onLinkClick)
                        }
                        i = urlClose + 1
                    } else {
                        out.append(text[i]); i++
                    }
                } else {
                    out.append(text[i]); i++
                }
            }
            else -> { out.append(text[i]); i++ }
        }
    }
}
