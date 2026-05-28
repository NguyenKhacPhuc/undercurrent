package dev.weft.undercurrent.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Platform-provided hooks the commonMain App composable needs but can't
 * implement itself. Three buckets:
 *
 *  1. **Substrate-coupled screens** ([chatRoute], [renderedTreeRoute],
 *     [creatorRoute], [miniAppsRoute]) — these depend on Weft's
 *     `ComposeUiBridge` / `AgentRenderedTreeScreen` / `TreeRenderer`,
 *     which are Android-only. The Android host wires the real
 *     composables; the iOS host renders a "coming soon" placeholder.
 *
 *  2. **OS bridges** ([onOpenUrl], [onCopyText], [onRestartProcess],
 *     [onOpenAppDetailsSettings]) — Android Intent / ClipboardManager /
 *     iOS UIApplication shaped callbacks.
 *
 *  3. **Save-as-feature opener** ([onOpenSaveDialog]) — populates the
 *     save-as-mini-app dialog from the RenderedTree screen's "Save"
 *     action. Lives outside the chat route so the dialog mounts at the
 *     App root and survives screen transitions.
 *
 * KMP — commonMain. The host owns the implementation; the App composable
 * just receives a fully-populated [PlatformAdapter] at the entry point.
 */
@Immutable
public class PlatformAdapter(
    public val chatRoute: @Composable () -> Unit,
    public val renderedTreeRoute: @Composable () -> Unit,
    public val miniAppsRoute: @Composable () -> Unit,
    public val creatorRoute: @Composable () -> Unit,
    public val onOpenUrl: (String) -> Unit,
    public val onCopyText: (String) -> Unit,
    public val onRestartProcess: () -> Unit,
    public val onOpenAppDetailsSettings: () -> Unit,
    public val onOpenSaveDialog: (suggestedPrompt: String) -> Unit,
)
