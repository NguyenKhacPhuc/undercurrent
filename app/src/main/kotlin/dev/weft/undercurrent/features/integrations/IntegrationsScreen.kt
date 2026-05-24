package dev.weft.undercurrent.features.integrations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.undercurrent.theme.UndercurrentTheme
import dev.weft.undercurrent.ui.ScreenScaffold
import org.koin.androidx.compose.koinViewModel

/**
 * Settings → Integrations.
 *
 * Lists every supported third-party integration in [Integrations.All].
 * Each row shows the integration's name + tagline + a Connect /
 * Disconnect affordance that drives [IntegrationsViewModel] through the
 * OAuth flow.
 *
 * A persistent banner appears above the list whenever the live enabled
 * set differs from what the running runtime was built with — the agent's
 * tool registry is fixed at runtime construction time, so newly-toggled
 * integrations only become available after a restart.
 */
@Composable
internal fun IntegrationsScreen(
    onBack: () -> Unit,
    onRestart: () -> Unit,
    vm: IntegrationsViewModel = koinViewModel(),
) {
    val enabled by vm.enabledIds.collectAsState()
    val pendingRestart by vm.pendingRestart.collectAsState()
    val lastAction by vm.lastAction.collectAsState()

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    ScreenScaffold(title = "Integrations", onBack = onBack) {
        Column(modifier = Modifier.fillMaxSize().weight(1f)) {
            if (pendingRestart) {
                RestartBanner(onRestart = onRestart)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item("intro") {
                    Text(
                        text = "Connect a service so I can read from and write to it on " +
                            "your behalf. Each connection lives on this device — revoke " +
                            "any time, here or at the provider.",
                        style = typography.serifBody.copy(
                            color = colors.ink,
                            fontStyle = FontStyle.Italic,
                            fontSize = 16.sp,
                            lineHeight = 24.sp,
                        ),
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }

                items(Integrations.All, key = { it.id }) { integration ->
                    val status = vm.statusFor(integration, enabled)
                    IntegrationCard(
                        integration = integration,
                        status = status,
                        action = lastAction.takeIf { it !is ActionStatus.Idle && actionTargets(it, integration.id) },
                        onConnect = { vm.connect(integration) },
                        onDisconnect = { vm.disconnect(integration) },
                    )
                }

                item("footer") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Tokens are stored encrypted on this device. They never " +
                            "transit through any server I control.",
                        style = typography.sansSmall.copy(color = colors.inkSubtle),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun actionTargets(action: ActionStatus, id: String): Boolean = when (action) {
    is ActionStatus.InProgress -> action.integrationId == id
    is ActionStatus.Success -> action.integrationId == id
    is ActionStatus.Failure -> action.integrationId == id
    ActionStatus.Idle -> false
}

/**
 * One row per integration. Name + tagline on the left, primary action
 * (Connect / Disconnect) on the right. When an action is in-flight or
 * has just landed for this integration, the per-row [action] status
 * shows below in muted type.
 */
@Composable
private fun IntegrationCard(
    integration: Integration,
    status: IntegrationStatus,
    action: ActionStatus?,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .border(width = 1.dp, color = colors.divider, shape = shapes.medium)
            .background(colors.surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = integration.displayName,
                    style = typography.serifBody.copy(
                        color = colors.ink,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = integration.tagline,
                    style = typography.serifBody.copy(
                        color = colors.inkMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    ),
                )
            }
            Spacer(Modifier.padding(horizontal = 4.dp))
            when (status) {
                IntegrationStatus.Connected -> ActionButton(
                    label = "Disconnect",
                    primary = false,
                    onClick = onDisconnect,
                )
                IntegrationStatus.Disconnected -> ActionButton(
                    label = "Connect",
                    primary = true,
                    onClick = onConnect,
                )
            }
        }
        val message = when (action) {
            is ActionStatus.InProgress -> action.message
            is ActionStatus.Success -> action.message
            is ActionStatus.Failure -> action.message
            else -> null
        }
        if (message != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                style = typography.sansSmall.copy(
                    color = if (action is ActionStatus.Failure) colors.error else colors.inkSubtle,
                ),
            )
        }
    }
}

@Composable
private fun ActionButton(label: String, primary: Boolean, onClick: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Box(
        modifier = Modifier
            .clip(shapes.small)
            .background(if (primary) colors.ink else colors.surfaceMuted)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = label,
            style = typography.sansLabel.copy(
                color = if (primary) colors.background else colors.ink,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

/**
 * Top-of-screen banner. Visible whenever the persisted enabled set
 * doesn't match what the live runtime was built with — i.e. the user
 * has just connected or disconnected something and the tool registry
 * needs a restart to reflect it.
 */
@Composable
private fun RestartBanner(onRestart: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.accent)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Restart Undercurrent to apply changes.",
            style = typography.sansSmall.copy(
                color = colors.background,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(
            text = "RESTART",
            style = typography.sansLabel.copy(
                color = colors.background,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier
                .clickable(onClick = onRestart)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}
