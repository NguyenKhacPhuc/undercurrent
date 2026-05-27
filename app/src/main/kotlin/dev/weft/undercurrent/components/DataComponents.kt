package dev.weft.undercurrent.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.theme.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// DataTable — proper sortable grid with headers
// =============================================================================

@Serializable
internal data class DataColumn(
    val key: String,
    val label: String,
    /** start | center | end. */
    val align: String = "start",
    /** Optional explicit width. 0 = auto-share evenly. */
    val width: Int = 0,
)

@Serializable
internal data class DataTableProps(
    val id: String,
    val columns: List<DataColumn>,
    /** Each row is a map of column-key → display value. */
    val rows: List<Map<String, String>>,
    /** Show alternating row backgrounds for readability. */
    val striped: Boolean = true,
    /** Show tap-to-sort affordance on headers (fires Action with action='sort:colKey'). */
    val sortable: Boolean = false,
)

internal class DataTableComponent : WeftComponent<DataTableProps>(
    name = "DataTable",
    description = "Sortable table grid — header row + data rows. columns: list of {key, label, align, width}. rows: list of maps keyed by column.key. striped: alternating row fills. sortable: makes headers tappable (fires Action with 'sort:<colKey>'). Use for tabular data; horizontally scrolls when narrow.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = DataTableProps.serializer(),
    example = """{"type": "DataTable", "props": {"id": "leaderboard", "sortable": true, "columns": [{"key": "rank", "label": "#", "width": 40}, {"key": "name", "label": "Name"}, {"key": "score", "label": "Score", "align": "end"}], "rows": [{"rank": "1", "name": "Maria", "score": "342"}, {"rank": "2", "name": "Alex", "score": "318"}, {"rank": "3", "name": "Sam", "score": "291"}]}}""",
) {
    @Composable
    override fun Render(props: DataTableProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .background(cs.background)
                .border(1.dp, cs.divider, UndercurrentTheme.shapes.small)
                .horizontalScroll(rememberScrollState()),
        ) {
            // Header row.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.background(cs.surfaceMuted).padding(vertical = 8.dp),
            ) {
                props.columns.forEach { col ->
                    HeaderCell(col = col, sortable = props.sortable, cs = cs, tp = tp) {
                        if (props.sortable) {
                            onEvent(
                                ComponentEvent.Action(
                                    action = "sort:${col.key}",
                                    sourceType = "DataTable",
                                    sourceLabel = col.label,
                                ),
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = cs.divider)
            props.rows.forEachIndexed { rowIdx, row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            if (props.striped && rowIdx % 2 == 1) cs.surfaceMuted.copy(alpha = 0.5f)
                            else cs.background,
                        )
                        .padding(vertical = 8.dp),
                ) {
                    props.columns.forEach { col ->
                        DataCell(col = col, value = row[col.key].orEmpty(), cs = cs, tp = tp)
                    }
                }
            }
        }
    }

    @Composable
    private fun HeaderCell(
        col: DataColumn,
        sortable: Boolean,
        cs: dev.weft.undercurrent.theme.UndercurrentColors,
        tp: dev.weft.undercurrent.theme.UndercurrentTypography,
        onClick: () -> Unit,
    ) {
        val mod = if (col.width > 0) Modifier.width(col.width.dp) else Modifier.width(120.dp)
        Box(
            modifier = mod
                .let { if (sortable) it.clickable(onClick = onClick) else it }
                .padding(horizontal = 12.dp),
            contentAlignment = textAlignBox(col.align),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = col.label.uppercase(),
                    style = tp.sansSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp,
                    ),
                    color = cs.inkMuted,
                )
                if (sortable) {
                    Text(
                        text = "  ↕",
                        style = tp.sansSmall.copy(fontSize = 11.sp),
                        color = cs.inkSubtle,
                    )
                }
            }
        }
    }

    @Composable
    private fun DataCell(
        col: DataColumn,
        value: String,
        cs: dev.weft.undercurrent.theme.UndercurrentColors,
        tp: dev.weft.undercurrent.theme.UndercurrentTypography,
    ) {
        val mod = if (col.width > 0) Modifier.width(col.width.dp) else Modifier.width(120.dp)
        Box(
            modifier = mod.padding(horizontal = 12.dp),
            contentAlignment = textAlignBox(col.align),
        ) {
            Text(
                text = value,
                style = tp.serifBody.copy(fontSize = 14.sp),
                color = cs.ink,
            )
        }
    }

    private fun textAlignBox(align: String): Alignment = when (align.lowercase()) {
        "center" -> Alignment.Center
        "end" -> Alignment.CenterEnd
        else -> Alignment.CenterStart
    }
}

// =============================================================================
// FileCard — file icon + name + size + optional actions
// =============================================================================

@Serializable
internal data class FileCardProps(
    val name: String,
    /** Human-readable size like "2.4 MB". */
    val size: String = "",
    /** File extension or type hint. Picks the icon emoji. */
    val kind: String = "",
    /** Optional caption — modified date, owner, etc. */
    val detail: String = "",
    /** Action fired on tap. */
    val onTap: String = "",
    /** Whether to show a download icon affordance on the right. */
    val downloadable: Boolean = false,
)

internal class FileCardComponent : WeftComponent<FileCardProps>(
    name = "FileCard",
    description = "File attachment row — colored icon block + name + size/detail + optional download affordance. kind: extension hint ('pdf', 'image', 'doc', 'zip', etc.) — drives the emoji. onTap: open action. downloadable: shows a small download arrow.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = FileCardProps.serializer(),
) {
    @Composable
    override fun Render(props: FileCardProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val (emoji, accentTint) = fileVisuals(props.kind)
        val tappable = props.onTap.isNotBlank()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .background(cs.surfaceMuted)
                .let { m ->
                    if (tappable) m.clickable {
                        onEvent(
                            ComponentEvent.Action(
                                action = props.onTap,
                                sourceType = "FileCard",
                                sourceLabel = props.name,
                            ),
                        )
                    } else m
                }
                .padding(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(UndercurrentTheme.shapes.small)
                    .background(accentTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = tp.serifBodyLarge.copy(fontSize = 20.sp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    text = props.name,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.ink,
                )
                val meta = listOf(props.size, props.detail).filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(
                        text = meta,
                        style = tp.sansSmall,
                        color = cs.inkMuted,
                    )
                }
            }
            if (props.downloadable) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(cs.accent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "↓", style = tp.serifBody.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = cs.accent)
                }
            }
        }
    }

    private fun fileVisuals(kind: String): Pair<String, androidx.compose.ui.graphics.Color> {
        // We can't reference cs here — return a sentinel and let the caller pick.
        // (Compose composable function constraints prevent it cleanly.)
        return when (kind.lowercase()) {
            "image", "img", "png", "jpg", "jpeg", "gif" -> "🖼" to androidx.compose.ui.graphics.Color(0xFF6366F1)
            "pdf" -> "📕" to androidx.compose.ui.graphics.Color(0xFFEF4444)
            "doc", "docx", "word" -> "📘" to androidx.compose.ui.graphics.Color(0xFF3B82F6)
            "sheet", "xls", "xlsx", "csv" -> "📗" to androidx.compose.ui.graphics.Color(0xFF10B981)
            "zip", "tar", "gz", "rar" -> "🗜" to androidx.compose.ui.graphics.Color(0xFFF59E0B)
            "video", "mp4", "mov" -> "🎬" to androidx.compose.ui.graphics.Color(0xFF8B5CF6)
            "audio", "mp3", "wav" -> "🎵" to androidx.compose.ui.graphics.Color(0xFFEC4899)
            "code", "txt", "md", "json" -> "📝" to androidx.compose.ui.graphics.Color(0xFF06B6D4)
            else -> "📄" to androidx.compose.ui.graphics.Color(0xFF6B7280)
        }
    }
}

// =============================================================================
// JsonTree — expandable JSON viewer
// =============================================================================

@Serializable
internal data class JsonNode(
    val key: String = "",
    /** string | number | boolean | null | object | array. */
    val type: String,
    /** Primitive value when type ∈ {string, number, boolean, null}. */
    val value: String = "",
    val children: List<JsonNode> = emptyList(),
)

@Serializable
internal data class JsonTreeProps(
    val root: JsonNode,
    val initialExpanded: Boolean = true,
)

internal class JsonTreeComponent : WeftComponent<JsonTreeProps>(
    name = "JsonTree",
    description = "Expandable JSON viewer. root: top-level node (type 'object' / 'array' / etc.). Tap collapsibles to expand/collapse. Use for API responses, debug payloads, config previews.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = JsonTreeProps.serializer(),
    example = """{"type": "JsonTree", "props": {"root": {"type": "object", "children": [{"key": "id", "type": "number", "value": "42"}, {"key": "name", "type": "string", "value": "Maria"}, {"key": "active", "type": "boolean", "value": "true"}, {"key": "tags", "type": "array", "children": [{"type": "string", "value": "admin"}, {"type": "string", "value": "design"}]}]}}}""",
) {
    @Composable
    override fun Render(props: JsonTreeProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .background(cs.codeBg)
                .border(1.dp, cs.codeBorder, UndercurrentTheme.shapes.small)
                .padding(12.dp),
        ) {
            RenderNode(props.root, depth = 0, initialOpen = props.initialExpanded)
        }
    }

    @Composable
    private fun RenderNode(node: JsonNode, depth: Int, initialOpen: Boolean) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val isContainer = node.type == "object" || node.type == "array"
        var open by remember { mutableStateOf(initialOpen) }
        val keyPart = if (node.key.isNotEmpty()) "\"${node.key}\": " else ""

        Row(modifier = Modifier.padding(start = (depth * 12).dp)) {
            if (isContainer) {
                Text(
                    text = if (open) "▾ " else "▸ ",
                    style = tp.serifBody.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    ),
                    color = cs.accent,
                    modifier = Modifier
                        .clickable { open = !open }
                        .padding(end = 2.dp),
                )
            } else {
                Box(modifier = Modifier.size(width = 14.dp, height = 1.dp))
            }
            Text(
                text = buildString {
                    append(keyPart)
                    when (node.type) {
                        "string" -> append("\"${node.value}\"")
                        "number" -> append(node.value)
                        "boolean" -> append(node.value)
                        "null" -> append("null")
                        "object" -> append(if (open) "{" else "{…}")
                        "array" -> append(if (open) "[" else "[${node.children.size}]")
                    }
                },
                style = tp.serifBody.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                ),
                color = when (node.type) {
                    "string" -> cs.accent
                    "number" -> cs.codeInk
                    "boolean" -> cs.error
                    "null" -> cs.inkSubtle
                    else -> cs.codeInk
                },
            )
        }
        if (isContainer && open) {
            node.children.forEach { child ->
                RenderNode(child, depth = depth + 1, initialOpen = true)
            }
            Row(modifier = Modifier.padding(start = (depth * 12).dp)) {
                Box(modifier = Modifier.size(width = 14.dp, height = 1.dp))
                Text(
                    text = if (node.type == "array") "]" else "}",
                    style = tp.serifBody.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                    ),
                    color = cs.codeInk,
                )
            }
        }
    }
}

// =============================================================================
// LogLine — timestamp + level + message
// =============================================================================

@Serializable
internal data class LogLineProps(
    val timestamp: String,
    val message: String,
    /** trace | debug | info | warn | error. */
    val level: String = "info",
    val source: String = "",
)

internal class LogLineComponent : WeftComponent<LogLineProps>(
    name = "LogLine",
    description = "Single log entry — timestamp (mono) + level pill + optional source + message. level: 'trace' / 'debug' / 'info' (default) / 'warn' / 'error' — colors the pill. Use multiple in a Stack for a log viewer.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = LogLineProps.serializer(),
) {
    @Composable
    override fun Render(props: LogLineProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val (levelLabel, levelColor) = when (props.level.lowercase()) {
            "trace", "debug" -> "DEBUG" to cs.inkSubtle
            "warn", "warning" -> "WARN" to androidx.compose.ui.graphics.Color(0xFFD97706)
            "error" -> "ERROR" to cs.error
            else -> "INFO" to cs.accent
        }
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        ) {
            Text(
                text = props.timestamp,
                style = tp.sansSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                ),
                color = cs.inkSubtle,
                modifier = Modifier.padding(top = 2.dp, end = 8.dp),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(levelColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            ) {
                Text(
                    text = levelLabel,
                    style = tp.sansSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = levelColor,
                )
            }
            if (props.source.isNotBlank()) {
                Text(
                    text = " ${props.source}",
                    style = tp.sansSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                    ),
                    color = cs.inkMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text(
                text = " ${props.message}",
                style = tp.sansSmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 17.sp,
                ),
                color = cs.ink,
                modifier = Modifier.weight(1f).padding(start = 4.dp, top = 2.dp),
            )
        }
    }
}

// =============================================================================
// ApiResponse — HTTP method + status + duration + body preview
// =============================================================================

@Serializable
internal data class ApiResponseProps(
    val method: String,
    val url: String,
    val statusCode: Int,
    /** Optional latency, e.g. "124ms". */
    val duration: String = "",
    /** Body snippet (JSON/text). Shown in a monospace box. Empty = no body. */
    val body: String = "",
)

internal class ApiResponseComponent : WeftComponent<ApiResponseProps>(
    name = "ApiResponse",
    description = "HTTP response summary — colored method pill (GET/POST/…), URL, status code (auto-colored), optional duration, optional body preview in mono. Use for API integration explanations, debug surfaces.",
    category = ComponentCategory.DISPLAY,
    propsSerializer = ApiResponseProps.serializer(),
    example = """{"type": "ApiResponse", "props": {"method": "POST", "url": "/api/users", "statusCode": 201, "duration": "184ms", "body": "{\n  \"id\": 42,\n  \"name\": \"Maria\"\n}"}}""",
) {
    @Composable
    override fun Render(props: ApiResponseProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val methodColor = when (props.method.uppercase()) {
            "POST" -> androidx.compose.ui.graphics.Color(0xFF10B981)
            "PUT", "PATCH" -> androidx.compose.ui.graphics.Color(0xFFD97706)
            "DELETE" -> cs.error
            else -> cs.accent
        }
        val statusColor = when (props.statusCode) {
            in 200..299 -> cs.accent
            in 300..399 -> androidx.compose.ui.graphics.Color(0xFF6366F1)
            in 400..499 -> androidx.compose.ui.graphics.Color(0xFFD97706)
            in 500..599 -> cs.error
            else -> cs.inkMuted
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(UndercurrentTheme.shapes.small)
                .background(cs.codeBg)
                .border(1.dp, cs.codeBorder, UndercurrentTheme.shapes.small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(methodColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = props.method.uppercase(),
                        style = tp.sansSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp,
                        ),
                        color = cs.background,
                    )
                }
                Text(
                    text = props.url,
                    style = tp.serifBody.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                    ),
                    color = cs.codeInk,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                )
                Text(
                    text = props.statusCode.toString(),
                    style = tp.sansLabel.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = statusColor,
                )
                if (props.duration.isNotBlank()) {
                    Text(
                        text = "  ${props.duration}",
                        style = tp.sansSmall.copy(fontFamily = FontFamily.Monospace),
                        color = cs.inkSubtle,
                    )
                }
            }
            if (props.body.isNotBlank()) {
                HorizontalDivider(color = cs.codeBorder)
                Text(
                    text = props.body,
                    style = tp.serifBody.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
                    color = cs.codeInk,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                )
            }
        }
    }
}

/** Every data-tier component. */
internal val undercurrentDataComponents: List<WeftComponent<*>> = listOf(
    DataTableComponent(),
    FileCardComponent(),
    JsonTreeComponent(),
    LogLineComponent(),
    ApiResponseComponent(),
)
