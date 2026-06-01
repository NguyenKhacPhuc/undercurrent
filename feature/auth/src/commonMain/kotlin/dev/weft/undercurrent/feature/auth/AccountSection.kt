package dev.weft.undercurrent.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import dev.weft.undercurrent.core.domain.auth.dto.AccountDto
import dev.weft.undercurrent.core.ui.PrimaryButton
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun AccountSection(
    state: AccountState,
    onIntent: (AccountIntent) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccountCard(view = state.view, onRetry = { onIntent(AccountIntent.Refresh) })
        PrimaryButton(
            label = "Sign Out",
            onClick = { onIntent(AccountIntent.SignOut) },
        )
    }
}

@Composable
private fun AccountCard(
    view: AccountState.View,
    onRetry: () -> Unit,
) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    val shapes = UndercurrentTheme.shapes
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shapes.medium)
            .background(colors.surface)
            .border(1.dp, colors.divider, shapes.medium)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text("Account", style = typography.sansLabel.copy(color = colors.inkSubtle))
        Spacer(Modifier.height(8.dp))
        when (view) {
            AccountState.View.Loading -> LoadingRow()
            is AccountState.View.Loaded -> LoadedRow(view.account)
            AccountState.View.LoadError -> LoadErrorRow(onRetry = onRetry)
        }
    }
}

@Composable
private fun LoadingRow() {
    val colors = UndercurrentTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 2.dp, color = colors.accent)
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = "Loading account…",
            style = UndercurrentTheme.typography.serifBody.copy(color = colors.inkMuted),
        )
    }
}

@Composable
private fun LoadedRow(account: AccountDto) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Column {
        Text(account.displayName, style = typography.sansHeader.copy(color = colors.ink))
        Spacer(Modifier.height(2.dp))
        Text(account.email, style = typography.sansSmall.copy(color = colors.inkMuted))
    }
}

@Composable
private fun LoadErrorRow(onRetry: () -> Unit) {
    val colors = UndercurrentTheme.colors
    val typography = UndercurrentTheme.typography
    Column {
        Text(
            text = "Couldn't load account.",
            style = typography.serifBody.copy(color = colors.error),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Retry",
            style = typography.sansLabel.copy(color = colors.accent),
            modifier = Modifier
                .clickable(onClick = onRetry)
                .padding(vertical = 4.dp),
        )
    }
}

@Preview
@Composable
private fun AccountSectionPreviewLoading() {
    UndercurrentTheme {
        AccountSection(state = AccountState(view = AccountState.View.Loading))
    }
}

@Preview
@Composable
private fun AccountSectionPreviewLoaded() {
    UndercurrentTheme {
        AccountSection(
            state = AccountState(
                view = AccountState.View.Loaded(
                    AccountDto("acct.abc", "Phuc", "phuc@example.com", 1L),
                ),
            ),
        )
    }
}

@Preview
@Composable
private fun AccountSectionPreviewError() {
    UndercurrentTheme {
        AccountSection(state = AccountState(view = AccountState.View.LoadError))
    }
}
