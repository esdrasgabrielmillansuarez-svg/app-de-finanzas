# Finanzas — Android (offline-first)

> **Creado por Esdras Millan.**

App de finanzas personales **bimoneda** para Android, con entrada en menos de 3
segundos y **offline-first**: registrar, consultar y presupuestar funciona sin
red. `INTERNET` se usa solo para traer referencias públicas de cambio (BCV,
paralelo, USDT, EUR, ARS y una tabla global). Kotlin + Compose + Room + Hilt.

- Dirección de diseño: [`docs/diseno/direccion-de-diseno.md`](docs/diseno/direccion-de-diseno.md)
- Prototipo navegable (HTML): [`prototipo/prototipo.html`](prototipo/prototipo.html)

---

## Arrancar

Requisitos: **Android Studio** (Ladybug o superior, trae JDK 17) y Android SDK 35.

1. Abre la carpeta del proyecto en Android Studio.
2. Sync → Run en un dispositivo/emulador **API 26+**.

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest    # importes, divisas, facturas, reporte mensual, auth, CSV
```

> El repo incluye wrapper de Gradle 8.9. En esta máquina el build se hizo con un
> toolchain portable (JDK 17 + SDK 35 + Gradle 8.9) fuera de Android Studio.

## Entrada (perfil local)

- **Crear cuenta / Entrar** con nombre, correo y contraseña.
- La contraseña se guarda como hash **PBKDF2-HMAC-SHA256** (120.000 iteraciones,
  sal aleatoria de 16 bytes por usuario) y se compara en tiempo constante.
- La sesión se recuerda entre arranques; **Cerrar sesión** está en Ajustes.
- Todo es local: no hay servidor, recuperación por correo ni sincronización.
  Si se olvida la contraseña se pierde el acceso al perfil (documentado en la UI).
- **Sin datos de ejemplo**: la app no precarga cuentas, movimientos ni
  categorías. La primera cuenta de una divisa se crea cuando guardas un
  movimiento en esa divisa; las categorías las creas tú desde Registrar o Plan.

## Tutorial de bienvenida

La primera vez que un perfil entra a la app se muestra un tutorial de **cuatro
pasos**, prescindible con **Saltar**:

1. **Privacidad offline**: los datos viven en el teléfono y la contraseña se
   guarda con hash.
2. **Registro rápido y facturas**: el botón `+`, el deshacer y el OCR local.
3. **Bimoneda y calculadora**: referencia del dólar, tasa manual y conversión
   USD ↔ VES sin crear movimientos.
4. **Plan y control**: sobres, pagos, recordatorios y exportación CSV/PDF.

Se marca **por perfil local** (`onboarding_seen_<userId>`), no por teléfono: si
otra persona crea su cuenta, también ve la introducción. No usa cifras ni tasas
de ejemplo y se puede repetir desde **Ajustes → Ayuda → Ver tutorial**.

## Qué funciona hoy

**Hoy**
- Saldo por divisa (local y USD), "te queda este mes", proyección de cierre y
  últimos movimientos.
- **Disponible real**: "te queda este mes" es **ingresos del mes menos todos los
  gastos**, incluso los registrados en otra divisa. El límite de sobres se
  muestra aparte como tope por categoría. Debajo se muestran las equivalencias
  en **USD y VES**, recalculadas cuando cambia la tasa.
- **Sticker contextual**: emoji + frase que cambia según vencimientos, sobres al
  límite, presupuesto y hábito. Botón "Otro" para rotar variantes.
- Pill de tasa → pantalla de divisas.
- Acceso directo a la **calculadora de cambio** con la tasa del día.

**Calculadora de cambio**
- Pantalla de consulta **USD ↔ VES**: se intercambia el sentido con un toque y
  se elige la referencia del dólar (BCV, paralelo o USDT) sin salir.
- Usa la **misma tasa activa** que el resto de la app (manual > en vivo) y
  muestra la fuente y cuándo se actualizó.
- Teclado propio 4×4 con aritmética inline (`12+450`, `1200/2`) y atajos
  (US$ 1, 5, 10, 20, 50, 100 o Bs. 1.000…100.000).
- **No crea movimientos ni guarda nada.** Si falta la tasa, avisa "sin tasa" y
  no inventa una equivalencia.

**Registrar (FAB)**
- Tipo, divisa, categoría, importe con aritmética inline (`12+450`, `1200/2`).
- **Facturas en tanda**: cámara dentro de la app (CameraX, permiso pedido solo
  al abrirla) para disparar varias seguidas sin salir, o **hasta 10 fotos** de
  la galería de una vez. Se leen en cola con ML Kit (una a la vez, para no
  agotar memoria) y caen en una **bandeja de revisión**: importe, comercio,
  divisa y categoría sugeridos, todo editable. El OCR **nunca guarda solo**;
  “Guardar N gastos” crea la tanda completa y el snackbar permite **Deshacer**.
  Las fotos de cámara viven en el caché y se borran al guardar o descartar.
  Cada factura puede **sumar varios montos** (líneas del ticket): el total se
  recalcula en vivo y se guarda como **un solo gasto**.
- Guardado con **Deshacer** en el snackbar.

**Movimientos**
- Búsqueda por comercio, nota o categoría.
- Filtros por tipo (gastos/ingresos) y por divisa.
- Toque en una fila → detalle con **Duplicar** y **Eliminar** (con deshacer).

**Plan**
- **Sobres**: uno por categoría de gasto, con **nombre e ícono propios**, límite
  por **periodo** (mensual, quincenal o semanal) y opción de **arrastrar el
  sobrante**. Los periodos siguen el calendario (1–fin de mes, 1–15 / 16–fin,
  lunes–domingo). Cada gasto se descuenta de su sobre aunque esté en otra divisa:
  se usa la tasa guardada al registrarlo y, si no existe, la cotización actual.
  Cada sobre muestra cuánto queda, su rango actual y, si aplica, cuánto arrastra;
  las equivalencias en USD y VES se recalculan al actualizar la tasa.
- **Pagos**: alta con selector de fecha y recurrencia, marcar pagado (crea el
  movimiento y permite deshacer) y eliminar.

**Ajustes**
- Perfil y cierre de sesión.
- **Apariencia**: modo Sistema/Claro/Oscuro y seis temas (Grafito, Océano,
  Bosque, Atardecer, Lavanda, Monocromo). Se aplican al instante y se recuerdan
  entre arranques.
- **Recordatorios**: aviso diario de vencimientos (WorkManager a las 9:00), con
  interruptor para apagarlo y permiso de notificaciones pedido solo cuando hay
  algo que recordar.
- Divisas base/secundaria y tasa (auto o manual por divisa).
- **Tasas de cambio** (Ajustes): elegir la referencia del bolívar (BCV, paralelo
  o USDT) y fijar a mano la equivalencia de cada referencia, del euro y del peso
  argentino; la manual siempre gana y se puede quitar.
- **Exportar movimientos (CSV)** por el selector del sistema (FileProvider).
- **Generar reporte PDF del mes**: resumen (ingresos, gastos, balance), tabla de
  movimientos y estado de sobres. Cada movimiento usa su tasa histórica; si
  falta una tasa, el importe no se suma y el PDF lo avisa. Se comparte con el
  selector del sistema, sin subir nada.
- **Borrar todos los movimientos** con confirmación.
- **Ayuda → Ver tutorial**: repite el tutorial de bienvenida cuando quieras.
- **Acerca de**: creador y versión visibles.
- Seguridad local: contraseña PBKDF2; sin bloqueo biométrico.

**Voz de la app**
- Mensajes contextuales y deterministas (sin nube): prioridad vencido → vence
  hoy → próximo → sobre excedido → sobre al límite → primer movimiento →
  presupuesto → celebración → aliento.
- Hitos al guardar (1, 5, 10, 25, 50 y 100 movimientos) con emoji, nunca culpa
  ni alarmismo.
- El tono se inspira en las notificaciones de Duolingo: una idea por mensaje,
  concreto, con acción; sin "¡estás fallando!".

**Divisas**
- Catálogo de ~56 divisas con país, bandera y región (incluye USDT para
  Venezuela).
- Equivalencia en **USD** y **EUR** por unidad.
- **Referencias en vivo** del bolívar (VES): BCV oficial, dólar paralelo y USDT
  (estimado con el paralelo). Se actualizan al abrir la app y con el botón
  *Actualizar*, se guardan en caché 6 h y muestran cuándo se obtuvieron.
- **Euro** (tipo del BCE vía Frankfurter), **peso argentino oficial** (dolarapi)
  y **tabla global** (ExchangeRate-API) para el resto del catálogo.
- Elegir base y secundaria; fijar tasa manual por divisa o referencia (siempre
  gana sobre la de internet) o volver a la de referencia.
- Si falla la red, se conserva la última cotización. Si una divisa nunca tuvo
  dato, se muestra **sin cotización** y se puede fijar a mano: no hay una tabla
  de valores aproximados ni información inventada.

## Estructura

```
app/src/main/java/com/suarezmillan/finanzas/
├── FinanzasApp.kt              # @HiltAndroidApp
├── MainActivity.kt             # edge-to-edge + RootGate
├── core/
│   ├── auth/                   # PasswordHasher (PBKDF2), AuthValidation
│   ├── coach/CoachEngine.kt    # mensajes/stickers contextuales (JVM puro)
│   ├── money/Money.kt          # unidades menores + formato es-AR
│   ├── currency/               # catálogo (~56) + tasas + referencias remotas + ExchangeMath
│   ├── receipt/                # ReceiptParser (heurística) + ReceiptScanner (ML Kit)
│   ├── export/                 # CsvExporter + shareCsv + PDF mensual (FileProvider)
│   ├── report/                 # MonthlyReport: resumen y filas del mes (JVM puro)
│   ├── reminder/               # WorkManager diario + notificación de vencimientos
│   ├── fx/Fx.kt                # FxRate, FxSource, conversión
│   └── settings/               # AppSettings + PreferenceStore (JVM testeable)
├── data/
│   ├── local/
│   │   ├── FinanzasDatabase.kt # v3
│   │   ├── Migrations.kt       # 1 → 2 (users), 2 → 3 (limpia datos de ejemplo)
│   │   ├── Converters.kt
│   │   ├── entity/             # Account, Category, Txn, Envelope, ScheduledPayment, Debt, User
│   │   ├── dao/                # un DAO por agregado
│   │   └── model/EnvelopeProgress.kt
│   └── repo/                   # Txn, Account, Budget, Category, ScheduledPayment, Auth
├── di/                         # Hilt: base de datos y bindings
└── ui/
    ├── RootGate.kt             # splash / login / tutorial / app
    ├── auth/                   # AuthViewModel + AuthScreen
    ├── onboarding/             # tutorial de bienvenida (4 pasos, por perfil)
    ├── FinanzasRoot.kt         # Scaffold, tabs, FAB, hoja de registro, diálogo de detalle
    ├── MainViewModel.kt        # estado único del MVP
    ├── home/                   # Hoy + StickerCard
    ├── cambio/                 # calculadora de cambio USD ↔ VES
    ├── movs/ plan/ ajustes/ divisas/
    ├── quick/                  # hoja de registro + AmountParser
    ├── components/             # MoneyText, Keypad, ConfirmDialog
    └── theme/                  # Color, Type, Shape, Motion, Theme
```

## Decisiones que no conviene deshacer

| Decisión | Por qué |
|---|---|
| Importes en `Long` (unidades menores) + código ISO | `Double` pierde centavos y mezcla divisas. |
| Tasa de cambio **por transacción** (`fxRateToBase`, `fxSource`, `fxTimestamp`) | Cambiar la tasa de hoy no debe reescribir el pasado. |
| Una cuenta = una divisa | El saldo deja de ser una suma de tasas distintas. |
| Importes firmados (`gasto` negativo, `ingreso` positivo) | Un `SUM` cuadra cualquier cuenta, incluidas transferencias. |
| Transferencias = dos filas unidas por `transferGroupId` | Cada cuenta cuadra por separado; conversión explícita. |
| `INTERNET` solo para referencias públicas de cambio | El resto es offline de verdad: movimientos, facturas y perfil no salen del teléfono. |
| Sin Material You dinámico | El color codifica divisa; el wallpaper no puede decidirlo. |
| `MoneyText` como único formateador | Importes con cifras tabulares y formato es-AR en un solo lugar. |
| Migraciones Room escritas a mano | Los movimientos del usuario no son recuperables. |
| PBKDF2 con sal y comparación en tiempo constante | Nunca se guarda la contraseña; una filtración de la DB no la revela. |
| OCR local (ML Kit empaquetado) + parser propio | Sin subir facturas a ningún lado y con lógica testeable en JVM. |
| Cámara propia (CameraX) en vez de la app del sistema | Para cargar varias facturas seguidas sin salir y volver; la foto queda en el caché de la app. |
| OCR en cola de a una foto | Diez bitmaps a la vez en un teléfono modesto terminan en `OutOfMemoryError`. |
| Bandeja de facturas editable antes de guardar | El OCR propone; el usuario confirma. Nunca se crea un movimiento sin revisión. |
| Desglose por factura que suma al total | Un ticket con varios conceptos se guarda como un solo gasto; se evita duplicar movimientos. |
| PDF con `PdfDocument`, sin librerías nuevas | Genera el reporte offline y el APK no crece con un motor de PDF. |
| El reporte usa la tasa histórica de cada fila | El PDF cuenta lo que pasó, no lo que vale hoy la divisa. |
| El tema cambia **acento y superficies**, nunca el significado | USD sigue ámbar y la moneda local sigue cian: el color no deja de informar. |
| Mensajes del coach deterministas y contextuales | Sin nube, sin IA impredecible y testeables; prioridad explícita por reglas. |
| Recordatorios con WorkManager, no `AlarmManager` | No hace falta precisión al minuto ni permisos de alarmas exactas. |
| Sin cuentas, categorías ni tasas de ejemplo | La app muestra solo datos del usuario o referencias reales; si falta una tasa, dice "sin cotización". |
| Cuenta creada al primer movimiento de cada divisa | Se evita pedir una configuración inicial y nunca se inventa un saldo o una cuenta. |
| Calculadora de cambio separada del registro | Consultar una equivalencia no debe crear movimientos ni ensuciar el historial. |
| `ExchangeMath` puro y tasa activa de la app | La conversión se prueba en JVM; la calculadora nunca inventa una tasa ni una equivalencia. |
| Tutorial de bienvenida por `userId`, no global | Cada perfil local merece su propia introducción; saltarlo no se lo quita a otra cuenta. |

## Tests

```bash
./gradlew :app:testDebugUnitTest
```

- `AmountParserTest` — aritmética inline y decimales.
- `CurrencyRatesTest` — paridad vía USD, referencias BCV/paralelo/USDT, EUR/ARS
  en vivo, ausencia de tasas inventadas y formato de tasas.
- `ExchangeMathTest` — conversión USD ↔ VES, redondeo al centavo y ausencia de
  equivalencia cuando falta el monto o la tasa.
- `ReceiptParserTest` — extracción de importe/comercio/categoría en tickets.
- `ReceiptBatchTest` — bandeja de facturas: divisa/fecha aceptadas, categoría
  solo si coincide con una del usuario, desglose que suma al total y qué se
  guarda al confirmar.
- `MonthlyReportTest` — totales del mes, traspasos fuera de ingresos/gastos,
  tasa histórica, falta de tasa y estado de sobres.
- `PasswordHasherTest` — hash/verificación, sal distinta, no reversible.
- `AuthValidationTest` — nombre, correo normalizado, contraseña mínima.
- `CsvExporterTest` — cabecera, decimales y escapado RFC 4180.
- `CoachEngineTest` — prioridad de vencimientos, sobres, hitos y rotación de variantes.
- `BcvRateParserTest` — parser del BCV/paralelo, rechazo de fuente no esperada y caché de 6 h.
- `MarketRateParsersTest` — euro de Frankfurter, ARS oficial y tabla global.
- `LiveRateSettingsTest` — referencias, elección, base/secundaria y overrides
  sobreviven al reinicio.
- `OnboardingSettingsTest` — el tutorial se recuerda por perfil y sobrevive al reinicio.
- `EnvelopePeriodTest` — rangos mensuales, quincenales y semanales por calendario.
- `EnvelopeCarryTest` — arrastre solo desde periodos completos, acumulación y
  ausencia de deuda negativa.
- `HomeUiStateBudgetTest` — disponible como ingresos menos gastos, sin sumar
  límites de sobres.

**120 tests, 0 fallos** en `:app:testDebugUnitTest`.

## Qué falta (a propósito)

- Otras cotizaciones (MEP/CCL) y actualización en segundo plano de la tasa.
- Edición de movimientos ya guardados (hoy: duplicar/eliminar).
- CRUD de cuentas y renombrar la cuenta automática de cada divisa.
- Deudas: entidad y DAO listos, **sin UI**.
- SQLCipher + clave en Android Keystore (no incluido).
- Bloqueo biométrico (no incluido; `androidx.biometric` está en dependencias).
- DataStore para la sesión (apariencia, recordatorios, divisas y tasas ya
  persisten en SharedPreferences).
- Edición de los textos del coach desde la UI (hoy son constantes testeables).
- Widget Glance + deep link `finanzas://registrar` (el manifiesto ya lo declara).
- Backup propio cifrado (Auto Backup está **desactivado** para la base).

## Versiones

Versión de la app: **0.10.0** (`versionCode 9`).

Fijadas a un set conservador y mutuamente compatible: Kotlin 2.0.21, AGP 8.7.3,
Gradle 8.9, Compose BOM 2024.10.01, Room 2.6.1, Hilt 2.52, KSP 2.0.21-1.0.28,
ML Kit Text Recognition 16.0.1, CameraX 1.4.2 y WorkManager 2.9.1.

Actualización recomendada cuando se quiera tocar el tema:

- `material3` 1.4.0 (estable) y cambiar `MaterialTheme` por
  `MaterialExpressiveTheme(motionScheme = MotionScheme.expressive())` en
  `ui/theme/Theme.kt`.
- AGP / Kotlin / KSP / Hilt a sus últimas estables (subir en bloque y volver a
  correr `./gradlew :app:assembleDebug`).
