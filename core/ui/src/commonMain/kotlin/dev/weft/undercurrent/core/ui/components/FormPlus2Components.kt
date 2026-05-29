package dev.weft.undercurrent.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.weft.compose.components.WeftComponent
import dev.weft.contracts.ComponentCategory
import dev.weft.contracts.ComponentEvent
import dev.weft.undercurrent.core.designsystem.UndercurrentTheme
import kotlinx.serialization.Serializable

// =============================================================================
// OtpInput — N-box one-time-password input
// =============================================================================

@Serializable
internal data class OtpInputProps(
    val id: String,
    val label: String = "",
    /** Number of digit boxes (typical 4 or 6). */
    val length: Int = 6,
    /** Optional hint shown below. */
    val hint: String = "",
)

internal class OtpInputComponent : WeftComponent<OtpInputProps>(
    name = "OtpInput",
    description = "One-time-passcode input with N boxes (default 6). id: stable identifier (fires TextChanged with the joined digits on every keystroke; when the user types the last digit it ALSO fires an Action with action='complete'). length: 4-8. label: optional. hint: optional below. Auto-advances between boxes as you type.",
    category = ComponentCategory.INPUT,
    propsSerializer = OtpInputProps.serializer(),
) {
    @Composable
    override fun Render(props: OtpInputProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        val len = props.length.coerceIn(4, 8)
        var value by remember(props.id) { mutableStateOf("") }
        val focus = remember { FocusRequester() }

        LaunchedEffect(props.id) { runCatching { focus.requestFocus() } }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (props.label.isNotBlank()) {
                Text(
                    text = props.label,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.ink,
                )
            }
            // Hidden text field accepts the keyboard input; visible boxes mirror it.
            Box {
                BasicTextField(
                    value = value,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }.take(len)
                        val wasIncomplete = value.length < len
                        value = digits
                        onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = digits))
                        if (digits.length == len && wasIncomplete) {
                            onEvent(
                                ComponentEvent.Action(
                                    action = "complete",
                                    sourceType = "OtpInput",
                                    sourceLabel = props.id,
                                ),
                            )
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    textStyle = TextStyle(color = androidx.compose.ui.graphics.Color.Transparent),
                    cursorBrush = SolidColor(androidx.compose.ui.graphics.Color.Transparent),
                    modifier = Modifier
                        .focusRequester(focus)
                        .size(0.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(len) { i ->
                        val char = value.getOrNull(i)?.toString() ?: ""
                        val active = i == value.length
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 52.dp)
                                .clip(UndercurrentTheme.shapes.small)
                                .background(cs.background)
                                .border(
                                    width = if (active) 2.dp else 1.dp,
                                    color = if (active) cs.accent else if (char.isNotEmpty()) cs.accent.copy(alpha = 0.5f) else cs.divider,
                                    shape = UndercurrentTheme.shapes.small,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = char,
                                style = tp.serifBodyLarge.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = cs.ink,
                            )
                        }
                    }
                }
            }
            if (props.hint.isNotBlank()) {
                Text(text = props.hint, style = tp.sansSmall, color = cs.inkMuted)
            }
        }
    }
}

// =============================================================================
// PhoneField — country prefix + phone digits
// =============================================================================

@Serializable
internal data class PhoneFieldProps(
    val id: String,
    val label: String = "Phone",
    /** Country dial code, e.g. "+1", "+44". */
    val dialCode: String = "+1",
    val initial: String = "",
    val hint: String = "",
)

internal class PhoneFieldComponent : WeftComponent<PhoneFieldProps>(
    name = "PhoneField",
    description = "Phone number input with country dial-code prefix. id: stable identifier (fires TextChanged with the full international form 'dialCode rest' on every change). label/dialCode/initial/hint: optional (defaults: 'Phone', '+1', '', '').",
    category = ComponentCategory.INPUT,
    propsSerializer = PhoneFieldProps.serializer(),
) {
    @Composable
    override fun Render(props: PhoneFieldProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var value by remember(props.id) { mutableStateOf(props.initial) }
        var focused by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (props.label.isNotBlank()) {
                Text(
                    text = props.label,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.ink,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(UndercurrentTheme.shapes.small)
                    .background(cs.background)
                    .border(
                        width = if (focused) 1.5.dp else 1.dp,
                        color = if (focused) cs.accent else cs.divider,
                        shape = UndercurrentTheme.shapes.small,
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Text(
                    text = props.dialCode,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
                    color = cs.inkMuted,
                    modifier = Modifier.padding(end = 10.dp),
                )
                Box(modifier = Modifier.width(1.dp).height(20.dp).background(cs.divider))
                BasicTextField(
                    value = value,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { it.isDigit() || it == ' ' || it == '-' }
                        value = cleaned
                        onEvent(
                            ComponentEvent.TextChanged(
                                sourceId = props.id,
                                value = "${props.dialCode} $cleaned",
                            ),
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    textStyle = TextStyle(
                        color = cs.ink,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    cursorBrush = SolidColor(cs.accent),
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .fillMaxWidth()
                        .onFocusEvent { focused = it },
                    decorationBox = { inner ->
                        Box {
                            if (value.isEmpty()) {
                                Text(
                                    text = "555 0100",
                                    style = tp.sansLabel.copy(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
                                    color = cs.inkSubtle,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            if (props.hint.isNotBlank()) {
                Text(
                    text = props.hint,
                    style = tp.sansSmall,
                    color = cs.inkMuted,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
        }
    }
}

private fun Modifier.onFocusEvent(onFocus: (Boolean) -> Unit): Modifier =
    this.onFocusChanged { onFocus(it.isFocused) }

// =============================================================================
// CurrencyField — amount input with currency selector
// =============================================================================

@Serializable
internal data class CurrencyFieldProps(
    val id: String,
    val label: String = "Amount",
    /** ISO currency code. */
    val currency: String = "USD",
    val initial: String = "",
    val hint: String = "",
)

internal class CurrencyFieldComponent : WeftComponent<CurrencyFieldProps>(
    name = "CurrencyField",
    description = "Monetary amount input with currency code shown on the right. id: stable identifier (fires TextChanged with the numeric value on each keystroke). currency: ISO code (USD/EUR/GBP/JPY…). label/initial/hint: optional. Use for prices, expenses, transfers.",
    category = ComponentCategory.INPUT,
    propsSerializer = CurrencyFieldProps.serializer(),
) {
    @Composable
    override fun Render(props: CurrencyFieldProps, children: @Composable () -> Unit, onEvent: (ComponentEvent) -> Unit) {
        val cs = UndercurrentTheme.colors
        val tp = UndercurrentTheme.typography
        var value by remember(props.id) { mutableStateOf(props.initial) }
        var focused by remember { mutableStateOf(false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (props.label.isNotBlank()) {
                Text(
                    text = props.label,
                    style = tp.sansLabel.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                    color = cs.ink,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(UndercurrentTheme.shapes.small)
                    .background(cs.background)
                    .border(
                        width = if (focused) 1.5.dp else 1.dp,
                        color = if (focused) cs.accent else cs.divider,
                        shape = UndercurrentTheme.shapes.small,
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Text(
                    text = currencySymbol(props.currency),
                    style = tp.serifBodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    color = cs.inkMuted,
                )
                BasicTextField(
                    value = value,
                    onValueChange = { raw ->
                        // Accept digits + at most one decimal point.
                        val cleaned = buildString {
                            var sawDot = false
                            for (c in raw) when {
                                c.isDigit() -> append(c)
                                c == '.' && !sawDot -> { append(c); sawDot = true }
                            }
                        }
                        value = cleaned
                        onEvent(ComponentEvent.TextChanged(sourceId = props.id, value = cleaned))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(
                        color = cs.ink,
                        fontSize = 22.sp,
                        fontFamily = tp.serifBodyLarge.fontFamily,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.End,
                    ),
                    cursorBrush = SolidColor(cs.accent),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .onFocusEvent { focused = it },
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (value.isEmpty()) {
                                Text(
                                    text = "0.00",
                                    style = tp.serifBodyLarge.copy(fontSize = 22.sp),
                                    color = cs.inkSubtle,
                                )
                            }
                            inner()
                        }
                    },
                )
                Text(
                    text = props.currency.uppercase(),
                    style = tp.sansSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
                    color = cs.inkMuted,
                )
            }
            if (props.hint.isNotBlank()) {
                Text(
                    text = props.hint,
                    style = tp.sansSmall,
                    color = cs.inkMuted,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                )
            }
        }
    }

    private fun currencySymbol(code: String): String = when (code.uppercase()) {
        "USD", "CAD", "AUD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "JPY", "CNY" -> "¥"
        "INR" -> "₹"
        "KRW" -> "₩"
        "VND" -> "₫"
        else -> code.take(1)
    }
}

/** Every FormPlus2 component. */
internal val undercurrentFormPlus2Components: List<WeftComponent<*>> = listOf(
    OtpInputComponent(),
    PhoneFieldComponent(),
    CurrencyFieldComponent(),
)
