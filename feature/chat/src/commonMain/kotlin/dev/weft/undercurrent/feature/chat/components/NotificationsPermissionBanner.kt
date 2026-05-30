package dev.weft.undercurrent.feature.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Slim banner asking for notification permission so the agent can
 * schedule reminders. Pure-Compose: the platform check
 * (Android Tiramisu+, granted state, request launcher) lives in the
 * host's nav glue. The host decides whether to render this composable
 * at all (no-op on iOS, no-op on already-granted Android, no-op on
 * pre-Tiramisu Android).
 *
 * KMP — commonMain. Moved from
 * `app/.../features/chat/NotificationsPermissionBanner.kt`. Adjustments:
 *   - Android `Manifest` + `ContextCompat.checkSelfPermission` +
 *     `rememberLauncherForActivityResult` lifted out — host owns the
 *     full permission-request flow and supplies [onGrant].
 *   - The Tiramisu SDK check + "already granted?" check happen in the
 *     host before this is rendered.
 */
@Composable
fun NotificationsPermissionBanner(
    onGrant: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surfaceMuted)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Notifications off",
                style = typography.sansLabel.copy(color = colors.inkMuted),
            )
            Spacer(Modifier.padding(1.dp))
            Text(
                text = "The agent can't schedule reminders without them.",
                style = typography.sansSmall.copy(color = colors.inkMuted),
            )
        }
        Box(
            modifier = Modifier
                .clip(shapes.small)
                .background(colors.surface)
                .clickable(onClick = onGrant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Grant",
                style = typography.sansSmall.copy(color = colors.accent),
            )
        }
    }
}

@Preview
@Composable
private fun NotificationsPermissionBannerPreview() {
    UndercurrentTheme {
        NotificationsPermissionBanner(onGrant = { })
    }
}
