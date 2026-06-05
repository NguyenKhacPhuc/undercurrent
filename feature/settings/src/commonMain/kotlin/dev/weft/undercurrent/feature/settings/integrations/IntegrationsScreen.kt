package dev.weft.undercurrent.feature.settings.integrations

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
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.resources.Res
import dev.weft.undercurrent.core.resources.integration_tagline_linear
import dev.weft.undercurrent.core.resources.integrations_connect
import dev.weft.undercurrent.core.resources.integrations_disconnect
import dev.weft.undercurrent.core.resources.integrations_footer
import dev.weft.undercurrent.core.resources.integrations_intro
import dev.weft.undercurrent.core.resources.integrations_restart_action
import dev.weft.undercurrent.core.resources.integrations_restart_banner
import dev.weft.undercurrent.core.resources.integrations_title
import dev.weft.undercurrent.core.ui.ScreenScaffold
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/**
 * Settings → Integrations. Lists every supported third-party
 * integration in [Integrations.All] with a Connect / Disconnect
 * affordance driving [IntegrationsViewModel] through OAuth.
 *
 * A persistent banner appears above the list whenever the live
 * enabled set differs from what the running runtime was built with
 * — the agent's tool registry is fixed at runtime construction time,
 * so newly-toggled integrations only become available after restart.
 *
 * Stateful entry point — hoists state from [IntegrationsViewModel]
 * and forwards to the stateless overload.
 */
@Composable
fun IntegrationsScreen(
    onBack: () -> Unit,
    onRestart: () -> Unit,
    viewModel: IntegrationsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsState()
    IntegrationsScreen(
        state = state,
        onBack = onBack,
        onRestart = onRestart,
        onConnect = { viewModel.dispatch(IntegrationsIntent.Connect(it)) },
        onDisconnect = { viewModel.dispatch(IntegrationsIntent.Disconnect(it)) },
    )
}

/**
 * Stateless variant — takes [state] and per-action callbacks. Used by
 * the stateful overload above plus `@Preview` / snapshot harnesses.
 */
@Composable
fun IntegrationsScreen(
    state: IntegrationsState,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    onConnect: (Integration) -> Unit = {},
    onDisconnect: (Integration) -> Unit = {},
) {
    val enabled = state.enabledIds
    val pendingRestart = state.pendingRestart
    val lastAction = state.lastAction

    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography

    ScreenScaffold(title = stringResource(Res.string.integrations_title), onBack = onBack) {
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
                        text = stringResource(Res.string.integrations_intro),
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
                    val status = if (integration.id in enabled) {
                        IntegrationStatus.Connected
                    } else {
                        IntegrationStatus.Disconnected
                    }
                    IntegrationCard(
                        integration = integration,
                        status = status,
                        action = lastAction.takeIf { it !is ActionStatus.Idle && actionTargets(it, integration.id) },
                        onConnect = { onConnect(integration) },
                        onDisconnect = { onDisconnect(integration) },
                    )
                }

                item("footer") {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(Res.string.integrations_footer),
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
                    text = integration.taglineRes()?.let { stringResource(it) } ?: integration.tagline,
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
                    label = stringResource(Res.string.integrations_disconnect),
                    primary = false,
                    onClick = onDisconnect,
                )
                IntegrationStatus.Disconnected -> ActionButton(
                    label = stringResource(Res.string.integrations_connect),
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

@Preview
@Composable
private fun IntegrationsScreenPreview() {
    UndercurrentTheme {
        IntegrationsScreen(
            state = IntegrationsState(
                enabledIds = setOf(Integrations.All.first().id),
                pendingRestart = true,
                lastAction = ActionStatus.Success(
                    integrationId = Integrations.All.first().id,
                    message = "Connected",
                ),
            ),
            onBack = {},
            onRestart = {},
        )
    }
}

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
            text = stringResource(Res.string.integrations_restart_banner),
            style = typography.sansSmall.copy(
                color = colors.background,
                fontWeight = FontWeight.SemiBold,
            ),
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.padding(horizontal = 4.dp))
        Text(
            text = stringResource(Res.string.integrations_restart_action),
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

/** Localized tagline for a built-in integration; null for unknown ids. */
private fun Integration.taglineRes(): StringResource? = when (id) {
    Integrations.Linear.id -> Res.string.integration_tagline_linear
    else -> null
}
