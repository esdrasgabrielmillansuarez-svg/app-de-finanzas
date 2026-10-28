package com.suarezmillan.finanzas.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.suarezmillan.finanzas.core.money.MoneyFormat

/**
 * Importe con cifras tabulares. Único punto de formateo de dinero en la UI:
 * si un importe se pinta con `Text(money.toString())` es un bug.
 */
@Composable
fun MoneyText(
    minor: Long,
    currency: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    signed: Boolean = false,
    approximate: Boolean = false,
) {
    val text = when {
        approximate -> MoneyFormat.formatApprox(minor, currency)
        signed -> MoneyFormat.formatSigned(minor, currency)
        else -> MoneyFormat.format(minor, currency)
    }
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(fontFeatureSettings = "tnum, zero"),
        color = color,
        maxLines = 1,
    )
}
