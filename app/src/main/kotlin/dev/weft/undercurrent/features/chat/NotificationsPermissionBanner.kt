package dev.weft.undercurrent.features.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import dev.weft.undercurrent.theme.UndercurrentTheme

/**
 * Slim banner asking for notification permission so the agent can schedule
 * reminders. Auto-dismisses once granted, no-ops on pre-Tiramisu devices.
 *
 * Visual: subtle muted strip across the top of the chat surface — never
 * pulls focus away from the conversation, but always visible until the
 * user acts on it.
 */
@Composable
internal fun NotificationsPermissionBanner() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }
    if (granted) return

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> granted = isGranted },
    )

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
                .clickable { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = "Grant",
                style = typography.sansSmall.copy(color = colors.accent),
            )
        }
    }
}
