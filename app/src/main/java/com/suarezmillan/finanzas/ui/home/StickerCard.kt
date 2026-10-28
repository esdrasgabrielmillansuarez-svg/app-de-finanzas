package com.suarezmillan.finanzas.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suarezmillan.finanzas.core.coach.CoachAction
import com.suarezmillan.finanzas.core.coach.CoachMessage
import com.suarezmillan.finanzas.core.coach.CoachTone

/**
 * "Sticker" del día: emoji grande, frase corta y una acción.
 *
 * El emoji va en un círculo rotado con sombra para que se lea como sticker y no
 * como un ícono más del sistema. Cambia con Crossfade cuando el mensaje muta,
 * así el movimiento se siente vivo sin distraer.
 */
@Composable
fun StickerCard(
    message: CoachMessage,
    onAnother: () -> Unit,
    onAction: (CoachAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Crossfade(
        targetState = message,
        label = "coach-sticker",
        modifier = modifier.fillMaxWidth(),
    ) { current ->
        val container = when (current.tone) {
            CoachTone.CELEBRATE -> MaterialTheme.colorScheme.primaryContainer
            CoachTone.WARN -> MaterialTheme.colorScheme.errorContainer
            CoachTone.NUDGE, CoachTone.CALM -> MaterialTheme.colorScheme.surfaceVariant
        }
        val content = when (current.tone) {
            CoachTone.CELEBRATE -> MaterialTheme.colorScheme.onPrimaryContainer
            CoachTone.WARN -> MaterialTheme.colorScheme.onErrorContainer
            CoachTone.NUDGE, CoachTone.CALM -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = container, contentColor = content),
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(64.dp).rotate(-8f),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(current.emoji, fontSize = 32.sp)
                    }
                }
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = current.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = current.body,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val action = current.action
                        if (action != null) {
                            val actionLabel = when (action) {
                                CoachAction.OPEN_PLAN -> "Ver plan"
                                CoachAction.OPEN_QUICK -> "Anotar"
                            }
                            TextButton(
                                onClick = { onAction(action) },
                                colors = ButtonDefaults.textButtonColors(contentColor = content),
                            ) {
                                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        TextButton(
                            onClick = onAnother,
                            colors = ButtonDefaults.textButtonColors(contentColor = content),
                        ) {
                            Text("Otro", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
