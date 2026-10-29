# Dirección de diseño — App de Finanzas

> Documento de diseño para el MVP (Fase 1 del roadmap). Complementa el brief de producto.
> Prototipo navegable: `prototipo/prototipo.html`.

---

## 1. La tesis

El brief dice "cero fricción". Eso no es una feature, es una **restricción de diseño** que ordena todo lo demás:

> **La app es un instrumento de registro. El dashboard es un subproducto.**

La mayoría de apps de finanzas fallan porque construyen un panel de control (6 tarjetas, 2 gráficos, un anillo) y esconden el botón de registrar. Aquí es al revés: registrar es la acción primaria, siempre a un toque, y el resumen existe para responder tres preguntas y nada más:

1. ¿Cuánto tengo?
2. ¿Cuánto puedo gastar aún este mes?
3. ¿Qué se viene?

Si una pantalla no responde a una de esas tres, no entra en el MVP.

### Criterio de aceptación del diseño (medible)

| Métrica | Objetivo |
|---|---|
| Tiempo para registrar un gasto típico (p95) | **< 3 s** desde pantalla apagada, con widget |
| Toques para guardar | **≤ 4** (monto → categoría → guardar) |
| Scroll necesario para guardar | **0** |
| Zona táctil mínima | 48 dp |
| Contraste texto | WCAG AA (4.5:1), AAA en montos |
| Alcance con una mano | Todo lo interactivo en el 45% inferior de la pantalla |

---

## 2. Benchmark: qué copiar y qué no

Referentes revisados: **Ivy Wallet** (open source, Kotlin + Compose + M3, el mejor ejemplo de "registro rápido" en Android), **Wallet by BudgetBakers** (multimoneda y cuentas), **Money Manager** (clásico, densidad alta), **YNAB / Copilot Money** (envelope budgeting y tono), **Monzo / Revolut** (claridad de marca y jerarquía tipográfica).

### Copiar

| Patrón | De quién | Por qué |
|---|---|---|
| Teclado numérico propio con aritmética inline (`12+8` → 20) | Ivy Wallet | Elimina la calculadora externa; es *el* atajo real de velocidad |
| Monto como protagonista tipográfico (40sp+, tabular) | Monzo | El número es la interfaz; todo lo demás orbita |
| Chips de categoría en una sola fila horizontal con la predicha resaltada | Ivy Wallet | 1 toque en vez de abrir un selector |
| Cuentas agrupadas con saldo por divisa, no sumadas a la fuerza | Wallet | Evita el "total mentiroso" con bimoneda |
| Sobres con "queda X" en vez de "gastaste Y" | YNAB | Encuadre accionable: gastaste es pasado, queda es decisión |
| Estados de pago por color **+ icono + texto** | Wallet | Nunca color solo (accesibilidad + daltonismo) |
| Animación de conteo del saldo (400 ms) al abrir | Copilot | Comunica "esto está vivo", sin costar un toque |
| Undo de 5 s tras guardar, en vez de diálogo de confirmación | Gmail/Monzo | Confirmar antes es fricción; deshacer después es red de seguridad |

### Evitar (anti-patrones deliberados)

- **Seis tarjetas de KPI.** Nadie decide nada con "gasto medio diario".
- **Pie chart de 12 categorías.** Ilegible e inútil. Top 5 + "Otros" en barras horizontales.
- **Colores aleatorios por categoría.** 12 tonos indistinguibles. Categoría = icono + nombre; el color se reserva para **estado** (presupuesto, vencimiento) y **divisa**.
- **Modales sobre modales.** Registrar es *una* sheet, con todo dentro.
- **Teclado del sistema para montos.** Siempre teclado propio, siempre en la mitad inferior.
- **Confeti al pagar una factura.** Gamificación solo en metas de ahorro, nunca en obligaciones.
- **Onboarding de 7 pantallas.** 2 pantallas: crear cuenta + divisa secundaria. Se puede saltar.

---

## 3. Identidad visual

### Concepto: **Grafito + dos tintas**

Base neutra oscura (los números brillan, no los adornos) y **el color identifica la divisa**, no decora. Es la decisión que resuelve el problema bimoneda a nivel visual: en cualquier lista, sabes en qué moneda estás sin leer el símbolo.

**Regla de un solo eje:** el color tiene dos usos y nunca se mezclan.
- **Identidad** → divisa (punto/rail de color).
- **Semántica** → dirección del flujo (+/−). Reservado, desaturado en dark.

| Rol | Dark | Light | Uso |
|---|---|---|---|
| Fondo | `#0E1116` | `#F7F8FA` | Lienzo |
| Superficie | `#161B22` | `#FFFFFF` | Tarjetas |
| Superficie alta | `#1E252E` | `#EEF1F5` | Sheets, inputs |
| Texto primario | `#F2F5F8` | `#0E1116` | Montos |
| Texto secundario | `#93A0AE` | `#5B6673` | Labels |
| **Divisa local** | `#4CC9F0` | `#0E86A8` | Punto, rail, chip activo |
| **Divisa USD** | `#FFB020` | `#A86A00` | Punto, rail, chip activo |
| Ingreso | `#34D399` | `#0B7A55` | Signo `+` |
| Gasto | `#FF7A6B` | `#B23A2C` | Signo `−` |
| Alerta (80% sobre) | `#FFB020` | `#A86A00` | Barra de progreso |
| Peligro (vencido) | `#FF5A5A` | `#C02B2B` | Badge de vencido |

> Nota: en dark, el verde/rojo se usan **solo en el signo y el monto**, nunca como fondo de tarjeta. Fondos saturados en listas = ruido.

### Tipografía

- **UI:** Inter (o `system-ui` en el prototipo). Pesos 400/500/600/700.
- **Dinero:** misma familia con **cifras tabulares** (`font-variant-numeric: tabular-nums`) y cero rayado. Sin esto, los montos "bailan" al actualizarse y el efecto es de juguete.
- Escala:

| Token | Tamaño / interlínea | Uso |
|---|---|---|
| `displayL` | 40 / 44, w700, tabular | Saldo consolidado |
| `amountL` | 32 / 36, w700, tabular | Monto en la sheet de registro |
| `titleM` | 20 / 28, w600 | Títulos de sección |
| `bodyM` | 15 / 22, w400 | Texto, filas de lista |
| `labelS` | 12 / 16, w600, tracking +0.08em, MAYÚSCULAS | Cabeceras de sección, metadatos |

### Forma y movimiento

- Radios: **28 dp** tarjetas hero, **20 dp** filas/inputs, **pill** en chips y botón guardar, **24 dp** teclas del keypad.
- Formas "cookie" de M3 Expressive: reservadas a estados vacíos y celebración de metas. Nunca en elementos funcionales.
- Movimiento: springs (`dampingRatio 0.8`, `stiffness 400`) entre 180–260 ms. El sheet entra desde abajo con el teclado ya visible. Teclas con `HapticFeedbackType.SegmentTick`.
- Un solo momento "wow": el **count-up del saldo** al abrir Hoy. Todo lo demás es sobrio.

### Iconografía

Material Symbols Rounded, 24 dp, peso 400. Categorías siempre icono + texto (nunca icono solo en formularios).

### Temas y voz

**Temas.** Seis presets (Grafito, Océano, Bosque, Atardecer, Lavanda, Monocromo)
y modo Sistema/Claro/Oscuro. El preset toca **solo** `primary`, `onPrimary`,
`primaryContainer` y `onPrimaryContainer`; las superficies, la semántica de flujo
y los colores de divisa quedan fijos. Regla: *el tema es la marca, el color de
divisa es información*.

**Stickers.** Emoji grande en un círculo rotado −8° con sombra, frase de una idea
y una acción concreta. Cambian con Crossfade solo cuando cambia el mensaje.
El motor (`core/coach/CoachEngine.kt`) prioriza:

1. Pago vencido / vence hoy → aviso con acción "Ver plan".
2. Pago en los próximos 3 días.
3. Sobre excedido → aviso sin culpa ("no es el fin del mundo").
4. Sobre al 80–100 %.
5. Sin movimientos → invitar a anotar el primero.
6. Presupuesto mensual excedido.
7. Mes ganador cerca del cierre (celebración).
8. Sprint final ajustado (≥ 85 %).
9. Aliento general rotativo.

Anti-patrones: culpa, alarmismo, más de una idea por mensaje, repetir el genérico
cuando hay un dato concreto. Los textos son constantes testeables en JVM, no
generados en la nube.

---

## 4. Arquitectura de información

Cuatro destinos + una acción global.

```
[ Hoy ]  [ Movimientos ]  ( + )  [ Plan ]  [ Ajustes ]
                              │
                              └─ Long-press → plantillas de 1 toque
```

- **Hoy** — saldo consolidado, "te queda este mes", proyección, últimos movimientos.
- **Movimientos** — lista agrupada por día, búsqueda, filtro por divisa/cuenta.
- **Plan** — sobres (presupuestos), calendario de pagos, deudas por cobrar/pagar.
- **Ajustes** — cuentas, divisas y tasa, categorías, seguridad, exportación.
- **`+`** — acción global, presente en las 4 pantallas. No es una pestaña.

> **Por qué no 5 pestañas:** "Registrar" no es un destino, es una acción. Como pestaña, obliga a un toque extra de vuelta y rompe el gesto de "abrir → monto".

---

## 5. Especificación de pantallas (MVP)

### 5.1 Hoy

De arriba a abajo, en este orden exacto (el orden *es* la jerarquía):

1. **Saldo consolidado.** Monto en `displayL` + chip de tasa (`1 US$ = 4.120,50 · hace 2 h ⟳`). Si hay tasa manual, badge `fija` y punto ámbar.
   - Sub-línea: `US$ 1.020,45` (la otra divisa, nunca mezclada en el número grande).
2. **"Te queda este mes"** — el dato más accionable. **Ingresos del mes menos
   todos los gastos** convertidos a la base, con barra y equivalencia en USD/VES
   que se recalcula con la tasa. El **límite de sobres** se muestra aparte como
   tope por categoría; no se suma al disponible. Tocar → Plan.
3. **Proyección de cierre** — `≈ $ 152.000 al 31 oct`, con micro-explicación (`3 pagos fijos, 1 ingreso esperado`). Sin esto, es un número mágico y se ignora.
4. **Últimos 5 movimientos** — fila: icono categoría · nombre · cuenta/divisa · monto. Swipe izquierda = editar, derecha = duplicar.
5. FAB central `+`.

### 5.2 Registrar (sheet)

La pantalla más importante del producto. Una sola sheet, sin pasos.

```
┌──────────────────────────────────────┐
│  ────                                │  drag handle
│  Gasto ▸ Ingreso   Transferencia     │  segmented, default = último usado
│                                      │
│            $ 12.450,00               │  amountL, tabular, cursor parpadeante
│         = 12+450 (expr)              │  aritmética inline
│                                      │
│  [● local]  [● USD]                  │  toggle divisa (punto de color)
│  ─────────────────────────────────   │
│  🛒 Tus categorías...   ＋ Categoría │  chips del usuario, sin datos de ejemplo
│                                      │
│  ─────────────────────────────────   │
│  Cuenta VES · hoy                    │  se crea al primer movimiento
│                                      │
│  [        Guardar        ]           │  pill, 56 dp, ancho completo
│  1    2    3    ⌫                     │
│  4    5    6    +                     │  keypad 4×4, mitad inferior
│  7    8    9    −                     │
│  ,    0    ×    ÷                     │
└──────────────────────────────────────┘
```

Reglas:
- El teclado está **visible desde el primer frame**. Sin teclado no hay "3 segundos".
- Monto con **aritmética inline**: `+`, `−`, `×`, `÷` y `=` resuelven sin salir.
- La categoría predicha (regla por nombre de comercio, luego historial, luego "Otros") viene **pre-seleccionada**; el usuario solo la cambia si quiere.
- Fecha por defecto = hoy, hora actual. Cambiarla es 1 toque, no un campo obligatorio.
- Guardar → sheet se cierra → **snackbar con `Deshacer` (5 s)**. Nunca diálogo de confirmación.
- Si no hay monto, el botón está deshabilitado (no muestra error; no hay nada que corregir todavía).

### 5.3 Plan → Sobres

- Una tarjeta por sobre: icono, nombre, `queda $ X de $ Y`, barra y equivalencia
  en USD/VES cuando la divisa del sobre no es una de ellas.
- El gasto del sobre incluye movimientos de otras divisas: se convierte con la
  tasa histórica del movimiento o, si falta, con la cotización actual.
- La barra cambia a ámbar al 80% y a rojo al 100%, **y añade texto** (`te pasaste $ 12.000`) para no depender del color.
- Acción primaria: **mover dinero entre sobres** (cubrir un sobre desde otro), no "editar presupuesto". Cubrir es lo que la gente hace de verdad.
- Reordenable por drag; los 3 sobres más usados suben solos.

### 5.4 Plan → Calendario de pagos

- Mes en grid, día con hasta 3 puntos (color + forma por estado: pagado = relleno, próximo = anillo, vencido = triángulo).
- Debajo, lista de **próximos 7 días** con monto y cuenta. El calendario es contexto; la lista es la acción.
- Tocar un pago → `Pagar` (registra transacción y marca el pago) o `Posponer`.

### 5.5 Plan → Deudas

- Dos tabs: **Me deben** / **Debo**.
- Por deuda: contraparte, monto original, `pagado $ X de $ Y`, fecha límite, barra de abonos.
- `Registrar abono` abre la misma sheet de registro con monto prellenado y tipo `Transferencia`.

### 5.6 Movimientos

- Agrupado por día con subtotal del día en `labelS`.
- Búsqueda por texto + filtros (divisa, cuenta, categoría) como chips, no como menú.
- Fila: icono · comercio/nota · hora · cuenta · monto (con signo, tabular).

---

## 6. Bimoneda: las cinco reglas que evitan el desastre

Este es el punto donde la mayoría de apps mienten. Reglas de diseño:

1. **Un movimiento se guarda en su divisa original.** Nunca se convierte "de ida". Se persiste `monto` + `divisa` + `tasaAplicada` + `fuente` + `timestamp`.
2. **Sumar convirtiendo a la divisa base, nunca mezclando unidades.** El total se
   etiqueta como convertido, usa la tasa histórica del movimiento cuando la base
   guardada coincide y, si falta una cotización, excluye el movimiento y lo avisa.
   El chip de tasa siempre está visible y es tocable.
3. **`≈` obligatorio** en cualquier cifra convertida. La ausencia de `≈` significa "esto es exacto".
4. **Tasa manual = badge permanente** (`fija`, punto ámbar). Una tasa fija vieja es una decisión del usuario; debe verse.
5. **Cambiar la tasa no reescribe el pasado.** Recalcula solo las vistas agregadas; el histórico conserva la tasa con la que se registró.

**Implicación de modelo de datos** (entra en Sprint 1, no después):

```kotlin
// Montos en unidades menores (Long), nunca Double.
data class Txn(
  val amountMinor: Long,        // 1245000 = $12.450,00
  val currency: String,         // "ARS", "USD"
  val fxRateToBase: Double?,    // null si currency == base
  val fxBaseCurrency: String?,  // divisa base con la que se guardó esa tasa
  val fxSource: FxSource,       // NONE | AUTO | MANUAL | FIXED
  val fxTimestamp: Long?,
  val occurredAt: Long,
)
```

Si se guarda `Double` para dinero, el bug aparece en la Fase 3 y no se puede arreglar sin migración.

---

## 7. Accesibilidad y una mano

- Todo lo interactivo en el 45% inferior; el saldo arriba puede ser solo lectura.
- Targets ≥ 48 dp; el keypad usa 50 dp de alto con separación 8 dp (evita toques vecinos).
- Estados nunca solo por color: `pagado` lleva check, `vencido` lleva triángulo y texto.
- Escalado de fuente hasta 200%: la sheet de registro se vuelve scrollable **solo** si el sistema tiene fuente grande; el teclado y Guardar siguen fijos abajo.
- TalkBack: cada fila de movimiento se anuncia como `Gasto, Súper, hoy, 12.450 pesos`; el saldo como `Saldo total, 1.284.500 pesos, equivalente a 1.020 dólares`.
- Reducción de movimiento: si el sistema pide `removeAnimations`, el count-up se reemplaza por aparición directa.

---

## 8. Mapeo a Compose (Material 3 Expressive)

`androidx.compose.material3:material3:1.4.0` (estable, oct-2026) ya incluye el tema Expressive y `MotionScheme`.

```kotlin
@Composable
fun FinanzasTheme(dark: Boolean, content: @Composable () -> Unit) {
  val scheme = if (dark) DarkGrafito else LightGrafito   // colorScheme de arriba
  MaterialExpressiveTheme(
    colorScheme = scheme,
    motionScheme = MotionScheme.expressive(),
    typography = FinTypography,   // con tabular-nums en los estilos de dinero
    shapes = FinShapes,           // 28 / 20 / pill / 24
    content = content,
  )
}

/** Monto con cifras tabulares: sin esto los números "bailan". */
@Composable
fun MoneyText(amountMinor: Long, currency: String, style: TextStyle) {
  Text(
    text = formatMoney(amountMinor, currency),
    style = style.copy(fontFeatureSettings = "tnum, zero"),
    maxLines = 1,
  )
}
```

Piezas clave y su equivalente:

| Diseño | Compose |
|---|---|
| Sheet de registro | `ModalBottomSheet(skipPartiallyExpanded = true)` + `WindowInsets.ime` |
| FAB central anclado | `Scaffold(floatingActionButtonPosition = Center)` + `LargeFloatingActionButton` |
| Teclado | 4 filas de `Row` con `Surface(onClick)`, 50 dp (el sheet no scrollea, así que no `LazyVerticalGrid`); `HapticFeedback` pendiente |
| Chips de categoría | `LazyRow` de `FilterChip` (predicha = `selected`) |
| Barra de sobre | `LinearProgressIndicator` + `animateFloatAsState` |
| Count-up del saldo | `animateLongAsState` (spring) o `Animatable` 400 ms |
| Snackbar deshacer | `SnackbarHost` con `duration = Short`, acción `Deshacer` |
| Widget quick-record | `Glance` (Fase 3): 3 botones → `PendingIntent` al deep link `finanzas://registrar?tipo=gasto` |
| Bloqueo biométrico | `androidx.biometric` en `MainActivity` + `FLAG_SECURE` |

---

## 9. Qué NO construir en el MVP (aunque sea tentador)

- Gráficos de tendencia. No responden a las tres preguntas.
- Multi-cuenta con transferencias complejas. Una cuenta por divisa basta para validar.
- Sincronización en la nube. Offline-first es la promesa; sync es Fase 3.
- Lector de SMS. Permiso sensible + Play Store scrutiny; primero valida que el registro manual se usa.
- Temas dinámicos (Material You). La identidad de color *es* el diferencial (divisa = color); dejarla al wallpaper lo destruye. Ofrecer solo claro/oscuro.

---

## 10. Siguiente paso sugerido

1. Validar esta dirección (colores, jerarquía de Hoy, sheet de registro) sobre el prototipo.
2. Congelar tokens (`Color.kt`, `Type.kt`, `Shape.kt`, `Motion.kt`) antes de escribir pantallas.
3. Sprint 1: esquema Room con `amountMinor` + `fxRateToBase` + `fxSource`, y la sheet de registro con datos falsos. Sin dashboard.

---

## 11. Estado de implementación

El esqueleto ya está en la raíz del repo: `app/` con Kotlin + Compose + Room + Hilt.
Instrucciones de arranque y deuda técnica en [`README.md`](../../README.md).

Alineado con este documento:

- Tokens en `ui/theme/` (color, tipografía, forma, movimiento) con el concepto **Grafito + dos tintas**.
- **Entrada con perfil local**: registro/login con nombre, correo y contraseña
  (PBKDF2-HMAC-SHA256, 120.000 iteraciones, sal por usuario), sesión recordada y
  salida desde Ajustes. Sin servidor; `INTERNET` se usa solo para referencias
  públicas de cambio.
- Hoja de registro con teclado 4×4, aritmética inline (`12+450`), chips de categoría, divisa por chip y deshacer.
- Hoy (saldo por divisa, "te queda este mes", proyección estimada), Movimientos con búsqueda y filtros de tipo/divisa, Plan (sobres + pagos programados) y Ajustes.
- Detalle de movimiento (duplicar / eliminar con deshacer) desde Hoy y Movimientos.
- Divisas: catálogo de ~56 monedas agrupadas por país/región, equivalencia en USD y EUR, base/secundaria y tasa manual por divisa.
- **Referencias en vivo**: BCV oficial, dólar paralelo y USDT (estimado con el paralelo) para el bolívar; euro del BCE y peso argentino oficial. Cacheadas con fecha visible y override manual por referencia en Ajustes → Tasas de cambio.
- Facturas: OCR en el dispositivo (ML Kit, modelo empaquetado) + parser determinista que precarga importe, comercio, categoría y divisa.
- Exportación CSV de movimientos vía FileProvider (el usuario elige el destino) y borrado masivo con confirmación.
- **Apariencia**: modo claro/oscuro/sistema y seis temas con selector en vivo; el acento no altera la semántica de divisa. Persistido en `PreferenceStore`.
- **Sticker contextual** en Hoy (`CoachEngine` + `StickerCard`) con rotación de variantes y mensajes de hito al guardar.
- **Recordatorios** diarios de vencimientos con WorkManager + notificación resumen, permiso de notificaciones pedido solo cuando corresponde e interruptor en Ajustes.
- Room v2 con siete entidades y migración `1 → 2` escrita a mano (nunca destructiva), tasa de cambio guardada **por transacción**.

Deliberadamente pendiente: count-up animado del saldo, hápticos del teclado, bloqueo
biométrico, SQLCipher, widget Glance, edición de movimientos ya guardados (solo
duplicar/eliminar), CRUD de deudas, DataStore y backup cifrado.
