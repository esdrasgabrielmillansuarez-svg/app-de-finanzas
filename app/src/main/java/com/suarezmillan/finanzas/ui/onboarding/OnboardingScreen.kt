package com.suarezmillan.finanzas.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CurrencyExchange
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.suarezmillan.finanzas.core.branding.Branding
import kotlinx.coroutines.launch

private data class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val bullets: List<String>,
)

/**
 * Tutorial corto de bienvenida.
 *
 * Cuatro pasos que explican lo esencial sin pedir datos ni mostrar cifras: la
 * app no inventa tasas ni montos de ejemplo. Se puede saltar y repetir luego
 * desde Ajustes.
 */
private val steps = listOf(
    OnboardingStep(
        icon = Icons.Rounded.Lock,
        title = "Tus datos se quedan acá",
        description = "Sin nube ni cuentas en línea: el perfil, los movimientos y " +
            "las facturas viven en este teléfono.",
        bullets = listOf(
            "La app solo se conecta para traer tasas públicas",
            "La contraseña se guarda con hash, nunca en claro",
        ),
    ),
    OnboardingStep(
        icon = Icons.Rounded.Add,
        title = "Registra en segundos",
        description = "El botón + abre el registro rápido: monto, moneda y categoría. " +
            "También puedes fotografiar una factura y dejar que el OCR la lea.",
        bullets = listOf(
            "Deshaz justo después de guardar",
            "Varias facturas por tanda, sin salir de la app",
        ),
    ),
    OnboardingStep(
        icon = Icons.Rounded.CurrencyExchange,
        title = "Dólares y bolívares a la vez",
        description = "Cada movimiento guarda su tasa. Puedes ver el equivalente en las " +
            "dos divisas y elegir la referencia que uses.",
        bullets = listOf(
            "BCV, paralelo o USDT, y tasa manual si la necesitas",
            "La calculadora convierte USD ↔ VES sin crear movimientos",
        ),
    ),
    OnboardingStep(
        icon = Icons.AutoMirrored.Rounded.ReceiptLong,
        title = "Plan y control",
        description = "Reparte el mes en sobres, programa pagos y revisa lo que viene. " +
            "Cuando quieras, saca tus datos en CSV o PDF.",
        bullets = listOf(
            "Recordatorios de vencimientos, incluso sin conexión",
            "Reporte mensual con el estado de tus sobres",
            "Puedes repetir este tutorial desde Ajustes",
        ),
    ),
)

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { steps.size })
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == steps.lastIndex

    fun goTo(page: Int) {
        scope.launch { pagerState.animateScrollToPage(page) }
    }

    BackHandler {
        if (pagerState.currentPage > 0) goTo(pagerState.currentPage - 1) else onFinish()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
        ) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = Branding.APP_NAME,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onFinish) {
                        Text("Saltar")
                    }
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f).padding(bottom = 140.dp),
                ) { page ->
                    StepContent(step = steps[page], index = page, total = steps.size)
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(top = 8.dp, bottom = 16.dp),
            ) {
                PageDots(count = steps.size, current = pagerState.currentPage)

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (lastPage) onFinish() else goTo(pagerState.currentPage + 1)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text(if (lastPage) "Empezar" else "Siguiente")
                }
            }
        }
    }
}

@Composable
private fun StepContent(step: OnboardingStep, index: Int, total: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(96.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(48.dp),
                )
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "PASO ${index + 1} DE $total",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = step.title,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = step.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            step.bullets.forEach { bullet ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = bullet,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val active = index == current
            Surface(
                shape = CircleShape,
                color = if (active) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier
                    .width(if (active) 24.dp else 8.dp)
                    .height(8.dp),
            ) {}
        }
    }
}
