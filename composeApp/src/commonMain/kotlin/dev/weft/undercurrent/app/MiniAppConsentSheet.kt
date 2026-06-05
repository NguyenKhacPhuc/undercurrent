package dev.weft.undercurrent.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.weft.undercurrent.core.model.MiniAppConsentRequest

/**
 * First-run consent prompt: a modal sheet listing, in plain language,
 * the actions a mini-app is asking for. The user approves (grants exactly
 * these) or denies (grants nothing); either way the mini-app then renders,
 * scope-gated to the result. Dismissing the sheet is treated as a denial.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiniAppConsentSheet(
    request: MiniAppConsentRequest,
    onApprove: () -> Unit = {},
    onDeny: () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDeny,
        sheetState = rememberModalBottomSheetState(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "${request.miniAppEmoji} ${request.miniAppName} wants to:",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            request.actions.forEach { action ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                    ) {}
                    Text(
                        text = action.description.ifBlank { action.name },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                TextButton(onClick = onDeny) { Text("Deny") }
                Button(onClick = onApprove) { Text("Approve") }
            }
        }
    }
}
