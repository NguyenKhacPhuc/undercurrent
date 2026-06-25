package dev.weft.undercurrent.core.model

import kotlinx.serialization.Serializable

/**
 * The backend-driven assistant base prompt the client runs on.
 * [preamble] is the app-owned base prompt text (the substrate's standard
 * defaults are appended client-side). [revision] is an opaque marker that
 * changes whenever the text changes; [updatedAtMs] is the BE's last-change
 * time. Serializable so it doubles as the wire payload and the local cache
 * shape.
 */
@Serializable
data class PromptConfig(
    val preamble: String,
    val revision: String,
    val updatedAtMs: Long,
)
